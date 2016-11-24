/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.policyiteration;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;

public class PolicyMergeOperator implements MergeOperator {

  private final PathFormulaManager pfmgr;

  PolicyMergeOperator(PathFormulaManager pPfmgr) {
    pfmgr = pPfmgr;
  }

  @Override
  public AbstractState merge(AbstractState state1, AbstractState state2, Precision precision)
      throws CPAException, InterruptedException {
    PolicyState policyState1 = (PolicyState) state1;
    PolicyState policyState2 = (PolicyState) state2;
    Preconditions.checkState(
        policyState1.isAbstract() == policyState2.isAbstract(),
        "Only states with the same abstraction status should be allowed to merge");
    if (policyState1.isAbstract()) {

      // No merge.
      return policyState2;
    }

    return joinIntermediateStates(policyState1.asIntermediate(), policyState2.asIntermediate());
  }

  /** At every join, update all the references to starting states to the latest ones. */
  private PolicyIntermediateState joinIntermediateStates(
      PolicyIntermediateState newState, PolicyIntermediateState oldState)
      throws InterruptedException {

    Preconditions.checkState(newState.getNode() == oldState.getNode());

    if (!newState.getBackpointerState().equals(oldState.getBackpointerState())) {

      // Different parents: do not merge.
      return oldState;
    }

    if (newState.isMergedInto(oldState)) {
      return oldState;
    } else if (oldState.isMergedInto(newState)) {
      return newState;
    }

    PathFormula mergedPath = pfmgr.makeOr(newState.getPathFormula(), oldState.getPathFormula());
    PolicyIntermediateState out =
        PolicyIntermediateState.of(newState.getNode(), mergedPath, oldState.getBackpointerState());

    newState.setMergedInto(out);
    oldState.setMergedInto(out);
    return out;
  }
}
