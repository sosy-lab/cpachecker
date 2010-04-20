package fllesh.fql.fllesh.reachability;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

import cfa.objectmodel.CFANode;
import fllesh.fql.backend.pathmonitor.Automaton;
import fllesh.fql.fllesh.cpa.QueryStandardElement;

/*
 * A target point describes the target of a reachability query.
 * TODO implement predicate handling
 * TODO implement source point coverage
 */
public class TargetPoint {
  
  private CFANode mCFANode;
  private Automaton mFirstAutomaton;
  private ImmutableSet<Integer> mStatesOfFirstAutomaton;
  private Automaton mSecondAutomaton;
  private ImmutableSet<Integer> mStatesOfSecondAutomaton;
  
  public TargetPoint(CFANode pCFANode, Automaton pFirstAutomaton, Set<Integer> pStatesOfFirstAutomaton, Automaton pSecondAutomaton, Set<Integer> pStatesOfSecondAutomaton) {
    assert(pCFANode != null);
    assert(pFirstAutomaton != null);
    assert(pStatesOfFirstAutomaton != null);
    assert(pSecondAutomaton != null);
    assert(pStatesOfSecondAutomaton != null);
    
    mCFANode = pCFANode;
    mFirstAutomaton = pFirstAutomaton;
    mStatesOfFirstAutomaton = ImmutableSet.copyOf(pStatesOfFirstAutomaton);
    mSecondAutomaton = pSecondAutomaton;
    mStatesOfSecondAutomaton = ImmutableSet.copyOf(pStatesOfSecondAutomaton);
  }
  
  public CFANode getCFANode() {
    return mCFANode;
  }
  
  public Automaton getFirstAutomaton() {
    return mFirstAutomaton;
  }
  
  public ImmutableSet<Integer> getStatesOfFirstAutomaton() {
    return mStatesOfFirstAutomaton;
  }
  
  public Automaton getSecondAutomaton() {
    return mSecondAutomaton;
  }
  
  public ImmutableSet<Integer> getStatesOfSecondAutomaton() {
    return mStatesOfSecondAutomaton;
  }
  
  public boolean satisfiesTarget(QueryStandardElement pElement) {
    assert(pElement != null);
    
    Integer lState1 = pElement.getAutomatonState1();
    
    if (mStatesOfFirstAutomaton.contains(lState1)) {
      Integer lState2 = pElement.getAutomatonState2();
      
      return mStatesOfSecondAutomaton.contains(lState2);
    }
    
    return false;
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (pOther.getClass() == getClass()) {
      TargetPoint lTargetPoint = (TargetPoint)pOther;

      return lTargetPoint.mCFANode.equals(mCFANode) 
          && lTargetPoint.mFirstAutomaton.equals(mFirstAutomaton)
          && lTargetPoint.mStatesOfFirstAutomaton.equals(mStatesOfFirstAutomaton)
          && lTargetPoint.mSecondAutomaton.equals(mSecondAutomaton)
          && lTargetPoint.mStatesOfSecondAutomaton.equals(mStatesOfSecondAutomaton);
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 7877 + mCFANode.hashCode() + mFirstAutomaton.hashCode() + mStatesOfFirstAutomaton.hashCode() + mSecondAutomaton.hashCode() + mStatesOfSecondAutomaton.hashCode();
  }
  
  @Override
  public String toString() {
    return "(" + mCFANode.toString() + ", " + mStatesOfFirstAutomaton.toString() + ", " + mStatesOfSecondAutomaton.toString() + ")";
  }
  
}
