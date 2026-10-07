# Antique Network QA submission

## Latest verification and review guide

The corrected local run on 7 October 2026 passed **67/67 Maven/TestNG executions**, **33/33 additional boundary regressions**, and **5/5 report utility checks**, with no failures, errors or skips. These are executions across both browsers and two API seeds, not 67 unique test designs.

- [Full execution report](run-report/Full_Project_Run_Report.html) (or [PDF](run-report/Full_Project_Run_Report.pdf)): original failure analysis, fixes, complete final results and limitations.
- `ui-automation/`: Java Selenium/TestNG framework, Docker Compose, FLAKINESS.md and PYRAMID.md.
- `api-tests/`: layered Java Rest Assured framework, TEST_PLAN.md and DEFECT_REPORT.md.
- `DECISIONS.md` and `COVERAGE.md`: trade-offs, AI use and scope.
- `.github/workflows/`: supplied UI/API CI workflows; hosted execution is not claimed.

**The default bank Compose file runs the original official demo and deliberately exposes its defects.** To reproduce the green corrected run, use `api-tests/patch/README.md` and its additional Compose overlay. Public ParaBank was not modified. Original defect evidence remains under `evidence/`; the optional application patch supplements the assignment's defect analysis.

This essential submission includes source, wrappers, configuration, documentation, sanitized defect evidence, final raw Surefire XML and result summaries. Build outputs, credentials, caches, duplicate Allure assets and development diagnostic archives are excluded. The full diagnostic bundle referenced in the report is retained in the candidate's local fixed submission ZIP; those diagnostic paths are not all included here. Allure attachments are generated when the suites run.

## Framework setup and commands


An opt-in local remediation is available in [api-tests/patch/README.md](api-tests/patch/README.md).
It patches the local bank and preserves the original demo as the default baseline.
Use the Compose overlay documented there to reproduce the corrected application runs.

Java UI and API automation for the supplied SDET take-home assignment. Read **COVERAGE.md** first: it distinguishes verified behavior, product defects, framework failures and unsupported requirements. Six ParaBank defects were reproduced on the public deployment and a pinned local instance; see api-tests/DEFECT_REPORT.md and evidence/. Do not assume that defect-detecting integration suites are green.

## Prerequisites
JDK 17+ with JAVA_HOME pointing to the JDK, Docker Desktop with Linux containers / Docker Compose v2, Python 3 for evidence utilities, Chrome and Firefox for local mode (Selenium Manager resolves drivers), internet access on first build. Maven Wrapper downloads Maven 3.9.9; no global Maven installation required. In Windows PowerShell, use .\mvnw.cmd; in Bash use ./mvnw. Docker Grid supplies browser binaries as an alternative.

## Windows: start the exact local environments
From this repository root, in PowerShell:
```powershell
# Change only this example JDK path to your actual installation.
$env:JAVA_HOME='C:\Program Files\Java\jdk-24'
. .\scripts\configure.ps1
docker compose -f ui-automation/docker-compose.yml up -d --build --wait
docker compose -f api-tests/docker-compose.yml up -d --wait
```
configure.ps1 generates credentials into ignored ui-automation/.env once and imports them into the current shell. Dot-source it again when opening a new shell. Keep that file private. UI: http://localhost:8080/ and /admin/. API: http://localhost:8081/parabank. The admin is a uniquely named QA account, not a seeded default account. If port 8081 is occupied by an earlier manual QA-bank container, stop that owned container before Compose startup; do not remove unrelated containers.

## Linux / macOS
```bash
source scripts/configure.sh
chmod +x ui-automation/mvnw api-tests/mvnw
docker compose -f ui-automation/docker-compose.yml up -d --build --wait
docker compose -f api-tests/docker-compose.yml up -d --wait
```

