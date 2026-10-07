# Coverage and verified limits

## Latest patched local rerun - 7 October 2026

The final corrected run passed 67/67 project executions, 33/33 boundary checks and five report utility checks. Read run-report/Full_Project_Run_Report.html for the final matrix. Firefox used one method thread; Chrome used two. The sections below describe the original baseline verification and remain historical evidence, not the latest patched results.


This is a reviewable assignment submission, with reproducible Java frameworks and actual evidence. It does not claim unsupported OpenCart behavior or passing banking assertions when ParaBank violates them.

## Hosted API verification — 7 October 2026

The corrected API workflow passed 45/45 executions on GitHub with Temurin JDK 17. Both integration seeds passed 15/15; unit tests passed 13/13; loans and reset each passed 1/1. Evidence: run-report/github-ci/results.json and https://github.com/PIYUSHGUPTAKTL/antique-network-qa-assignment/actions/runs/37611113628. The initial GitHub run failed because its startup used the original unpatched bank. Hosted UI results remain separate from this API result.

## Historical original baseline environment
Windows 11, JDK 24 compiling with Java release 17, Maven 3.9.9, local Docker Linux containers. Application images/source are pinned in Compose and the store Dockerfile. Browser verification uses installed headless Chrome and Firefox with two parallel test methods and separate admin/customer sessions. GitHub Actions API verification has now passed as recorded above; the older baseline sections below describe historical local evidence. Docker Grid configuration is supplied; full Grid scenarios have not been executed locally.

## UI requirements and declared capability limits
| Scenario | Implemented verification | Limits |
|---|---|---|
| Required options and quantity | Required validation; fixed addition; reduction; quantity recalculation | Native percentage option pricing is unavailable; reduction fixture converts 10% to a fixed amount |
| Coupon and voucher | Category restriction, minimum rejection, eligible/ineligible lines, fractional-cent discount rounding, combined deduction | Native customer-facing voucher remaining-balance label is unavailable |
| COD order lifecycle | Checkout/admin/customer line and total reconciliation; Pending, Processing, Shipped, Complete and comments in history | Distinct server timestamps avoid ambiguous same-second native history ordering |
| Returns | Own completed order, reason/opened submission, admin status/comments, customer visibility and another customer's denial | Reason/opened are submitted; detailed reverse-stock/refund behavior is outside this scenario |
| Stock | Order depletion, out-of-stock display, cancellation restock | Deterministic local stock settings |
| Search | Seven matching products over three pages, exact ranges/count, tied prices descending, IDs without duplicates; matching outsider excluded by category | Owned fixture set |
| Localization/currency | Selected translated cart/account/order labels, switching, nontrivial exchange rate and aggregate rounding at checkout | Minimal QA Spanish pack; full production translation certification is not claimed |

Categories, option values, category associations and currency fixtures use owned, parameterized SQL. Products, promotions, customers, orders and returns use browser flows. Consequently full admin-UI-only fixture setup is not claimed. UUID markers register cleanup before browser submission, so a success followed by a timeout still leaves discoverable owned data. Browser actions use explicit waits; failure evidence includes screenshot, DOM and console collection result. No automatic retries.

## Historical original API observations
| Requirement | Result / implementation |
|---|---|
| Fresh data/session | Unique short HTML registration, cookie capture, REST login, owned accounts per case |
| Money and ledger | Checking/savings, deposits, transfer and bill payment; exact decimal balances plus signed ledger reconciliation |
| Invalid transfers | Insufficient, negative, zero, nonnumeric and absent destination; response shape, both balances and transaction histories |
| Authorization | Another owned customer's transaction read and source-account debit must be denied; actual service accepts them |
| Loans | Local provider, 20% threshold; five down-payment values and three available-funds boundaries; settings restored |
| Concurrency | Ten synchronized workers, collected status/body/errors, readiness/termination, overdraft and balance/ledger checks |
| Contract/parity | Live unchanged WADL inline XSD; secure XML parser; JSON/XML comparison canonicalized by record ID |
| Search | Amount search plus month/type search, using month 1..12 from a transaction timestamp |
| Repeat/reset | Random order seeds 42 and 43; local mid-run reset recognized without replaying mutations; generic 500 is not classified as reset |
| Strategy/defects | 46 risk cases including undefined behavior; six actual public/local defect reproductions, severity, curl steps, hypotheses and a nonbug |

Both final local integration attempts ran 15 tests: **5 passed, 10 failed, 0 skipped**. Failures preserve authorization, negative/invalid-money, concurrent balance and deployed XML namespace expectations. They are not environmental setup failures. The 11 API framework unit tests, the eight-case loan scenario and reset scenario passed. The API suite cannot truthfully be reported as passing twice without fixing the demo application; this submission intentionally leaves defect-detecting assertions active.

Public reproduction is bounded to synthetic customers. The public service has no per-customer deletion; no global reset or loan administration was performed there. Local Compose teardown removes owned ephemeral bank data. Evidence JSON contains observed responses and balances without registration passwords.

## Historical original execution evidence
The five result-aggregation utility tests passed, including skip handling, missing runs, duplicate JUnit exports and Windows case-insensitive TestNG exports. The API Maven Wrapper also executed the 11 framework tests successfully. Class-file major version 61 confirms the Java 17 compilation target; the hosted API JDK 17 run is now verified above; no native local JDK 17 execution is claimed.

Cart-navigation synchronization was changed before the revised matrix. The exact final-total label assertion was strengthened during verification; the subsequent Firefox repeats and the subtotal-only framework regression check cover that stricter oracle. Earlier revised Chrome attempts already exercised discounted totals that differ from subtotal. Firefox then exposed input interaction during a coupon accordion animation; an expanded-state wait was added during the matrix. Failures remain in the measured rate, and a focused follow-up validates the final coupon flow. This does not justify discarding observed results.

Final browser verification ran **42 scenario executions across six attempts (Chrome x3, Firefox x3)**: 41 passed, 1 failed, 0 errors, 0 skipped. The measured first-attempt pass rate is **97.62%**; the 95% target is met. No automatic retries were used. UI framework checks: 6 tests, 0 failures, 0 errors, 0 skips.

Essential original raw Surefire XML and result summaries are under `evidence/release/`; duplicate HTML/Allure assets and development diagnostic samples are retained locally rather than in this trimmed repository. `verification.json` records per-run counts/failures and execution boundaries; `ui-summary.json` records the measured rate. TestNG duplicate JUnit exports are excluded. Development screenshot/DOM diagnostics are excluded from release rates and retained locally outside this essential repository.

## Submission review fixes

See run-report/Reviewer_Report.html for the independent review and corrected assertion/CI gaps. Current post-review raw evidence and hosted run IDs are saved under run-report/reviewer-verification/ after the workflows complete. Earlier hosted UI verification passed 42/42 scenarios across all six named attempts (100%), on commit 66c3df1. Earlier hosted API verification passed 45/45. Those results predate the additional regression checks and strengthened oracles; they are not presented as verification of later changes.

## Submission boundary
Source, Maven Wrapper, Compose, JSON test data, docs, CI workflows and captured evidence are included. The essential source and evidence are published under PIYUSHGUPTAKTL/antique-network-qa-assignment. The repository is public and reviewers can view it without signing in. Sending the assignment email is the remaining submission step. Read DECISIONS.md for AI assistance disclosure.
