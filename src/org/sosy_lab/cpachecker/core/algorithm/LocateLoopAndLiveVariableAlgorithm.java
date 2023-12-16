package org.sosy_lab.cpachecker.core.algorithm;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class LocateLoopAndLiveVariableAlgorithm implements Algorithm {
  private final CFA cfa;
  private final CProgramScope cProgramScope;

  public LocateLoopAndLiveVariableAlgorithm(CFA pCfa, LogManager pLogger) {
    cfa = pCfa;
    cProgramScope = new CProgramScope(pCfa, pLogger);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'run'");
  }
}