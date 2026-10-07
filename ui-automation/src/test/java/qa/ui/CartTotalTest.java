package qa.ui;

import static org.testng.Assert.*;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.List;
import org.openqa.selenium.*;
import org.testng.annotations.Test;
import qa.ui.pages.StorePage;

/** A subtotal-only component must never satisfy the cart's final-total oracle. */
@Test(groups = "unit")
public class CartTotalTest {
  private WebElement node(String text, List<WebElement> children) {
    return (WebElement)
        Proxy.newProxyInstance(
            WebElement.class.getClassLoader(),
            new Class<?>[] {WebElement.class},
            (proxy, method, args) ->
                switch (method.getName()) {
                  case "getText" -> text;
                  case "findElements" -> children;
                  case "toString" -> "cart component node";
                  default -> throw new UnsupportedOperationException(method.getName());
                });
  }

  private WebElement row(String label, String amount) {
    return node("", List.of(node(label, List.of()), node(amount, List.of())));
  }

  private WebDriver driver(List<WebElement> rows) {
    return (WebDriver)
        Proxy.newProxyInstance(
            WebDriver.class.getClassLoader(),
            new Class<?>[] {WebDriver.class},
            (proxy, method, args) ->
                switch (method.getName()) {
                  case "findElements" -> rows;
                  case "toString" -> "cart component driver";
                  default -> throw new UnsupportedOperationException(method.getName());
                });
  }

  public void subtotalCannotStandInForMissingTotal() {
    String prior = System.getProperty("ui.timeout.seconds");
    try {
      System.setProperty("ui.timeout.seconds", "1");
      assertThrows(
          TimeoutException.class,
          () -> new StorePage(driver(List.of(row("Sub-Total", "$100.00")))).total());
    } finally {
      if (prior == null) System.clearProperty("ui.timeout.seconds");
      else System.setProperty("ui.timeout.seconds", prior);
    }
  }

  public void finalTotalWinsOverSubtotal() {
    assertEquals(
        new StorePage(driver(List.of(row("Sub-Total", "$100.00"), row("Total:", "$90.00"))))
            .total(),
        new BigDecimal("90.00"));
  }
}
