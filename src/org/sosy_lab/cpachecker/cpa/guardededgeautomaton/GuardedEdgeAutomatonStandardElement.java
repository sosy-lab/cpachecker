package org.sosy_lab.cpachecker.cpa.guardededgeautomaton;

import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;

public class GuardedEdgeAutomatonStandardElement extends GuardedEdgeAutomatonStateElement {

  public GuardedEdgeAutomatonStandardElement(NondeterministicFiniteAutomaton.State pState, boolean pIsFinalState) {
    super(pState, pIsFinalState);
  }

  public GuardedEdgeAutomatonStandardElement(GuardedEdgeAutomatonPredicateElement pElement) {
    super(pElement.getAutomatonState(), pElement.isFinalState());
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }

    if (pOther == null) {
      return false;
    }

    if (!pOther.getClass().equals(getClass())) {
      return false;
    }

    GuardedEdgeAutomatonStandardElement lOther = (GuardedEdgeAutomatonStandardElement)pOther;

    return (lOther.isFinalState() == isFinalState()) && lOther.getAutomatonState().equals(getAutomatonState());
  }

  @Override
  public int hashCode() {
    return getAutomatonState().hashCode() + 37239;
  }

  @Override
  public Object getPartitionKey() {
    return this;
  }

}
