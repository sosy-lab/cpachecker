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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.cpa.location.WeavingState;
import org.sosy_lab.cpachecker.cpa.location.WeavingType;
import org.sosy_lab.cpachecker.cpa.location.WeavingVariable;
import org.sosy_lab.cpachecker.util.Pair;

public class MultiGoalState implements AbstractState, Targetable, Graphable, WeavingState {

  protected ImmutableSet<CFAEdgesGoal> coveredGoals;
  protected ImmutableSet<Pair<WeavingVariable, WeavingType>> variablesToWeave;
  // TODO handle regions
  protected ImmutableMap<CFAEdgesGoal, Integer> goalStates;
  protected ImmutableMap<CFAEdgesGoal, ImmutableMap<PartialPath, PathState>> negatedPathStates;
  protected Set<CFAEdge> weavedEdges;
  protected boolean isInitialState;
  private int hash = 0;

  public static MultiGoalState createInitialState() {
    return new MultiGoalState();
  }

  protected MultiGoalState() {
    isInitialState = true;
    goalStates = ImmutableMap.copyOf(Collections.emptyMap());
    negatedPathStates = ImmutableMap.copyOf(Collections.emptyMap());
    variablesToWeave = ImmutableSet.copyOf(Collections.emptySet());
    weavedEdges = Collections.emptySet();
    coveredGoals = ImmutableSet.copyOf(Collections.emptyList());
  }

  public MultiGoalState(
      Map<CFAEdgesGoal, Integer> pGoals,
      LinkedHashSet<Pair<WeavingVariable, WeavingType>> pEdgesToWeave,
      Set<CFAEdge> pWeavedEdges,
      Map<CFAEdgesGoal, Map<PartialPath, PathState>> pNegatedPathStates) {
    Map<CFAEdgesGoal, ImmutableMap<PartialPath, PathState>> map = new HashMap<>();
    if (pNegatedPathStates == null) {
      map = Collections.emptyMap();
    } else {
      for (Entry<CFAEdgesGoal, Map<PartialPath, PathState>> entry : pNegatedPathStates.entrySet()) {
        map.put(entry.getKey(), ImmutableMap.copyOf(entry.getValue()));
      }
    }
    init(ImmutableMap.copyOf(pGoals), pEdgesToWeave, pWeavedEdges, map);
  }

  public MultiGoalState(
      Map<CFAEdgesGoal, Integer> pGoals,
      LinkedHashSet<Pair<WeavingVariable, WeavingType>> pEdgesToWeave,
      ImmutableSet<CFAEdge> pWeavedEdges,
      ImmutableMap<CFAEdgesGoal, ImmutableMap<PartialPath, PathState>> pNegatedPathStates) {
    init(ImmutableMap.copyOf(pGoals), pEdgesToWeave, pWeavedEdges, pNegatedPathStates);
  }

  // public MultiGoalState(
  // ImmutableMap<CFAEdgesGoal, Integer> pGoals,
  // LinkedHashSet<Pair<CFAEdge, WeavingType>> pEdgesToWeave,
  // Set<CFAEdge> pWeavedEdges,
  // Map<CFAEdgesGoal, ImmutableSet<ImmutableSet<CFAEdge>>> pUnlockedNegatedEdgesPerGoal) {
  //
  // isInitialState = false;
  // hasFinishedGoal = false;
  //
  // goalStates =
  // pGoals == null ? ImmutableMap.copyOf(Collections.emptySet()) : ImmutableMap.copyOf(pGoals);
  // negatedPathStates = ImmutableMap.copyOf(pUnlockedNegatedEdgesPerGoal);
  // for (Entry<CFAEdgesGoal, Integer> goal : goalStates.entrySet()) {
  // if (goal.getValue() >= goal.getKey().getEdges().size()) {
  // if (!getUnlockedNegatedEdgesPerGoal().containsKey(goal.getKey())
  // || getUnlockedNegatedEdgesPerGoal().get(goal.getKey()).isEmpty()) {
  // hasFinishedGoal = true;
  // }
  // break;
  // }
  // }
  // weavedEdges =
  // pWeavedEdges == null
  // ? Collections.emptySet()
  // : new HashSet<>(pWeavedEdges);
  // edgesToWeave =
  // pEdgesToWeave == null
  // ? ImmutableSet.copyOf(Collections.emptySet())
  // : ImmutableSet.copyOf(pEdgesToWeave);
  // }

