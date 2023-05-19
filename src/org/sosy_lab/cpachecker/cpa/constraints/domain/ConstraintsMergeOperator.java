// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints.domain;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsStatistics;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LogicalNotExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;

/**
 * Merge operator for {@link ConstraintsCPA}. Removes the last added constraint <code>c</code> of
 * the second state if <code>!c</code> is the last added constraint of the first state.
 */
public class ConstraintsMergeOperator implements MergeOperator {

  private final ConstraintsStatistics stats;

  public ConstraintsMergeOperator(ConstraintsStatistics pStats) {
    stats = pStats;
  }

  /**
   * Merges the two given states. Weakens the second state with information of the first state. The
   * precision is ignored.
   *
   * <p><code>merge(s, s') = s'</code> if the last added constraint in <code>s</code> is not <code>
   * !c</code>, with <code>c</code> being the last added constraint in <code>s'</code>.
   *
   * <p><code>merge(s, s') = s' \ {c}</code> if the last added constraint in <code>s</code> is
   * <code>!c</code>, with <code>c</code> being the last added constraint in <code>s'</code>.
   *
   * @param pState1 the state to use to weaken the second state
   * @param pState2 the state to weaken
   * @param pPrecision unused. No precision is used for this computation
   * @return the merge of the two given states
   */
  @Override
  public AbstractState merge(
      final AbstractState pState1, final AbstractState pState2, final Precision pPrecision) {
    assert pState1 instanceof ConstraintsState && pState2 instanceof ConstraintsState;

    final ConstraintsState stateToUseForWeakening = (ConstraintsState) pState1;
    final ConstraintsState stateToWeaken = (ConstraintsState) pState2;

    ConstraintsState weakenedState = stateToWeaken.copyOf();

    if (stateToUseForWeakening.isEmpty() || weakenedState.isEmpty()) {
      return pState2;
    }

    Constraint lastConstraintOfState1 =
        stateToUseForWeakening.getLastAddedConstraint().orElseThrow();
    Constraint lastConstraintOfState2 = weakenedState.getLastAddedConstraint().orElseThrow();

    if (lastConstraintOfState1 instanceof LogicalNotExpression) {
      lastConstraintOfState1 =
          (Constraint) ((LogicalNotExpression) lastConstraintOfState1).getOperand();

      if (lastConstraintOfState1.equals(lastConstraintOfState2)) {
        weakenedState.remove(lastConstraintOfState2);
        stats.constraintsRemovedInMerge.inc();
      }

    } else if (lastConstraintOfState2 instanceof LogicalNotExpression) {
      SymbolicValue innerExpression = ((LogicalNotExpression) lastConstraintOfState2).getOperand();

      if (lastConstraintOfState1.equals(innerExpression)) {
        weakenedState.remove(lastConstraintOfState2);
        stats.constraintsRemovedInMerge.inc();
      }
    }

    if (weakenedState.equals(pState2)) {
      return pState2;
    } else {
      return weakenedState;
    }
  }
}
