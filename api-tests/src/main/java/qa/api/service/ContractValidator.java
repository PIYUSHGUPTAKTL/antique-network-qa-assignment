package qa.api.service;

import com.fasterxml.jackson.databind.*;
import java.io.*;
import java.math.*;
import java.util.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import javax.xml.validation.*;
import org.w3c.dom.*;

public final class ContractValidator {
  private static Document parse(String s) throws Exception {
    var f = DocumentBuilderFactory.newInstance();
    f.setNamespaceAware(true);
    f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return f.newDocumentBuilder()
        .parse(new ByteArrayInputStream(s.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  }

  private static Object scalar(String key, String value) {
    if (Set.of("id", "customerId", "accountId", "balance", "amount", "accountNumber").contains(key))
      return new BigDecimal(value).stripTrailingZeros();
    if (key.equals("approved")) return Boolean.parseBoolean(value);
    return value;
  }

  private static Object json(JsonNode node, String key) {
    if (node.isArray()) {
      List<Object> out = new ArrayList<>();
      node.forEach(x -> out.add(json(x, "")));
      return canonical(out);
    }
    if (node.isObject()) {
      Map<String, Object> out = new TreeMap<>();
      node.fields().forEachRemaining(x -> out.put(x.getKey(), json(x.getValue(), x.getKey())));
      return out;
    }
    return scalar(key, node.asText());
  }

  private static Object xml(Element e) {
    List<Element> children = new ArrayList<>();
    for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling())
      if (n instanceof Element child) children.add(child);
    if (children.isEmpty()) {
      if (Set.of("transactions", "accounts").contains(e.getLocalName())) return new ArrayList<>();
      return scalar(e.getLocalName(), e.getTextContent());
    }
    if (Set.of("transactions", "accounts").contains(e.getLocalName())) {
      List<Object> out = new ArrayList<>();
      children.forEach(x -> out.add(xml(x)));
      return canonical(out);
    }
    Map<String, Object> out = new TreeMap<>();
    for (Element child : children) {
      Object previous = out.put(child.getLocalName(), xml(child));
      if (previous != null)
        throw new IllegalArgumentException("Unexpected repeated field: " + child.getLocalName());
    }
    return out;
  }

  public static Object normalize(String s, boolean xml) {
    try {
      return xml ? xml(parse(s).getDocumentElement()) : json(new ObjectMapper().readTree(s), "");
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid representation", e);
    }
  }

  private static List<Object> canonical(List<Object> records) {
    if (records.stream().allMatch(x -> x instanceof Map<?, ?> m && m.containsKey("id")))
      records.sort(
          (a, b) ->
              new BigDecimal(((Map<?, ?>) a).get("id").toString())
                  .compareTo(new BigDecimal(((Map<?, ?>) b).get("id").toString())));
    return records;
  }

  public static void validate(String wadl, String path, String method, String body) {
    try {
      Document d = parse(wadl);
      boolean found = false;
      var resources = d.getElementsByTagNameNS("http://wadl.dev.java.net/2009/02", "resource");
      for (int i = 0; i < resources.getLength(); i++) {
        Element r = (Element) resources.item(i);
        if (r.getAttribute("path").equals(path)) {
          var methods = r.getElementsByTagNameNS("http://wadl.dev.java.net/2009/02", "method");
          for (int j = 0; j < methods.getLength(); j++)
            if (((Element) methods.item(j)).getAttribute("name").equals(method)) found = true;
        }
      }
      if (!found)
        throw new IllegalArgumentException(
            "Endpoint/method absent from WADL: " + path + " " + method);
      NodeList nodes = d.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "schema");
      Element schema = null;
      String root = parse(body).getDocumentElement().getLocalName();
      for (int i = 0; i < nodes.getLength(); i++) {
        Element candidate = (Element) nodes.item(i);
        var declarations =
            candidate.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "element");
        for (int j = 0; j < declarations.getLength(); j++)
          if (((Element) declarations.item(j)).getAttribute("name").equals(root)) {
            schema = candidate;
            break;
          }
      }
      if (schema == null) throw new IllegalArgumentException("No service schema for " + root);
      TransformerFactory tf = TransformerFactory.newInstance();
      tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
      StringWriter sw = new StringWriter();
      tf.newTransformer().transform(new DOMSource(schema), new StreamResult(sw));
      SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      sf.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      sf.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      sf.newSchema(new StreamSource(new StringReader(sw.toString())))
          .newValidator()
          .validate(new StreamSource(new StringReader(body)));
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Deployed contract validation failed: " + e.getMessage(), e);
    }
  }
}
