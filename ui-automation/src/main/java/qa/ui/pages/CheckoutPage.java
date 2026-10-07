package qa.ui.pages;

import java.math.*;
import java.util.*;
import org.openqa.selenium.*;

public final class CheckoutPage extends Page {
  public CheckoutPage(WebDriver d) {
    super(d);
  }

  public CheckoutPage prepare() {
    open("checkout/checkout");
    element("#button-payment-address");
    if (!d.findElements(By.cssSelector("#input-payment-firstname")).isEmpty()
        && d.findElement(By.id("input-payment-firstname")).isDisplayed()) {
      type("#input-payment-firstname", "QA");
      type("#input-payment-lastname", "Customer");
      type("#input-payment-address-1", "1 Test St");
      type("#input-payment-city", "Test");
      type("#input-payment-postcode", "90001");
      selectValue("#input-payment-country", "223");
      wait.until(
          x -> {
            try {
              new org.openqa.selenium.support.ui.Select(x.findElement(By.id("input-payment-zone")))
                  .selectByVisibleText("California");
              return true;
            } catch (org.openqa.selenium.NoSuchElementException
                | StaleElementReferenceException loading) {
              return false;
            }
          });
    }
    click("#button-payment-address");
    click("#button-shipping-address");
    element("#button-shipping-method");
    click("input[name='shipping_method'][value='free.free']");
    click("#button-shipping-method");
    click("input[name='payment_method'][value='cod']");
    click("input[name='agree']");
    click("#button-payment-method");
    element("#button-confirm");
    return this;
  }

  public Map<String, String> summary() {
    Map<String, String> out = new LinkedHashMap<>();
    for (var r : d.findElements(By.cssSelector("#collapse-checkout-confirm table tr"))) {
      var c = r.findElements(By.tagName("td"));
      if (!c.isEmpty()) out.put(c.get(0).getText(), c.get(c.size() - 1).getText());
    }
    return out;
  }

  public CheckoutPage confirm() {
    click("#button-confirm");
    wait.until(x -> x.getCurrentUrl().contains("checkout/success"));
    return this;
  }
}
