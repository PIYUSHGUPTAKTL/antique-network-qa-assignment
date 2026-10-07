package qa.ui.pages;

import java.util.*;
import org.openqa.selenium.*;
import qa.core.Config;
import qa.ui.core.*;

public final class AdminPage extends Page {
  private String token;
  private int orderId;

  public AdminPage(WebDriver d) {
    super(d);
  }

  public AdminPage login() {
    d.get(Config.get("ui.base") + "admin/");
    type("#input-username", Config.get("ui.admin.user"));
    type("#input-password", Config.get("ui.admin.password"));
    click("button[type=submit]");
    wait.until(x -> x.getCurrentUrl().contains("user_token="));
    token = d.getCurrentUrl().split("user_token=")[1].split("&")[0];
    return this;
  }

  public AdminPage route(String route) {
    d.get(Config.get("ui.base") + "admin/index.php?route=" + route + "&user_token=" + token);
    return this;
  }

  public int product(Data data, String name, String price, int quantity) {
    data.ownProductMarker(name);
    route("catalog/product/add");
    var languages = Database.rows("SELECT language_id FROM oc_language WHERE status=1");
    for (var lang : languages) {
      String id = lang.get("language_id").toString();
      click("a[href='#language" + id + "']");
      type("#input-name" + id, name);
      type("#input-meta-title" + id, name);
    }
    click("a[href='#tab-data']");
    type("#input-model", name);
    type("#input-price", price);
    type("#input-quantity", Integer.toString(quantity));
    selectValue("#input-status", "1");
    selectValue("#input-subtract", "1");
    selectValue("#input-stock-status", "5");
    click("button[form='form-product']");
    wait.until(x -> !x.findElements(By.cssSelector(".alert-success")).isEmpty());
    int id = Database.id("SELECT product_id FROM oc_product WHERE model=?", name);
    return id;
  }

  public AdminPage productInfo(int id) {
    return route("catalog/product/edit&product_id=" + id);
  }

  public AdminPage stockTab() {
    click("a[href='#tab-data']");
    return this;
  }

  public AdminPage order(int id) {
    orderId = id;
    return route("sale/order/info&order_id=" + id);
  }

  public AdminPage status(String status, String comment) {
    // Native history ordering uses second-resolution date_added. Wait for a distinct
    // server timestamp so this fixture has an unambiguous chronological sequence.
    wait.until(
        x ->
            ((Number)
                        Database.rows(
                                "SELECT NOW() > MAX(date_added) AS ready FROM oc_order_history"
                                    + " WHERE order_id=?",
                                orderId)
                            .get(0)
                            .get("ready"))
                    .intValue()
                == 1);
    select("#input-order-status", status);
    type("#input-comment", comment);
    var notify = element("#input-notify");
    if (!notify.isSelected()) notify.click();
    click("#button-history");
    wait.until(x -> x.findElement(By.id("input-comment")).getDomProperty("value").isEmpty());
    ajax();
    return this;
  }

  public AdminPage coupon(Data data, String code, String category) {
    data.cleanup.register(
        "coupon marker " + code,
        () -> {
          for (var row : Database.rows("SELECT coupon_id FROM oc_coupon WHERE code=?", code)) {
            Object id = row.get("coupon_id");
            for (String table :
                List.of("coupon_category", "coupon_product", "coupon_history", "coupon"))
              Database.execute("DELETE FROM oc_" + table + " WHERE coupon_id=?", id);
          }
        });
    route("marketing/coupon/add");
    type("#input-name", code);
    type("#input-code", code);
    selectValue("#input-type", "P");
    type("#input-discount", Data.scenario("couponPercent"));
    type("#input-total", Data.scenario("couponMinimum"));
    type("#input-category", category);
    wait.until(
        x ->
            x.findElements(By.cssSelector("#input-category + ul.dropdown-menu li a")).stream()
                .anyMatch(e -> e.getText().equals(category)));
    d.findElements(By.cssSelector("#input-category + ul.dropdown-menu li a")).stream()
        .filter(e -> e.getText().equals(category))
        .findFirst()
        .orElseThrow()
        .click();
    type("#input-date-start", "2020-01-01");
    type("#input-date-end", "2030-01-01");
    type("#input-uses-total", "100");
    type("#input-uses-customer", "100");
    click("button[form='form-coupon']");
    wait.until(x -> !x.findElements(By.cssSelector(".alert-success")).isEmpty());
    int id = Database.id("SELECT coupon_id FROM oc_coupon WHERE code=?", code);
    data.cleanup.register(
        "coupon " + id,
        () -> {
          for (String table :
              List.of("coupon_category", "coupon_product", "coupon_history", "coupon"))
            Database.execute("DELETE FROM oc_" + table + " WHERE coupon_id=?", id);
        });
    return this;
  }

  public AdminPage voucher(Data data, String code) {
    data.cleanup.register(
        "voucher marker " + code,
        () -> {
          for (var row : Database.rows("SELECT voucher_id FROM oc_voucher WHERE code=?", code)) {
            Database.execute(
                "DELETE FROM oc_voucher_history WHERE voucher_id=?", row.get("voucher_id"));
            Database.execute("DELETE FROM oc_voucher WHERE voucher_id=?", row.get("voucher_id"));
          }
        });
    route("sale/voucher/add");
    type("#input-code", code);
    type("#input-from-name", code);
    type("#input-from-email", "qa-from@example.test");
    type("#input-to-name", code);
    type("#input-to-email", "qa-to@example.test");
    type("#input-amount", Data.scenario("voucherValue"));
    selectValue("#input-status", "1");
    click("button[form='form-voucher']");
    wait.until(x -> !x.findElements(By.cssSelector(".alert-success")).isEmpty());
    int id = Database.id("SELECT voucher_id FROM oc_voucher WHERE code=?", code);
    data.cleanup.register(
        "voucher " + id,
        () -> {
          Database.execute("DELETE FROM oc_voucher_history WHERE voucher_id=?", id);
          Database.execute("DELETE FROM oc_voucher WHERE voucher_id=?", id);
        });
    return this;
  }

  public AdminPage returnInfo(int id) {
    return route("sale/return/edit&return_id=" + id);
  }

  public AdminPage returnStatus(String status, String comment) {
    click("a[href='#tab-history']");
    select("#tab-history #input-return-status", status);
    type("#input-history-comment", comment);
    click("#button-history");
    wait.until(
        x -> x.findElement(By.id("input-history-comment")).getDomProperty("value").isEmpty());
    ajax();
    return this;
  }
}
