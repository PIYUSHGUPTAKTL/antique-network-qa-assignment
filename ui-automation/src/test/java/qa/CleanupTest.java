package qa;

import static org.testng.Assert.*;

import java.util.*;
import org.testng.annotations.Test;
import qa.core.CleanupJournal;

@Test(groups = "unit")
public class CleanupTest {
  public void reverseOrderAndContinue() {
    var seen = new ArrayList<Integer>();
    var j = new CleanupJournal();
    j.register("first", () -> seen.add(1));
    j.register(
        "second",
        () -> {
          seen.add(2);
          throw new IllegalStateException("delete failed");
        });
    j.register("third", () -> seen.add(3));
    try {
      j.close();
      fail("cleanup failures must surface");
    } catch (RuntimeException ex) {
      assertEquals(seen, List.of(3, 2, 1));
      assertEquals(ex.getSuppressed().length, 1);
    }
  }
}