## Framework checks and UI scenarios
```powershell
.\ui-automation\mvnw.cmd -f ui-automation/pom.xml test
.\api-tests\mvnw.cmd -f api-tests/pom.xml test
.\ui-automation\mvnw.cmd -f ui-automation/pom.xml '-Dgroups=browser' test
.\ui-automation\mvnw.cmd -f ui-automation/pom.xml -Pintegration test
$env:UI_BROWSER='firefox'
.\ui-automation\mvnw.cmd -f ui-automation/pom.xml -Pintegration test
```
The default UI suite has two parallel methods, each with an admin/customer browser pair. UI_BROWSER=chrome|firefox, UI_HEADLESS=true|false. For Docker Grid:
```powershell
docker compose -f ui-automation/docker-compose.yml --profile grid up -d
$env:UI_GRID='http://localhost:4444/wd/hub'  # Firefox node: port 4445
$env:UI_BASE='http://host.docker.internal:8080/'
$env:STORE_URL=$env:UI_BASE
docker compose -f ui-automation/docker-compose.yml up -d --force-recreate store
.\ui-automation\mvnw.cmd -f ui-automation/pom.xml -Pintegration test
```
For Firefox set UI_BROWSER=firefox and UI_GRID=http://localhost:4445/wd/hub. Before returning to local browsers, clear UI_GRID, restore UI_BASE and STORE_URL to http://localhost:8080/, and recreate the store. Local and CI use local browsers by default. See COVERAGE.md for which paths were actually exercised.

## API execution
```powershell
.\api-tests\mvnw.cmd -f api-tests/pom.xml -Pintegration '-Dseed=42' test
.\api-tests\mvnw.cmd -f api-tests/pom.xml -Pintegration '-Dseed=43' test
.\api-tests\mvnw.cmd -f api-tests/pom.xml '-Dgroups=loans' test
.\api-tests\mvnw.cmd -f api-tests/pom.xml '-Dgroups=reset' test
```
Loan/reset groups require localhost and run separately. The normal local integration suite includes one ten-worker withdrawal case. API tests run serially except that explicit concurrency case; every test registers a fresh customer using the HTML form, preserves cookies and logs in through REST. Registration and account-opening credits must be accounted for; ledger tests use newly funded accounts and independently signed transactions.

Public defect reproduction is bounded and never changes global administration:
```powershell
python scripts/reproduce_api_defects.py --base https://parabank.parasoft.com/parabank --output evidence/my-public-reproductions.json
```
The public API offers no per-customer deletion, so synthetic records remain. Use the local bank for repeatable suites and complete data teardown. Mid-run fixture loss is classified as EnvironmentInterrupted and must not be counted as a passed assertion or trigger mutation replay.

## Configuration
Properties in each project's src/test/resources/config.properties supply nonsecret defaults. Environment variables override properties and -D properties: UI_ADMIN_USER, UI_ADMIN_PASSWORD, DB_PASSWORD, DB_URL, DB_USER, UI_BASE, UI_BROWSER, UI_GRID, UI_HEADLESS, API_BASE, API_TIMEOUT_MS. Defaults use local application URLs; no real credentials/tokens are committed. Financial amounts are decimal strings from JSON or explicit test data.

## Reports and CI
Surefire XML: module/target/surefire-reports. Allure results: module/target/allure-results. Generate/view reports:
```powershell
.\ui-automation\mvnw.cmd -f ui-automation/pom.xml allure:serve
.\api-tests\mvnw.cmd -f api-tests/pom.xml allure:serve
```
Archived essential results are the raw Surefire XML under `evidence/release/` and `run-report/`. Open `run-report/Full_Project_Run_Report.html` for the complete final results. Allure attachments and rendered Surefire HTML are generated by a fresh suite execution; duplicate historical report assets are excluded from this trimmed repository.
UI failures attach screenshot, page source and console result for both sessions; Firefox may not support console logs. API failures attach request/response history with login secrets redacted. No automatic retries. Separate result directories per attempt keep evidence from being overwritten. UI GitHub Actions executes Chrome/Firefox three times and reports skips/missing runs honestly against 95%; API repeats seeds 42 and 43 and runs local loan/reset groups. These workflows are supplied; only actual local verification is claimed in COVERAGE.md.

## Teardown and submission
After saving reports:
```powershell
docker compose -f ui-automation/docker-compose.yml down -v
docker compose -f api-tests/docker-compose.yml down -v
```
These commands destroy only this assignment's owned Compose data. Upload the repository contents (including .github) to your GitHub repository, grant access to the panel, and reply to the assignment email with the repository link. Do not upload .env, local database volumes or credentials. DECISIONS.md discloses AI assistance and compromises. The project is published at https://github.com/PIYUSHGUPTAKTL/antique-network-qa-assignment. Panel invitation and the assignment email still require the company reviewer's destination; they are not yet completed.
