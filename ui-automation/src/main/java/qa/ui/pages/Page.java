package qa.ui.pages;

import java.math.*;
import java.time.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import qa.core.*;

public class Page {
  protected final WebDriver d;
  protected final WebDriverWait wait;

  public Page(WebDriver driver) {
    d = driver;
    wait =
        new WebDriverWait(
            d, Duration.ofSeconds(Long.parseLong(Config.get("ui.timeout.seconds", "15"))));
  }

  public Page open(String route) {
    d.get(Config.get("ui.base") + "index.php?route=" + route);
    return this;
  }

  public WebElement element(String css) {
    return wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(css)));
  }

  public Page click(String css) {
    wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(css))).click();
    return this;
  }

  public Page type(String css, String value) {
    WebElement e = element(css);
    e.clear();
    e.sendKeys(value);
    return this;
  }

  public Page select(String css, String label) {
    new Select(element(css)).selectByVisibleText(label);
    return this;
  }

  public Page selectValue(String css, String value) {
    new Select(element(css)).selectByValue(value);
    return this;
  }

  public String text(String css) {
    return element(css).getText();
  }

  public void ajax() {
    wait.until(
        x ->
            Boolean.TRUE.equals(
                ((JavascriptExecutor) x)
                    .executeScript("return !window.jQuery || jQuery.active === 0")));
  }

  public static BigDecimal price(String s) {
    return Money.amount(s.replaceAll("[^0-9.,-]", "").replace(",", ""));
  }
}
