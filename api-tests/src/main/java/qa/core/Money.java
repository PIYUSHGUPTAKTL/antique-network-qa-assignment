package qa.core;

import java.math.*;

public final class Money {
  private Money() {}

  public static BigDecimal amount(String s) {
    return new BigDecimal(s).setScale(2, RoundingMode.HALF_UP);
  }

  public static BigDecimal convert(BigDecimal a, BigDecimal rate) {
    return a.multiply(rate).setScale(2, RoundingMode.HALF_UP);
  }
}
