# ParaBank risk-based test plan

## Purpose and strategy
Protect customer data, money conservation and auditability before optimizing broad endpoint coverage. Test independent balances and signed ledger entries, not just HTTP codes. The public practice bank is a shared, resettable demo. A security weakness here is evidence about the demo's behavior, not an assertion that a production financial institution is affected.

## Scope, environments and data
Cover Login, Customers, Accounts, Transfer, Deposit/Withdraw, Bill Pay, Loans and Transactions. Fresh synthetic customers own all target account IDs; never use seeded john/demo accounts. Bounded public tests use https://parabank.parasoft.com/parabank/services/bank/. Local Docker uses the pinned official ParaBank image in docker-compose.yml, localhost:8081. Loan configuration, reset simulation and the one ten-thread withdrawal case run locally only. No performance/load/stress testing.

Every case gets a new customer. Registration is an HTML form POST because REST has no registration endpoint; REST login captures the customer ID. Discover the initial account and balance. Serial fixture creation avoids ParaBank sequence races; test order remains seeded-random. Per-customer deletion is unavailable: public records cannot be fully removed, and local ephemeral container teardown is the complete cleanup boundary. Report this limitation explicitly.

## Risk and priority
Likelihood L and impact I each range 1 (low) to 5 (high). Score = L x I. P0: score >=20, P1: 12-19, P2: 6-11, P3: 1-5. Security and financial invariants can be promoted to P0 even when exploit frequency is uncertain. U labels a behavior not fully defined by the deployed service definition; the expectation is an explicit business hypothesis requiring product-owner agreement.

