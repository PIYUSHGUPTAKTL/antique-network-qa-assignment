# ParaBank defect analysis

Observed on **6 October 2026** using our own synthetic customers. All six findings reproduced on both `https://parabank.parasoft.com/parabank` and the pinned local official ParaBank image. Exact requests, responses and balance observations: `../evidence/api-public-reproductions.json` and `../evidence/api-local-reproductions.json`. The reproduction utility prints REPRODUCED only when its observable predicate holds. No seeded account was read or modified; public administration and database resets were not used.

## Context and severity
The service describes internal banking operations and is a deliberately exposed simulated bank. Security severity below states the business impact **if this interface is exposed as a customer banking API**; it does not allege compromise of real funds or a production bank. Authorization expectations are explicitly required by the assignment. Insufficient-funds rejection follows the assignment's negative-money expectation; a product owner would need to confirm whether an overdraft facility is deliberately supported. A plain HTTP status difference is not used as evidence for the monetary/security findings.

## Fresh data and minimal curl setup
Requires curl, jq and openssl. Set BASE to the public or local deployment; account IDs in the saved evidence are historical, so do not reuse them. Create your own IDs each time:
```bash
BASE=http://localhost:8081/parabank
create_customer() {
  local tag=$1 user="qa_$(openssl rand -hex 8)" pass="$(openssl rand -hex 10)"
  curl -fsS -c "$tag.cookies" "$BASE/register.htm" >/dev/null
  curl -fsS -b "$tag.cookies" -c "$tag.cookies" "$BASE/register.htm" \
    --data-urlencode 'customer.firstName=QA' --data-urlencode 'customer.lastName=Evidence' \
    --data-urlencode 'customer.address.street=1 Test St' --data-urlencode 'customer.address.city=Test' \
    --data-urlencode 'customer.address.state=CA' --data-urlencode 'customer.address.zipCode=90001' \
    --data-urlencode 'customer.phoneNumber=5550100000' --data-urlencode 'customer.ssn=999990000' \
    --data-urlencode "customer.username=$user" --data-urlencode "customer.password=$pass" \
    --data-urlencode "repeatedPassword=$pass" >/dev/null
  local cid=$(curl -fsS -b "$tag.cookies" -H 'Accept: application/json' "$BASE/services/bank/login/$user/$pass" | jq -r .id)
  curl -fsS -b "$tag.cookies" -H 'Accept: application/json' "$BASE/services/bank/customers/$cid/accounts" | jq -r '.[0].id'
}
A=$(create_customer a)
B=$(create_customer b)
balance() { curl -fsS -H 'Accept: application/json' "$BASE/services/bank/accounts/$1" | jq .balance; }
```
For loan/account-create IDs, the Java suite supplies fresh fixtures; for the six findings below the two IDs above suffice, with a fresh customer for each monetary defect to avoid compounding effects. Cookies identify the requester, although the observed REST implementation does not enforce account ownership.

## API-01: Another customer can read account transaction history

**Severity:** High. Reveals another customer's financial ledger and account activity to an authenticated unrelated user.

**Minimal reproduction:** create fresh A/B IDs using the setup above; run:
```bash
curl -i -b a.cookies -H 'Accept: application/json' "$BASE/services/bank/accounts/$B/transactions"
```

**Expected:** 401/403 with no transaction records.

**Actual:** HTTP 200 and victim transaction records returned to requester customer 25199 (owner 25310).

**Evidence:** `API-01` in both saved reproduction JSON files. Response body: `[{"id":31681,"accountId":32103,"type":"Credit","date":1791244800000,"amount":10.00,"description":"Deposit via Web Service"}]`. All reported predicates were true on both deployments.

**Root-cause hypothesis:** Account lookup appears to trust the path account ID without resolving its owner against the current session. The upstream service exposes internal operations, so an external authorization boundary may be intentionally absent. This is a hypothesis based on observed behavior and upstream source inspection; deployed source provenance is not assumed.

## API-02: Transfer can debit a different customer's account

**Severity:** Critical. An unrelated requester can remove victim funds and credit their own account. This is a money-integrity failure, beyond a status-code mismatch.

**Minimal reproduction:** create fresh A/B IDs using the setup above; run:
```bash
balance "$B"; balance "$A"
curl -i -b a.cookies -X POST -H 'Accept: application/json' "$BASE/services/bank/transfer?fromAccountId=$B&toAccountId=$A&amount=1.00"
balance "$B"; balance "$A"
```

**Expected:** 401/403; both balances remain unchanged.

**Actual:** HTTP 200. Before: `{'victim': '525.5', 'requester': '515.5'}`. After: `{'victim': '524.5', 'requester': '516.5'}`.

**Evidence:** `API-02` in both saved reproduction JSON files. Response body: `Successfully transferred $1.00 from account #32103 to account #31992`. All reported predicates were true on both deployments.

**Root-cause hypothesis:** Transfer likely accepts raw source/destination IDs without checking the source owner against the authenticated customer; financial service authorization is absent at this boundary. This is a hypothesis based on observed behavior and upstream source inspection; deployed source provenance is not assumed.

## API-03: Negative deposit reduces the account balance

**Severity:** High. A deposit endpoint can withdraw money by accepting a negative value, violating transaction direction and potentially bypassing withdrawal controls.

**Minimal reproduction:** create fresh A/B IDs using the setup above; run:
```bash
balance "$A"
curl -i -b a.cookies -X POST -H 'Accept: application/json' "$BASE/services/bank/deposit?accountId=$A&amount=-10.00"
balance "$A"
```

**Expected:** Reject nonpositive deposit; balance/ledger unchanged.

**Actual:** HTTP 200. Before: `515.5`. After: `505.5`.

