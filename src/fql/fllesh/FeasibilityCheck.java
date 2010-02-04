package fql.fllesh;

import java.util.LinkedList;
import java.util.Set;

import compositeCPA.CompositeElement;

import fql.backend.pathmonitor.Automaton;
import fql.backend.targetgraph.Node;
import fql.fllesh.reachability.Query;
import fql.fllesh.reachability.SingletonQuery;
import fql.fllesh.reachability.StandardQuery;
import fql.fllesh.reachability.Waypoint;

public class FeasibilityCheck {
  public static Witness run(LinkedList<Automaton> pAutomatonSequence, LinkedList<Node> pWaypointSequence, Automaton pPassingMonitor, Node pInitialState) {
    
    assert(pAutomatonSequence != null);
    assert(pWaypointSequence != null);
    assert(pPassingMonitor != null);
    assert(pInitialState != null);
    assert(pAutomatonSequence.size() == pWaypointSequence.size());
    
    
    // TODO remove output
    System.out.println(pAutomatonSequence);
    System.out.println(pWaypointSequence);
    
    
    LinkedList<Query> lQueries = new LinkedList<Query>();
    LinkedList<Waypoint> lWaypoints = new LinkedList<Waypoint>();
    
    int lMaxIndex = 0;
    
    int lLastIndex = pAutomatonSequence.size() + 1;
    
    
    // TODO: wir brauchen hier ein CompositeElement (mit CompositePrecision?)
    // TODO: CompositeCPA anlegen ?
    // TODO: remove nulls
    CompositeElement lInitialElement = createInitialElement(pInitialState);
    Automaton lFirstAutomaton = pAutomatonSequence.getFirst();
    Query lInitialQuery = SingletonQuery.create(lInitialElement, lFirstAutomaton, lFirstAutomaton.getInitialStates(), pPassingMonitor, pPassingMonitor.getInitialStates());
    lQueries.add(lInitialQuery);
    
    while (!lQueries.isEmpty()) {
      Query lQuery = lQueries.getLast();
      
      if (lQuery.hasNext()) {
        Waypoint lWaypoint = lQuery.next();
        
        lWaypoints.addLast(lWaypoint);
        
        if (lQueries.size() == lLastIndex) {
          return generateWitness(lWaypoints);
        }
        else {
          
          // check and update backtrack level
          if (lQueries.size() > lMaxIndex) {
            lMaxIndex = lQueries.size();
          }
          
          Set<Integer> lFinalStates;
          
          if (lQueries.size() + 1 == lLastIndex) {
            lFinalStates = pPassingMonitor.getFinalStates();
          }
          else {
            lFinalStates = pPassingMonitor.getStates();
          }
          
          int lQueryIndex = lQueries.size() - 1;
          
          CompositeElement lNextElement = createNextElement(pWaypointSequence.get(lQueryIndex));
          
          Automaton lNextAutomaton = pAutomatonSequence.get(lQueryIndex);
          
          Query lNextQuery = StandardQuery.create(lNextAutomaton, pPassingMonitor, lWaypoint.getElement(), lNextAutomaton.getInitialStates(), lWaypoint.getStatesOfSecondAutomaton(), lNextElement, lNextAutomaton.getFinalStates(), lFinalStates);
          
          lQueries.addLast(lNextQuery);
        }

      }
      else {
        lQueries.removeLast();        
      }
    }
    
    return new InfeasibilityWitness(lMaxIndex);
  }
  
  private static CompositeElement createInitialElement(Node pInitialNode) {
    assert(pInitialNode != null);
    
    // TODO: implement
    
    return null;
  }
  
  private static CompositeElement createNextElement(Node pNextNode) {
    assert(pNextNode != null);
    
    // TODO: implement
    
    return null;
  }
  
  private static FeasibilityWitness generateWitness(LinkedList<Waypoint> lWaypoints) {
    assert(lWaypoints != null);
    
    // TODO implement
    
    return new FeasibilityWitness();
  }
}
