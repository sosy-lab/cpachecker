package org.sosy_lab.cpachecker.core.algorithm.tgar.interfaces;

import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.cpa.automaton.SafetyProperty;

import java.util.Set;

public interface TestificationOperator {

  void feasibleCounterexample(CounterexampleInfo pCounterexample, Set<SafetyProperty> pForProperties)
      throws InterruptedException;

}
