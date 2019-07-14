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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.location.WeavingType;
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
    HashMap<CFAEdgesGoal, Set<Set<CFAEdge>>> unlockedNegatedEdges = null;
    if (predState.isInitialState()) {
     weavingEdges = initializeWeavingForAllGoals();
      unlockedNegatedEdges = new HashMap<>();
      for (CFAEdgesGoal goal : goals) {
        if (goal.getNegatedEdges().size() > 0) {
          unlockedNegatedEdges.put(goal, new HashSet<>(goal.getNegatedEdges()));
        }
      }
    } else {
      unlockedNegatedEdges = new HashMap<>();
      for(Entry<CFAEdgesGoal, ImmutableSet<ImmutableSet<CFAEdge>>> entry: predState.getUnlockedNegatedEdgesPerGoal().entrySet()) {
        HashSet<Set<CFAEdge>> sets = new HashSet<>();
        for (ImmutableSet<CFAEdge> set : entry.getValue()) {
          sets.add(new HashSet<CFAEdge>(set));
        }
        unlockedNegatedEdges.put(entry.getKey(), sets);
      }

      weavingEdges = new LinkedHashSet<>();
    }
    // if edge is weaved, it needs to be removed from weaved edges instead of processing the edge
    Set<CFAEdge> weavedEdges = new HashSet<>(predState.getWeavedEdges());
    if (weavedEdges.contains(pCfaEdge)) {
      weavedEdges.remove(pCfaEdge);
      return Collections
          .singleton(
              createSuccessor(
                  predState.getGoals(),
                  weavingEdges,
                  weavedEdges,
                  predState.getUnlockedNegatedEdgesPerGoal()));
    } else {
      return processEdge(predState, pCfaEdge, weavingEdges, unlockedNegatedEdges);
    }
  }


  private Collection<MultiGoalState>
      processEdge(
          MultiGoalState predState,
          final CFAEdge pCfaEdge,
          LinkedHashSet<Pair<CFAEdge, WeavingType>> edgesToWeave,
          HashMap<CFAEdgesGoal, Set<Set<CFAEdge>>> unlockedNegatedEdges) {
    Map<CFAEdgesGoal, Integer> succGoals = new HashMap<>(predState.getGoals());
    processEdge(edgesToWeave, succGoals, unlockedNegatedEdges, pCfaEdge);
    HashSet<CFAEdgesGoal> finishedGoals =
        getFinishedGoals(succGoals, unlockedNegatedEdges, predState);

    if (finishedGoals.size() == 0) {
      return Collections
          .singleton(
              createSuccessor(
                  succGoals,
                  edgesToWeave,
                  predState,
                  unlockedNegatedEdges));
    }
    // make sure each successor only covers 1 goal
    // otherwise weaving will break
    // use successor to create the new states, but do not include successor in returned states
    return generateSuccessorPerFinishedGoal(
        finishedGoals,
        succGoals,
        edgesToWeave,
        predState,
        unlockedNegatedEdges);
  }

  private MultiGoalState createSuccessor(
      ImmutableMap<CFAEdgesGoal, Integer> pGoals,
      LinkedHashSet<Pair<CFAEdge, WeavingType>> pWeavingEdges,
      Set<CFAEdge> pWeavedEdges,
      Map<CFAEdgesGoal, ImmutableSet<ImmutableSet<CFAEdge>>> pUnlockedNegatedEdgesPerGoal) {
      return new MultiGoalState(pGoals, pWeavingEdges, pWeavedEdges, pUnlockedNegatedEdgesPerGoal);
  }

  private MultiGoalState createSuccessor(
      Map<CFAEdgesGoal, Integer> pSuccessorGoals,
      LinkedHashSet<Pair<CFAEdge, WeavingType>> pNewEdgesToWeave,
      MultiGoalState predState,
      Map<CFAEdgesGoal, Set<Set<CFAEdge>>> pUnlockedNegatedEdges) {
    return new MultiGoalState(
        pSuccessorGoals,
        pNewEdgesToWeave,
        predState.getWeavedEdges(),
        pUnlockedNegatedEdges);
  }

  private HashSet<MultiGoalState> generateSuccessorForGoal(
      CFAEdgesGoal goal,
      HashSet<CFAEdgesGoal> finishedGoals,
      Map<CFAEdgesGoal, Integer> succGoals,
      LinkedHashSet<Pair<CFAEdge, WeavingType>> edgesToWeave,
      MultiGoalState predState) {
    HashSet<MultiGoalState> succs = new HashSet<>();
    Map<CFAEdgesGoal, Integer> successorGoals = new HashMap<>(succGoals);
    successorGoals.keySet().removeAll(finishedGoals);
    successorGoals.put(goal, goal.getEdges().size());
    LinkedHashSet<Pair<CFAEdge, WeavingType>> newEdgesToWeave = new LinkedHashSet<>(edgesToWeave);
    if (goal.getEdges().size() > 1) {
      for (CFAEdge cfaEdge : goal.getEdges()) {
        newEdgesToWeave.add(Pair.of(cfaEdge, WeavingType.ASSUMPTION));
      }
    }
    if (goal.getNegatedEdges().size() > 0) {

      List<Set<CFAEdge>> negated = new ArrayList<>();
      negated.addAll(goal.getNegatedEdges());
      Set<List<CFAEdge>> product = Sets.cartesianProduct(negated);
      for (List<CFAEdge> negatedEdges : product) {
        LinkedHashSet<Pair<CFAEdge, WeavingType>> edgesToWeaveCopy =
            new LinkedHashSet<>(newEdgesToWeave);
        for (CFAEdge edge : negatedEdges) {
          edgesToWeaveCopy.add(Pair.of(edge, WeavingType.NEGATEDASSUMPTION));
        }
        MultiGoalState succ =
            new MultiGoalState(
                successorGoals,
                edgesToWeaveCopy,
                predState.getWeavedEdges(),
                null);
        succs.add(succ);

        //TODO test with only one succ
        // break;
      }
    } else {
      succs.add(
          createSuccessor(
              successorGoals,
              newEdgesToWeave,
              predState,
              null));
    }
    return succs;
  }
  private Collection<MultiGoalState> generateSuccessorPerFinishedGoal(
      HashSet<CFAEdgesGoal> finishedGoals,
      Map<CFAEdgesGoal, Integer> succGoals,
      LinkedHashSet<Pair<CFAEdge, WeavingType>> edgesToWeave,
      MultiGoalState predState,
      HashMap<CFAEdgesGoal, Set<Set<CFAEdge>>> unlockedNegatedEdges) {

    HashSet<MultiGoalState> succs = new HashSet<>();
    Iterator<CFAEdgesGoal> iter = finishedGoals.iterator();
    while (iter.hasNext()) {
      CFAEdgesGoal goal = iter.next();
      // if goal wasnt unlocked until now, it should not return a successor
      if (!unlockedNegatedEdges.containsKey(goal) || unlockedNegatedEdges.get(goal).size() == 0) {
      succs.addAll(
          generateSuccessorForGoal(goal, finishedGoals, succGoals, edgesToWeave, predState));
      }
    }
    return succs;
  }

  private HashSet<CFAEdgesGoal> getFinishedGoals(
      Map<CFAEdgesGoal, Integer> succGoals,
      HashMap<CFAEdgesGoal, Set<Set<CFAEdge>>> unlockedNegatedEdges,
      MultiGoalState predState) {
    HashSet<CFAEdgesGoal> finishedGoals = new HashSet<>();
    for (CFAEdgesGoal goal : goals) {
      if (succGoals.containsKey(goal)) {
        if (succGoals.get(goal) >= goal.getEdges().size()
            && (!unlockedNegatedEdges.containsKey(goal) || unlockedNegatedEdges.get(goal).isEmpty())
            && predState.getWeavedEdges().isEmpty()
            && predState.getEdgesToWeave().isEmpty()) {
            finishedGoals.add(goal);
        }
      }
    }
    return finishedGoals;
  }

  private void processEdge(
      LinkedHashSet<Pair<CFAEdge, WeavingType>> edgesToWeave,
      Map<CFAEdgesGoal, Integer> succGoals,
      HashMap<CFAEdgesGoal, Set<Set<CFAEdge>>> succUnlocked,
      CFAEdge pCfaEdge) {
    boolean needsWeaving = false;
    for (Entry<CFAEdgesGoal, Set<Set<CFAEdge>>> entry : succUnlocked.entrySet()) {
      Iterator<Set<CFAEdge>> iter = entry.getValue().iterator();
      if (pCfaEdge instanceof AssumeEdge) {
        while (iter.hasNext()) {
          Set<CFAEdge> next = iter.next();
          if (!next.contains(pCfaEdge)) {
            iter.remove();
          }
        }
      }
    }
    if (edgesToWeave == null) {
      edgesToWeave = new LinkedHashSet<>();
    }
    for (CFAEdgesGoal goal : goals) {
      if (goal.containsNegatedEdge(pCfaEdge)) {
        needsWeaving = true;
      }
      int index = 0;
      if (succGoals.containsKey(goal)) {
        index = succGoals.get(goal);
      }
      if (goal.acceptsEdge(pCfaEdge, index)) {
        index++;
        succGoals.put(goal, index);
        if (goal.getEdges().size() > 1) {
          needsWeaving = true;
        }
      }
    }
    if (needsWeaving) {
      edgesToWeave.add(Pair.of(pCfaEdge, WeavingType.ASSIGNMENT));
    }
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
      if (goal.getNegatedEdges() != null) {
        for (Collection<CFAEdge> negatedEdges : goal.getNegatedEdges()) {
          for (CFAEdge negatedEdge : negatedEdges) {
            weavingEdges.add(Pair.of(negatedEdge, WeavingType.DECLARATION));
          }
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

  public void addGoal(CFAEdgesGoal pGoal) {
    goals.add(pGoal);
  }

}
