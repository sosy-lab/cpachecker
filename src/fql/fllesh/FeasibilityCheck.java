package fql.fllesh;

import java.util.LinkedList;

import fql.backend.pathmonitor.Automaton;
import fql.backend.targetgraph.Node;

public class FeasibilityCheck {
  public static Witness run(LinkedList<Automaton> pAutomatonSequence, LinkedList<Node> pWaypointSequence, Automaton pPassingMonitor, Node pInitialState) {
    
    System.out.println(pAutomatonSequence);
    System.out.println(pWaypointSequence);
    
    return new InfeasibilityWitness(pAutomatonSequence.size() - 1);
  }
}
