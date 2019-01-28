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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
    LinkedHashSet<Pair<CFAEdge, WeavingType>> weavingEdges = null;
    if (predState.isInitialState()) {
     weavingEdges = initializeWeavingForAllGoals();
    }
    // if edge is weaved, it needs to be removed from weaved edges instead of processing the edge
    Set<CFAEdge> weavedEdges = new HashSet<>(predState.getWeavedEdges());
    if (weavedEdges.contains(pCfaEdge)) {
      weavedEdges.remove(pCfaEdge);
      return Collections
          .singleton(new MultiGoalState(predState.getGoals(), weavingEdges, weavedEdges));
    } else {
      return processEdge(predState, pCfaEdge, weavingEdges);
    }
  }

  private Collection<MultiGoalState>
      processEdge(
          MultiGoalState predState,
          final CFAEdge pCfaEdge,
          LinkedHashSet<Pair<CFAEdge, WeavingType>> edgesToWeave) {
    Map<CFAEdgesGoal, Integer> succGoals = new HashMap<>(predState.getGoals());
    HashSet<MultiGoalState> succs = new HashSet<>();
    HashSet<CFAEdgesGoal> finishedGoals = new HashSet<>();
    if (edgesToWeave == null) {
      edgesToWeave = new LinkedHashSet<>();
    }
    for (CFAEdgesGoal goal : goals) {
      int index = 0;
      if (succGoals.containsKey(goal)) {
        index = succGoals.get(goal);
      }
      if (goal.acceptsEdge(pCfaEdge, index)) {
        index++;
        succGoals.put(goal, index);
        if (goal.getEdges().size() > 1) {
          edgesToWeave.add(Pair.of(pCfaEdge, WeavingType.ASSIGNMENT));
          if (index >= goal.getEdges().size()) {
            finishedGoals.add(goal);
          }
        }
      }
    }


    Iterator<CFAEdgesGoal> iter = finishedGoals.iterator();
    if (!iter.hasNext()) {
      return Collections
          .singleton(new MultiGoalState(succGoals, edgesToWeave, predState.getWeavedEdges()));
    }
    // make sure each successor only covers 1 goal
    // otherwise weaving will break
    // use successor to create the new states, but do not include successor in returned states
    while (iter.hasNext()) {
      CFAEdgesGoal goal = iter.next();
      Map<CFAEdgesGoal, Integer> successorGoals = new HashMap<>(succGoals);
      successorGoals.keySet().removeAll(finishedGoals);
      successorGoals.put(goal, goal.getEdges().size());
      LinkedHashSet<Pair<CFAEdge, WeavingType>> newEdgesToWeave = new LinkedHashSet<>(edgesToWeave);
      for (CFAEdge cfaEdge : goal.getEdges()) {
        newEdgesToWeave.add(Pair.of(cfaEdge, WeavingType.ASSUMPTION));
      }
      succs.add(new MultiGoalState(successorGoals, newEdgesToWeave, predState.getWeavedEdges()));

    }
    return succs;
  }


  public LinkedHashSet<Pair<CFAEdge, WeavingType>> initializeWeavingForAllGoals() {
    LinkedHashSet<Pair<CFAEdge, WeavingType>> weavingEdges = new LinkedHashSet<>();
    for (CFAEdgesGoal goal : goals) {
      // no need to weave goal consists of only 1 edge
      if (goal.getEdges().size() > 1) {
        for (CFAEdge edge : goal.getEdges()) {
          weavingEdges.add(Pair.of(edge, WeavingType.DECLARATION));
        }
      }
    }
    return weavingEdges;
  }

  public Set<CFAEdgesGoal> getGoals() {
    return goals;
  }


  public void setGoals(Set<CFAEdgesGoal> pGoals) {
    goals = pGoals;
  }

  public void addCoveredGoal(CFAEdgesGoal pGoal) {
    coveredGoals.add(pGoal);
    goals.remove(pGoal);
  }

}
