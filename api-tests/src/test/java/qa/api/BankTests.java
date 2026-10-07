package qa.api;

import static org.testng.Assert.*;

import io.qameta.allure.Allure;
import io.restassured.response.Response;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.testng.annotations.*;
import org.testng.asserts.SoftAssert;
import qa.api.service.*;
import qa.core.*;

@Listeners(RandomOrder.class)
@Test(groups = "api")
public class BankTests extends ApiBase {
  private void success(Response r) {
    assertTrue(
        r.statusCode() >= 200 && r.statusCode() < 300,
        "Unexpected status " + r.statusCode() + ": " + r.asString());
  }

  private void money(BigDecimal actual, BigDecimal expected) {
    assertEquals(actual.compareTo(expected), 0, "Expected " + expected + " but got " + actual);
  }

  public void registrationAndIdentity() {
    var x = f();
    success(x.client().get("customers/" + x.customerId()));
    assertEquals(
        x.client().get("accounts/" + x.accountId()).jsonPath().getInt("customerId"),
        x.customerId());
    assertTrue(x.client().balance(x.accountId()).signum() >= 0);
  }

  public void movementAndLedger() {
    var x = f();
    var c = x.client();
    BigDecimal original = c.balance(x.accountId());
    var a =
        c.post(
            "createAccount",
            Map.of(
                "customerId", x.customerId(), "newAccountType", 0, "fromAccountId", x.accountId()));
    success(a);
    int checking = a.jsonPath().getInt("id");
    BigDecimal checkingOpen = c.balance(checking);
    money(c.balance(x.accountId()), original.subtract(checkingOpen));
    var b =
        c.post(
            "createAccount",
            Map.of(
                "customerId", x.customerId(), "newAccountType", 1, "fromAccountId", x.accountId()));
    success(b);
    int savings = b.jsonPath().getInt("id");
    BigDecimal savingsOpen = c.balance(savings);
    money(c.balance(x.accountId()), original.subtract(checkingOpen).subtract(savingsOpen));
    success(c.post("deposit", Map.of("accountId", checking, "amount", "200.10")));
    money(c.balance(checking), checkingOpen.add(Money.amount("200.10")));
    money(c.balance(savings), savingsOpen);
    success(
        c.post(
            "transfer",
            Map.of("fromAccountId", checking, "toAccountId", savings, "amount", "50.05")));
    money(c.balance(checking), checkingOpen.add(Money.amount("150.05")));
    money(c.balance(savings), savingsOpen.add(Money.amount("50.05")));
    success(c.bill(checking, "25.02"));
    money(c.balance(checking), checkingOpen.add(Money.amount("125.03")));
    money(c.balance(savings), savingsOpen.add(Money.amount("50.05")));
    for (int id : List.of(checking, savings))
      money(Ledger.signedSum(c.transactions(id)), c.balance(id));
  }

  @DataProvider
  public Object[][] invalid() {
    return new Object[][] {
      {"insufficient", "999999.00"},
      {"negative", "-1.00"},
      {"zero", "0"},
      {"nonnumeric", "abc"},
      {"missing-target", "1.00"}
    };
  }

