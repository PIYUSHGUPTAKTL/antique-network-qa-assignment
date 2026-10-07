package qa.ui.core;

import java.net.*;
import java.time.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.firefox.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import qa.core.Config;

public final class Drivers {
  public record SessionPair(WebDriver admin, WebDriver customer) {}

  private static final ThreadLocal<SessionPair> LOCAL = new ThreadLocal<>();

  private static WebDriver create() {
    String browser = Config.get("ui.browser");
    boolean headless = Boolean.parseBoolean(Config.get("ui.headless"));
    MutableCapabilities options;
    if (browser.equals("chrome")) {
      ChromeOptions o = new ChromeOptions();
      if (headless) o.addArguments("--headless=new");
      o.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--window-size=1440,1100");
      options = o;
    } else if (browser.equals("firefox")) {
      FirefoxOptions o = new FirefoxOptions();
      if (headless) o.addArguments("-headless");
      options = o;
    } else throw new IllegalArgumentException("Unsupported browser: " + browser);
    WebDriver driver;
    String grid = Config.get("ui.grid", "");
    try {
      driver =
          grid.isBlank()
              ? (browser.equals("chrome")
                  ? new ChromeDriver((ChromeOptions) options)
                  : new FirefoxDriver((FirefoxOptions) options))
              : new RemoteWebDriver(URI.create(grid).toURL(), options);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException(e);
    }
    driver.manage().timeouts().implicitlyWait(Duration.ZERO);
    driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(45));
    var capabilities = ((RemoteWebDriver) driver).getCapabilities();
    System.out.println(
        "{\"event\":\"browser-created\",\"browser\":\""
            + capabilities.getBrowserName()
            + "\",\"version\":\""
            + capabilities.getBrowserVersion()
            + "\"}");
    return driver;
  }

  public static SessionPair sessions() {
    if (LOCAL.get() == null) {
      WebDriver admin = create();
      try {
        LOCAL.set(new SessionPair(admin, create()));
      } catch (RuntimeException e) {
        admin.quit();
        throw e;
      }
    }
    return LOCAL.get();
  }

  public static void close() {
    SessionPair p = LOCAL.get();
    try {
      if (p != null) {
        try {
          p.admin.quit();
        } finally {
          p.customer.quit();
        }
      }
    } finally {
      LOCAL.remove();
    }
  }
}
