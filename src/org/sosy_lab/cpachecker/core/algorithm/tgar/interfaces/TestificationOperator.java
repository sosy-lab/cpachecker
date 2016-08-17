package org.sosy_lab.cpachecker.core.algorithm.tgar.interfaces;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.cpa.automaton.SafetyProperty;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;

import java.util.List;
import java.util.Set;

public interface TestificationOperator {

  void feasibleCounterexample(CounterexampleInfo pCounterexample, Set<SafetyProperty> pForProperties)
      throws InterruptedException;
}
