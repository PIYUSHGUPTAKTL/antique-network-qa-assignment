package qa.api.service;

import java.util.*;
import org.jsoup.Jsoup;
import qa.api.client.BankClient;

public final class CustomerSetup {
  public static Fixture create() {
    BankClient c = new BankClient();
    String suffix = UUID.randomUUID().toString().replace("-", "");
    String user = "qa_" + suffix.substring(0, 16), password = suffix.substring(0, 20);
    String first = "QA" + suffix.substring(0, 8);
    c.html("register.htm");
    var fields = new HashMap<String, String>();
    fields.put("customer.firstName", first);
    fields.put("customer.lastName", "Automation");
    fields.put("customer.address.street", "1 Test St");
    fields.put("customer.address.city", "Test");
    fields.put("customer.address.state", "CA");
    fields.put("customer.address.zipCode", "90001");
    fields.put("customer.phoneNumber", "5550100000");
    fields.put("customer.ssn", "999990000");
    fields.put("customer.username", user);
    fields.put("customer.password", password);
    fields.put("repeatedPassword", password);
    var registered = c.form("register.htm", fields);
    if (registered.statusCode() != 200
        || !registered.asString().contains("Your account was created successfully"))
      throw new IllegalStateException(
          "Registration failed: " + Jsoup.parse(registered.asString()).select(".error").text());
    var logged = c.get("login/" + user + "/" + password);
    if (logged.statusCode() != 200)
      throw new IllegalStateException("API login failed: " + logged.statusCode());
    int id = logged.jsonPath().getInt("id");
    var accounts = c.get("customers/" + id + "/accounts");
    if (accounts.statusCode() != 200)
      throw new IllegalStateException("Cannot fetch initial account");
    int account = accounts.jsonPath().getInt("[0].id");
    return new Fixture(c, id, account, user, password);
  }
}