| ID | Area / check | Expected result / oracle | L | I | Score | Priority |
|---|---|---|---:|---:|---:|---|
| L01 | Fresh customer's correct login | Correct customer ID and own profile | 3 | 4 | 12 | P1 |
| L02 | Wrong password | No successful identity/session | 4 | 4 | 16 | P1 |
| L03 | Unknown username | No identity and nonempty error | 3 | 3 | 9 | P2 |
| L04 U | Login errors and account enumeration | Same public information for wrong user/password; agree policy | 3 | 4 | 12 | P1 |
| L05 U | Credential characters and length boundary | Valid supported credentials work; oversize errors accurate | 3 | 3 | 9 | P2 |
| C01 | Read own customer | Profile equals synthetic registration | 3 | 4 | 12 | P1 |
| C02 | Read another owned customer's profile | Session cannot expose another profile | 5 | 5 | 25 | P0 |
| C03 | Customer update | Allowed fields persist, identity remains own | 3 | 4 | 12 | P1 |
| C04 | Unauthorized profile update | No victim mutation | 4 | 5 | 20 | P0 |
| C05 U | Duplicate registration | No duplicate customer; accurate validation | 3 | 3 | 9 | P2 |
| A01 | Create checking account | Type CHECKING, owner correct, opening funds conserved | 4 | 5 | 20 | P0 |
| A02 | Create savings account | Type SAVINGS, owner correct, source funded correctly | 4 | 5 | 20 | P0 |
| A03 | Read own accounts | Only own IDs, balance numeric and exact | 3 | 5 | 15 | P1 |
| A04 | Another customer's funding account | Reject createAccount and conserve victim funds | 4 | 5 | 20 | P0 |
| A05 U | Invalid account type | Reject unsupported type, no new account/funding debit | 3 | 4 | 12 | P1 |
| T01 | Normal transfer | Source - amount; destination + amount; paired ledger | 4 | 5 | 20 | P0 |
| T02 | Transfer over balance | Reject and leave both accounts unchanged under no-overdraft hypothesis | 5 | 5 | 25 | P0 |
| T03 | Negative transfer | Reject, no reverse fund movement | 5 | 5 | 25 | P0 |
| T04 U | Zero transfer | Reject nonpositive amount; no zero-value ledger under chosen policy | 3 | 3 | 9 | P2 |
| T05 | Nonnumeric transfer | Client error, never 500, consistent nonempty error | 4 | 4 | 16 | P1 |
| T06 | Nonexistent destination | Reject atomically; source unchanged | 4 | 5 | 20 | P0 |
| T07 | Foreign source transfer | 401/403, no victim debit | 5 | 5 | 25 | P0 |
| T08 U | Transfer to same account | Net zero; agreed ledger/rejection policy | 3 | 3 | 9 | P2 |
| T09 U | Timeout / duplicate mutation | Do not blindly replay; agree idempotency contract | 3 | 5 | 15 | P1 |
| D01 | Positive deposit | Exact increase and one credit transaction | 4 | 5 | 20 | P0 |
| D02 | Positive withdrawal | Exact decrease and one debit transaction | 4 | 5 | 20 | P0 |
| D03 | Negative deposit | Reject; never debit via deposit | 5 | 5 | 25 | P0 |
| D04 | Negative withdrawal | Reject; never create money via withdrawal | 5 | 5 | 25 | P0 |
| D05 U | Fractional cent / rounding | Agree scale and rounding, balance equals posted ledger | 3 | 5 | 15 | P1 |
| D06 | Ten simultaneous withdrawals | Every outcome collected, no overdraft/lost update; final balance reconciled | 4 | 5 | 20 | P0 |
| B01 | Valid bill payment | Balance decreases exactly; bill transaction recorded | 4 | 5 | 20 | P0 |
| B02 | Missing payee fields | Reject without balance mutation | 3 | 4 | 12 | P1 |
| B03 | Foreign account bill payment | Reject; victim balance unchanged | 4 | 5 | 20 | P0 |
| B04 | Negative or insufficient bill amount | Reject atomically, no invalid ledger entry | 4 | 5 | 20 | P0 |
| N01 | Down-payment approval boundary | Local down processor threshold 20%, ratio rounded to 3 decimals | 3 | 5 | 15 | P1 |
| N02 | Down payment exceeds funds | Denial; no source debit or funded loan account | 4 | 5 | 20 | P0 |
| N03 | Available-funds rule | Approval matches captured processor/threshold and customer funds | 3 | 5 | 15 | P1 |
| N04 U | Zero/negative loan amount | Controlled validation, no division-by-zero/created funds | 4 | 5 | 20 | P0 |
| X01 | Signed ledger reconciliation | Opening/funding credits minus debits equal account balance | 4 | 5 | 20 | P0 |
| X02 | Amount search | Nonempty generated match, exact amount for every returned row | 3 | 4 | 12 | P1 |
| X03 | Month/type search | Correct numeric month (1-12) and type; known created ID included | 3 | 4 | 12 | P1 |
| X04 | Date range search | Inclusive boundaries follow agreed documentation; all rows in range | 3 | 4 | 12 | P1 |
| X05 | Foreign transaction history | Reject rather than expose ledger | 5 | 5 | 25 | P0 |
| X06 | JSON/XML parity | Identical normalized business values | 3 | 4 | 12 | P1 |
| X07 | WADL/XSD validation | Response validates unchanged deployed contract | 3 | 4 | 12 | P1 |
| X08 U | Mid-run environment reset | Classify fixture loss, attach evidence; never replay mutation | 4 | 3 | 12 | P1 |

## Execution and coverage
Execute P0 money/security cases first, then contracts/loans and P1/P2 validation. TEST_PLAN is broader than the automation; COVERAGE.md states precisely which cases were executed. An accepted vulnerable response deliberately fails the business regression assertion. Do not redefine expected behavior to make the suite green. Duplicate/reset/concurrency observations need fresh fixtures and exact timestamps/run seeds.

## Entry / exit criteria
Entry: application healthy, deploy/service definition saved, own registration works, required local loan settings known, enough ephemeral disk, request timeouts set. Exit: mandatory scenarios attempted, no uninvestigated setup failures, repeat/random-order results preserved, every defect has minimal repro and balance/body evidence, automation limitations disclosed. Product defects may prevent green exit; an honest defect-producing suite is acceptable, but it must not be described as passing twice when it fails twice.

## Not at API level
Visual layout, browser validation placement, accessible focus behavior, localized text rendering and responsive checkout belong in browser/accessibility tests. Email delivery and real bank settlement require integration environments with controlled downstream systems. Full financial load/performance testing needs an authorized isolated load environment and is excluded here. Mocked loan-provider contract tests belong in a controlled component suite; changing shared public loan parameters is excluded.
