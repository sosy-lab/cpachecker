/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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

import com.google.common.collect.ImmutableSet;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class MultiGoalAbstractDomain implements AbstractDomain {

  @Override
  public AbstractState join(AbstractState pElement1, AbstractState pElement2) throws CPAException {
    throw new UnsupportedOperationException();
  }

  private boolean sameSets(Set<?> set1, Set<?> set2) {
    if ((set1 == null && set2 == null)) {
      return true;
    }
    if (set1 == null || set2 == null) {
      return false;
    }
    if (set1.size() != set2.size()) {
      return false;
    }
    return set1.containsAll(set2);
  }

  @Override
  public boolean isLessOrEqual(AbstractState pState1, AbstractState pState2)
      throws CPAException, InterruptedException {
    // checks if state2 is equal or less than state1
    // meaning if state2 also fully covers state1
    MultiGoalState mgs1 = (MultiGoalState) pState1;
    MultiGoalState mgs2 = (MultiGoalState) pState2;

    // if equal return true;
    if (mgs1.equals(mgs2)) {
      return true;
    }

    // check for less than

    // if state2 contains a goal further explored than state1 it is not less or equal
    for (Entry<CFAEdgesGoal, Integer> goal : mgs1.goals.entrySet()) {
      if (!mgs2.goals.containsKey(goal.getKey())
          || mgs2.goals.get(goal.getKey()) < goal.getValue()) {
        return false;
      }
    }

    // if state2 contains less negatedEdges that need unlocking it is not less or equal
    for (Entry<CFAEdgesGoal, ImmutableSet<ImmutableSet<CFAEdge>>> unlockedNegatedGoals : mgs1.unlockedNegatedEdgesPerGoal
        .entrySet()) {
      if (mgs2.unlockedNegatedEdgesPerGoal.containsKey(unlockedNegatedGoals.getKey())
          && !unlockedNegatedGoals.getValue()
              .containsAll(mgs2.unlockedNegatedEdgesPerGoal.get(unlockedNegatedGoals.getKey()))) {
        return false;
      }
    }

    // if the edges to weave differ, return false
    // safety measure to not break weaving, might be possible to improve this comparison if
    // necessary
    if (!sameSets(mgs1.getEdgesToWeave(), mgs2.getEdgesToWeave())) {
      return false;
    }
    // same for weaved edges
    if (!sameSets(mgs1.getWeavedEdges(), mgs2.getWeavedEdges())) {
      return false;
    }

    return true;
  }

}
