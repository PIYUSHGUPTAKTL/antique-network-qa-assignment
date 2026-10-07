package qa.ui;

import static org.testng.Assert.*;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.*;
import org.openqa.selenium.*;
import org.testng.annotations.Test;
import qa.ui.pages.OrderSnapshot;

@Test(groups = "unit")
public class OrderSnapshotTest {
  private WebElement cell(String text) {
    return node(text, List.of());
  }

  private WebElement node(String text, List<WebElement> children) {
    return (WebElement)
        Proxy.newProxyInstance(
            WebElement.class.getClassLoader(),
            new Class<?>[] {WebElement.class},
            (p, m, a) ->
                switch (m.getName()) {
                  case "getText" -> text;
                  case "findElements" -> children;
                  default -> throw new UnsupportedOperationException(m.getName());
                });
  }

  private WebElement row(String... text) {
    return node("", Arrays.stream(text).map(this::cell).toList());
  }

  private WebDriver driver(List<WebElement> rows) {
    WebElement table =
        (WebElement)
            Proxy.newProxyInstance(
                WebElement.class.getClassLoader(),
                new Class<?>[] {WebElement.class},
                (p, m, a) ->
                    m.getName().equals("findElements")
                        ? a[0].toString().contains("thead")
                            ? List.of(
                                cell("Product"),
                                cell("Model"),
                                cell("Quantity"),
                                cell("Price"),
                                cell("Total"))
                            : rows
                        : "product table");
    return (WebDriver)
        Proxy.newProxyInstance(
            WebDriver.class.getClassLoader(),
            new Class<?>[] {WebDriver.class},
            (p, m, a) ->
                m.getName().equals("findElements")
                    ? a[0].toString().endsWith("table tr") ? rows : List.of(table)
                    : "snapshot driver");
  }

  public void unexpectedProductIsRetainedForReconciliation() {
    var snapshot =
        OrderSnapshot.read(
            driver(
                List.of(
                    row("Owned", "QA_owned", "2", "$100.00", "$200.00"),
                    row("Unexpected", "other-model", "1", "$5.00", "$5.00"),
                    row("Total:", "$205.00"))),
            "#content",
            "QA_owned");
    assertEquals(snapshot.lines().size(), 2);
    assertEquals(snapshot.lines().get(1).model(), "other-model");
  }

  public void unexpectedChargeIsRetainedForReconciliation() {
    var snapshot =
        OrderSnapshot.read(
            driver(
                List.of(
                    row("Owned", "QA_owned", "2", "$100.00", "$200.00"),
                    row("Sub-Total:", "$200.00"),
                    row("Tax:", "$5.00"),
                    row("Total:", "$205.00"))),
            "#content",
            "QA_owned");
    assertEquals(snapshot.totals().get("Tax"), new BigDecimal("5.00"));
  }
}
