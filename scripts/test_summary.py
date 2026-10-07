import unittest,tempfile
from pathlib import Path
from summarize_results import summarize
class SummaryTests(unittest.TestCase):
 def test_six_incomplete_attempts_cannot_meet_target(self):
  with tempfile.TemporaryDirectory() as tmp:
   for browser in ['chrome','firefox']:
    for attempt in [1,2,3]:
     p=Path(tmp)/f'{browser}-{attempt}'/'surefire-reports';p.mkdir(parents=True);(p/'TEST-TestSuite.xml').write_text('<testsuite tests="1" failures="0" errors="0" skipped="0"><testcase classname="qa.ui.ScenarioTests" name="requiredOptionsAndQuantity"/></testsuite>')
   self.assertFalse(summarize(tmp,6)['target_met'])
 def test_arbitrary_six_folders_cannot_meet_target(self):
  with tempfile.TemporaryDirectory() as tmp:
   for i in range(6):
    p=Path(tmp)/f'other-{i}';p.mkdir();(p/'TEST.xml').write_text('<testsuite tests="7" failures="0" errors="0" skipped="0"/>')
   self.assertFalse(summarize(tmp,6)['target_met'])
 def test_lowercase_testng_exports_do_not_add_runs(self):
  with tempfile.TemporaryDirectory() as tmp:
   p=Path(tmp)/'one';p.mkdir();(p/'TEST-TestSuite.xml').write_text('<testsuite tests="1" failures="0" errors="0" skipped="0"/>');q=p/'Surefire suite';q.mkdir();(q/'testng-failed.xml').write_text('<suite/>');r=summarize(tmp,2);self.assertEqual(r['runs'],1);self.assertEqual(r['missing_runs'],1)
 def test_testng_duplicate_junit_exports_are_excluded(self):
  with tempfile.TemporaryDirectory() as tmp:
   p=Path(tmp)/'chrome-1'/'surefire-reports';p.mkdir(parents=True);(p/'TEST-TestSuite.xml').write_text('<testsuite tests="7" failures="0" errors="0" skipped="0"/>');q=p/'junitreports';q.mkdir();(q/'TEST-duplicate.xml').write_text('<testsuite tests="7" failures="0" errors="0" skipped="0"/>');r=summarize(tmp,1);self.assertEqual(r['tests'],7);self.assertEqual(r['runs'],1)
 def test_skips_are_not_success(self):
  with tempfile.TemporaryDirectory() as tmp:
   p=Path(tmp)/'chrome-1';p.mkdir();(p/'TEST.xml').write_text('<testsuite tests="4" failures="1" errors="0" skipped="1"/>');r=summarize(tmp,1);self.assertEqual(r['pass_rate'],50.0);self.assertFalse(r['target_met'])
 def test_missing_runs_fail_target(self):
  with tempfile.TemporaryDirectory() as tmp:
   p=Path(tmp)/'chrome-1';p.mkdir();(p/'TEST.xml').write_text('<testsuite tests="1" failures="0" errors="0" skipped="0"/>');r=summarize(tmp,6);self.assertEqual(r['missing_runs'],5);self.assertFalse(r['target_met'])
 def test_no_tests(self):
  with tempfile.TemporaryDirectory() as tmp:
   r=summarize(tmp);self.assertEqual(r['pass_rate'],0.0);self.assertFalse(r['target_met'])
if __name__=='__main__':unittest.main()
