package fql.fllesh.reachability;

import java.util.Set;

import compositeCPA.CompositeElement;

import fql.backend.pathmonitor.Automaton;

public class StandardQuery extends AbstractQuery {

  public static StandardQuery create(Automaton pFirstAutomaton, Automaton pSecondAutomaton, CompositeElement pSourceElement, Set<Integer> pSourceStatesOfFirstAutomaton, Set<Integer> pSourceStatesOfSecondAutomaton, CompositeElement pTargetElement, Set<Integer> pTargetStatesOfFirstAutomaton, Set<Integer> pTargetStatesOfSecondAutomaton) {
    StandardQuery lQuery = new StandardQuery(pFirstAutomaton, pSecondAutomaton);
    
    Waypoint lSource = new Waypoint(lQuery, pSourceElement, pSourceStatesOfFirstAutomaton, pSourceStatesOfSecondAutomaton);
    Waypoint lTarget = new Waypoint(lQuery, pTargetElement, pTargetStatesOfFirstAutomaton, pTargetStatesOfSecondAutomaton);
    
    lQuery.mSource = lSource;
    lQuery.mTarget = lTarget;
    
    return lQuery;
  }
  
  private Waypoint mSource;
  private Waypoint mTarget;
  
  private StandardQuery(Automaton pFirstAutomaton, Automaton pSecondAutomaton) {
    super(pFirstAutomaton, pSecondAutomaton);
    
  }
  
  public Waypoint getSource() {
    return mSource;
  }
  
  public Waypoint getTarget() {
    return mTarget;
  }
  
  @Override
  public boolean hasNext() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Waypoint next() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("remove is not supported!");
  }

}
