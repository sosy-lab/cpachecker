package org.sosy_lab.cpachecker.fllesh.fql.fllesh.reachability;

import java.util.Iterator;

import org.sosy_lab.cpachecker.fllesh.fql.backend.pathmonitor.Automaton;

public interface Query extends Iterator<Waypoint> {
  
  public Automaton getFirstAutomaton();
  public Automaton getSecondAutomaton();
  
}
