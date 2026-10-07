package qa;

import static org.testng.Assert.*;

import java.math.*;
import java.util.*;
import org.testng.annotations.Test;
import qa.core.*;

@Test(groups = "unit")
public class CoreTest {
  public void rounding() {
    assertEquals(
        Money.convert(new BigDecimal("10.00"), new BigDecimal("1.23456")).toPlainString(), "12.35");
    assertEquals(Money.amount("0.10").add(Money.amount("0.20")).toPlainString(), "0.30");
  }

  public void envOverrides() {
    Properties p = new Properties();
    p.setProperty("api.base", "property");
    assertEquals(Config.resolve("api.base", p, Map.of("API_BASE", "environment")), "environment");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void missingSecret() {
    Config.resolve("ui.admin.password", new Properties(), Map.of());
  }
}
