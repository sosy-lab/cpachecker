/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.smg;

import com.google.common.base.Function;
import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

import java.io.PrintStream;
import java.util.Collection;


public class SMGPrecisionAdjustment implements PrecisionAdjustment, StatisticsProvider {

  // statistics
  final StatCounter abstractions    = new StatCounter("Number of abstraction computations");
  final StatTimer totalAbstraction  = new StatTimer("Total time for abstraction computation");

  private final Statistics statistics;


  public SMGPrecisionAdjustment() {

    statistics = new Statistics() {
      @Override
      public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {

        StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(pOut);
        writer.put(abstractions);
        writer.put(totalAbstraction);
      }

      @Override
      public String getName() {
        return SMGPrecisionAdjustment.this.getClass().getSimpleName();
      }
    };

  }



  @Override
  public Optional<PrecisionAdjustmentResult> prec(AbstractState pState, Precision pPrecision,
      UnmodifiableReachedSet pStates, Function<AbstractState, AbstractState> pStateProjection, AbstractState pFullState)
          throws CPAException, InterruptedException {

    return prec((SMGState) pState, (VariableTrackingPrecision) pPrecision,
        AbstractStates.extractStateByType(pFullState, LocationState.class));
  }

  private Optional<PrecisionAdjustmentResult> prec(SMGState pState, VariableTrackingPrecision pPrecision,
      LocationState location) throws CPAException {

    if(!pPrecision.allowsAbstraction()) {
      return Optional.of(PrecisionAdjustmentResult.create(pState, pPrecision, Action.CONTINUE));
    }

    totalAbstraction.start();
    SMGState resultState = new SMGState(pState);

    for (MemoryLocation memoryLocation : pState.getTrackedMemoryLocations()) {

      CType typeOfValueAtMemloc = pState.getTypeForMemoryLocation(memoryLocation);

      if (!pPrecision.isTracking(memoryLocation, typeOfValueAtMemloc,
          location.getLocationNode())) {
        resultState.forget(memoryLocation);
      }
    }

    resultState.pruneUnreachable();
    totalAbstraction.stop();
    abstractions.inc();
    return Optional.of(PrecisionAdjustmentResult.create(resultState, pPrecision, Action.CONTINUE));
  }

  @Override
  public Optional<PrecisionAdjustmentResult> postAdjustmentStrengthen(
      AbstractState result,
      Precision precision,
      Iterable<AbstractState> otherStates,
      Iterable<Precision> otherPrecisions,
      UnmodifiableReachedSet states,
      Function<AbstractState, AbstractState> stateProjection,
      AbstractState resultFullState) throws CPAException, InterruptedException {
    return Optional.of(PrecisionAdjustmentResult.create(result, precision, Action.CONTINUE));
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(statistics);
  }
}