  private void init(
      ImmutableMap<CFAEdgesGoal, Integer> pGoals,
      LinkedHashSet<Pair<WeavingVariable, WeavingType>> pEdgesToWeave,
      Set<CFAEdge> pWeavedEdges,
      Map<CFAEdgesGoal, ImmutableMap<PartialPath, PathState>> pNegatedPathStates) {
    goalStates =
        pGoals == null ? ImmutableMap.copyOf(Collections.emptySet()) : ImmutableMap.copyOf(pGoals);
    negatedPathStates =
        pNegatedPathStates == null
            ? ImmutableMap.copyOf(Collections.emptySet())
            : ImmutableMap.copyOf(pNegatedPathStates);

    weavedEdges = pWeavedEdges == null ? Collections.emptySet() : new HashSet<>(pWeavedEdges);
    variablesToWeave =
        pEdgesToWeave == null
            ? ImmutableSet.copyOf(Collections.emptySet())
            : ImmutableSet.copyOf(pEdgesToWeave);
    coveredGoals =
        ImmutableSet.copyOf(
            calculateCoveredGoals(pGoals, negatedPathStates, weavedEdges, variablesToWeave));
  }

  public MultiGoalState(
      ImmutableMap<CFAEdgesGoal, Integer> pGoals,
      LinkedHashSet<Pair<WeavingVariable, WeavingType>> pEdgesToWeave,
      Set<CFAEdge> pWeavedEdges,
      Map<CFAEdgesGoal, ImmutableMap<PartialPath, PathState>> pNegatedPathStates) {
    init(pGoals, pEdgesToWeave, pWeavedEdges, pNegatedPathStates);
  }


  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (isTarget()) {
      builder.append("TARGET");
    } else {
      builder.append("NO_TARGET");
    }
    for (Entry<CFAEdgesGoal, Integer> goal : goalStates.entrySet()) {
      builder.append("\n");
      builder.append(goal.getKey().getPath().toString());
      builder.append("\t:" + goal.getValue());
    }
    builder.append("\nVariables to Weave:\n");
    for (Pair<WeavingVariable, WeavingType> edge : getEdgesToWeave()) {
      builder.append(edge.getFirst().toString() + edge.getSecond().toString() + "\n");
    }
    builder.append("\nWeaved Edges:\n");
    for (CFAEdge edge : getWeavedEdges()) {
      builder.append(edge.toString() + "\n");
    }

