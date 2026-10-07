package qa.api;

import static org.testng.Assert.*;

import io.qameta.allure.Allure;
import java.nio.file.*;
import org.testng.annotations.*;
import qa.api.service.*;

@Test(groups = "api")
@Listeners(RandomOrder.class)
public class ContractTests extends ApiBase {
  public void jsonXmlParity() {
    for (String path :
        new String[] {
          "customers/" + f().customerId(),
          "accounts/" + f().accountId(),
          "accounts/" + f().accountId() + "/transactions"
        }) {
      var json = f().client().get(path, "application/json");
      var xml = f().client().get(path, "application/xml");
      assertEquals(json.statusCode(), 200);
      assertEquals(xml.statusCode(), 200);
      assertEquals(
          ContractValidator.normalize(json.asString(), false),
          ContractValidator.normalize(xml.asString(), true),
          "Format mismatch " + path);
    }
  }

  public void deployedXmlSchema() throws Exception {
    var c = f().client();
    var wadl = c.get("?_wadl", "application/xml");
    assertEquals(wadl.statusCode(), 200);
    Files.createDirectories(Path.of("target/contracts"));
    Files.writeString(Path.of("target/contracts/deployed.wadl"), wadl.asString());
    Allure.addAttachment("deployed WADL", "application/xml", wadl.asString(), ".wadl");
    var account = c.get("accounts/" + f().accountId(), "application/xml");
    ContractValidator.validate(wadl.asString(), "accounts/{accountId}", "GET", account.asString());
  }
}
