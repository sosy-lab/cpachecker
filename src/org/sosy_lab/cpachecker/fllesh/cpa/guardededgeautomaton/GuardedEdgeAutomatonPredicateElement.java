package org.sosy_lab.cpachecker.fllesh.cpa.guardededgeautomaton;

import java.util.Iterator;
import java.util.List;

import org.sosy_lab.cpachecker.util.ecp.ECPPredicate;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;

public class GuardedEdgeAutomatonPredicateElement extends GuardedEdgeAutomatonStateElement implements Iterable<ECPPredicate> {

  private final List<ECPPredicate> mPredicates;
  private final GuardedEdgeAutomatonStandardElement mStandardElement;
  
  public GuardedEdgeAutomatonPredicateElement(NondeterministicFiniteAutomaton.State pState, List<ECPPredicate> pPredicates, boolean pIsFinalState) {
    super(pState, pIsFinalState);
    mStandardElement = new GuardedEdgeAutomatonStandardElement(this);
    mPredicates = pPredicates;
  }
  
  public GuardedEdgeAutomatonStandardElement getStandardElement() {
    return mStandardElement;
  }

  @Override
  public Iterator<ECPPredicate> iterator() {
    return mPredicates.iterator();
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
    
    GuardedEdgeAutomatonPredicateElement lOther = (GuardedEdgeAutomatonPredicateElement)pOther;
    
    return (lOther.isFinalState() == isFinalState()) && lOther.getAutomatonState().equals(getAutomatonState()) && lOther.mPredicates.equals(mPredicates);
  }
  
  @Override
  public int hashCode() {
    return getAutomatonState().hashCode() + mPredicates.hashCode() + 3459;
  }
  
  @Override
  public String toString() {
    return super.toString() + mPredicates.toString();
  }

  @Override
  public Object getPartitionKey() {
    return this;
  }

}
