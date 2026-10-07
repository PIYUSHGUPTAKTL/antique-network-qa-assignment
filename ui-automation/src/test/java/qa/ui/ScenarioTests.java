package qa.ui;

import static org.testng.Assert.*;

import io.qameta.allure.Allure;
import java.math.*;
import java.util.*;
import org.openqa.selenium.*;
import org.testng.annotations.*;
import qa.core.*;
import qa.ui.core.*;
import qa.ui.pages.*;

@Test(groups = "ui")
public class ScenarioTests extends UiBase {
  private int product(String label, String price, int stock) {
    return c().admin().product(c().data(), c().data().name(label), price, stock);
  }

  private void total(String want) {
    BigDecimal actual = c().store().total();
    assertEquals(
        actual.compareTo(Money.amount(want)),
        0,
        "Cart total expected=" + want + " actual=" + actual);
  }

  private void total(BigDecimal want) {
    total(want.toPlainString());
  }

  private void cartLine(BigDecimal price, int quantity) {
    var rows =
        Drivers.sessions()
            .customer()
            .findElements(By.cssSelector("#content .table-responsive tbody tr"));
    assertEquals(rows.size(), 1, "Option scenario must contain exactly its owned cart line");
    var cells = rows.get(0).findElements(By.tagName("td"));
    assertEquals(Page.price(cells.get(4).getText()), price);
    assertEquals(Page.price(cells.get(5).getText()), price.multiply(BigDecimal.valueOf(quantity)));
  }

  public void requiredOptionsAndQuantity() {
    BigDecimal base = Money.amount(Data.scenario("basePrice"));
    BigDecimal plus = base.add(Money.amount(Data.scenario("fixedAddition")));
    BigDecimal reduction =
        base.multiply(new BigDecimal(Data.scenario("reductionPercent")))
            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    BigDecimal minus = base.subtract(reduction);
    int quantity = Integer.parseInt(Data.scenario("quantity"));
    int id = product("options", Data.scenario("basePrice"), 100);
    c().data().options(id);
    c().admin().productInfo(id).click("a[href='#tab-option']");
    assertTrue(c().admin().text("#tab-option").contains(c().data().name("size")));
    var s = c().store().product(id).add(1);
    assertTrue(s.text(".text-danger").contains("required"));
    s.option("Plus").add(1).cart();
    cartLine(plus, 1);
    total(plus);
    s.quantity(quantity);
    cartLine(plus, quantity);
    total(plus.multiply(BigDecimal.valueOf(quantity)));
    s.removeAll().product(id).option("Minus").add(1).cart();
    cartLine(minus, 1);
    total(minus);
    Allure.addAttachment(
        "capability gap",
        "OpenCart core supports fixed + and - option prices. The 10% reduction was converted to a"
            + " fixed 10.00 fixture; native percentage option behavior remains unsupported.");
  }

  public void couponAndVoucher() {
    BigDecimal eligible = Money.amount(Data.scenario("couponEligiblePrice"));
    BigDecimal ineligible = Money.amount(Data.scenario("couponIneligiblePrice"));
    int quantity = Integer.parseInt(Data.scenario("quantity"));
    BigDecimal eligibleTotal = eligible.multiply(BigDecimal.valueOf(quantity));
    BigDecimal discount =
        eligibleTotal
            .multiply(new BigDecimal(Data.scenario("couponPercent")))
            .divide(new BigDecimal("100"));
    BigDecimal afterCoupon =
        eligibleTotal.add(ineligible).subtract(discount).setScale(2, RoundingMode.HALF_UP);
    assertTrue(eligible.compareTo(Money.amount(Data.scenario("couponMinimum"))) < 0);
    assertTrue(
        eligibleTotal.add(ineligible).compareTo(Money.amount(Data.scenario("couponMinimum"))) >= 0);
    var data = c().data();
    String cat = data.name("eligible");
    int category = data.category(cat);
    int a = product("eligible", Data.scenario("couponEligiblePrice"), 100),
        b = product("ineligible", Data.scenario("couponIneligiblePrice"), 100);
    data.productCategory(a, category);
    String coupon = "C" + data.suffix.substring(0, 8), voucher = "V" + data.suffix.substring(0, 8);
    c().admin().coupon(data, coupon, cat).voucher(data, voucher);
    var s = c().store().product(a).add(1).cart().coupon(coupon);
    assertTrue(s.text(".alert-danger").contains("invalid"));
    total(eligible);
    s.quantity(quantity);
    s.product(b).add(1).cart().coupon(coupon);
    total(afterCoupon);
    s.voucher(voucher);
    total(afterCoupon.subtract(Money.amount(Data.scenario("voucherValue"))));
    Allure.addAttachment(
        "voucher remainder gap",
        "Core cart displays the voucher deduction. A customer-facing remaining-balance label is not"
            + " implemented in this version; that requested assertion is unsupported.");
  }

