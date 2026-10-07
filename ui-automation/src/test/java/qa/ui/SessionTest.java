package qa.ui;

import static org.testng.Assert.*;

import org.openqa.selenium.Cookie;
import org.testng.annotations.Test;
import qa.ui.core.Drivers;

public class SessionTest {
  @Test(groups = "browser")
  public void separateCookies() {
    var p = Drivers.sessions();
    try {
      p.admin().get("http://localhost:8080/");
      p.customer().get("http://localhost:8080/");
      p.admin().manage().addCookie(new Cookie("qa-isolation", "admin"));
      assertNull(p.customer().manage().getCookieNamed("qa-isolation"));
    } finally {
      Drivers.close();
    }
  }
}
