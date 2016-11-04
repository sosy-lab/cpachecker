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

import java.io.PrintStream;
import java.util.Collection;

import javax.annotation.Nullable;

import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.constraints.refiner.precision.ConstraintsPrecision;
import org.sosy_lab.cpachecker.util.AbstractStates;

import com.google.common.base.Function;
import java.util.Optional;

/**
 * {@link PrecisionAdjustment} for
 * {@link org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA ConstraintsCPA}.
 */
public class ConstraintsPrecisionAdjustment implements PrecisionAdjustment, StatisticsProvider {

  // Statistics
  private int maxFullConstraintNumber = 0;
  private int maxRealConstraintNumber = 0;
  private int overallFullConstraintNumber = 0;
  private int overallRealConstraintNumber = 0;
  private final Timer adjustmentTime = new Timer();

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

    int fullConstraintNumber = 0;
    int realConstraintNumber = 0;


    adjustmentTime.start();
    ConstraintsState result = pStateToAdjust.copyOf();

    try {
      for (Constraint c : pStateToAdjust) {
        fullConstraintNumber++;
        overallFullConstraintNumber++;
        CFANode currentLocation = AbstractStates.extractLocation(pFullState);

        if (!pPrecision.isTracked(c, currentLocation)) {
          result.remove(c);

        } else {
          realConstraintNumber++;
          overallRealConstraintNumber++;
        }
      }
    } finally {
      adjustmentTime.stop();
    }

    if (fullConstraintNumber > maxFullConstraintNumber) {
      maxFullConstraintNumber = fullConstraintNumber;
    }

    if (realConstraintNumber > maxRealConstraintNumber) {
      maxRealConstraintNumber = realConstraintNumber;
    }

    result = result.equals(pStateToAdjust) ? pStateToAdjust : result;

    return Optional.of(PrecisionAdjustmentResult.create(result, pPrecision, Action.CONTINUE));
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(new Statistics() {

      @Override
      public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
        out.println("Most constraints after refinement in state: " + maxRealConstraintNumber);
        out.println("Most constraints before refinement in state: " + maxFullConstraintNumber);
        out.println("Constraints after refinement in state: " + overallRealConstraintNumber);
        out.println("Constraints before refinement in state: " + overallFullConstraintNumber);
        out.println("Average time for constraints adjustment: " + adjustmentTime.getAvgTime());
        out.println("Complete time for constraints adjustment: " + adjustmentTime.getSumTime());
      }

      @Nullable
      @Override
      public String getName() {
        return ConstraintsPrecisionAdjustment.this.getClass().getSimpleName();
      }
    });
  }
}
