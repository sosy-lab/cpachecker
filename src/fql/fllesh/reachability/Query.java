package fql.fllesh.reachability;

import java.util.Iterator;

import fql.backend.pathmonitor.Automaton;

public interface Query extends Iterator<Waypoint> {
  
  public Automaton getFirstAutomaton();
  public Automaton getSecondAutomaton();
  
}
