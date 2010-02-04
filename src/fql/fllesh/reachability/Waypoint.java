package fql.fllesh.reachability;

import java.util.Set;

import compositeCPA.CompositeElement;

public class Waypoint {
  private Query mQuery;
  
  private CompositeElement mCompositeElement;
  
  private Set<Integer> mStatesOfFirstAutomaton;
  private Set<Integer> mStatesOfSecondAutomaton;
  
  public Waypoint(Query pQuery, CompositeElement pCompositeElement, Set<Integer> pStatesOfFirstAutomaton, Set<Integer> pStatesOfSecondAutomaton) {
    assert(pQuery != null);
    // TODO: add assertion
    
    mQuery = pQuery;
    mCompositeElement = pCompositeElement;
    mStatesOfFirstAutomaton = pStatesOfFirstAutomaton;
    mStatesOfSecondAutomaton = pStatesOfSecondAutomaton;
  }
  
  public Query getQuery() {
    return mQuery;
  }
  
  public CompositeElement getElement() {
    return mCompositeElement;
  }
  
  public Set<Integer> getStatesOfFirstAutomaton() {
    return mStatesOfFirstAutomaton;
  }
  
  public Set<Integer> getStatesOfSecondAutomaton() {
    return mStatesOfSecondAutomaton;
  }
  
  @Override
  public String toString() {
    return "(" + mCompositeElement.toString() + ", " + mStatesOfFirstAutomaton.toString() + ", " + mStatesOfSecondAutomaton.toString() + ")";
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
      Waypoint lWaypoint = (Waypoint)pOther;
      
      return lWaypoint.mQuery.equals(mQuery) &&
              lWaypoint.mStatesOfFirstAutomaton.equals(mStatesOfFirstAutomaton) &&
              lWaypoint.mStatesOfSecondAutomaton.equals(mStatesOfSecondAutomaton);
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 234223 + mQuery.hashCode() + mCompositeElement.hashCode() + mStatesOfFirstAutomaton.hashCode() + mStatesOfSecondAutomaton.hashCode();
  }
  
}
