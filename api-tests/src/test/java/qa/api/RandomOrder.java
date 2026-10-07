package qa.api;

import java.util.*;
import org.testng.*;

public class RandomOrder implements IMethodInterceptor {
  public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {
    var ordered = new ArrayList<>(methods);
    long seed = Long.parseLong(System.getProperty("seed", "42"));
    Collections.shuffle(ordered, new Random(seed));
    System.out.println("{\"event\":\"random-order\",\"seed\":" + seed + "}");
    return ordered;
  }
}
