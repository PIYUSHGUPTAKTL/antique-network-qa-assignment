"""Aggregate real Surefire XML. Skips and absent runs cannot inflate success."""
import argparse,json,xml.etree.ElementTree as ET
from pathlib import Path
def summarize(root,expected_runs=6):
 files=[p for p in Path(root).rglob('TEST*.xml') if p.name.startswith('TEST') and 'junitreports' not in p.parts];counts={'tests':0,'failures':0,'errors':0,'skipped':0};runs=set()
 for p in files:
  suite=ET.parse(p).getroot();runs.add(str(p.parent))
  for key in counts:counts[key]+=int(suite.get(key,'0'))
 passed=max(0,counts['tests']-counts['failures']-counts['errors']-counts['skipped']);rate=100*passed/counts['tests'] if counts['tests'] else 0.0
 missing=max(0,expected_runs-len(runs));return {**counts,'passed':passed,'pass_rate':round(rate,2),'first_attempt_rate':round(rate,2),'runs':len(runs),'missing_runs':missing,'target_met':counts['tests']>0 and missing==0 and rate>=95}
if __name__=='__main__':
 ap=argparse.ArgumentParser();ap.add_argument('root');ap.add_argument('--expected-runs',type=int,default=6);ap.add_argument('--enforce',action='store_true');a=ap.parse_args();result=summarize(a.root,a.expected_runs);print(json.dumps(result,indent=2));raise SystemExit(1 if a.enforce and not result['target_met'] else 0)
