/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.monitor;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSetView;
import org.sosy_lab.cpachecker.cpa.monitor.MonitorState.TimeoutState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.assumptions.PreventingHeuristic;

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;

/**
 * Precision Adjustment for Monitoring.
 * Simply delegates the operation to the wrapped CPA's precision adjustment operator
 * and updates the {@link MonitorState} based on this computation.
 */
public class MonitorPrecisionAdjustment implements PrecisionAdjustment {

  private final PrecisionAdjustment wrappedPrecAdjustment;

  final Timer totalTimeOfPrecAdj = new Timer();

  public MonitorPrecisionAdjustment(PrecisionAdjustment pWrappedPrecAdjustment) {
    wrappedPrecAdjustment = pWrappedPrecAdjustment;
  }

  @Override
  public Triple<AbstractState, Precision, Action> prec(
      AbstractState pElement, Precision oldPrecision,
      UnmodifiableReachedSet pElements) throws CPAException, InterruptedException {

    Preconditions.checkArgument(pElement instanceof MonitorState);
    MonitorState element = (MonitorState)pElement;

    if (element.getWrappedState() == TimeoutState.INSTANCE) {
      // we can't call prec() in this case because we don't have an element of the CPA
      return Triple.of(pElement, oldPrecision, Action.CONTINUE);
    }

    UnmodifiableReachedSet elements = new UnmodifiableReachedSetView(
        pElements,  MonitorState.getUnwrapFunction(), Functions.<Precision>identity());
    // TODO we really would have to filter out all TimeoutElements in this view

    AbstractState oldElement = element.getWrappedState();

    totalTimeOfPrecAdj.start();
    Triple<AbstractState, Precision, Action> unwrappedResult = wrappedPrecAdjustment.prec(oldElement, oldPrecision, elements);
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

    AbstractState newElement = unwrappedResult.getFirst();
    Precision newPrecision = unwrappedResult.getSecond();
    Action action = unwrappedResult.getThird();

      // no. of nodes and no. of branches on the path does not change, just update the
      // set the adjusted wrapped element and update the time
    MonitorState resultElement =
      new MonitorState(newElement, updatedTotalTime, preventingCondition);

    return Triple.<AbstractState, Precision, Action>of(resultElement, newPrecision, action);
  }
}
