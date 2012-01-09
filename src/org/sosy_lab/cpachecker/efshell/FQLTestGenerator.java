package org.sosy_lab.cpachecker.efshell;

import org.sosy_lab.cpachecker.fshell.testcases.TestCase;

public interface FQLTestGenerator {

  public FShell3Result run(String pFQLSpecification, boolean pApplySubsumptionCheck, boolean pApplyInfeasibilityPropagation, boolean pGenerateTestGoalAutomataInAdvance, boolean pCheckCorrectnessOfCoverageCheck, boolean pPedantic, boolean pAlternating,TestCase pTestCase);

}
