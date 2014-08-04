/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.algorithm.tiger.goals.heuristics;

import java.util.Comparator;
import java.util.Deque;
import java.util.Map;

import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;

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
    throw new UnsupportedOperationException("Implement reordering!!!");

    /*
    Preconditions.checkNotNull(pGoals);

    HashMap<Goal, Integer> lWeights = new HashMap<>();

    for (Goal lGoal : pGoals) {
      int lGoalWeight;

      //lGoalWeight = Integer.MAX_VALUE;
      lGoalWeight = 0;

      for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lEdge : lGoal.getAutomaton().getEdges()) {
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

    PriorityQueue<Goal> lQueue = new PriorityQueue<>(pGoals.size(), new WeightComparator(lWeights));
    lQueue.addAll(pGoals);

    if (lQueue.size() != pGoals.size()) {
      throw new RuntimeException();
    }

    return new LinkedList<>(lQueue);
    */
  }

}
