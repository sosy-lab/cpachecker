/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.multigoal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.location.LocationTransferRelation.WeavingType;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;

public class MutliGoalTransferRelation extends SingleEdgeTransferRelation {
  private Set<CFAEdgesGoal> goals;
  private Set<CFAEdgesGoal> coveredGoals;

  MutliGoalTransferRelation() {
    coveredGoals = new HashSet<>();
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      final AbstractState pState,
      final Precision pPrecision,
      final CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {

    MultiGoalState predState = (MultiGoalState) pState;
    MultiGoalState successor = new MultiGoalState(predState);
    if (predState.isInitialState()) {
      initializeWeavingForAllGoals(successor);
    }
    // if edge is weaved, it needs to be removed from weaved edges instead of processing the edge
    if (successor.getWeavedEdges().contains(pCfaEdge)) {
      successor.removeWeavedEdge(pCfaEdge);
      return Collections.singleton(successor);
    } else {
      return processEdge(successor, pCfaEdge);
    }
  }

  private Collection<MultiGoalState>
      processEdge(MultiGoalState successor, final CFAEdge pCfaEdge) {
    Map<CFAEdgesGoal, Integer> succGoals = successor.getGoals();
    HashSet<MultiGoalState> succs = new HashSet<>();
    HashSet<CFAEdgesGoal> finishedGoals = new HashSet<>();
    for (CFAEdgesGoal goal : goals) {
      int index = 0;
      if (succGoals.containsKey(goal)) {
        index = succGoals.get(goal);
      }
      if (goal.acceptsEdge(pCfaEdge, index)) {
        index++;
        successor.putGoal(goal, index);
        if (goal.getEdges().size() > 1) {
          successor.addWeavingEdge(pCfaEdge, WeavingType.ASSIGNMENT);
          if (index >= goal.getEdges().size()) {
            finishedGoals.add(goal);
          }
        }
      }
    }


    Iterator<CFAEdgesGoal> iter = finishedGoals.iterator();
    if (!iter.hasNext()) {
      succs.add(successor);
    }
    // make sure each successor only covers 1 goal
    // otherwise weaving will break
    // use sucessor to create the new states, but do not include sucessor in returned states
    while (iter.hasNext()) {
      MultiGoalState newSuccessor = new MultiGoalState(successor);
      for (Pair<CFAEdge, WeavingType> weaveEdge : successor.getEdgesToWeave()) {
        newSuccessor.addWeavingEdge(weaveEdge.getFirst(), weaveEdge.getSecond());
      }
      CFAEdgesGoal goal = iter.next();
      newSuccessor.removeGoals(finishedGoals);
      newSuccessor.putGoal(goal, goal.getEdges().size());
      succs.add(newSuccessor);
      for (CFAEdge cfaEdge : goal.getEdges()) {
        newSuccessor.addWeavingEdge(cfaEdge, WeavingType.ASSUMPTION);
      }
    }
    return succs;
  }


  public void initializeWeavingForAllGoals(MultiGoalState successor) {
    for (CFAEdgesGoal goal : goals) {
      // no need to weave goal consists of only 1 edge
      if (goal.getEdges().size() > 1) {
        for (CFAEdge edge : goal.getEdges()) {
          successor.addWeavingEdge(edge, WeavingType.DECLARATION);
        }
      }
    }
  }

  public Set<CFAEdgesGoal> getGoals() {
    return goals;
  }


  public void setGoals(Set<CFAEdgesGoal> pGoals) {
    assert goals == null;
    goals = pGoals;
  }

  public void addCoveredGoal(CFAEdgesGoal pGoal) {
    coveredGoals.add(pGoal);
    goals.remove(pGoal);
  }

}
