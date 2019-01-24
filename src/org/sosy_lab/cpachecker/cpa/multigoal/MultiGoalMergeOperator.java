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

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class MultiGoalMergeOperator implements MergeOperator {

  @Override
  public AbstractState merge(AbstractState pState1, AbstractState pState2, Precision pPrecision)
      throws CPAException, InterruptedException {
    MultiGoalState state1 = (MultiGoalState) pState1;
    MultiGoalState state2 = (MultiGoalState) pState2;

    // merge only, if there is no difference in covered goals and weaving states
    if (state1.getCoveredGoal().equals(state2.getCoveredGoal())
        && state1.getWeavedEdges().equals(state2.getWeavedEdges())
        && state1.getEdgesToWeave().equals(state2.getEdgesToWeave())) {
      MultiGoalState merged = MultiGoalState.createMergedState(state1, state2);
      return merged;
    }
    return state2;

  }

}
