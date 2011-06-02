package org.sosy_lab.cpachecker.fshell.interfaces;

import org.sosy_lab.cpachecker.fshell.FShell3Result;

public interface FQLTestGenerator {

  public FShell3Result run(String pFQLSpecification, boolean pApplySubsumptionCheck, boolean pApplyInfeasibilityPropagation, boolean pGenerateTestGoalAutomataInAdvance, boolean pCheckCorrectnessOfCoverageCheck, boolean pPedantic, boolean pAlternating);

}
