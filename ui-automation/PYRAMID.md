# Placement of the seven scenarios in a real project

| Scenario | Primary level | Thin browser coverage retained |
|---|---|---|
| Option price modifiers | Pricing unit/component tests for required flags, signs, quantities, rounding | Required-option feedback and one displayed cart price |
| Coupon versus voucher | Promotion-service/component tests for category scope, thresholds, balance and combined ordering | Enter codes and verify one displayed total |
| Order lifecycle | Order-service integration, database/history and notification contract | Customer/admin access and history rendering |
| Returns | Return-service integration and ownership authorization tests | Submit reason/opened flag and display resulting status |
| Stock and cancellation | Transactional inventory/order integration, including concurrent purchase/restock | One stock label and cancellation action |
| Search/filter/sort | Search/query integration tests for matching, pagination counts and stable sort | User controls and one pagination transition |
| Localization/currency | Monetary rounding unit tests, translation-key completeness tests | Actual labels and formatted money on three page types |

The assignment intentionally keeps cross-role browser journeys for demonstration. Production should exercise arithmetic and state machines below UI so failures are faster and easier to diagnose. Retain a small number of critical UI journeys for integration of browser controls with services; do not replicate every financial boundary in Selenium.
