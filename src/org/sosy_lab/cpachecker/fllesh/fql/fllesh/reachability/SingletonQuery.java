package org.sosy_lab.cpachecker.fllesh.fql.fllesh.reachability;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.sosy_lab.cpachecker.cpa.composite.CompositeElement;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;

import org.sosy_lab.cpachecker.fllesh.fql.backend.pathmonitor.Automaton;

public class SingletonQuery extends AbstractQuery {
  Iterator<Waypoint> mIterator;
  
  public static SingletonQuery create(CompositeElement pElement, CompositePrecision pPrecision, Automaton pFirstAutomaton, Set<Integer> pStatesOfFirstAutomaton, Automaton pSecondAutomaton, Set<Integer> pStatesOfSecondAutomaton) {
    assert(pElement != null);
    assert(pPrecision != null);
    
    SingletonQuery lQuery = new SingletonQuery(pFirstAutomaton, pSecondAutomaton);
    
    Waypoint lWaypoint = new Waypoint(lQuery, pElement, pPrecision, pStatesOfFirstAutomaton, pStatesOfSecondAutomaton);
    
    lQuery.mIterator = Collections.singleton(lWaypoint).iterator();
    
    return lQuery;
  }
  
  private SingletonQuery(Automaton pFirstAutomaton, Automaton pSecondAutomaton) {
    super(pFirstAutomaton, pSecondAutomaton);
  }
  
  @Override
  public boolean hasNext() {
    return mIterator.hasNext();
  }

  @Override
  public Waypoint next() {
    return mIterator.next();   
  }

  @Override
  public void remove() {
    mIterator.remove();
  }

}
