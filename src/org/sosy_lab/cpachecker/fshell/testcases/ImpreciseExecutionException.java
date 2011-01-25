package org.sosy_lab.cpachecker.fshell.testcases;

import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonCPA;

public class ImpreciseExecutionException extends Exception {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private final ImpreciseExecutionTestCase mTestCase;
  private final GuardedEdgeAutomatonCPA mCoverCPA;
  private final GuardedEdgeAutomatonCPA mPassingCPA;
  
  public ImpreciseExecutionException(TestCase pTestCase, GuardedEdgeAutomatonCPA pCoverCPA, GuardedEdgeAutomatonCPA pPassingCPA) {
    super();
    mTestCase = new ImpreciseExecutionTestCase(pTestCase);
    
    mCoverCPA = pCoverCPA;
    mPassingCPA = pPassingCPA;
  }
  
  public ImpreciseExecutionTestCase getTestCase() {
    return mTestCase;
  }
  
  public GuardedEdgeAutomatonCPA getCoverCPA() {
    return mCoverCPA;
  }
  
  public GuardedEdgeAutomatonCPA getPassingCPA() {
    return mPassingCPA;
  }
  
  @Override
  public String toString() {
    return "The test case " + mTestCase.toString() + " causes a wrong execution.\n" + mCoverCPA.toString() + ((mPassingCPA == null)?"":("\n" + mPassingCPA.toString()));
  }
  
}
