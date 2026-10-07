package qa.api;

import org.testng.*;
import org.testng.annotations.*;
import qa.api.service.*;

public class ApiBase {
  private static final ThreadLocal<Fixture> F = new ThreadLocal<>();

  protected Fixture f() {
    return F.get();
  }

  @BeforeMethod(alwaysRun = true)
  public void setup() {
    F.set(CustomerSetup.create());
  }

  @AfterMethod(alwaysRun = true)
  public void evidence(ITestResult result) {
    try {
      if (F.get() != null) {
        if (!result.isSuccess()) {
          var fixture = F.get();
          try {
            fixture
                .client()
                .verifyIdentity(
                    new qa.api.client.BankClient.FixtureIdentity(
                        fixture.customerId(),
                        "QA" + fixture.username().substring(3, 11),
                        fixture.username(),
                        fixture.password()));
          } catch (qa.api.client.BankClient.EnvironmentInterrupted lost) {
            if (result.getThrowable() != null) lost.addSuppressed(result.getThrowable());
            result.setThrowable(lost);
          } catch (RuntimeException unavailable) {
            if (result.getThrowable() != null) result.getThrowable().addSuppressed(unavailable);
          }
          fixture.client().attach();
        }
      }
    } finally {
      F.remove();
    }
  }
}
