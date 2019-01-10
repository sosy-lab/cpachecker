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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.interval;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.conditions.path.AssignmentsInPathCondition.UniqueAssignmentsInPathConditionState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class IntervalAnalysisPrecisionAdjustment implements PrecisionAdjustment {

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pState,
      Precision pPrecision,
      UnmodifiableReachedSet pStates,
      Function<AbstractState, AbstractState> projection,
      AbstractState fullState)
      throws CPAException, InterruptedException {

    return prec(
        (IntervalAnalysisState) pState,
        (IntervalAnalysisPrecision) pPrecision,
        AbstractStates.extractStateByType(fullState, LocationState.class),
        AbstractStates.extractStateByType(fullState, UniqueAssignmentsInPathConditionState.class));
  }

  private Optional<PrecisionAdjustmentResult> prec(
      IntervalAnalysisState pState,
      IntervalAnalysisPrecision pPrecision,
      LocationState location,
      UniqueAssignmentsInPathConditionState assignments) {
    IntervalAnalysisState resultState = IntervalAnalysisState.copyOf(pState);

    enforcePrecision(resultState, location, pPrecision);

    return Optional.of(PrecisionAdjustmentResult.create(resultState, pPrecision, Action.CONTINUE));
  }

  /**
   * This method performs an abstraction computation on the current value-analysis state.
   *
   * @param location the current location
   * @param state the current state
   * @param precision the current precision
   */
  private void enforcePrecision(
      IntervalAnalysisState state, LocationState location, IntervalAnalysisPrecision precision) {
    for (Entry<String, Interval> memoryLocation : state.getConstants()) {
      String memString = memoryLocation.getKey();
      MemoryLocation mem = MemoryLocation.valueOf(memString);
      if (location != null && !precision.isTracking(memString)) {
        state.forget(mem);
      } else { // precision is tracking that variable
        Interval stateInterval = state.getInterval(memString);
        if(stateInterval.getHigh() - stateInterval.getLow() < precision.getValue(memString)){
          long low = stateInterval.getLow();
          state.removeInterval(memString);
          state.addInterval(memString, new Interval(low, low + precision.getValue(memString)), 2000);
        }
      }
    }
  }

  @Options(prefix = "cpa.interval.abstraction")
  public static class PrecAdjustmentOptions {

    @Option(secure = true, description = "restrict abstraction computations to loop heads")
    private boolean alwaysAtLoop = false;

    private final ImmutableSet<CFANode> loopHeads;

    public PrecAdjustmentOptions(Configuration config, CFA pCfa)
        throws InvalidConfigurationException {
      config.inject(this);

      if (alwaysAtLoop && pCfa.getAllLoopHeads().isPresent()) {
        loopHeads = pCfa.getAllLoopHeads().get();
      } else {
        loopHeads = null;
      }
    }
  }
}
