package fql.fllesh;

import fql.backend.pathmonitor.Automaton;
import fql.backend.targetgraph.Edge;
import fql.backend.targetgraph.Node;
import fql.backend.testgoals.EdgeSequence;
import fql.backend.testgoals.TestGoal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.List;

import common.Pair;

public class TestGoalEnumeration {
  
  public static Set<FeasibilityWitness> run(List<Pair<Automaton, Set<? extends TestGoal>>> pCoverageSequence, Automaton pPassingMonitor, Node pInitialState) {
    assert(pCoverageSequence != null);
    
    HashSet<FeasibilityWitness> lWitnesses = new HashSet<FeasibilityWitness>();
    
    int lLength = pCoverageSequence.size();
    
    ArrayList<Set<? extends TestGoal>> lTargetSets = new ArrayList<Set<? extends TestGoal>>();
    ArrayList<Automaton> lAutomata = new ArrayList<Automaton>();
    
    LinkedList<Automaton> lAutomatonSequence = new LinkedList<Automaton>();
    LinkedList<Node> lWaypointSequence = new LinkedList<Node>();
    
    ArrayList<Iterator<? extends TestGoal>> lIterators = new ArrayList<Iterator<? extends TestGoal>>();
    
    int[] lEntries = new int[lLength];
    
    int lIndex = 0;
    
    for (Pair<Automaton, Set<? extends TestGoal>> lPair : pCoverageSequence) {
      lIterators.add(lPair.getSecond().iterator());
      
      lTargetSets.add(lPair.getSecond());
      lAutomata.add(lPair.getFirst());
      
      lEntries[lIndex] = 0;
      
      lIndex++;
    }
    
    lIndex = 0;
    
    while (lIndex >= 0) {
      
      while (lEntries[lIndex] > 0) {
        lAutomatonSequence.removeLast();
        lWaypointSequence.removeLast();
        
        lEntries[lIndex]--;
      }
      
      if (!lIterators.get(lIndex).hasNext()) {
        lIterators.set(lIndex, lTargetSets.get(lIndex).iterator());
        lIndex--;
      }
      else {
        TestGoal lGoal = lIterators.get(lIndex).next();
        
        if (lGoal instanceof Node) {
          lAutomatonSequence.add(lAutomata.get(lIndex));
          lWaypointSequence.add((Node)lGoal);
          lEntries[lIndex] = 1;
        }
        else {
          Node lFirstNode;
          Node lLastNode;
          
          Automaton lTargetAutomaton;
          
          if (lGoal instanceof Edge) {
            Edge lEdge = (Edge)lGoal;
            lTargetAutomaton = Automaton.create(lEdge);
            lFirstNode = lEdge.getSource();
            lLastNode = lEdge.getTarget();
          }
          else {
            EdgeSequence lEdgeSequence = (EdgeSequence)lGoal;
            lTargetAutomaton = Automaton.create(lEdgeSequence);
            lFirstNode = lEdgeSequence.getStartNode();
            lLastNode = lEdgeSequence.getEndNode();
          }
          
          lAutomatonSequence.add(lAutomata.get(lIndex));
          lWaypointSequence.add(lFirstNode);
          
          lAutomatonSequence.add(lTargetAutomaton);
          lWaypointSequence.add(lLastNode);
          
          lEntries[lIndex] = 2;
        }
        
        if (lIndex < lLength - 1) {
          lIndex++;
        }
        else {
          Witness lWitness = FeasibilityCheck.run(lAutomatonSequence, lWaypointSequence, pPassingMonitor, pInitialState);
          
          if (lWitness instanceof InfeasibilityWitness) {
            InfeasibilityWitness lInfeasibilityWitness = (InfeasibilityWitness)lWitness;
            
            int lBacktrackLevel = determineBacktrackLevel(lInfeasibilityWitness, lEntries);
            
            // TODO: remove output
            System.out.println("lLength: " + lLength);
            System.out.println("lAutomatonSequence.size(): " + lAutomatonSequence.size());
            System.out.println("lInfeasibilityWitness.getBacktrackIndex(): " + lInfeasibilityWitness.getBacktrackIndex());
            System.out.println("lBacktrackLevel: " + lBacktrackLevel);
            
            for (int i = lBacktrackLevel; i < lLength; i++) {
              Set<? extends TestGoal> lEmptySet = Collections.emptySet();
              lIterators.set(i, lEmptySet.iterator());
            }
          }
          else {
            FeasibilityWitness lFeasibilityWitness = (FeasibilityWitness)lWitness;
            
            lWitnesses.add(lFeasibilityWitness);
          }
        }
        
      }
      
    }
    
    return lWitnesses;
  }
  
  private static int determineBacktrackLevel(InfeasibilityWitness pWitness, int[] pEntries) {
    assert(pWitness != null);
    assert(pEntries != null);
    
    int lSum = 0;
    
    for (int lIndex = 0; lIndex < pEntries.length; lIndex++) {
      lSum += pEntries[lIndex];
    }
    
    // normalize to sum
    int lBacktrackIndex = pWitness.getBacktrackIndex() + 1;
    
    int lBacktrackLevel = pEntries.length - 1;
    
    while (lSum > lBacktrackIndex) {
      lSum -= pEntries[lBacktrackLevel];
      lBacktrackLevel--;
    }
    
    return lBacktrackLevel;
  }
  
}
