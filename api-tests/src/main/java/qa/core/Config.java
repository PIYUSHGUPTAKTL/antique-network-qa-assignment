package qa.core;

import java.io.*;
import java.util.*;

public final class Config {
  public static String resolve(String key, Properties p, Map<String, String> env) {
    String s = env.get(key.toUpperCase(Locale.ROOT).replace('.', '_'));
    if (s == null) s = p.getProperty(key);
    if (s == null) throw new IllegalArgumentException("Missing configuration: " + key);
    return s;
  }

  public static String get(String key) {
    Properties p = new Properties();
    try (var in = Config.class.getResourceAsStream("/config.properties")) {
      if (in != null) p.load(in);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    p.putAll(System.getProperties());
    return resolve(key, p, System.getenv());
  }

  public static String get(String key, String fallback) {
    try {
      return get(key);
    } catch (IllegalArgumentException e) {
      return fallback;
    }
  }
}
