package qa.ui.pages;

import java.math.BigDecimal;
import java.util.*;
import org.openqa.selenium.*;

/** Read product columns, ignoring the customer's extra action column. */
public record OrderSnapshot(List<Line> lines, Map<String, BigDecimal> totals) {
  public record Line(String model, int quantity, BigDecimal unitPrice, BigDecimal total) {}

  public static OrderSnapshot read(WebDriver driver, String scope, String ownedModel) {
    List<Line> lines = new ArrayList<>();
    Map<String, BigDecimal> totals = new TreeMap<>();
    for (WebElement row : driver.findElements(By.cssSelector(scope + " table tr"))) {
      List<WebElement> cells = row.findElements(By.tagName("td"));
      if (cells.size() >= 5 && cells.get(1).getText().equals(ownedModel)) {
        lines.add(
            new Line(
                ownedModel,
                Integer.parseInt(cells.get(2).getText()),
                Page.price(cells.get(3).getText()),
                Page.price(cells.get(4).getText())));
      } else if (cells.size() >= 2) {
        int labelIndex = cells.size() > 2 && cells.get(0).getText().isBlank() ? 1 : 0;
        String label = cells.get(labelIndex).getText().replace(":", "").trim();
        if (Set.of("Sub-Total", "Free Shipping", "Total").contains(label)) {
          totals.put(label, Page.price(cells.get(labelIndex + 1).getText()));
        }
      }
    }
    if (lines.isEmpty() || !totals.containsKey("Total")) {
      throw new IllegalStateException("Rendered order products/totals missing in " + scope);
    }
    return new OrderSnapshot(List.copyOf(lines), Map.copyOf(totals));
  }
}
