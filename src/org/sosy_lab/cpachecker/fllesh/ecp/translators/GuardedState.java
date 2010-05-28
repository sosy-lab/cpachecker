package org.sosy_lab.cpachecker.fllesh.ecp.translators;

import org.sosy_lab.cpachecker.fllesh.ecp.ECPGuard;
import org.sosy_lab.cpachecker.fllesh.util.Automaton;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class GuardedState {

  private Automaton<GuardedLabel>.State mState;
  private Set<ECPGuard> mGuards;
  
  public GuardedState(Automaton<GuardedLabel>.State pCurrentState) {
    mState = pCurrentState;
    mGuards = Collections.emptySet();
  }
  
  public GuardedState(Automaton<GuardedLabel>.State pState, Set<ECPGuard> pGuards) {
    mState = pState;
    mGuards = pGuards;
  }
  
  public GuardedState(Automaton<GuardedLabel>.State pState, GuardedState pPreceedingState, Set<ECPGuard> pGuards) {
    mState = pState;
    mGuards = new HashSet<ECPGuard>();
    mGuards.addAll(pPreceedingState.mGuards);
    mGuards.addAll(pGuards);
  }
  
  public Automaton<GuardedLabel>.State getState() {
    return mState;
  }
  
  public Set<ECPGuard> getGuards() {
    return mGuards;
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (pOther.getClass().equals(getClass())) {
      GuardedState lGuardedState = (GuardedState)pOther;
      
      return mState.equals(lGuardedState.mState) && mGuards.equals(lGuardedState.mGuards);
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return mState.hashCode() + mGuards.hashCode() + 870890;
  }
  
  @Override
  public String toString() {
    return "(" + mState.toString() + ", " + mGuards.toString() + ")";
  }
  
  public boolean covers(GuardedState pState) {
    if (!mState.equals(pState.mState)) {
      return false;
    }
    
    return pState.mGuards.containsAll(mGuards);
  }
  
}
