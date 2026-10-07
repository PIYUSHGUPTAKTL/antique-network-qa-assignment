# Antique Network QA assignment

Java 17, Selenium 4, TestNG and Rest Assured frameworks for the supplied SDET assignment. Each UI test has separate admin/customer sessions and owned fixtures; API tests use fresh customers, independent decimal arithmetic and ledger reconciliation.

## Reviewer guide

1. Read [COVERAGE.md](COVERAGE.md) for requirements and limits, [DECISIONS.md](DECISIONS.md) for trade-offs and AI disclosure.
2. Inspect [API risk plan](api-tests/TEST_PLAN.md), [six reproduced defects](api-tests/DEFECT_REPORT.md), [UI instability analysis](ui-automation/FLAKINESS.md) and [test pyramid](ui-automation/PYRAMID.md).
3. Read [review findings and fixes](run-report/Reviewer_Report.html) and run the commands below. Raw XML and JSON evidence are kept alongside reports.

The optional local ParaBank patch fixes the demonstrated banking defects. **Passing patched tests do not mean the public demo is fixed.** The original pinned image remains available for defect reproduction. OpenCart business logic is unchanged; native percentage options and customer-visible voucher remainder are disclosed unsupported assertions. SQL category/option fixtures and a minimal language pack are also disclosed.

## Verification

