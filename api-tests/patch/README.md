# Local ParaBank remediation

This is an opt-in patched application for the local QA assignment. The default
`api-tests/docker-compose.yml` still runs the original official demo. Historical
failures and public findings remain valid records; a green patched run does not
mean the public ParaBank was fixed.

## Build and run

From the repository root:

```powershell
$env:DOCKER_API_VERSION='1.47' # Required by this host's Compose/engine combination.
docker compose -f api-tests/docker-compose.yml -f api-tests/docker-compose.patched.yml up -d --build --wait
$env:JAVA_HOME='C:/Program Files/Java/jdk-24'
. ./scripts/configure.ps1
$env:API_BASE='http://127.0.0.1:8081/parabank'
./api-tests/mvnw.cmd -f api-tests/pom.xml clean test
./api-tests/mvnw.cmd -f api-tests/pom.xml clean test -Pintegration -Dseed=42
./api-tests/mvnw.cmd -f api-tests/pom.xml clean test -Pintegration -Dseed=43
./api-tests/mvnw.cmd -f api-tests/pom.xml clean test -Dgroups=loans
./api-tests/mvnw.cmd -f api-tests/pom.xml clean test -Dgroups=reset
python scripts/verify_bank_patch.py --output boundary-verification.json
```

The overlay binds the bank to `127.0.0.1:8081`. Recreating the bank loses its
ephemeral synthetic data; the initializer restores seed data. The assignment's
administration/reset endpoints remain available locally. This is not a general
production deployment or a complete security audit.

## Provenance and changes

The runtime is the unchanged pinned official base image
`parasoft/parabank@sha256:cebe472d86a10be23ce448f0f419f3c939a818112ab78cd57fc09e185f35d1c0`.
Overlay copies of Account, AccountDao, JdbcAccountDao, BankManagerImpl and
AccessModeController come from upstream commit
`d9ff65e4447942c4e2b55cfa9a36ff478b032b80`, retaining its Apache 2.0 license.
web.xml and cxf.xml were taken from the pinned deployed image. Docker compiles
only the overlay against that image's actual libraries, using the pinned
Temurin 21 JDK. The QA framework continues to target Java 17.

- The servlet boundary requires a customer session or valid Basic credentials
  and verifies owned customer/account/transaction identifiers before either
  API family or SOAP operations execute. URL decoding and signed IDs receive
  the same ownership check. Unsupported authentication and duplicate relevant
  parameters are rejected.
- External deposit/withdraw/transfer/bill-payment amounts must be positive,
  representable with at most two nonzero decimal places, and within the
  supported amount precision. Invalid inputs get nonempty plain-text errors.
- Core money methods reject negative/unsupported-precision amounts. Internal
  zero down payments and zero configured opening funding remain valid.
- JDBC changes balances with arithmetic SQL. Debits include a sufficient-funds
  predicate. Existing Spring transactions encompass balance and ledger writes;
  transfer validates its destination before debiting. The selected local policy
  rejects overdrafts, deliberately changing the original demo's behavior.
- Domain XML roots and JAXB types use the namespace published in the service
  definition, with child fields kept unqualified. This also prevents split
  inline schemas from referencing types in an inconsistent namespace.
- Optional RESTJSON/RESTXML/SOAP access modes forward the caller's identity only
  to the same application's loopback endpoint and port. Credentials are not
  forwarded to arbitrary configured remote hosts. Such remote deployments need
  their own identity configuration and were not certified here.

The regression probe exercises real owned customer fixtures, foreign read/debit
attempts, encoded IDs, malformed authentication, invalid monetary inputs, SOAP,
metadata, and local optional modes. The original business tests remain active.
The companion six-defect utility reports `NOT REPRODUCED` on the patched app;
strict rejection/balance assertions in the Java suite and regression probe are
the passing evidence, not that label alone.

## UI synchronization correction

The full rerun also exposed an OpenCart quantity-change/cart-validation race.
StorePage.add now blurs the quantity field and waits for its recurring-description
AJAX request before clicking Add to Cart. The validation and exact money
assertions were preserved. The failed initial run is retained with the fresh
report as diagnostics.

BankClient.form also merges response cookies into the existing saved session.
The previous implementation replaced the session with an empty cookie map when
registration did not re-send Set-Cookie. The authenticated patched application
exposed this client bug; fixture-setup failures are preserved in diagnostics.

ContractValidator's business-value normalization uses XML local names, so a
namespace prefix does not alter a collection or numeric field's meaning. Two
regressions verify qualified empty collections and qualified decimal fields;
both were observed failing before the change. Live XSD namespace validation
remains a separate unchanged assertion.

## Limits

Full upstream ParaBank unit/IT suites were not executed; some upstream tests
explicitly expect overdrafts, which conflict with the approved local policy.
The verification boundary is the compiled pinned-image overlay and the real
assignment/regression suites. Grid, hosted CI, native JDK 17 execution, external
REST/SOAP deployments and production security completeness are not claimed.
Existing UI feature gaps in COVERAGE.md still apply. Zero-value HTML bill-payment
policy is unchanged; the external API rejects it.
