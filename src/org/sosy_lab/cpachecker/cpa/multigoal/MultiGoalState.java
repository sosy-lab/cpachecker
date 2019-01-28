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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import org.sosy_lab.cpachecker.cpa.location.LocationTransferRelation.WeavingType;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;

public class MultiGoalState implements AbstractState, Targetable, Graphable {


  private boolean hasFinishedGoal;
  private ImmutableSet<Pair<CFAEdge, WeavingType>> edgesToWeave;
  // TODO handle regions
  private Region region;
  ImmutableMap<CFAEdgesGoal, Integer> goals;
  Set<CFAEdge> weavedEdges;
  boolean isInitialState;
  private int hash = 0;

  public static MultiGoalState createInitialState() {
    return new MultiGoalState();
  }

  private MultiGoalState() {
    isInitialState = true;
    hasFinishedGoal = false;
    goals = ImmutableMap.copyOf(Collections.emptyMap());
    edgesToWeave = ImmutableSet.copyOf(Collections.emptySet());
    weavedEdges = new HashSet<>();
  }

  public MultiGoalState(
      Map<CFAEdgesGoal, Integer> pGoals,
      LinkedHashSet<Pair<CFAEdge, WeavingType>> pEdgesToWeave,
      Set<CFAEdge> pWeavedEdges) {
    hasFinishedGoal = false;
    goals =
        pGoals == null ? ImmutableMap.copyOf(Collections.emptySet()) : ImmutableMap.copyOf(pGoals);
    for (Entry<CFAEdgesGoal, Integer> goal : goals.entrySet()) {
      if (goal.getValue() >= goal.getKey().getEdges().size()) {
        hasFinishedGoal = true;
        break;
      }
    }
    weavedEdges = pWeavedEdges == null ? new HashSet<>() : new HashSet<>(pWeavedEdges);
    edgesToWeave =
        pEdgesToWeave == null
            ? ImmutableSet.copyOf(Collections.emptySet())
            : ImmutableSet.copyOf(pEdgesToWeave);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (isTarget()) {
      builder.append("TARGET");
    } else {
      builder.append("NO_TARGET");
    }
    for (Entry<CFAEdgesGoal, Integer> goal : goals.entrySet()) {
      builder.append("\n");
      Iterator<CFAEdge> iter = goal.getKey().getEdges().iterator();
      while (iter.hasNext()) {
        builder.append(iter.next().toString());
        if (iter.hasNext()) {
          builder.append("->");
        }
      }

      builder.append("\t:" + goal.getValue());
    }
    builder.append("\nEdges to Weave:\n");
    for (Pair<CFAEdge, WeavingType> edge : getEdgesToWeave()) {
      builder.append(edge.getFirst().toString() + edge.getSecond().toString() + "\n");
    }
    builder.append("\nWeaved Edges:\n");
    for (CFAEdge edge : getWeavedEdges()) {
      builder.append(edge.toString() + "\n");
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
    return hasFinishedGoal
        && getWeavedEdges().isEmpty()
        && getEdgesToWeave().isEmpty();
  }

  @Override
  public @NonNull Set<Property> getViolatedProperties() throws IllegalStateException {
    return Collections.emptySet();
  }

  public Set<CFAEdgesGoal> getCoveredGoal() {
    Set<CFAEdgesGoal> coveredGoals = new HashSet<>();
    for (Entry<CFAEdgesGoal, Integer> entry : goals.entrySet()) {
      if (entry.getValue() >= entry.getKey().getEdges().size()) {
        coveredGoals.add(entry.getKey());
      }
    }
    return coveredGoals;
  }


  @Override
  public boolean equals(Object pObj) {
    if(!(pObj instanceof MultiGoalState)) {
      return false;
    }
    MultiGoalState other = (MultiGoalState)pObj;
    // TODO only check if its target or not, needs rework for stop operator
    if (other.hasFinishedGoal != this.hasFinishedGoal) {
      return false;
    }

    if (!other.getCoveredGoal().equals(this.getCoveredGoal())) {
      return false;
    }

    if (!other.getEdgesToWeave().equals(this.getEdgesToWeave())) {
      return false;
    }
    if (!other.getWeavedEdges().equals(this.getWeavedEdges())) {
      return false;
    }
    if (other.region != null && other.region.equals(this.region)) {
      return false;
    }

    return true;
  }

  public ImmutableMap<CFAEdgesGoal, Integer> getGoals() {
    return goals;
  }

  public boolean needsWeaving() {
    return !getEdgesToWeave().isEmpty();
  }

  public ImmutableSet<Pair<CFAEdge, WeavingType>> getEdgesToWeave() {
    return edgesToWeave;
  }

  public void addWeavedEdge(CFAEdge pWeaveEdge) {
    weavedEdges.add(pWeaveEdge);
  }

  public Set<CFAEdge> getWeavedEdges() {
    return weavedEdges;
  }

  public void removeWeavedEdge(CFAEdge pCfaEdge) {
    weavedEdges.remove(pCfaEdge);
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
      hash = prime * hash + (hasFinishedGoal ? 0 : 1);
      hash = prime * hash + ((edgesToWeave == null) ? 0 : edgesToWeave.hashCode());
      hash = prime * hash + ((goals == null) ? 0 : goals.hashCode());
      hash = prime * hash + ((region == null) ? 0 : region.hashCode());
    }
    return hash;
  }

  private static <T> Set<T> union(Set<T> set1, Set<T> set2) {
    if (set1 == null && set2 == null) {
      return Collections.emptySet();
    } else if (set1 != null && set2 == null) {
      return set1;
    } else if (set1 == null && set2 != null) {
      return set2;
    } else {
      return Sets.union(set1, set2);
    }
  }

  private static ImmutableMap<CFAEdgesGoal, Integer>
      mergeGoals(MultiGoalState pState1, MultiGoalState pState2) {
    if (pState1.goals == null && pState2.goals == null) {
      return ImmutableMap.copyOf(Collections.emptyMap());
    } else if (pState1.goals != null && pState2.goals == null) {
      return ImmutableMap.copyOf(pState1.goals);
    } else if (pState1.goals == null && pState2.goals != null) {
      return ImmutableMap.copyOf(pState2.goals);
    } else {
      HashMap<CFAEdgesGoal, Integer> newGoals = new HashMap<>(pState1.goals);
      pState2.goals
          .forEach((key, value) -> newGoals.merge(key, value, (v1, v2) -> v1 > v2 ? v1 : v2));
      return ImmutableMap.copyOf(newGoals);
    }
  }

  public static MultiGoalState createMergedState(MultiGoalState pState1, MultiGoalState pState2) {
    MultiGoalState mergedState = new MultiGoalState();
    mergedState.hasFinishedGoal = pState1.hasFinishedGoal || pState2.hasFinishedGoal;
    mergedState.isInitialState = false;

    mergedState.edgesToWeave =
        ImmutableSet.copyOf(union(pState1.edgesToWeave, pState2.edgesToWeave));

    mergedState.weavedEdges = union(pState1.weavedEdges, pState2.weavedEdges);

    mergedState.goals = mergeGoals(pState1, pState2);

    return mergedState;
  }
}
