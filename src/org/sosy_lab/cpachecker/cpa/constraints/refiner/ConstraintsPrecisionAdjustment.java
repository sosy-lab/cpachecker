// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints.refiner;

import com.google.common.base.Function;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsStatistics;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.constraints.refiner.precision.ConstraintsPrecision;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * {@link PrecisionAdjustment} for {@link org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA
 * ConstraintsCPA}.
 */
public class ConstraintsPrecisionAdjustment implements PrecisionAdjustment {

  private ConstraintsStatistics stats;

  public ConstraintsPrecisionAdjustment(final ConstraintsStatistics pStats) {
    stats = pStats;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      final AbstractState pStateToAdjust,
      final Precision pPrecision,
      final UnmodifiableReachedSet pReachedStates,
      final Function<AbstractState, AbstractState> pStateProjection,
      final AbstractState pFullState) {

    return prec((ConstraintsState) pStateToAdjust, (ConstraintsPrecision) pPrecision, pFullState);
  }

  private Optional<PrecisionAdjustmentResult> prec(
      final ConstraintsState pStateToAdjust,
      final ConstraintsPrecision pPrecision,
      final AbstractState pFullState) {

    int constraintsBefore = 0;
    int constraintsAfter = 0;

    stats.adjustmentTime.start();
    try {
      ConstraintsState result = pStateToAdjust.copyOf();
      for (Constraint c : pStateToAdjust) {
        constraintsBefore++;
        CFANode currentLocation = AbstractStates.extractLocation(pFullState);

        if (!pPrecision.isTracked(c, currentLocation)) {
          result.remove(c);

        } else {
          constraintsAfter++;
        }
      }
      stats.constraintNumberBeforeAdj.setNextValue(constraintsBefore);
      stats.constraintNumberAfterAdj.setNextValue(constraintsAfter);

      result = result.equals(pStateToAdjust) ? pStateToAdjust : result;

      return Optional.of(PrecisionAdjustmentResult.create(result, pPrecision, Action.CONTINUE));
    } finally {
      stats.adjustmentTime.stop();
    }
  }
}