    builder.append("\nNegated Path States:\n");
    int i = 0;
    for (Entry<CFAEdgesGoal, ImmutableMap<PartialPath, PathState>> entry : getNegatedPathsPerGoal()
        .entrySet()) {
      for (Entry<PartialPath, PathState> pathState : entry.getValue().entrySet()) {
        // builder.append(pathState.getKey().toString());
        builder.append("Path " + i + " state");
        i++;
        builder.append("\t:");
        builder.append(pathState.getValue());
      }
    }
    return builder.toString();
  }

  @Override
  public String toDOTLabel() {
    return toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public boolean isTarget() {
    if (!getCoveredGoal().isEmpty()
        && getWeavedEdges().isEmpty()
        && getEdgesToWeave().isEmpty()) {
      return true;
    }
    return false;
  }

  @Override
  public @NonNull Set<Property> getViolatedProperties() throws IllegalStateException {
    return new HashSet<>(goalStates.keySet());
  }

  public static Set<CFAEdgesGoal> calculateCoveredGoals(
      ImmutableMap<CFAEdgesGoal, Integer> goalStates,
      ImmutableMap<CFAEdgesGoal, ImmutableMap<PartialPath, PathState>> negatedPathStates,
      Set<CFAEdge> pWeavedEdges,
      ImmutableSet<Pair<WeavingVariable, WeavingType>> pEdgesToWeave) {

    if (!pWeavedEdges.isEmpty() || !pEdgesToWeave.isEmpty()) {
      return Collections.emptySet();
    }

    if (goalStates.isEmpty()) {
      return Collections.emptySet();
    }else {
      Set<CFAEdgesGoal> tempCoveredGoals = new HashSet<>();

      for (Entry<CFAEdgesGoal, Integer> goal : goalStates.entrySet()) {
          if (goal.getValue() >= goal.getKey().getPath().size()) {
          if (!negatedPathStates.containsKey(goal.getKey())) {
              tempCoveredGoals.add(goal.getKey());
            } else {
            ImmutableMap<PartialPath, PathState> negatedPaths =
                negatedPathStates.get(goal.getKey());
            boolean allPathsFree = true;
            for (Entry<PartialPath, PathState> path : negatedPaths.entrySet()) {
              if (!path.getValue().isPathFound()) {
                allPathsFree = false;
                }
              }
            if (allPathsFree) {
              tempCoveredGoals.add(goal.getKey());
            }
            }
          }
        }
      return tempCoveredGoals;
    }
  }

  public ImmutableSet<CFAEdgesGoal> getCoveredGoal() {
    return coveredGoals;
  }


  @Override
  public boolean equals(Object pObj) {

    if (pObj == this) {
      return true;
    }

    if(!(pObj instanceof MultiGoalState)) {
      return false;
    }
    MultiGoalState other = (MultiGoalState)pObj;

    if ((other.goalStates == null && this.goalStates == null)
        || !other.goalStates.entrySet().equals(this.goalStates.entrySet())) {
      return false;
    }

    if ((other.negatedPathStates == null && this.negatedPathStates == null)
        || !other.negatedPathStates.entrySet().equals(this.negatedPathStates.entrySet())) {
      return false;
    }

    if ((other.getCoveredGoal() == null && this.getCoveredGoal() == null)
        || !other.getCoveredGoal().equals(this.getCoveredGoal())) {
      return false;
    }

    if (!other.getEdgesToWeave().equals(this.getEdgesToWeave())) {
      return false;
    }
    if (!other.getWeavedEdges().equals(this.getWeavedEdges())) {
      return false;
    }

    return true;
  }

  public ImmutableMap<CFAEdgesGoal, Integer> getGoals() {
    return goalStates;
  }

  @Override
  public boolean needsWeaving() {
    return !getEdgesToWeave().isEmpty();
  }

  @Override
  public ImmutableSet<Pair<WeavingVariable, WeavingType>> getEdgesToWeave() {
    return variablesToWeave;
  }

  @Override
  public void addWeavedEdge(CFAEdge pWeaveEdge) {
    weavedEdges.add(pWeaveEdge);
  }

  public ImmutableSet<CFAEdge> getWeavedEdges() {
    return ImmutableSet.copyOf(weavedEdges);
  }

  public boolean isInitialState() {
    return isInitialState;
  }



  @Override
  public int hashCode() {
    if (hash == 0) {
      // Important: we cannot use weavedEdges.hashCode(), because the hash code of a map
      // depends on the hash code of its values, and those may change.
      final int prime = 31;
      hash = 1;
      hash = prime * hash + (isInitialState ? 0 : 1);
      hash = prime * hash + ((getCoveredGoal() == null) ? 0 : getCoveredGoal().hashCode());
      hash = prime * hash + ((variablesToWeave == null) ? 0 : variablesToWeave.hashCode());
      hash = prime * hash + ((goalStates == null) ? 0 : goalStates.hashCode());
      // hash = prime * hash + ((region == null) ? 0 : region.hashCode());
    }
    return hash;
  }

  protected static <T> Set<T> union(Set<T> set1, Set<T> set2) {
    if (set1 == null && set2 == null) {
      return Collections.emptySet();
    } else if (set1 != null && set2 == null) {
      return new HashSet<>(set1);
    } else if (set1 == null && set2 != null) {
      return new HashSet<>(set2);
    } else {
      Set<T> set = new HashSet<>(set1);
      set.addAll(set2);
      return set;
    }
  }

  protected static ImmutableMap<CFAEdgesGoal, Integer>
      mergeGoals(MultiGoalState pState1, MultiGoalState pState2) {
    if (pState1.goalStates == null && pState2.goalStates == null) {
      return ImmutableMap.copyOf(Collections.emptyMap());
    } else if (pState1.goalStates != null && pState2.goalStates == null) {
      return ImmutableMap.copyOf(pState1.goalStates);
    } else if (pState1.goalStates == null && pState2.goalStates != null) {
      return ImmutableMap.copyOf(pState2.goalStates);
    } else {
      Map<CFAEdgesGoal, Integer> newGoals = new HashMap<>(pState1.goalStates);
      pState2.goalStates
          .forEach((key, value) -> newGoals.merge(key, value, (v1, v2) -> v1 > v2 ? v1 : v2));
      return ImmutableMap.copyOf(newGoals);
    }
  }

  public static MultiGoalState createMergedState(MultiGoalState pState1, MultiGoalState pState2) {
    MultiGoalState mergedState = new MultiGoalState();
    mergedState.isInitialState = false;


    mergedState.variablesToWeave =
        ImmutableSet.copyOf(union(pState1.variablesToWeave, pState2.variablesToWeave));

    mergedState.weavedEdges = union(pState1.weavedEdges, pState2.weavedEdges);


    mergedState.goalStates = mergeGoals(pState1, pState2);

    mergedState.negatedPathStates = mergeNegatedPaths(pState1, pState2);
    // do not calculate covered Goals during merging!
    Set<CFAEdgesGoal> goals = new HashSet<>();
    goals.addAll(pState1.getCoveredGoal());
    goals.addAll(pState2.getCoveredGoal());


    mergedState.coveredGoals = ImmutableSet.copyOf(goals);

    return mergedState;
  }

  protected static ImmutableMap<CFAEdgesGoal, ImmutableMap<PartialPath, PathState>>
      mergeNegatedPaths(MultiGoalState pState1, MultiGoalState pState2) {
    if (pState1.negatedPathStates == null && pState2.negatedPathStates == null) {
      return ImmutableMap.copyOf(Collections.emptyMap());
    } else if (pState1.negatedPathStates != null && pState2.negatedPathStates == null) {
      return pState1.negatedPathStates;
    } else if (pState1.negatedPathStates == null && pState2.negatedPathStates != null) {
      return pState2.negatedPathStates;
    } else {
      Map<CFAEdgesGoal, ImmutableMap<PartialPath, PathState>> newGoals = new HashMap<>();
      for (Entry<CFAEdgesGoal, ImmutableMap<PartialPath, PathState>> entry : pState1.negatedPathStates
          .entrySet()) {
        Map<PartialPath, PathState> newStates = new HashMap<>();
        ImmutableMap<PartialPath, PathState> pState2PartialPathStates =
            pState2.negatedPathStates.get(entry.getKey());
        for (Entry<PartialPath, PathState> partialPathState : entry.getValue().entrySet()) {
          PathState pState2PartialPathState =
              pState2PartialPathStates.get(partialPathState.getKey());
          int index;

          assert !(pState2PartialPathState.getIndex() >= 0
              && partialPathState.getValue().getIndex() >= 0);
          if (pState2PartialPathState.getIndex() >= 0) {
            index = pState2PartialPathState.getIndex();
          } else if (partialPathState.getValue().getIndex() >= 0) {
            index = partialPathState.getValue().getIndex();
          } else {
            index = -1;
          }
          boolean pathFound =
              pState2PartialPathState.isPathFound() || partialPathState.getValue().isPathFound();
          newStates.put(
              partialPathState.getKey(),
              new PathState(index, pathFound));
        }
        newGoals.put(entry.getKey(), ImmutableMap.copyOf(newStates));
      }
      return ImmutableMap.copyOf(newGoals);
    }
  }

  public ImmutableMap<CFAEdgesGoal, ImmutableMap<PartialPath, PathState>> getNegatedPathsPerGoal() {
    return negatedPathStates;
  }


}
