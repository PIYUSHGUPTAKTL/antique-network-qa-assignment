package qa.api;

import static org.testng.Assert.*;

import io.qameta.allure.Allure;
import java.math.*;
import java.util.*;
import org.jsoup.Jsoup;
import org.testng.annotations.*;
import org.testng.asserts.SoftAssert;
import qa.api.client.*;
import qa.api.service.*;
import qa.core.*;

public class LocalTests {
  @Test(groups = "loans")
  public void loanRules() {
    BankService.requireLocal();
    BankClient admin = new BankClient();
    var doc = Jsoup.parse(admin.html("admin.htm").asString());
    Map<String, String> before = new LinkedHashMap<>();
    for (String k : List.of("loanProvider", "loanProcessor", "loanProcessorThreshold")) {
      var e = doc.getElementById(k);
      before.put(k, e.tagName().equals("select") ? e.select("option[selected]").val() : e.val());
    }
    SoftAssert sa = new SoftAssert();
    try {
      assertEquals(admin.post("setParameter/loanProvider/local", Map.of()).statusCode(), 204);
      assertEquals(admin.post("setParameter/loanProcessor/down", Map.of()).statusCode(), 204);
      assertEquals(
          admin.post("setParameter/loanProcessorThreshold/20", Map.of()).statusCode(), 204);
      Allure.addAttachment(
          "loan settings",
          "provider=local processor=down threshold=20% (ratio rounded to three decimals)");
      for (String down : List.of("0.00", "19.90", "20.00", "30.00", "9999.00")) {
        Fixture f = CustomerSetup.create();
        BigDecimal initial = f.client().balance(f.accountId());
        boolean expected =
            new BigDecimal(down).compareTo(initial) <= 0
                && new BigDecimal(down)
                        .divide(new BigDecimal("100"), 3, RoundingMode.HALF_UP)
                        .compareTo(new BigDecimal("0.200"))
                    >= 0;
        var r =
            f.client()
                .post(
                    "requestLoan",
                    Map.of(
                        "customerId",
                        f.customerId(),
                        "amount",
                        "100",
                        "downPayment",
                        down,
                        "fromAccountId",
                        f.accountId()));
        sa.assertEquals(r.statusCode(), 200, "Loan response " + down);
        if (r.statusCode() == 200) {
          boolean approved = r.jsonPath().getBoolean("approved");
          sa.assertEquals(approved, expected, "Approval down=" + down);
          sa.assertEquals(
              f.client()
                  .balance(f.accountId())
                  .compareTo(expected ? initial.subtract(new BigDecimal(down)) : initial),
              0,
              "Down-payment account balance");
        }
        f.client().attach();
      }
      assertEquals(admin.post("setParameter/loanProcessor/funds", Map.of()).statusCode(), 204);
      for (String available : List.of("19.90", "20.00", "30.00")) {
        Fixture f = CustomerSetup.create();
        BigDecimal initial = f.client().balance(f.accountId());
        var preparation =
            f.client()
                .post(
                    "withdraw",
                    Map.of(
                        "accountId",
                        f.accountId(),
                        "amount",
                        initial.subtract(new BigDecimal(available)).toPlainString()));
        BigDecimal baseline = f.client().balance(f.accountId());
        assertEquals(preparation.statusCode(), 200, "Loan balance preparation failed");
        assertEquals(
            baseline.compareTo(new BigDecimal(available)),
            0,
            "Loan fixture must have the requested available balance before evaluating rules");
        boolean expected =
            new BigDecimal(available)
                    .divide(new BigDecimal("100"), 3, RoundingMode.HALF_UP)
                    .compareTo(new BigDecimal("0.200"))
                >= 0;
        var r =
            f.client()
                .post(
                    "requestLoan",
                    Map.of(
                        "customerId",
                        f.customerId(),
                        "amount",
                        "100",
                        "downPayment",
                        "0",
                        "fromAccountId",
                        f.accountId()));
        sa.assertEquals(r.statusCode(), 200, "Available-funds loan response");
        if (r.statusCode() == 200)
          sa.assertEquals(
              r.jsonPath().getBoolean("approved"), expected, "Available funds=" + baseline);
        sa.assertEquals(
            f.client().balance(f.accountId()).compareTo(baseline),
            0,
            "Zero down payment changed source funds");
        f.client().attach();
      }
    } finally {
      before.forEach((k, v) -> admin.post("setParameter/" + k + "/" + v, Map.of()));
    }
    sa.assertAll();
  }

  @Test(groups = "reset")
  public void midRunResetClassified() {
    BankService.requireLocal();
    var f = CustomerSetup.create();
    String first = f.client().get("customers/" + f.customerId()).jsonPath().getString("firstName");
    try {
      f.client().post("cleanDB", Map.of());
      assertThrows(
          BankClient.EnvironmentInterrupted.class,
          () ->
              f.client()
                  .verifyIdentity(
                      new BankClient.FixtureIdentity(
                          f.customerId(), first, f.username(), f.password())));
    } finally {
      f.client().post("initializeDB", Map.of());
      f.client().attach();
    }
  }
}