  public void orderLifecycleAndHistory() {
    int customer = c().store().register(c().data(), "lifecycle");
    int product = product("lifecycle", "100", 100);
    c().store().product(product).add(2);
    var checkout = new CheckoutPage(Drivers.sessions().customer()).prepare();
    Map<String, String> summary = checkout.summary();
    assertTrue(summary.values().stream().anyMatch(v -> v.contains("200.00")));
    var checkoutSnapshot =
        OrderSnapshot.read(
            Drivers.sessions().customer(),
            "#collapse-checkout-confirm",
            c().data().name("lifecycle"));
    assertEquals(
        checkoutSnapshot.lines(),
        List.of(
            new OrderSnapshot.Line(
                c().data().name("lifecycle"), 2, Money.amount("100"), Money.amount("200"))));
    assertEquals(checkoutSnapshot.totals().get("Total"), Money.amount("200"));
    assertEquals(
        checkoutSnapshot.totals(),
        Map.of(
            "Sub-Total",
            Money.amount("200"),
            "Free Shipping",
            Money.amount("0"),
            "Total",
            Money.amount("200")),
        "Unexpected checkout charges");
    checkout.confirm();
    int order =
        Database.id(
            "SELECT order_id FROM oc_order WHERE customer_id=? ORDER BY order_id DESC", customer);
    c().admin().order(order);
    assertTrue(c().admin().text("#content").contains(c().data().name("lifecycle")));
    assertEquals(
        OrderSnapshot.read(Drivers.sessions().admin(), "#content", c().data().name("lifecycle")),
        checkoutSnapshot);
    for (String status : List.of("Pending", "Processing", "Shipped", "Complete")) {
      c().admin().status(status, c().data().name(status));
    }
    var s = c().store().order(order);
    String page = s.text("#content");
    assertEquals(
        OrderSnapshot.read(Drivers.sessions().customer(), "#content", c().data().name("lifecycle")),
        checkoutSnapshot);
    assertTrue(page.contains("Complete"));
    int last = -1;
    for (String status : List.of("Pending", "Processing", "Shipped", "Complete")) {
      int i = page.indexOf(c().data().name(status));
      assertTrue(i > last, "History comment order: " + status);
      last = i;
    }
    assertTrue(page.contains("200.00"));
    for (var row :
        Database.rows(
            "SELECT quantity,price,total FROM oc_order_product WHERE order_id=?", order)) {
      assertEquals(((Number) row.get("quantity")).intValue(), 2);
      assertEquals(new BigDecimal(row.get("price").toString()).compareTo(Money.amount("100")), 0);
      assertEquals(new BigDecimal(row.get("total").toString()).compareTo(Money.amount("200")), 0);
    }
    Allure.addAttachment("checkout snapshot", summary.toString());
  }

  public void returnStatusAndIsolation() {
    int customer = c().store().register(c().data(), "returner");
    int product = product("return", "100", 100);
    c().store().product(product).add(1);
    new CheckoutPage(Drivers.sessions().customer()).prepare().confirm();
    int order =
        Database.id(
            "SELECT order_id FROM oc_order WHERE customer_id=? ORDER BY order_id DESC", customer);
    c().admin().order(order).status("Complete", c().data().name("completed"));
    c().store().requestReturn(c().data(), order, product);
    int ret = Database.id("SELECT return_id FROM oc_return WHERE order_id=?", order);
    String comment = c().data().name("admin-return");
    c().admin()
        .returnInfo(ret)
        .returnStatus("Awaiting Products", c().data().name("awaiting"))
        .returnStatus("Complete", comment);
    c().store().returnInfo(ret);
    String page = c().store().text("#content");
    assertTrue(page.contains("Complete"));
    assertTrue(page.contains(comment));
    c().store().logout().register(c().data(), "stranger");
    c().store().returnInfo(ret);
    assertFalse(c().store().text("#content").contains(comment));
  }

