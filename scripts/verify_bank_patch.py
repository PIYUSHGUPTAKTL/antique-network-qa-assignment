"""Local bank boundary regressions: real requests, owned synthetic fixtures only."""
import argparse, json, urllib.request, urllib.error
from pathlib import Path
from reproduce_api_defects import Bank

def run(base):
    from urllib.parse import urlparse
    target=urlparse(base)
    if target.scheme!='http' or target.hostname not in {'localhost','127.0.0.1','::1'}:
        raise ValueError('This regression probe is restricted to localhost')
    a=Bank(base); aid,aa=a.customer(); b=Bank(base);bid,ba=b.customer()
    b.request('POST','deposit',{'accountId':ba,'amount':'10.00'})
    tx=json.loads(b.request('GET',f'accounts/{ba}/transactions')['body'])[0]['id']
    checks=[]
    def request(opener,path,method='GET',params=None,headers=None,body=None):
        url=base.rstrip('/')+path
        if params:
            from urllib.parse import urlencode
            url+='?'+urlencode(params)
        req=urllib.request.Request(url,method=method,data=body,headers=headers or {'Accept':'application/json'})
        try:
            with opener.open(req,timeout=20) as r:
                return {'status':r.status,'body':r.read().decode(),'content_type':r.headers.get('Content-Type','')}
        except urllib.error.HTTPError as e:
            return {'status':e.code,'body':e.read().decode(),'content_type':e.headers.get('Content-Type','')}
    def check(name,r,passed):checks.append({'name':name,'passed':bool(passed),'response':r})
    for family in ['services','services_proxy']:
        for suffix in [f'accounts/{ba}',f'accounts/{ba}/transactions',f'accounts/{ba}/transactions/amount/10.00',f'transactions/{tx}',f'customers/{bid}/accounts']:
            r=request(a.http,f'/{family}/bank/{suffix}')
            check(f'{family}: foreign {suffix}',r,r['status']==403)
        for encoded in [f'+{ba}',f'%2b{ba}',f'%{ord(str(ba)[0]):02x}{str(ba)[1:]}']:
            r=request(a.http,f'/{family}/bank/accounts/{encoded}')
            check(f'{family}: encoded foreign identifier {encoded}',r,r['status']==403)
        before=b.balance(ba)
        r=request(a.http,f'/{family}/bank/transfer','POST',{'fromAccountId':ba,'toAccountId':aa,'amount':'1.00'})
        check(f'{family}: foreign debit rejected unchanged',r,r['status']==403 and b.balance(ba)==before)
        r=request(a.http,f'/{family}/bank/accounts/{aa}')
        check(f'{family}: own account works',r,r['status']==200 and json.loads(r['body'])['id']==aa)
        r=request(urllib.request.build_opener(),f'/{family}/bank/accounts/{aa}',headers={'Authorization':'Bearer invalid','Accept':'application/json'})
        check(f'{family}: unsupported authentication rejected',r,r['status']==401)
    for amount in ['abc','0','-1.00','0.001']:
        before=a.balance(aa);ledger=a.request('GET',f'accounts/{aa}/transactions')['body']
        r=a.request('POST','deposit',{'accountId':aa,'amount':amount})
        check(f'invalid deposit {amount} rejected unchanged',r,r['status']==400 and bool(r['body']) and 'text/plain' in r['content_type'] and a.balance(aa)==before and a.request('GET',f'accounts/{aa}/transactions')['body']==ledger)
    def soap(account):
        return ('<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" xmlns:p="http://service.parabank.parasoft.com/"><s:Body><p:getTransactions><p:accountId>'+str(account)+'</p:accountId></p:getTransactions></s:Body></s:Envelope>').encode()
    c=Bank(base);_,ca=c.customer()
    for owner,expected in [(ba,403),(ca,200)]:
        r=request(c.http,'/services/ParaBank','POST',headers={'Content-Type':'text/xml'},body=soap(owner))
        check('SOAP foreign denied' if owner==ba else 'SOAP own allowed',r,r['status']==expected)
    r=c.request('GET',f'accounts/{ca}',accept='application/xml')
    import xml.etree.ElementTree as ET
    check('Account XML root qualified',r,r['status']==200 and ET.fromstring(r['body']).tag=='{http://service.parabank.parasoft.com/}account')
    r=request(c.http,'/services/ParaBank?wsdl')
    check('SOAP WSDL metadata accessible',r,r['status']==200 and 'definitions' in r['body'])
    try:
        for mode in ['RESTJSON','RESTXML','SOAP']:
            r=c.request('POST','setParameter/accessmode/'+mode)
            if r['status']!=204:raise RuntimeError('Cannot set local access mode')
            r=request(c.http,f'/services_proxy/bank/accounts/{ca}')
            check('Optional '+mode+' own account works',r,r['status']==200 and json.loads(r['body'])['id']==ca)
    finally:c.request('POST','setParameter/accessmode/jdbc')
    return {'base':base,'checks':checks,'passed':sum(c['passed'] for c in checks),'total':len(checks)}

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--base',default='http://localhost:8081/parabank');parser.add_argument('--output',required=True);args=parser.parse_args()
    result=run(args.base);Path(args.output).write_text(json.dumps(result,indent=2)+'\n')
    for c in result['checks']:print(('PASS' if c['passed'] else 'FAIL')+' '+c['name'])
    raise SystemExit(0 if result['passed']==result['total'] else 1)
