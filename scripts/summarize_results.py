"""Aggregate real Surefire XML. Skips and absent runs cannot inflate success."""
import argparse,json,xml.etree.ElementTree as ET
from pathlib import Path
def summarize(root,expected_runs=6):
 files=[p for p in Path(root).rglob('TEST*.xml') if p.name.startswith('TEST') and 'junitreports' not in p.parts];counts={'tests':0,'failures':0,'errors':0,'skipped':0};runs=set()
 for p in files:
  suite=ET.parse(p).getroot();runs.add(str(p.parent))
  for key in counts:counts[key]+=int(suite.get(key,'0'))
 passed=max(0,counts['tests']-counts['failures']-counts['errors']-counts['skipped']);rate=100*passed/counts['tests'] if counts['tests'] else 0.0
 missing=max(0,expected_runs-len(runs));matrix_issues=[]
 if expected_runs==6:
  required={'requiredOptionsAndQuantity','couponAndVoucher','orderLifecycleAndHistory','returnStatusAndIsolation','stockAndCancellation','paginatedSearchFilterSort','localizationAndCurrency'}
  expected={f'{browser}-{attempt}' for browser in ['chrome','firefox'] for attempt in [1,2,3]}
  observed={}
  for p in files:
   relative=p.relative_to(Path(root));attempt=relative.parts[0]
   observed.setdefault(attempt,[]).extend((c.get('classname'),c.get('name')) for c in ET.parse(p).getroot().iter('testcase'))
  if set(observed)!=expected:matrix_issues.append('Expected exactly Chrome/Firefox attempts 1, 2 and 3')
  for attempt,cases in observed.items():
   wanted={('qa.ui.ScenarioTests',name) for name in required}
   if len(cases)!=7 or set(cases)!=wanted:matrix_issues.append(f'{attempt}: expected all seven unique UI scenarios')
 return {**counts,'passed':passed,'pass_rate':round(rate,2),'first_attempt_rate':round(rate,2),'runs':len(runs),'missing_runs':missing,'matrix_issues':matrix_issues,'target_met':counts['tests']>0 and missing==0 and not matrix_issues and rate>=95}
if __name__=='__main__':
 ap=argparse.ArgumentParser();ap.add_argument('root');ap.add_argument('--expected-runs',type=int,default=6);ap.add_argument('--enforce',action='store_true');a=ap.parse_args();result=summarize(a.root,a.expected_runs);print(json.dumps(result,indent=2));raise SystemExit(1 if a.enforce and not result['target_met'] else 0)