  @Test(dataProvider = "invalid", groups = "api")
  public void invalidTransfers(String kind, String amount) {
    var c = f().client();
    int from = f().accountId();
    int to = 2147483647;
    if (!kind.equals("missing-target")) {
      var account =
          c.post(
              "createAccount",
              Map.of("customerId", f().customerId(), "newAccountType", 1, "fromAccountId", from));
      success(account);
      to = account.jsonPath().getInt("id");
    }
    BigDecimal before = c.balance(from);
    var ledgerBefore = c.transactions(from);
    BigDecimal destinationBefore = kind.equals("missing-target") ? null : c.balance(to);
    var destinationLedgerBefore = destinationBefore == null ? null : c.transactions(to);
    var r = c.post("transfer", Map.of("fromAccountId", from, "toAccountId", to, "amount", amount));
    var sa = new SoftAssert();
    sa.assertNotEquals(r.statusCode(), 500, "Unhandled server error");
    sa.assertTrue(
        r.statusCode() >= 400 && r.statusCode() < 500,
        "Invalid movement must be rejected: " + r.statusCode());
    sa.assertFalse(r.asString().isBlank(), "Error body missing");
    sa.assertTrue(
        r.asString().matches("(?s).*\\S.*") && r.contentType().contains("text/plain"),
        "Error response must have the documented plain-text error shape");
    sa.assertEquals(c.balance(from).compareTo(before), 0, "Rejected movement changed balance");
    sa.assertEquals(c.transactions(from), ledgerBefore, "Rejected movement changed source ledger");
    if (destinationBefore != null)
      sa.assertEquals(
          c.balance(to).compareTo(destinationBefore), 0, "Rejected movement changed destination");
    if (destinationLedgerBefore != null)
      sa.assertEquals(
          c.transactions(to),
          destinationLedgerBefore,
          "Rejected movement changed destination ledger");
    Allure.addAttachment("negative response", r.statusCode() + " " + r.asString());
    sa.assertAll();
  }

  public void crossCustomerTransactionRead() {
    var other = CustomerSetup.create();
    var c = f().client();
    success(other.client().post("deposit", Map.of("accountId", other.accountId(), "amount", "10")));
    var r = c.get("accounts/" + other.accountId() + "/transactions");
    Allure.addAttachment("authorization read", r.statusCode() + " " + r.asString());
    assertTrue(
        r.statusCode() == 401 || r.statusCode() == 403,
        "Another customer's transactions exposed: " + r.statusCode());
  }

  public void crossCustomerTransfer() {
    var other = CustomerSetup.create();
    var c = f().client();
    BigDecimal victim = other.client().balance(other.accountId()),
        mine = c.balance(f().accountId());
    var r =
        c.post(
            "transfer",
            Map.of(
                "fromAccountId",
                other.accountId(),
                "toAccountId",
                f().accountId(),
                "amount",
                "1.00"));
    var sa = new SoftAssert();
    sa.assertTrue(
        r.statusCode() == 401 || r.statusCode() == 403,
        "Unauthorized debit accepted: " + r.statusCode());
    sa.assertEquals(
        other.client().balance(other.accountId()).compareTo(victim), 0, "Victim debited");
    sa.assertEquals(c.balance(f().accountId()).compareTo(mine), 0, "Attacker credited");
    sa.assertAll();
  }

  public void negativeDepositCannotReduceBalance() {
    var c = f().client();
    BigDecimal before = c.balance(f().accountId());
    var r = c.post("deposit", Map.of("accountId", f().accountId(), "amount", "-10"));
    var sa = new SoftAssert();
    sa.assertTrue(r.statusCode() >= 400 && r.statusCode() < 500, "Negative deposit accepted");
    sa.assertEquals(
        c.balance(f().accountId()).compareTo(before), 0, "Negative deposit reduced balance");
    sa.assertAll();
  }

  public void negativeWithdrawalCannotIncreaseBalance() {
    var c = f().client();
    BigDecimal before = c.balance(f().accountId());
    var r = c.post("withdraw", Map.of("accountId", f().accountId(), "amount", "-10"));
    var sa = new SoftAssert();
    sa.assertTrue(r.statusCode() >= 400 && r.statusCode() < 500, "Negative withdrawal accepted");
    sa.assertEquals(
        c.balance(f().accountId()).compareTo(before), 0, "Negative withdrawal credited money");
    sa.assertAll();
  }

