/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.constraints.domain;

import java.io.PrintStream;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LogicalNotExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;

/**
 * Merge operator for {@link ConstraintsCPA}.
 * Removes the last added constraint <code>c</code> of the second state
 * if <code>!c</code> is the last added constraint of the first state.
 */
public class ConstraintsMergeOperator implements MergeOperator, Statistics {

  // Statistics
  private int removedConstraints = 0;

  /**
   * Merges the two given states. Weakens the second state with information of the first state.
   * The precision is ignored.
   *
   * <p><code>merge(s, s') = s'</code> if the last added constraint in <code>s</code> is not
   * <code>!c</code>, with <code>c</code> being the last added constraint in <code>s'</code>.</p>
   *
   * <p><code>merge(s, s') = s' \ {c}</code> if the last added constraint in <code>s</code> is
   * <code>!c</code>, with <code>c</code> being the last added constraint in <code>s'</code>.</p>
   *
   * @param pState1 the state to use to weaken the second state
   * @param pState2 the state to weaken
   * @param pPrecision unused. No precision is used for this computation
   * @return the merge of the two given states
   */
  @Override
  public AbstractState merge(
      final AbstractState pState1,
      final AbstractState pState2,
      final Precision pPrecision
  ) {
    assert pState1 instanceof ConstraintsState && pState2 instanceof ConstraintsState;

    final ConstraintsState stateToUseForWeakening = (ConstraintsState) pState1;
    final ConstraintsState stateToWeaken = (ConstraintsState) pState2;

    ConstraintsState weakenedState = stateToWeaken.copyOf();

    if (stateToUseForWeakening.isEmpty() || weakenedState.isEmpty()) {
      return pState2;
    }

    Constraint lastConstraintOfState1 = stateToUseForWeakening.getLastAddedConstraint();
    Constraint lastConstraintOfState2 = weakenedState.getLastAddedConstraint();

    if (lastConstraintOfState1 instanceof LogicalNotExpression) {
      lastConstraintOfState1 =
          (Constraint) ((LogicalNotExpression) lastConstraintOfState1).getOperand();

      if (lastConstraintOfState1.equals(lastConstraintOfState2)) {
        weakenedState.remove(lastConstraintOfState2);
        removedConstraints++;
      }

    } else if (lastConstraintOfState2 instanceof LogicalNotExpression) {
      SymbolicValue innerExpression = ((LogicalNotExpression) lastConstraintOfState2).getOperand();

      if (lastConstraintOfState1.equals(innerExpression)) {
        weakenedState.remove(lastConstraintOfState2);
        removedConstraints++;
      }
    }

    if (weakenedState.equals(pState2)) {
      return pState2;
    } else {
      return weakenedState;
    }
  }

  @Override
  public void printStatistics(
      final PrintStream pOut,
      final Result pResult,
      final ReachedSet pReached
  ) {
    pOut.println("Number of constraints removed in merge: " + removedConstraints);
  }

  @Nullable
  @Override
  public String getName() {
    return ConstraintsMergeOperator.class.getSimpleName();
  }
}
