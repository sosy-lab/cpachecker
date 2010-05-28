package org.sosy_lab.cpachecker.fllesh.cpa.guardededgeautomaton;

import org.sosy_lab.cpachecker.fllesh.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.fllesh.util.Automaton;

public class GuardedEdgeAutomatonStandardElement extends GuardedEdgeAutomatonStateElement {

  public GuardedEdgeAutomatonStandardElement(Automaton<GuardedEdgeLabel>.State pState, boolean pIsFinalState, String pStringRepresentation) {
    super(pState, pIsFinalState, pStringRepresentation);
  }
  
  public GuardedEdgeAutomatonStandardElement(GuardedEdgeAutomatonPredicateElement pElement) {
    super(pElement.getAutomatonState(), pElement.isFinalState(), pElement.mStringRepresentation);
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
  
}