  public void stockAndCancellation() {
    int customer = c().store().register(c().data(), "stock");
    int product = product("stock", "100", 2);
    c().store().product(product).add(2);
    new CheckoutPage(Drivers.sessions().customer()).prepare().confirm();
    int order =
        Database.id(
            "SELECT order_id FROM oc_order WHERE customer_id=? ORDER BY order_id DESC", customer);
    c().admin().order(order).status("Processing", c().data().name("paid"));
    c().admin().productInfo(product).stockTab();
    assertEquals(c().admin().element("#input-quantity").getDomProperty("value"), "0");
    c().store().product(product);
    assertTrue(c().store().text("#content").contains("Out Of Stock"));
    c().admin().order(order).status("Canceled", c().data().name("restock"));
    c().admin().productInfo(product).stockTab();
    assertEquals(c().admin().element("#input-quantity").getDomProperty("value"), "2");
  }

  public void paginatedSearchFilterSort() {
    var data = c().data();
    String term = data.name("search");
    int cat = data.category(data.name("search-category"));
    Set<String> expectedIds = new HashSet<>();
    for (int i = 0; i < 7; i++) {
      int id = c().admin().product(data, term + "_" + i, Integer.toString((i / 2 + 1) * 10), 100);
      data.productCategory(id, cat);
      expectedIds.add(Integer.toString(id));
    }
    int excluded = c().admin().product(data, term + "_outside", "999", 100);
    var s = c().store();
    s.open(
        "product/search&search="
            + term
            + "&category_id="
            + cat
            + "&sort=p.price&order=DESC&limit=3");
    Set<String> products = new HashSet<>();
    BigDecimal last = new BigDecimal("999999");
    int count = 0;
    while (true) {
      List<WebElement> cards =
          Drivers.sessions().customer().findElements(By.cssSelector(".product-thumb"));
      assertFalse(cards.isEmpty());
      for (var card : cards) {
        String title = card.findElement(By.cssSelector("h4 a")).getText();
        assertTrue(title.contains(term));
        String href = card.findElement(By.cssSelector("h4 a")).getDomAttribute("href");
        String productId = href.split("product_id=")[1].split("&")[0];
        assertNotEquals(productId, Integer.toString(excluded), "Category filter ignored");
        assertTrue(expectedIds.contains(productId));
        assertTrue(products.add(productId), "Duplicate across pages");
        String price = card.findElement(By.cssSelector(".price")).getText().split("\n")[0];
        BigDecimal current = Page.price(price);
        assertTrue(last.compareTo(current) >= 0);
        last = current;
        count++;
      }
      String showing = s.text("#content .row .text-right");
      int first = count - cards.size() + 1;
      assertEquals(showing, "Showing " + first + " to " + count + " of 7 (3 Pages)");
      var next =
          Drivers.sessions()
              .customer()
              .findElements(By.xpath("//ul[@class='pagination']/li/a[text()='>']"));
      if (next.isEmpty()) break;
      next.get(0).click();
      s.ajax();
    }
    assertEquals(count, 7);
    assertEquals(products, expectedIds);
  }

  public void localizationAndCurrency() {
    synchronized (ScenarioTests.class) {
      var data = c().data();
      int currency = data.currency();
      c().admin()
          .route("localisation/currency/edit&currency_id=" + currency)
          .type("#input-value", Data.scenario("exchangeRate"));
      c().admin().click("button[form='form-currency']");
      c().admin().ajax();
      int id = product("currency", "10", 100);
      BigDecimal rate = new BigDecimal(Data.scenario("exchangeRate"));
      BigDecimal expectedUnit =
          new BigDecimal("10").multiply(rate).setScale(2, RoundingMode.HALF_UP);
      BigDecimal expectedTotal =
          new BigDecimal("20").multiply(rate).setScale(2, RoundingMode.HALF_UP);
      c().admin().route("localisation/currency");
      assertTrue(c().admin().text("#content").contains("QAX"));
      var s = c().store().product(id).currency();
      assertEquals(Page.price(s.text("#content .list-unstyled h2")), expectedUnit);
      s.add(2).cart();
      total(expectedTotal);
      s.register(data, "currency");
      var checkout = new CheckoutPage(Drivers.sessions().customer()).prepare();
      var convertedOrder =
          OrderSnapshot.read(
              Drivers.sessions().customer(), "#collapse-checkout-confirm", data.name("currency"));
      assertEquals(convertedOrder.totals().get("Total"), expectedTotal);
      assertEquals(convertedOrder.lines().get(0).unitPrice(), expectedUnit);
      assertEquals(convertedOrder.lines().get(0).total(), expectedTotal);
      for (String route : List.of("checkout/cart", "account/account", "account/order")) {
        s.open(route);
        String selector = route.equals("account/account") ? "#content h2" : "#content h1";
        String english = s.text(selector);
        s.language("qa-es");
        String translated = s.text(selector);
        assertNotEquals(translated, english, "Untranslated label " + route);
        s.language("en-gb");
      }
    }
  }
}
