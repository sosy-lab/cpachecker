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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;

public class MultiGoalState implements AbstractState, Targetable, Graphable {

  private boolean isTarget;
  // TODO handle regions
  private Region region;
  Map<CFAEdgesGoal, Integer> goals;


  public MultiGoalState(MultiGoalState predState) {

    if (predState == null) {
      isTarget = false;
      goals = new HashMap<>();
    } else {
      isTarget = predState.isTarget;
      goals = new HashMap<>(predState.goals);
    }
  }


  @Override
  public String toString() {
      if (isTarget) {
        return "TARGET";
      }
      return "NO_TARGET";
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
    return isTarget;
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
    return other.isTarget == this.isTarget;
  }

  public Collection<CFAEdgesGoal> getAllGoals() {
    if (goals == null) {
      return Collections.emptySet();
    }
    return goals.keySet();
  }

  public void processEdge(CFAEdge pCfaEdge, Set<CFAEdgesGoal> allGoals) {
    for (CFAEdgesGoal goal : allGoals) {
      int index = 0;
      if (goals.containsKey(goal)) {
        index = goals.get(goal);
      }
      if (goal.acceptsEdge(pCfaEdge, index)) {
        index++;
        goals.put(goal, index);
        if (index >= goal.getEdges().size()) {
          isTarget = true;
        }
      }
    }
  }
}
