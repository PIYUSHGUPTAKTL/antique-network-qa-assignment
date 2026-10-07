package qa.api.service;

import java.net.URI;
import qa.core.Config;

public final class BankService {
  public static void requireLocal() {
    String host = URI.create(Config.get("api.base")).getHost();
    if (!SetHolder.HOSTS.contains(host))
      throw new IllegalStateException(
          "Destructive/concurrency cases require isolated localhost target");
  }

  private static final class SetHolder {
    static final java.util.Set<String> HOSTS = java.util.Set.of("localhost", "127.0.0.1", "::1");
  }
}