**Evidence:** `API-03` in both saved reproduction JSON files. Response body: `Successfully deposited $-10.00 to account #32214`. All reported predicates were true on both deployments.

**Root-cause hypothesis:** Missing positive-amount validation before account.credit(amount); adding a negative amount reduces funds and records a negative credit. This is a hypothesis based on observed behavior and upstream source inspection; deployed source provenance is not assumed.

## API-04: Negative withdrawal creates money in the account

**Severity:** Critical. A withdrawal increases available funds, enabling synthetic money creation rather than debiting an authorized amount.

**Minimal reproduction:** create fresh A/B IDs using the setup above; run:
```bash
balance "$A"
curl -i -b a.cookies -X POST -H 'Accept: application/json' "$BASE/services/bank/withdraw?accountId=$A&amount=-10.00"
balance "$A"
```

**Expected:** Reject nonpositive withdrawal; balance/ledger unchanged.

**Actual:** HTTP 200. Before: `515.5`. After: `525.5`.

**Evidence:** `API-04` in both saved reproduction JSON files. Response body: `Successfully withdrew $-10.00 from account #32325`. All reported predicates were true on both deployments.

**Root-cause hypothesis:** Missing positive-amount validation before account.debit(amount); subtracting a negative amount increases funds. This is a hypothesis based on observed behavior and upstream source inspection; deployed source provenance is not assumed.

## API-05: Transfer exceeding available funds leaves a negative source balance

**Severity:** High. The requested negative-case invariant fails: a source without sufficient available funds is debited below zero. Risk assumes no authorized overdraft arrangement; that business policy is not detailed in the WADL.

**Minimal reproduction:** create fresh A/B IDs using the setup above; run:
```bash
SOURCE=$(balance "$A")
AMOUNT=$(python3 -c 'from decimal import Decimal; import sys; print(Decimal(sys.argv[1])+1)' "$SOURCE")
curl -i -b a.cookies -X POST -H 'Accept: application/json' "$BASE/services/bank/transfer?fromAccountId=$A&toAccountId=$B&amount=$AMOUNT"
balance "$A"; balance "$B"
```

**Expected:** Reject insufficient funds and preserve both balances.

**Actual:** HTTP 200. Before: `415.5`. After: `-1.0`.

**Evidence:** `API-05` in both saved reproduction JSON files. Response body: `Successfully transferred $416.50 from account #32436 to account #32547`. All reported predicates were true on both deployments.

**Root-cause hypothesis:** Source debit likely does not check available funds or an allowed-overdraft policy. The assignment requires this rejection, but sample seeded negative balances mean product-policy confirmation is appropriate. This is a hypothesis based on observed behavior and upstream source inspection; deployed source provenance is not assumed.

## API-06: Account XML does not validate against the deployed WADL schema

**Severity:** Medium. Clients generated from the published service definition may reject otherwise readable account responses. This is a payload-contract failure, not a status mismatch.

**Minimal reproduction:** create fresh A/B IDs using the setup above; run:
```bash
curl -fsS -H 'Accept: application/xml' "$BASE/services/bank?_wadl" > deployed.wadl
curl -fsS -H 'Accept: application/xml' "$BASE/services/bank/accounts/$A" > account.xml
# Automated unchanged-schema validation:
./api-tests/mvnw -f api-tests/pom.xml -Pintegration '-Dtest=ContractTests#deployedXmlSchema' test
```

**Expected:** The account root uses the schema's declared namespace and validates unchanged XSD.

**Actual:** HTTP 200; declared schema namespaces `['http://service.parabank.parasoft.com/', 'http://service.parabank.parasoft.com/']`; actual root `account`. Java validation failed with `cvc-elt.1.a: Cannot find the declaration of element 'account'.`

**Evidence:** `API-06` in both saved reproduction JSON files. Response body: `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><account><id>32436</id><customerId>25643</customerId><type>CHECKING</type><balance>-1.00</balance></account>`. All reported predicates were true on both deployments.

**Root-cause hypothesis:** The deployed WADL declares targetNamespace http://service.parabank.parasoft.com/ while REST JAXB serializes an unqualified account root. SOAP/domain schema generation and REST marshalling may not share namespace configuration. This is a hypothesis based on observed behavior and upstream source inspection; deployed source provenance is not assumed.

## Additional concurrency observation
The local ten-thread withdrawal test collected ten responses. At least one run showed the final balance differing from initial balance minus the amounts of successful withdrawals (a lost-update indication). An overdraft is **not assumed**: whether it occurred is recorded explicitly in the Allure concurrency attachment for each run. This is not one of the six deterministic public findings and was never run against the public bank. A minimal local reproduction uses ten background curl POSTs to /withdraw against a newly created account, with each amount greater than one tenth of its balance, waits for all processes, then compares the final balance and ledger against collected successful outcomes.

## Observation deliberately not called a bug
The combined month/type transaction-search endpoint takes a numeric month 1..12. A yyyy-MM assumption would produce an invalid request; that would be a test-design error, not a product defect. The framework derives the month from the generated transaction date and checks the nonempty returned set, types and months. Likewise, a missing-account HTTP 400 versus a preferred 404 is not independently counted as a defect here: data mutation, information exposure or an actual published contract must establish business impact.

## Sources and reproducibility limits
- Runtime WADL: https://parabank.parasoft.com/parabank/services/bank?_wadl
- Upstream service and financial implementation: https://github.com/parasoft/parabank/tree/d9ff65e4447942c4e2b55cfa9a36ff478b032b80/src/main/java/com/parasoft/parabank
- Public data may disappear on an unrelated reset. Never treat historical account IDs as fixtures; use the reproduction utility or fresh curl setup. Do not reset the public database to make evidence repeatable.
