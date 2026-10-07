package qa;

import static org.testng.Assert.*;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.testng.annotations.Test;
import qa.api.client.BankClient;

@Test(groups = "unit")
public class ResetClassificationTest {
  public void serverErrorIsNotReset() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    server.createContext(
        "/services/bank/",
        exchange -> {
          byte[] body = "temporary server error".getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(500, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();
    String previous = System.getProperty("api.base");
    try {
      System.setProperty("api.base", "http://localhost:" + server.getAddress().getPort());
      var c = new BankClient();
      try {
        c.verifyIdentity(new BankClient.FixtureIdentity(1, "QA"));
        fail("Must signal unavailable identity check");
      } catch (BankClient.EnvironmentInterrupted e) {
        fail("A server error is not proof of reset", e);
      } catch (IllegalStateException expected) {
        assertTrue(expected.getMessage().contains("unavailable"));
      }
    } finally {
      if (previous == null) System.clearProperty("api.base");
      else System.setProperty("api.base", previous);
      server.stop(0);
    }
  }
}
