package qa.api.service;

import java.math.*;
import java.util.*;
import qa.core.Money;

public final class Ledger {
  public static BigDecimal signedSum(List<Map<String, Object>> tx) {
    BigDecimal total = Money.amount("0");
    for (var t : tx) {
      BigDecimal a = Money.amount(t.get("amount").toString());
      total =
          switch (t.get("type").toString()) {
            case "Credit" -> total.add(a);
            case "Debit" -> total.subtract(a);
            default ->
                throw new IllegalArgumentException("Unknown transaction type: " + t.get("type"));
          };
    }
    return total;
  }
}
