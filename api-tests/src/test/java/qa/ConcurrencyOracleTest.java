package qa;

import static org.testng.Assert.*;

import io.restassured.builder.ResponseBuilder;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.util.*;
import org.testng.annotations.Test;
import qa.api.BankTests;
import qa.api.client.BankClient;
import qa.api.service.Fixture;

/** Fault injection exercises the actual concurrent test oracle, without mutating a bank. */
@Test(groups = "unit")
public class ConcurrencyOracleTest {
  private BankTests scenario(int rejectedStatus, int acceptedLimit) {
    BankClient client =
        new BankClient() {
          private int accepted;

          public synchronized Response post(String path, Map<String, ?> params) {
            if (!path.equals("withdraw") || !params.get("amount").equals("20.00"))
              throw new IllegalArgumentException("Unexpected worker request");
            boolean ok = accepted < acceptedLimit;
            if (ok) accepted++;
            return new ResponseBuilder()
                .setStatusCode(ok ? 200 : rejectedStatus)
                .setContentType("text/plain")
                .setBody(
                    ok
                        ? "Successfully withdrew from account"
                        : rejectedStatus == 400 ? "Invalid banking request" : "Unexpected failure")
                .build();
          }

          public synchronized BigDecimal balance(int id) {
            return new BigDecimal("100.00")
                .subtract(new BigDecimal("20.00").multiply(BigDecimal.valueOf(accepted)));
          }

          public synchronized List<Map<String, Object>> transactions(int id) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (int i = 0; i < accepted; i++) rows.add(Map.of("type", "Debit", "amount", "20.00"));
            return rows;
          }
        };
    Fixture fixture = new Fixture(client, 1, 1, "synthetic", "unused");
    return new BankTests() {
      @Override
      protected Fixture f() {
        return fixture;
      }
    };
  }

  public void serverErrorsCannotPassAsOverdraftProtection() {
    assertThrows(AssertionError.class, () -> scenario(500, 0).concurrentWithdrawals());
  }

  public void blanketAuthorizationRejectionCannotPassAsSuccessfulConcurrency() {
    assertThrows(AssertionError.class, () -> scenario(403, 0).concurrentWithdrawals());
  }

  public void fiveSuccessfulDebitsAndFiveFundsRejectionsPass() throws Exception {
    scenario(400, 5).concurrentWithdrawals();
  }
}
