package qa.ui.core;

import com.fasterxml.jackson.databind.*;
import java.math.*;
import java.util.*;
import qa.core.CleanupJournal;

public final class Data {
  public final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
  public final CleanupJournal cleanup = new CleanupJournal();

  public String name(String label) {
    return "QA_" + suffix + "_" + label;
  }

  public static String scenario(String key) {
    try (var in = Data.class.getResourceAsStream("/scenarios.json")) {
      return new ObjectMapper().readTree(in).get(key).asText();
    } catch (Exception e) {
      throw new IllegalArgumentException("Scenario data unavailable", e);
    }
  }

  public int category(String name) {
    int id =
        Database.insert(
            "INSERT INTO oc_category SET"
                + " parent_id=0,`top`=1,`column`=1,sort_order=0,status=1,date_added=NOW(),date_modified=NOW()");
    cleanup.register(
        "category " + id,
        () -> {
          for (String table :
              List.of(
                  "category_description",
                  "category_to_store",
                  "category_path",
                  "category_to_layout",
                  "category"))
            Database.execute("DELETE FROM oc_" + table + " WHERE category_id=?", id);
        });
    for (var lang : Database.rows("SELECT language_id FROM oc_language"))
      Database.execute(
          "INSERT INTO oc_category_description SET"
              + " category_id=?,language_id=?,name=?,description='',meta_title=?",
          id,
          lang.get("language_id"),
          name,
          name);
    Database.execute("INSERT INTO oc_category_to_store VALUES (?,0)", id);
    Database.execute("INSERT INTO oc_category_path VALUES (?,?,0)", id, id);
    return id;
  }

  public void ownProduct(int id) {
    cleanup.register(
        "product " + id,
        () -> {
          for (String table :
              List.of(
                  "product_option_value",
                  "product_option",
                  "product_description",
                  "product_to_store",
                  "product_to_category",
                  "product_to_layout",
                  "product_image",
                  "product_special",
                  "product_discount",
                  "product_reward",
                  "product"))
            Database.execute("DELETE FROM oc_" + table + " WHERE product_id=?", id);
        });
  }

  public void ownProductMarker(String model) {
    cleanup.register(
        "product marker " + model,
        () -> {
          for (var row : Database.rows("SELECT product_id FROM oc_product WHERE model=?", model)) {
            ownProduct(((Number) row.get("product_id")).intValue());
          }
        });
  }

  public void productCategory(int id, int category) {
    Database.execute("INSERT IGNORE INTO oc_product_to_category VALUES (?,?)", id, category);
  }

  public void options(int product) {
    int option = Database.insert("INSERT INTO oc_option SET type='select',sort_order=0");
    cleanup.register(
        "option " + option,
        () -> {
          Database.execute("DELETE FROM oc_option_value_description WHERE option_id=?", option);
          Database.execute("DELETE FROM oc_option_value WHERE option_id=?", option);
          Database.execute("DELETE FROM oc_option_description WHERE option_id=?", option);
          Database.execute("DELETE FROM oc_option WHERE option_id=?", option);
        });
    int po =
        Database.insert(
            "INSERT INTO oc_product_option SET product_id=?,option_id=?,value='',required=1",
            product,
            option);
    for (var lang : Database.rows("SELECT language_id FROM oc_language"))
      Database.execute(
          "INSERT INTO oc_option_description SET option_id=?,language_id=?,name=?",
          option,
          lang.get("language_id"),
          name("size"));
    for (String label : List.of("Plus", "Minus")) {
      int v =
          Database.insert(
              "INSERT INTO oc_option_value SET option_id=?,image='',sort_order=0", option);
      for (var lang : Database.rows("SELECT language_id FROM oc_language"))
        Database.execute(
            "INSERT INTO oc_option_value_description SET"
                + " option_value_id=?,option_id=?,language_id=?,name=?",
            v,
            option,
            lang.get("language_id"),
            label);
      Database.execute(
          "INSERT INTO oc_product_option_value SET"
              + " product_option_id=?,product_id=?,option_id=?,option_value_id=?,quantity=100,subtract=0,price=?,price_prefix=?,points=0,points_prefix='+',weight=0,weight_prefix='+'",
          po,
          product,
          option,
          v,
          label.equals("Plus")
              ? scenario("fixedAddition")
              : new BigDecimal(scenario("basePrice"))
                  .multiply(new BigDecimal(scenario("reductionPercent")))
                  .divide(new BigDecimal("100")),
          label.equals("Plus") ? "+" : "-");
    }
  }

  public void ownCustomer(int id) {
    cleanup.register(
        "customer " + id,
        () -> {
          for (var o : Database.rows("SELECT order_id FROM oc_order WHERE customer_id=?", id)) {
            int oid = ((Number) o.get("order_id")).intValue();
            for (var ret : Database.rows("SELECT return_id FROM oc_return WHERE order_id=?", oid)) {
              Database.execute(
                  "DELETE FROM oc_return_history WHERE return_id=?", ret.get("return_id"));
            }
            Database.execute("DELETE FROM oc_return WHERE order_id=?", oid);
            for (String table :
                List.of(
                    "order_history",
                    "order_product",
                    "order_option",
                    "order_total",
                    "order_voucher",
                    "order"))
              Database.execute("DELETE FROM oc_" + table + " WHERE order_id=?", oid);
          }
          for (String table :
              List.of(
                  "address",
                  "customer_activity",
                  "customer_approval",
                  "customer_history",
                  "customer_ip",
                  "customer_reward",
                  "customer_transaction",
                  "cart",
                  "customer"))
            Database.execute("DELETE FROM oc_" + table + " WHERE customer_id=?", id);
        });
  }

  public void ownCustomerMarker(String email) {
    cleanup.register(
        "customer marker " + email,
        () -> {
          for (var row :
              Database.rows("SELECT customer_id FROM oc_customer WHERE email=?", email)) {
            ownCustomer(((Number) row.get("customer_id")).intValue());
          }
        });
  }

  public int currency() {
    int id =
        Database.insert(
            "INSERT INTO oc_currency SET"
                + " title=?,code='QAX',symbol_left='Q',symbol_right='',decimal_place='2',value=?,status=1,date_modified=NOW()",
            name("currency"),
            scenario("exchangeRate"));
    cleanup.register(
        "currency " + id,
        () -> Database.execute("DELETE FROM oc_currency WHERE currency_id=?", id));
    return id;
  }
}
