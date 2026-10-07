package qa.ui.pages;

import java.math.*;
import java.util.*;
import org.openqa.selenium.*;
import qa.ui.core.*;

public final class StorePage extends Page {
  public StorePage(WebDriver d) {
    super(d);
  }

  public StorePage product(int id) {
    open("product/product&product_id=" + id);
    return this;
  }

  public StorePage add(int qty) {
    type("#input-quantity", Integer.toString(qty));
    // Blurring quantity starts OpenCart's recurring-description request. Its late
    // response clears validation messages, so finish it before cart validation.
    element("#input-quantity").sendKeys(Keys.TAB);
    ajax();
    click("#button-cart");
    ajax();
    return this;
  }

  public StorePage option(String label) {
    var options = d.findElements(By.cssSelector("#product select option"));
    var e = options.stream().filter(x -> x.getText().startsWith(label)).findFirst().orElseThrow();
    selectValue("#product select", e.getDomAttribute("value"));
    return this;
  }

  public StorePage cart() {
    open("checkout/cart");
    return this;
  }

  public BigDecimal total() {
    return wait.until(
        x -> {
          try {
            var rows = x.findElements(By.cssSelector("#content .table-bordered tr"));
            for (int i = rows.size() - 1; i >= 0; i--) {
              var cells = rows.get(i).findElements(By.tagName("td"));
              if (cells.size() == 2
                  && cells.get(0).getText().replace(":", "").trim().equals("Total"))
                return price(cells.get(1).getText());
            }
          } catch (StaleElementReferenceException loading) {
          }
          return null;
        });
  }

  public StorePage quantity(int quantity) {
    WebElement before = element("#content");
    var input = element("input[name^='quantity[']");
    input.clear();
    input.sendKeys(Integer.toString(quantity));
    click("button[data-original-title='Update']");
    wait.until(org.openqa.selenium.support.ui.ExpectedConditions.stalenessOf(before));
    element("#content");
    return this;
  }

  public StorePage removeAll() {
    while (!d.findElements(By.cssSelector("button[data-original-title='Remove']")).isEmpty()) {
      WebElement before = element("#content");
      click("button[data-original-title='Remove']");
      wait.until(org.openqa.selenium.support.ui.ExpectedConditions.stalenessOf(before));
      element("#content");
    }
    return this;
  }

  public StorePage coupon(String code) {
    WebElement before = element("#content");
    click("a[href='#collapse-coupon']");
    element("#collapse-coupon.collapse.in #input-coupon");
    type("#input-coupon", code);
    click("#button-coupon");
    wait.until(
        x -> {
          try {
            before.isEnabled();
          } catch (StaleElementReferenceException reloaded) {
            return true;
          }
          return !x.findElements(By.cssSelector(".alert-danger")).isEmpty();
        });
    element("#content");
    return this;
  }

  public StorePage voucher(String code) {
    WebElement before = element("#content");
    click("a[href='#collapse-voucher']");
    element("#collapse-voucher.collapse.in #input-voucher");
    type("#input-voucher", code);
    click("#button-voucher");
    wait.until(org.openqa.selenium.support.ui.ExpectedConditions.stalenessOf(before));
    element("#content");
    return this;
  }

  public int register(Data data, String label) {
    String email = data.name(label) + "@example.test";
    data.ownCustomerMarker(email);
    open("account/register");
    type("#input-firstname", "QA");
    type("#input-lastname", label);
    type("#input-email", email);
    type("#input-telephone", "5550100000");
    type("#input-password", data.suffix);
    type("#input-confirm", data.suffix);
    click("input[name='agree']");
    click("input[type='submit']");
    wait.until(x -> x.getCurrentUrl().contains("account/success"));
    int id = Database.id("SELECT customer_id FROM oc_customer WHERE email=?", email);
    return id;
  }

  public StorePage logout() {
    open("account/logout");
    return this;
  }

  public StorePage order(int id) {
    open("account/order/info&order_id=" + id);
    return this;
  }

  public StorePage returnInfo(int id) {
    open("account/return/info&return_id=" + id);
    return this;
  }

  public StorePage requestReturn(Data data, int order, int product) {
    int op =
        Database.id(
            "SELECT order_product_id FROM oc_order_product WHERE order_id=? AND product_id=?",
            order,
            product);
    open("account/return/add&order_id=" + order + "&product_id=" + product);
    type("#input-date-ordered", java.time.LocalDate.now().toString());
    click("input[name='return_reason_id']");
    click("input[name='opened'][value='1']");
    type("#input-comment", data.name("customer-return"));
    if (!d.findElements(By.cssSelector("input[name='agree']")).isEmpty())
      click("input[name='agree']");
    click("input[type='submit']");
    wait.until(x -> x.getCurrentUrl().contains("account/return/success"));
    return this;
  }

  public StorePage currency() {
    WebElement before = element("#content");
    click("#form-currency button.dropdown-toggle");
    click("#form-currency button[name='QAX']");
    wait.until(org.openqa.selenium.support.ui.ExpectedConditions.stalenessOf(before));
    element("#content");
    return this;
  }

  public StorePage language(String code) {
    WebElement before = element("#content");
    click("#form-language button.dropdown-toggle");
    click("#form-language button[name='" + code + "']");
    wait.until(org.openqa.selenium.support.ui.ExpectedConditions.stalenessOf(before));
    element("#content");
    return this;
  }
}
