package org.sosy_lab.cpachecker.fllesh.heuristics;

import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.fllesh.Goal;
import org.sosy_lab.cpachecker.fllesh.ecp.ECPEdgeSet;
import org.sosy_lab.cpachecker.fllesh.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.fllesh.util.Automaton;

import com.google.common.base.Preconditions;

public class GoalReordering {
  
  private static class WeightComparator implements Comparator<Goal> {

    private final Map<Goal, Integer> mWeights;
    
    public WeightComparator(Map<Goal, Integer> pWeights) {
      mWeights = pWeights;
    }
    
    @Override
    public int compare(Goal pO1, Goal pO2) {
      int lDifference = mWeights.get(pO1) - mWeights.get(pO2);
            
      return lDifference;
    }
    
  }
  
  public static Deque<Goal> reorder(Deque<Goal> pGoals) {
    Preconditions.checkNotNull(pGoals);
    
    HashMap<Goal, Integer> lWeights = new HashMap<Goal, Integer>();
    
    for (Goal lGoal : pGoals) {
      int lGoalWeight;
      
      //lGoalWeight = Integer.MAX_VALUE;
      lGoalWeight = 0;
      
      for (Automaton<GuardedEdgeLabel>.Edge lEdge : lGoal.getAutomaton().getEdges()) {
        GuardedEdgeLabel lLabel = lEdge.getLabel();
        
        if (lLabel.getClass().equals(GuardedEdgeLabel.class)) {
          // we can savely access the edge set
          ECPEdgeSet lEdgeSet = lLabel.getEdgeSet();
          
          if (lEdgeSet.size() == 1) {
            int lTopSortId = Integer.MAX_VALUE;
            
            for (CFAEdge lTmpCFAEdge : lEdgeSet) {
              lTopSortId = Math.min(lTmpCFAEdge.getPredecessor().getTopologicalSortId(), lTmpCFAEdge.getSuccessor().getTopologicalSortId());
            }
            
            //lGoalWeight = Math.min(lGoalWeight, lTopSortId);
            lGoalWeight = lGoalWeight + lTopSortId;
          }
        }
      }
      
      //lGoalWeight = (int)(Math.random() * 2 * pGoals.size());
      
      lWeights.put(lGoal, lGoalWeight);
    }
    
    PriorityQueue<Goal> lQueue = new PriorityQueue<Goal>(pGoals.size(), new WeightComparator(lWeights));
    lQueue.addAll(pGoals);
    
    if (lQueue.size() != pGoals.size()) {
      throw new RuntimeException();
    }
    
    return new LinkedList<Goal>(lQueue);
  }
  
}
