# Antique Network QA submission design

Status: historical design approved on 6 October 2026. Implementation and local/hosted verification are complete; current results and limits are in README.md and COVERAGE.md. Research notes below preserve the original planning context.

## Goal and deliverables
Build a reviewable submission for the supplied five-page SDET assignment, with independent Maven projects in ui-automation/ and api-tests/, root README.md and DECISIONS.md, Docker Compose, GitHub Actions, Allure evidence, UI FLAKINESS.md and PYRAMID.md, API risk plan with at least 30 cases and five undefined-behaviour cases, and a defect report with verified reproductions. Disclose AI assistance. Do not invent defects, pass rates, or runtime observations.

## Approach and alternatives
Recommended: Java 17-compatible code, TestNG, Maven, Selenium 4 and Rest Assured. The installed JDK 24 can compile with release 17; CI uses Java 17. Two independent projects keep either suite runnable without the other. TestNG supports parallel tests and listeners for failure artifacts.

Alternative: JUnit 5 provides a similarly capable stack but offers no clear benefit for this assignment. Testcontainers could manage the environment but adds lifecycle complexity to tests that require durable local fixtures; Docker Compose keeps startup transparent to the panel.

## UI framework
Pin an OpenCart release and its dependencies in a reproducible local Docker environment; provision a dedicated test administrator rather than altering seeded admin data. Keep credentials in environment variables and provide an example configuration with no real secrets. Use a dedicated store with deterministic taxes, shipping and enabled COD. Include Selenium Grid Chrome and Firefox and a local headless mode.

Create a session-pair object containing separate admin and customer WebDrivers. Store each pair in ThreadLocal storage; the two browser sessions must never share cookies. Fluent page objects use explicit waits, no PageFactory and no Thread.sleep. Separate admin, storefront, checkout, customer order and return pages. Configuration properties support environment-variable overrides; scenario data comes from JSON.

Per-test runtime identifiers include a UUID. A cleanup journal registers each newly created entity immediately, removes entities in reverse dependency order in finally/after-method handling, continues cleanup after individual failures, and preserves the original assertion failure. Shared prerequisites use an idempotent bootstrap; parallel tests own distinct mutable records.

Implement all seven requested scenarios with independently calculated BigDecimal prices and cross-page comparisons. Keep application limitations visible as unsupported capability findings rather than silently changing the requirement. On failure, attach screenshots, DOM and available console logs for both sessions to Allure; record unsupported Firefox logging explicitly. Do not retry financial or order mutations. Optional UI retry is limited to one, disabled by default; report first-attempt and final pass rates separately.

CI runs Chrome and Firefox three times each, uploads reports even on failures, counts failures and skips, and reports the actual rate against the 95% target. No claim of achieving the target before the six runs finish.

## API framework
Use a client layer for endpoint paths, transport and formats; service layer for customer registration, account fixtures and money workflows; test layer for independent business assertions. Register a unique customer through the HTML form, retain cookies, login through REST, and capture the customer ID. Every test owns its customers and accounts.

Fetch and preserve the deployed service definition and schema references. Validate supported response schemas and endpoint/method contracts; compare normalized JSON and XML business values, ignoring representation-only wrappers and order where unspecified.

Money assertions use BigDecimal scale 2, independently derived expected balances, and signed transaction sums. Account creation funding and initial deposits must be understood before choosing the ledger baseline. Do not assume that transaction history excludes opening entries. Test checking/savings, deposit, transfer and bill pay after every step.

Negative requests, customer authorization, loan rules, transaction filters, concurrent withdrawals and reset isolation each have focused tests. A ten-worker latch coordinates the requested concurrency case once per fixture, with a bounded timeout and complete worker outcome collection. No stress loops. Never reset or modify loan/admin settings on the shared public deployment; use a disposable local ParaBank instance for those operations and document its parameters. Public tests operate only on our own runtime customers. Avoid replaying mutations after a reset or transport timeout.

Detect lost fixture identity after a shared reset and report an environment-interrupted result with evidence; do not turn it into a product pass. Run twice and in seeded random order and preserve the seeds. Parameterize public and local base URLs. Attach sanitized request/response evidence on failure and use structured logging with secrets and customer personal data redacted.

## Known specification questions and intended rulings
1. OpenCart 3.0.3.9 core cart code accepts + and - option price prefixes with fixed numeric amounts. A percentage option subtraction requires an extension or a different product requirement. Validate a fixed reduction calculated from a base percentage only as a documented substitute; label the native percentage requirement unsupported. Do not modify OpenCart to make the test pass.
2. Verify voucher balance visibility on the pinned version. If no customer-visible remainder exists, record the unavailable assertion and validate supported discount/remaining value behavior separately.
3. Install a compatible second language pack reproducibly rather than assuming it exists in a fresh container.
4. ParaBank shared administration is global. Loan parameter changes, reset simulation and cleanup requiring global destruction belong on an isolated local deployment.
5. Check whether ParaBank supports per-customer deletion. If absent, document public cleanup limitations and use disposable local storage for complete teardown; never delete unrelated customers or reset the public database.
6. Six confirmed defects are a discovery target, not permission to manufacture results. If fewer reproduce, report the exact count and put unconfirmed hypotheses in a separate section.

## Verification and submission boundary
First verify arithmetic and configuration behavior with focused unit tests. Compile both projects and validate Compose configuration. Start fresh local application instances, run integration suites, inspect Allure artifacts and repeat browser/random-order execution. Preserve command outputs and observed defects. Finish with an explicit requirements-to-evidence matrix distinguishing passed, failed, unsupported and unverified items.

Prepare an upload-ready repository folder and archive. Repository publication, panel access and sending the assignment email require the user's repository destination and recipient details; neither is included in the supplied PDF. Do not send an email without explicit authorization.

## Verified environment and research, 6 October 2026
- JDK 24 and Docker CLI 29.8.2 found.
- Docker Desktop launched; Docker server reports 29.8.2.
- Maven executable not yet found on PATH; a Maven dependency cache exists. Locate an installed Maven or use the Maven Wrapper.
- Deployed ParaBank WADL downloaded successfully for inspection.
- OpenCart 3.0.3.9 source inspected: https://raw.githubusercontent.com/opencart/opencart/3.0.3.9/upload/system/library/cart/cart.php

