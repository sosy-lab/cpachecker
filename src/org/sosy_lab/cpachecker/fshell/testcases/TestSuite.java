package org.sosy_lab.cpachecker.fshell.testcases;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

public class TestSuite implements Iterable<TestCase> {

  private HashSet<TestCase> mTestSuite;

  public TestSuite() {
    mTestSuite = new HashSet<TestCase>();
  }

  public void add(TestSuite lTestSuite) {
    if (lTestSuite != this) {
      for (TestCase lTestCase : lTestSuite) {
        add(lTestCase);
      }
    }
  }

  public boolean add(TestCase pTestCase) {
    return mTestSuite.add(pTestCase);
  }

  public static TestSuite load(String pString) throws IOException {
    TestSuite lTestSuite = new TestSuite();

    lTestSuite.mTestSuite.addAll(TestCase.fromFile(pString));

    return lTestSuite;
  }

  public void write(String pTestSuiteOutputFilename) throws FileNotFoundException {
    write(new File(pTestSuiteOutputFilename));
  }

  public void write(File pTestSuiteOutputFile) throws FileNotFoundException {
    TestCase.toFile(mTestSuite, pTestSuiteOutputFile);
  }

  @Override
  public Iterator<TestCase> iterator() {
    return mTestSuite.iterator();
  }

}
