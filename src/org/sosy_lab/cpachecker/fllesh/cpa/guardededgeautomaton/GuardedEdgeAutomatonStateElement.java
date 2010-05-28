package org.sosy_lab.cpachecker.fllesh.cpa.guardededgeautomaton;

import java.util.ArrayList;

import org.sosy_lab.cpachecker.fllesh.ecp.ECPGuard;
import org.sosy_lab.cpachecker.fllesh.ecp.ECPPredicate;
import org.sosy_lab.cpachecker.fllesh.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.fllesh.util.Automaton;

public abstract class GuardedEdgeAutomatonStateElement implements
    GuardedEdgeAutomatonElement {

  private Automaton<GuardedEdgeLabel>.State mAutomatonState;
  private boolean mIsFinalState;
  protected String mStringRepresentation;
  
  public GuardedEdgeAutomatonStateElement(Automaton<GuardedEdgeLabel>.State pState, boolean pIsFinalState, String pStringRepresentation) {
    mAutomatonState = pState;
    mIsFinalState = pIsFinalState;
    mStringRepresentation = pStringRepresentation;
  }
  
  public boolean isFinalState() {
    return mIsFinalState;
  }
  
  public Automaton<GuardedEdgeLabel>.State getAutomatonState() {
    return mAutomatonState;
  }
  
  public static GuardedEdgeAutomatonStateElement create(Automaton<GuardedEdgeLabel>.Edge pEdge, Automaton<GuardedEdgeLabel> pAutomaton, String pStringRepresentation) {
    Automaton<GuardedEdgeLabel>.State lAutomatonState = pEdge.getTarget();
    
    GuardedEdgeLabel lLabel = pEdge.getLabel();
    
    boolean lIsFinalState = pAutomaton.getFinalStates().contains(lAutomatonState);
    
    if (lLabel.hasGuards()) {
      ArrayList<ECPPredicate> lPredicates = new ArrayList<ECPPredicate>(lLabel.getNumberOfGuards());
      
      for (ECPGuard lGuard : lLabel) {
        assert(lGuard instanceof ECPPredicate);
        
        lPredicates.add((ECPPredicate)lGuard);
      }
      
      return new GuardedEdgeAutomatonPredicateElement(lAutomatonState, lPredicates, lIsFinalState, pStringRepresentation);
    }
    else {
      return new GuardedEdgeAutomatonStandardElement(lAutomatonState, lIsFinalState, pStringRepresentation);
    }
  }
  
  @Override
  public boolean isError() {
    return false;
  }
  
  @Override
  public String toString() {
    return mStringRepresentation;
  }

}
