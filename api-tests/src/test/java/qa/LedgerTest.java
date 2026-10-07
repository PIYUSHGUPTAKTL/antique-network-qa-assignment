package qa;

import static org.testng.Assert.*;

import java.util.*;
import org.testng.annotations.Test;
import qa.api.service.Ledger;

@Test(groups = "unit")
public class LedgerTest {
  public void creditsAndDebits() {
    assertEquals(
        Ledger.signedSum(
                List.of(
                    Map.of("type", "Credit", "amount", "100"),
                    Map.of("type", "Credit", "amount", "20"),
                    Map.of("type", "Debit", "amount", "30"),
                    Map.of("type", "Debit", "amount", "10")))
            .toPlainString(),
        "80.00");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void unknownType() {
    Ledger.signedSum(List.of(Map.of("type", "Adjustment", "amount", "10")));
  }
}