  public void transactionSearch() {
    var c = f().client();
    int id = f().accountId();
    success(c.post("deposit", Map.of("accountId", id, "amount", "12.34")));
    success(c.post("withdraw", Map.of("accountId", id, "amount", "5.67")));
    List<Map<String, Object>> amounts =
        c.get("accounts/" + id + "/transactions/amount/12.34").jsonPath().getList("$");
    assertFalse(amounts.isEmpty());
    for (var t : amounts) money(Money.amount(t.get("amount").toString()), Money.amount("12.34"));
    // Month is a number 1..12, not yyyy-MM; contract combines month and type.
    String date =
        c.transactions(id).stream()
            .filter(t -> t.get("amount").toString().equals("12.34"))
            .findFirst()
            .orElseThrow()
            .get("date")
            .toString();
    int month = Integer.parseInt(date.substring(5, 7));
    var r = c.get("accounts/" + id + "/transactions/month/" + month + "/type/Credit");
    success(r);
    List<Map<String, Object>> list = r.jsonPath().getList("$");
    assertFalse(list.isEmpty());
    for (var t : list) {
      assertEquals(t.get("type"), "Credit");
      assertEquals(Integer.parseInt(t.get("date").toString().substring(5, 7)), month);
    }
    assertTrue(
        list.stream()
            .anyMatch(
                t ->
                    Money.amount(t.get("amount").toString()).compareTo(Money.amount("12.34"))
                        == 0));
  }

  @Test(groups = "concurrency")
  public void concurrentWithdrawals() throws Exception {
    BankService.requireLocal();
    var c = f().client();
    int id = f().accountId();
    BigDecimal start = c.balance(id),
        each = start.divide(BigDecimal.valueOf(5), 2, RoundingMode.UP);
    var pool = Executors.newFixedThreadPool(10);
    var gate = new CountDownLatch(1);
    var ready = new CountDownLatch(10);
    List<Future<Response>> futures = new ArrayList<>();
    List<String> outcomes = new ArrayList<>();
    int accepted = 0;
    boolean readyInTime = false;
    boolean terminated = false;
    try {
      for (int i = 0; i < 10; i++)
        futures.add(
            pool.submit(
                () -> {
                  ready.countDown();
                  if (!gate.await(15, TimeUnit.SECONDS)) throw new TimeoutException("gate");
                  return c.post(
                      "withdraw", Map.of("accountId", id, "amount", each.toPlainString()));
                }));
      readyInTime = ready.await(15, TimeUnit.SECONDS);
      gate.countDown();
      for (var future : futures) {
        try {
          Response r = future.get(30, TimeUnit.SECONDS);
          outcomes.add(r.statusCode() + " " + r.asString());
          if (r.statusCode() >= 200 && r.statusCode() < 300) accepted++;
        } catch (Exception e) {
          outcomes.add("WORKER FAILED " + e.getClass().getSimpleName());
          future.cancel(true);
        }
      }
    } finally {
      gate.countDown();
      pool.shutdownNow();
      terminated = pool.awaitTermination(10, TimeUnit.SECONDS);
      Allure.addAttachment(
          "worker outcomes",
          "ready=" + readyInTime + " terminated=" + terminated + "\n" + outcomes);
    }
    BigDecimal end = c.balance(id),
        expected = start.subtract(each.multiply(BigDecimal.valueOf(accepted)));
    Allure.addAttachment(
        "concurrency observation",
        "start="
            + start
            + " each="
            + each
            + " accepted="
            + accepted
            + " final="
            + end
            + " overdraft="
            + (end.signum() < 0)
            + "\n"
            + outcomes);
    var sa = new SoftAssert();
    sa.assertTrue(readyInTime, "Workers were not all ready before release");
    sa.assertTrue(terminated, "Workers did not terminate; observation may be incomplete");
    sa.assertEquals(outcomes.size(), 10);
    sa.assertFalse(
        outcomes.stream().anyMatch(v -> v.startsWith("WORKER FAILED")), "Incomplete outcomes");
    sa.assertEquals(
        end.compareTo(expected), 0, "Lost update: accepted debits do not match balance");
    sa.assertEquals(
        end.compareTo(start.add(Ledger.signedSum(c.transactions(id)))),
        0,
        "Final balance does not reconcile with withdrawal ledger and opening baseline");
    sa.assertTrue(end.signum() >= 0, "Overdraft occurred: " + end);
    sa.assertAll();
  }
}
