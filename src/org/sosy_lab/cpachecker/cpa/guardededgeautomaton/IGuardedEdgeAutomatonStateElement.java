package org.sosy_lab.cpachecker.cpa.guardededgeautomaton;

import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;

public interface IGuardedEdgeAutomatonStateElement {

  public boolean isFinalState();
  public NondeterministicFiniteAutomaton.State getAutomatonState();

}