- [Hosted API run](https://github.com/PIYUSHGUPTAKTL/antique-network-qa-assignment/actions/runs/37611113628): 45/45 passed on JDK 17 before this review's stricter oracles.
- [Hosted UI run](https://github.com/PIYUSHGUPTAKTL/antique-network-qa-assignment/actions/runs/37611113458): all seven scenarios, Chrome/Firefox three attempts each, 42/42 passed (100%).
- [Earlier complete local report](run-report/Full_Project_Run_Report.html), also [PDF](run-report/Full_Project_Run_Report.pdf): 67 project executions, 33 boundary checks and five reporting checks passed. These are historical counts; the review adds regression checks.
- Latest post-review results are recorded in [Reviewer_Report.html](run-report/Reviewer_Report.html). Counts are executions, not unique designs; original failed evidence remains under `evidence/`.

## Prerequisites

JDK 17+ with `JAVA_HOME` set to your installation, Docker Desktop using Linux containers with Compose v2, Python 3, and Chrome/Firefox for local headless runs. Maven Wrapper downloads Maven 3.9.9; no global Maven is required. First builds need internet access. Commands below run from the repository root.

## Windows PowerShell: reproduce the corrected local suites

```powershell
# Set JAVA_HOME to your installed JDK 17+ if it is not already configured.
. .\scripts\configure.ps1
docker compose -f ui-automation/docker-compose.yml up -d --build --wait
docker compose -f api-tests/docker-compose.yml -f api-tests/docker-compose.patched.yml up -d --build --wait
$env:API_BASE='http://127.0.0.1:8081/parabank'

.\ui-automation\mvnw.cmd -f ui-automation/pom.xml test
.\api-tests\mvnw.cmd -f api-tests/pom.xml test
python scripts/test_summary.py

$env:UI_BROWSER='chrome'
.\ui-automation\mvnw.cmd -f ui-automation/pom.xml -Pintegration test
$env:UI_BROWSER='firefox'
.\ui-automation\mvnw.cmd -f ui-automation/pom.xml -Pintegration test
.\api-tests\mvnw.cmd -f api-tests/pom.xml -Pintegration '-Dseed=42' test
.\api-tests\mvnw.cmd -f api-tests/pom.xml -Pintegration '-Dseed=43' test
.\api-tests\mvnw.cmd -f api-tests/pom.xml '-Dgroups=loans' test
.\api-tests\mvnw.cmd -f api-tests/pom.xml '-Dgroups=reset' test
```

`configure.ps1` creates ignored local credentials once and imports them into the current shell. Dot-source it again in a new shell. Never upload `.env`. Storefront/admin: `http://localhost:8080/` and `/admin/`; bank: `http://127.0.0.1:8081/parabank`. Ports must be free. Use owned-container teardown before switching banking variants; do not remove unrelated containers. A Compose/engine compatibility workaround used on the candidate's Windows host is documented in the patch guide and is optional elsewhere.

## Linux/macOS

```bash
source scripts/configure.sh
chmod +x ui-automation/mvnw api-tests/mvnw
docker compose -f ui-automation/docker-compose.yml up -d --build --wait
docker compose -f api-tests/docker-compose.yml -f api-tests/docker-compose.patched.yml up -d --build --wait
export API_BASE=http://127.0.0.1:8081/parabank
ui-automation/mvnw -f ui-automation/pom.xml test
api-tests/mvnw -f api-tests/pom.xml test
python3 scripts/test_summary.py
UI_BROWSER=chrome ui-automation/mvnw -f ui-automation/pom.xml -Pintegration test
UI_BROWSER=firefox ui-automation/mvnw -f ui-automation/pom.xml -Pintegration test
api-tests/mvnw -f api-tests/pom.xml -Pintegration -Dseed=42 test
api-tests/mvnw -f api-tests/pom.xml -Pintegration -Dseed=43 test
api-tests/mvnw -f api-tests/pom.xml -Dgroups=loans test
api-tests/mvnw -f api-tests/pom.xml -Dgroups=reset test
```

## Original banking defects

To run the unchanged official baseline, first tear down the patched bank, then start only `api-tests/docker-compose.yml`. The same integration assertions intentionally fail on the reproduced ownership, invalid-money, concurrency and XML namespace defects. See [DEFECT_REPORT.md](api-tests/DEFECT_REPORT.md) for captured public/local evidence and minimal curl steps. The patch is an additional remediation demonstration, not a substitute for reporting the original system's behavior.

Every API case registers a unique customer through the HTML form, retains cookies, logs in through REST and captures IDs. Registration/account-opening credits are included in balance expectations. Loan parameter changes, reset and ten-thread concurrency require localhost. API methods run serially except the explicit ten-worker case. Mid-run fixture loss is reported as `EnvironmentInterrupted`, never as a pass or mutation retry. No per-customer deletion is available publicly; owned local bank data is removed with Compose teardown.

## Docker Grid and configuration

Local headless mode is the verified default. To use the supplied Grid on this Docker Desktop setup:

```powershell
docker compose -f ui-automation/docker-compose.yml --profile grid up -d --wait
$env:UI_GRID='http://localhost:4444/wd/hub'
$env:UI_BASE='http://host.docker.internal:8080/'
$env:STORE_URL=$env:UI_BASE
docker compose -f ui-automation/docker-compose.yml up -d --force-recreate store
.\ui-automation\mvnw.cmd -f ui-automation/pom.xml -Pintegration test
```

Firefox uses `UI_BROWSER=firefox` and Grid port 4445. Before returning locally, clear `UI_GRID`, restore `UI_BASE` and `STORE_URL` to `http://localhost:8080/`, and recreate the store. Full Grid scenario execution is not claimed. `-Dgroups=browser` runs paired-session isolation checks. Defaults are in each module's `config.properties`; environment variables override defaults and Java properties. Options include `UI_BASE`, `UI_BROWSER`, `UI_GRID`, `UI_HEADLESS`, `UI_ADMIN_USER`, `UI_ADMIN_PASSWORD`, `DB_URL`, `DB_USER`, `DB_PASSWORD`, `API_BASE` and `API_TIMEOUT_MS`.

## Reports and CI

Surefire XML and Allure results are generated in each module's `target/`. Preserve each attempt before another `clean test`. To view Allure:

```powershell
.\ui-automation\mvnw.cmd -f ui-automation/pom.xml allure:serve
.\api-tests\mvnw.cmd -f api-tests/pom.xml allure:serve
```

UI failures attach both sessions' screenshots, page sources and console collection result; unsupported Firefox logging is recorded explicitly. API failures attach sanitized request/response history. No automatic retries. The UI workflow validates exactly seven unique scenarios in each named Chrome/Firefox attempt, reports the actual rate, and fails if its 95% gate fails. The API workflow runs unit checks, two seeds, loans and reset against the patched local image. GitHub artifacts retain Allure data; this essential repository retains raw XML and compact summaries.

## Teardown and submission

Save evidence, then remove only this assignment's owned data:

```powershell
docker compose -f ui-automation/docker-compose.yml --profile grid down -v
docker compose -f api-tests/docker-compose.yml -f api-tests/docker-compose.patched.yml down -v
```

The [public submission repository](https://github.com/PIYUSHGUPTAKTL/antique-network-qa-assignment) is accessible without invitations. Reply to the assignment email with that link before the deadline. Source, wrappers, configuration, docs and essential evidence are included; credentials, caches, build outputs and duplicate report assets are excluded. Historical reports may mention diagnostic archives retained only in the candidate's local full bundle.
