package org.sosy_lab.cpachecker.fllesh.testcases;

public class ImpreciseExecutionException extends Exception {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private final ImpreciseExecutionTestCase mTestCase;
  
  public ImpreciseExecutionException(TestCase pTestCase) {
    super();
    mTestCase = new ImpreciseExecutionTestCase(pTestCase);
  }
  
  public ImpreciseExecutionTestCase getTestCase() {
    return mTestCase;
  }
  
}
