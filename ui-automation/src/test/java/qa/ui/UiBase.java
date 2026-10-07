package qa.ui;

import io.qameta.allure.Allure;
import java.io.*;
import org.openqa.selenium.*;
import org.testng.*;
import org.testng.annotations.*;
import qa.ui.core.*;
import qa.ui.pages.*;

public class UiBase {
  protected record Context(Data data, AdminPage admin, StorePage store) {}

  private static final ThreadLocal<Context> CTX = new ThreadLocal<>();

  protected Context c() {
    return CTX.get();
  }

  @BeforeMethod(alwaysRun = true)
  public void setup() {
    var data = new Data();
    var pair = Drivers.sessions();
    CTX.set(new Context(data, new AdminPage(pair.admin()), new StorePage(pair.customer())));
    c().admin.login();
  }

  @AfterMethod(alwaysRun = true)
  public void teardown(ITestResult result) {
    try {
      if (!result.isSuccess()) capture();
      if (CTX.get() != null)
        try {
          c().data.cleanup.close();
        } catch (RuntimeException cleanup) {
          if (result.getThrowable() != null) result.getThrowable().addSuppressed(cleanup);
          else {
            result.setStatus(ITestResult.FAILURE);
            result.setThrowable(cleanup);
            throw cleanup;
          }
        }
    } finally {
      try {
        Drivers.close();
      } finally {
        CTX.remove();
      }
    }
  }

  private void capture() {
    var pair = Drivers.sessions();
    for (var entry :
        java.util.Map.of("admin", pair.admin(), "customer", pair.customer()).entrySet()) {
      WebDriver d = entry.getValue();
      try {
        Allure.addAttachment(
            entry.getKey() + " screenshot",
            "image/png",
            new ByteArrayInputStream(((TakesScreenshot) d).getScreenshotAs(OutputType.BYTES)),
            ".png");
        Allure.addAttachment(
            entry.getKey() + " DOM",
            "text/html",
            d.getPageSource().replaceAll("(user_token|api_token)=[a-zA-Z0-9]+", "$1=REDACTED"),
            ".html");
        try {
          Allure.addAttachment(
              entry.getKey() + " console",
              d.manage()
                  .logs()
                  .get("browser")
                  .getAll()
                  .toString()
                  .replaceAll("(user_token|api_token)=[a-zA-Z0-9]+", "$1=REDACTED"));
        } catch (Exception e) {
          Allure.addAttachment(
              entry.getKey() + " console",
              "Browser does not support console collection: " + e.getClass().getSimpleName());
        }
      } catch (Exception e) {
        Allure.addAttachment(entry.getKey() + " evidence error", e.toString());
      }
    }
  }
}
