package org.sosy_lab.cpachecker.fshell.interfaces;

import java.util.Collection;

import org.sosy_lab.cpachecker.fshell.testcases.TestCase;

public interface FQLCoverageAnalyser {

  public void checkCoverage(String pFQLSpecification, Collection<TestCase> pTestSuite, boolean pPedantic);

}
