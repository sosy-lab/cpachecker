package org.sosy_lab.cpachecker.fshell.testcases;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Iterator;

// TODO refactor into interfaces instead of using inheritance
public class LoggingTestSuite extends TestSuite {

  private final TestSuite mTestSuite;
  private final PrintWriter mWriter;
  
  public LoggingTestSuite(TestSuite pTestSuite, String pTestSuiteFilename, boolean pAppend) throws FileNotFoundException {
    this(pTestSuite, new File(pTestSuiteFilename), pAppend);
  }
  
  @Override
  public void write(String pTestSuiteOutputFilename)
      throws FileNotFoundException {
    mTestSuite.write(pTestSuiteOutputFilename);
  }

  @Override
  public void write(File pTestSuiteOutputFile) throws FileNotFoundException {
    mTestSuite.write(pTestSuiteOutputFile);
  }

  @Override
  public Iterator<TestCase> iterator() {
    return mTestSuite.iterator();
  }

  public LoggingTestSuite(TestSuite pTestSuite, File pTestSuiteFile, boolean pAppend) throws FileNotFoundException {
    mTestSuite = pTestSuite;
    
    if (!pAppend) {
      mTestSuite.write(pTestSuiteFile);
    }
    
    mWriter = new PrintWriter(new FileOutputStream(pTestSuiteFile, true));
  }
  
  @Override
  public boolean add(TestCase pTestCase) {
    if (mTestSuite.add(pTestCase)) {
      mWriter.println(pTestCase);
      mWriter.flush();
      
      return true;
    }
    
    return false;
  }

  public void close() {
    mWriter.close();
  }
  
}
