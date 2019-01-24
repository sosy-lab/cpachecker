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
  private LinkedHashSet<Pair<CFAEdge, WeavingType>> edgesToWeave;
  // TODO handle regions
  private Region region;
  Map<CFAEdgesGoal, Integer> goals;
  Set<CFAEdge> weavedEdges;
  boolean isInitialState;

  public static MultiGoalState createInitialState() {
    return new MultiGoalState();
  }

  private MultiGoalState() {
    isInitialState = true;
    hasFinishedGoal = false;
    goals = new HashMap<>();
  }
  public MultiGoalState(MultiGoalState predState) {
    if (predState == null) {
      hasFinishedGoal = false;
    } else {
      hasFinishedGoal = predState.hasFinishedGoal;
      if (predState.goals != null) {
        goals = new HashMap<>(predState.goals);
      }
      if (predState.weavedEdges != null) {
        weavedEdges = new HashSet<>(predState.weavedEdges);
      }
    }

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

    if (!other.getEdgesToWeave().equals(other.edgesToWeave)) {
      return false;
    }
    if (!other.getWeavedEdges().equals(this.getWeavedEdges())) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = 1;
    result = hasFinishedGoal ? result * 37 + 1 : result * 37 + 0;
    for (CFAEdgesGoal goal : getCoveredGoal()) {
      result = result * 37 + goal.hashCode();
    }

    for (Pair<CFAEdge, WeavingType> edgeToWeave : getEdgesToWeave()) {
      result = result * 37 + edgeToWeave.getFirst().hashCode();
      result = result * 37 + edgeToWeave.getSecond().hashCode();
    }

    for (CFAEdge weavedEdge : getWeavedEdges()) {
      result = result * 37 + weavedEdge.hashCode();
    }
    return result;
  }

  public Map<CFAEdgesGoal, Integer> getGoals() {
    if (goals == null) {
      return Collections.emptyMap();
    }
    return goals;
  }

  public boolean needsWeaving() {
    return !getEdgesToWeave().isEmpty();
  }

  public LinkedHashSet<Pair<CFAEdge, WeavingType>> getEdgesToWeave() {
    if (edgesToWeave == null) {
      return new LinkedHashSet<>();
    }
    return edgesToWeave;
  }


  void addWeavingEdge(CFAEdge weavingEdge, WeavingType type) {
    if (edgesToWeave == null) {
      edgesToWeave = new LinkedHashSet<>();
    }
    edgesToWeave.add(Pair.of(weavingEdge, type));
  }

  public void putGoal(CFAEdgesGoal pGoal, int pIndex) {
    if (goals == null) {
      goals = new HashMap<>();
    }
    goals.put(pGoal, pIndex);
    if (pIndex >= pGoal.getEdges().size()) {
      hasFinishedGoal = true;
    }
  }

  public void addWeavedEdge(CFAEdge pWeaveEdge) {
    if (weavedEdges == null) {
      weavedEdges = new HashSet<>();
    }
    weavedEdges.add(pWeaveEdge);
  }

  public Set<CFAEdge> getWeavedEdges() {
    if (weavedEdges == null) {
      return Collections.emptySet();
    }
    return weavedEdges;
  }

  public void removeWeavedEdge(CFAEdge pCfaEdge) {
    weavedEdges.remove(pCfaEdge);
  }

  public boolean isInitialState() {
    return isInitialState;
  }


  public static MultiGoalState createMergedState(MultiGoalState pState1, MultiGoalState pState2) {
    MultiGoalState mergedState = new MultiGoalState();
    mergedState.hasFinishedGoal = pState1.hasFinishedGoal || pState2.hasFinishedGoal;
    mergedState.isInitialState = false;
    if (pState1.edgesToWeave != null || pState2.edgesToWeave != null) {
      mergedState.edgesToWeave = new LinkedHashSet<>();
      mergedState.edgesToWeave.addAll(pState1.getEdgesToWeave());
      mergedState.edgesToWeave.addAll(pState2.getEdgesToWeave());
    }
    if (pState1.weavedEdges != null || pState2.weavedEdges != null) {
    mergedState.weavedEdges = new HashSet<>();
      mergedState.weavedEdges.addAll(pState1.getWeavedEdges());
      mergedState.weavedEdges.addAll(pState2.getWeavedEdges());
    }
    if (pState1.goals != null || pState2.goals != null) {
      mergedState.goals = new HashMap<>();
      if (pState1.goals != null) {
        mergedState.goals.putAll(pState1.goals);
      }

      if (pState2.goals != null) {
        for (Entry<CFAEdgesGoal, Integer> goal : pState2.goals.entrySet()) {
          if (!mergedState.getGoals().containsKey(goal.getKey())
              || mergedState.getGoals().get(goal.getKey()) > goal.getValue()) {
            mergedState.putGoal(goal.getKey(), goal.getValue());
          }
        }
      }
    }
    return mergedState;
  }

  public void removeGoal(CFAEdgesGoal pGoal) {
    goals.remove(pGoal);
  }

  public void removeGoals(HashSet<CFAEdgesGoal> pFinishedGoals) {
    for (CFAEdgesGoal goal : pFinishedGoals) {
      removeGoal(goal);
    }
  }

}
