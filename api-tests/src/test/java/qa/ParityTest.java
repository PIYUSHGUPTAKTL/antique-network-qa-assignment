package qa;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import qa.api.service.ContractValidator;

@Test(groups = "unit")
public class ParityTest {
  @org.testng.annotations.Test(groups = "unit")
  public void qualifiedEmptyCollectionHasParity() {
    org.testng.Assert.assertEquals(
        qa.api.service.ContractValidator.normalize(
            "<p:transactions xmlns:p='http://service.parabank.parasoft.com/'/>", true),
        qa.api.service.ContractValidator.normalize("[]", false));
  }

  @org.testng.annotations.Test(groups = "unit")
  public void qualifiedFieldsHaveDecimalParity() {
    org.testng.Assert.assertEquals(
        qa.api.service.ContractValidator.normalize(
            "<p:account xmlns:p='http://service.parabank.parasoft.com/'>"
                + "<p:id>1</p:id><p:balance>12.30</p:balance></p:account>", true),
        qa.api.service.ContractValidator.normalize("{\"id\":1,\"balance\":12.3}", false));
  }
  public void reorderedRecordsHaveParity() {
    assertEquals(
        ContractValidator.normalize("[{\"id\":2},{\"id\":1}]", false),
        ContractValidator.normalize(
            "<accounts><account><id>1</id></account><account><id>2</id></account></accounts>",
            true));
  }

  public void singletonCollectionHasParity() {
    assertEquals(
        ContractValidator.normalize("[{\"id\":1}]", false),
        ContractValidator.normalize("<accounts><account><id>1</id></account></accounts>", true));
  }

  public void decimalParity() {
    assertEquals(
        ContractValidator.normalize("{\"id\":1,\"balance\":12.30}", false),
        ContractValidator.normalize("<account><id>1</id><balance>12.30</balance></account>", true));
  }

  public void detectsMissingFields() {
    assertNotEquals(
        ContractValidator.normalize("{\"id\":1,\"balance\":12.30}", false),
        ContractValidator.normalize("<account><id>1</id></account>", true));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void externalEntityRejected() {
    ContractValidator.normalize(
        "<!DOCTYPE x [<!ENTITY a SYSTEM 'file:///missing'>]><x>&a;</x>", true);
  }
}
