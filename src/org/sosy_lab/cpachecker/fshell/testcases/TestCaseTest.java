package org.sosy_lab.cpachecker.fshell.testcases;

import java.io.IOException;
import java.util.Collection;

import org.junit.Test;

public class TestCaseTest {

  @Test
  public void testFromFileString() throws IOException {
    Collection<TestCase> lTestSuite = TestCase.fromFile("/home/andreas/testsuite001.fts");
    
    System.out.println(lTestSuite);
    
    for (TestCase lTestCase : lTestSuite) {
      System.out.println(lTestCase.toCFunction());
    }
  }

}
