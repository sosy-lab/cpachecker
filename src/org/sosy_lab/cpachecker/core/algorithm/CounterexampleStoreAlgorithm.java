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
package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Map;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;

public final class CounterexampleStoreAlgorithm implements Algorithm, StatisticsProvider {

  private final Algorithm algorithm;
  private final ARGCPA argCpa;
  private final AssumptionToEdgeAllocator allocator;

  public CounterexampleStoreAlgorithm(
      final Algorithm pCpaAlgorithm,
      final ConfigurableProgramAnalysis pCpa,
      final Configuration pConfig,
      final LogManager pLogger,
      final MachineModel pMachineModel
  )
      throws InvalidConfigurationException {

    algorithm = pCpaAlgorithm;
    argCpa = CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, CounterexampleStoreAlgorithm.class);
    allocator = AssumptionToEdgeAllocator.create(pConfig, pLogger, pMachineModel);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException {

    AlgorithmStatus status = algorithm.run(pReachedSet);

    if (pReachedSet.hasViolatedProperties()) {
      Map<ARGState, CounterexampleInfo> cexs = getAllCounterexamples(pReachedSet);

      for (Map.Entry<ARGState, CounterexampleInfo> e : cexs.entrySet()) {
        ARGState targetState = e.getKey();
        if (!targetState.getCounterexampleInformation().isPresent()) {
          targetState.addCounterexampleInformation(e.getValue());
        }
      }

    }

    return status;
  }

  // TODO: This is duplicate code with ARGStatistics#getAllCounterexamples
  private Map<ARGState, CounterexampleInfo> getAllCounterexamples(
      final UnmodifiableReachedSet pReached) {
    ImmutableMap.Builder<ARGState, CounterexampleInfo> counterexamples = ImmutableMap.builder();

    for (AbstractState targetState : from(pReached).filter(IS_TARGET_STATE)) {
      ARGState s = (ARGState) targetState;
      CounterexampleInfo cex =
          ARGUtils.tryGetOrCreateCounterexampleInformation(s, argCpa, allocator)
              .orElse(null);
      if (cex != null) {
        counterexamples.put(s, cex);
      }
    }

    Map<ARGState, CounterexampleInfo> allCounterexamples = counterexamples.build();
    final Map<ARGState, CounterexampleInfo> preciseCounterexamples =
        Maps.filterValues(allCounterexamples, cex -> cex.isPreciseCounterExample());
    return preciseCounterexamples.isEmpty() ? allCounterexamples : preciseCounterexamples;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider) algorithm).collectStatistics(statsCollection);
    }
  }
}
