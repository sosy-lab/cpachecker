package org.sosy_lab.cpachecker.core.algorithm;

import java.util.Map;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.LoopStructure;

public class LocateLoopAndLiveVariableAlgorithm implements Algorithm {
  private final CFA cfa;
  private final CProgramScope cProgramScope;

  public LocateLoopAndLiveVariableAlgorithm(CFA pCfa, LogManager pLogger) {
    if (!LoopStructure.getRecursions(pCfa).isEmpty()) {
      throw new IllegalArgumentException("Program should not have recursion!");
    } else if (pCfa.getLoopStructure().orElseThrow().getAllLoops().isEmpty()) {
      throw new IllegalArgumentException("Program must have loop!");
    }
    cfa = pCfa;
    cProgramScope = new CProgramScope(pCfa, pLogger);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'run'");
  }
}

record LoopInfo(int loopLocation, Map<String, String> liveVariablesAndTypes) {
  LoopInfo(int loopLocation, Map<String, String> liveVariablesAndTypes) {
    this.loopLocation = loopLocation;
    this.liveVariablesAndTypes = liveVariablesAndTypes;
  }
}