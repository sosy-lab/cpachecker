package org.sosy_lab.cpachecker.fllesh.fql.fllesh.reachability;

import org.sosy_lab.cpachecker.fllesh.fql.backend.pathmonitor.Automaton;

public abstract class AbstractQuery implements Query {

  private Automaton mFirstAutomaton;
  private Automaton mSecondAutomaton;
  
  public AbstractQuery(Automaton pFirstAutomaton, Automaton pSecondAutomaton) {
    assert(pFirstAutomaton != null);
    assert(pSecondAutomaton != null);
    
    mFirstAutomaton = pFirstAutomaton;
    mSecondAutomaton = pSecondAutomaton;
  }
  
  @Override
  public Automaton getFirstAutomaton() {
    return mFirstAutomaton;
  }

  @Override
  public Automaton getSecondAutomaton() {
    return mSecondAutomaton;
  }

  @Override
  public abstract boolean hasNext();

  @Override
  public abstract Waypoint next();

  @Override
  public abstract void remove();
  
}
