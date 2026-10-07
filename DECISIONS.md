# Decisions and limitations

## Architecture
Java release 17, TestNG, two independently buildable Maven projects. Paired WebDrivers isolate admin and customer; fluent page methods use explicit waits. API client handles transport/evidence, setup/service classes own business fixtures and ledger arithmetic, tests contain independent assertions. Maven Wrapper supplies a portable Maven version; JAVA_HOME must point to a JDK.

## Evidence and failures
Security and amount-validation tests keep secure banking expectations even when the demo fails them. Six defects were reproduced with owned data on both local and public ParaBank; sanitized response/balance evidence is in evidence/. This is a demo service exposing internal banking operations, so severity describes business impact if exposed as a customer API, not a production-bank incident. Financial failures are not retried. Environment interruption is not a pass.

## Deliberate compromises
- OpenCart 3.0.3.9 has fixed + and - price prefixes; native percentage option subtraction is unsupported. A fixed amount calculated from 10% demonstrates the available flow and is not claimed as native percentage support.
- Native voucher cart output lacks a customer-facing remaining balance label. The combination/discount is tested; the unavailable label is documented.
- Category/option/currency fixtures and category associations use SQL, while products/promotions and customer/order/return actions use browser pages. This reduces setup fragility but does not satisfy every requested admin fixture entry through UI. SQL cleanup removes only owned IDs, including after assertion failures.
- A minimal owned Spanish QA pack translates selected labels. It proves switching mechanics only, not a full production language pack.
- API fixture registration is serial because concurrent ParaBank sequence allocation can cause setup errors. The required withdrawal case still has ten synchronized threads.
- Public REST exposes no per-customer deletion. Public test records remain; no global cleanup/reset is run. Local ephemeral bank container removal is complete cleanup, after evidence collection.
- Tests and docs record actual pass/fail/skip outcomes. No 95% UI claim or API twice-passing claim will be made unless measured results support it.
- A configured Docker Grid path is included alongside local headless mode. A validation matrix reports whether each path was actually exercised.
- Parallel admin requests exposed a race in OpenCart's native file cache. The disposable store selects its built-in Redis cache engine and uses an internal Redis service; no business behavior is patched.
- Fresh ParaBank containers require database initialization. A local-only bank-init service marks readiness after successful initialization; it does not reset the public service.

## AI assistance
Codex was used to read the assignment, research public application source/service definitions, design and implement Java frameworks and Docker configuration, execute tests, investigate failures, draft documentation and prepare this submission. All reported defects must point to actual captured observations. The candidate should review and understand the code before submitting or discussing it in an interview.

## Safety of shared targets
Only runtime-created synthetic customers/accounts were used on the public bank. Loan changes, reset and ten-thread withdrawal run on localhost. No seeded admin account was changed. The OpenCart bootstrap adds a separate QA administrator; deterministic configuration is confined to the disposable local store. Secrets live in ignored .env or environment variables. Email/publication is not performed by this package.
