// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.monitor;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import java.util.Optional;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSetView;
import org.sosy_lab.cpachecker.cpa.monitor.MonitorState.TimeoutState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.assumptions.PreventingHeuristic;

/**
 * Precision Adjustment for Monitoring. Simply delegates the operation to the wrapped CPA's
 * precision adjustment operator and updates the {@link MonitorState} based on this computation.
 */
public class MonitorPrecisionAdjustment implements PrecisionAdjustment {

  private final PrecisionAdjustment wrappedPrecAdjustment;

  final Timer totalTimeOfPrecAdj = new Timer();

  public MonitorPrecisionAdjustment(PrecisionAdjustment pWrappedPrecAdjustment) {
    wrappedPrecAdjustment = pWrappedPrecAdjustment;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pElement,
      Precision oldPrecision,
      UnmodifiableReachedSet pElements,
      Function<AbstractState, AbstractState> projection,
      AbstractState fullState)
      throws CPAException, InterruptedException {

    Preconditions.checkArgument(pElement instanceof MonitorState);
    MonitorState element = (MonitorState) pElement;

    if (element.getWrappedState() == TimeoutState.INSTANCE) {

      // we can't call prec() in this case because we don't have an element of the CPA
      return Optional.of(PrecisionAdjustmentResult.create(pElement, oldPrecision, Action.CONTINUE));
    }

    UnmodifiableReachedSet elements =
        new UnmodifiableReachedSetView(
            pElements,
            (state) -> ((MonitorState) state).getWrappedState(),
            Functions.<Precision>identity());
    // TODO we really would have to filter out all TimeoutElements in this view

    AbstractState oldElement = element.getWrappedState();

    totalTimeOfPrecAdj.start();
    Optional<PrecisionAdjustmentResult> unwrappedResult =
        wrappedPrecAdjustment.prec(
            oldElement,
            oldPrecision,
            elements,
            Functions.compose((state) -> ((MonitorState) state).getWrappedState(), projection),
            fullState);
    totalTimeOfPrecAdj.stop();
    long totalTimeOfExecution = totalTimeOfPrecAdj.getLengthOfLastInterval().asMillis();
    // add total execution time to the total time of the previous element
    long updatedTotalTime = totalTimeOfExecution + element.getTotalTimeOnPath();

    Pair<PreventingHeuristic, Long> preventingCondition = element.getPreventingCondition();
    // TODO we should check for timeLimitForPath here
    //    if (preventingCondition != null) {
    //      if (timeLimitForPath > 0 && updatedTotalTime > timeLimitForPath) {
    //        preventingCondition = Pair.of(PreventingHeuristicType.PATHCOMPTIME, timeLimitForPath);
    //      }
    //    }
    if (!unwrappedResult.isPresent()) {
      return Optional.empty();
    }

    PrecisionAdjustmentResult unwrapped = unwrappedResult.orElseThrow();

    // no. of nodes and no. of branches on the path does not change, just update the
    // set the adjusted wrapped element and update the time
    MonitorState resultElement =
        new MonitorState(unwrapped.abstractState(), updatedTotalTime, preventingCondition);

    return Optional.of(unwrapped.withAbstractState(resultElement));
  }
}
