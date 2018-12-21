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
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class MutliGoalTransferRelation extends SingleEdgeTransferRelation {
  private Set<CFAEdgesGoal> goals;

  MutliGoalTransferRelation(final Set<CFAEdgesGoal> pGoals) {
    this.goals = pGoals;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      final AbstractState pState,
      final Precision pPrecision,
      final CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    if(goals == null) {
      return Collections.singleton(MultiGoalState.NonTargetState());
    }
    for (CFAEdgesGoal goal : goals) {
      if (goal.processEdge(pCfaEdge)) {
        if (goal.isCovered()) {
          return Collections.singleton(MultiGoalState.TargetState(goal));
        }
      }

    }
    return Collections.singleton(MultiGoalState.NonTargetState());
  }

  public Set<CFAEdgesGoal> getGoals() {
    return goals;
  }

  public void setGoals(Set<CFAEdgesGoal> pGoals) {
    goals = pGoals;
  }

}
