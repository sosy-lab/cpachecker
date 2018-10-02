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
      final AbstractState pFullState
  ) {

    return prec((ConstraintsState) pStateToAdjust,
                (ConstraintsPrecision) pPrecision,
                pFullState);
  }

  private Optional<PrecisionAdjustmentResult> prec(
      final ConstraintsState pStateToAdjust,
      final ConstraintsPrecision pPrecision,
      final AbstractState pFullState
  ) {

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
