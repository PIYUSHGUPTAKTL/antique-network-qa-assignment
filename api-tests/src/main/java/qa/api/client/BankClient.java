package qa.api.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Allure;
import io.restassured.RestAssured;
import io.restassured.config.*;
import io.restassured.response.Response;
import java.math.*;
import java.nio.file.*;
import java.util.*;
import qa.core.Config;
import qa.core.Money;

public class BankClient {
  private final String base;
  private Map<String, String> cookies = Map.of();
  private final List<Map<String, Object>> trace = Collections.synchronizedList(new ArrayList<>());

  public BankClient() {
    base = Config.get("api.base").replaceAll("/$", "");
  }

  public String base() {
    return base;
  }

  private io.restassured.specification.RequestSpecification request() {
    return RestAssured.given()
        .config(
            RestAssuredConfig.config()
                .httpClient(
                    HttpClientConfig.httpClientConfig()
                        .setParam(
                            "http.connection.timeout",
                            Integer.parseInt(Config.get("api.timeout.ms", "15000")))
                        .setParam(
                            "http.socket.timeout",
                            Integer.parseInt(Config.get("api.timeout.ms", "15000")))))
        .cookies(cookies);
  }

  private Response record(String method, String path, Object params, Response response) {
    String safePath = path.startsWith("login/") ? "login/[REDACTED]/[REDACTED]" : path;
    String body = response.asString();
    if (path.startsWith("login/")) body = "[login body redacted]";
    trace.add(
        Map.of(
            "method",
            method,
            "path",
            safePath,
            "parameters",
            params,
            "status",
            response.statusCode(),
            "body",
            body));
    System.out.println(
        "{\"event\":\"http\",\"method\":\""
            + method
            + "\",\"path\":\""
            + safePath
            + "\",\"status\":"
            + response.statusCode()
            + "}");
    return response;
  }

  public Response form(String path, Map<String, String> fields) {
    Response r = request().formParams(fields).post(base + "/" + path);
    // A successful form response need not send Set-Cookie again. Retain the
    // session obtained by the initial page request and merge any rotated cookie.
    Map<String, String> updated = new HashMap<>(cookies);
    updated.putAll(r.cookies());
    cookies = Map.copyOf(updated);
    return r;
  }

  public Response html(String path) {
    Response r = request().get(base + "/" + path);
    if (!r.cookies().isEmpty()) cookies = Map.copyOf(r.cookies());
    return r;
  }

  public Response get(String path) {
    return get(path, "application/json");
  }

  public Response get(String path, String accept) {
    return record(
        "GET",
        path,
        Map.of("accept", accept),
        request().accept(accept).get(base + "/services/bank/" + path));
  }

  public Response post(String path, Map<String, ?> params) {
    return record(
        "POST",
        path,
        params,
        request()
            .accept("application/json")
            .queryParams(params)
            .post(base + "/services/bank/" + path));
  }

  public Response bill(int id, String amount) {
    var body =
        Map.of(
            "name",
            "QA Payee",
            "address",
            Map.of("street", "1 Test St", "city", "Test", "state", "CA", "zipCode", "90001"),
            "phoneNumber",
            "5550100000",
            "accountNumber",
            123456);
    Response r =
        request()
            .accept("application/json")
            .contentType("application/json")
            .queryParams(Map.of("accountId", id, "amount", amount))
            .body(body)
            .post(base + "/services/bank/billpay");
    return record("POST", "billpay", Map.of("accountId", id, "amount", amount), r);
  }

  public BigDecimal balance(int id) {
    Response r = get("accounts/" + id);
    if (r.statusCode() != 200)
      throw new IllegalStateException("Account fetch failed: " + r.statusCode());
    return Money.amount(r.jsonPath().get("balance").toString());
  }

  public List<Map<String, Object>> transactions(int id) {
    Response r = get("accounts/" + id + "/transactions");
    if (r.statusCode() != 200)
      throw new IllegalStateException("Transaction fetch failed: " + r.statusCode());
    return r.jsonPath().getList("$");
  }

  public void attach() {
    try {
      String json =
          new ObjectMapper()
              .writerWithDefaultPrettyPrinter()
              .writeValueAsString(List.copyOf(trace));
      Allure.addAttachment("HTTP request-response evidence", "application/json", json, ".json");
      Path p = Path.of("target", "evidence", UUID.randomUUID() + ".json");
      Files.createDirectories(p.getParent());
      Files.writeString(p, json);
    } catch (Exception e) {
      Allure.addAttachment("Evidence capture failure", e.toString());
    }
  }

  public record FixtureIdentity(int id, String firstName, String username, String password) {
    public FixtureIdentity(int id, String firstName) {
      this(id, firstName, "", "");
    }
  }

  public void verifyIdentity(FixtureIdentity identity) {
    Response r = get("customers/" + identity.id());
    if (r.statusCode() == 200 && identity.firstName().equals(r.jsonPath().getString("firstName")))
      return;
    if (r.statusCode() == 200)
      throw new EnvironmentInterrupted(
          "Owned customer identity changed; possible ID reuse after reset");
    if (r.statusCode() >= 500 || identity.username().isBlank())
      throw new IllegalStateException(
          "Identity verification unavailable; customer status=" + r.statusCode());
    Response loginProbe = get("login/" + identity.username() + "/" + identity.password());
    if ((r.statusCode() == 400 || r.statusCode() == 404) && loginProbe.statusCode() == 400) {
      throw new EnvironmentInterrupted(
          "Owned customer and previously valid login disappeared; possible shared reset");
    }
    throw new IllegalStateException(
        "Identity verification unavailable; customer/login statuses="
            + r.statusCode()
            + "/"
            + loginProbe.statusCode());
  }

  public static class EnvironmentInterrupted extends RuntimeException {
    public EnvironmentInterrupted(String s) {
      super(s);
    }
  }
}
