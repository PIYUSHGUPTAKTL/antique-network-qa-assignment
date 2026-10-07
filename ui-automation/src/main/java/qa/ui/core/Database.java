package qa.ui.core;

import java.sql.*;
import java.util.*;
import qa.core.Config;

/**
 * Fixture bootstrap, read-back and owned-record cleanup only. Business actions use browser pages.
 */
public final class Database {
  private static Connection connection() throws SQLException {
    Connection c =
        DriverManager.getConnection(
            Config.get("db.url"), Config.get("db.user"), Config.get("db.password"));
    c.createStatement().execute("SET SESSION sql_mode=''");
    return c;
  }

  public static int insert(String sql, Object... args) {
    try (var c = connection();
        var p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      bind(p, args);
      p.executeUpdate();
      try (var r = p.getGeneratedKeys()) {
        return r.next() ? r.getInt(1) : 0;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Fixture insert failed", e);
    }
  }

  public static void execute(String sql, Object... args) {
    try (var c = connection();
        var p = c.prepareStatement(sql)) {
      bind(p, args);
      p.executeUpdate();
    } catch (SQLException e) {
      throw new IllegalStateException("Fixture update failed", e);
    }
  }

  public static List<Map<String, Object>> rows(String sql, Object... args) {
    try (var c = connection();
        var p = c.prepareStatement(sql)) {
      bind(p, args);
      try (var r = p.executeQuery()) {
        var out = new ArrayList<Map<String, Object>>();
        while (r.next()) {
          Map<String, Object> row = new LinkedHashMap<>();
          for (int i = 1; i <= r.getMetaData().getColumnCount(); i++)
            row.put(r.getMetaData().getColumnLabel(i), r.getObject(i));
          out.add(row);
        }
        return out;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Fixture query failed", e);
    }
  }

  public static int id(String sql, Object... args) {
    var r = rows(sql, args);
    if (r.isEmpty()) throw new IllegalStateException("Owned fixture not found");
    return ((Number) r.get(0).values().iterator().next()).intValue();
  }

  private static void bind(PreparedStatement p, Object[] a) throws SQLException {
    for (int i = 0; i < a.length; i++) p.setObject(i + 1, a[i]);
  }
}
