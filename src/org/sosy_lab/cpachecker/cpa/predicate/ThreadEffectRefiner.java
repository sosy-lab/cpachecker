/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.predicate;

import static org.sosy_lab.cpachecker.util.statistics.StatisticsWriter.writingStatisticsTo;

import java.io.PrintStream;
import java.util.Collection;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

/**
 * Refines a path to target states and all paths to an effect independently
 */
public class ThreadEffectRefiner extends ThreadModularCPARefiner {
  private final StatCounter numberOfPaths = new StatCounter("Number of paths with effects");
  private final StatCounter numberOfSpuriousPathes =
      new StatCounter("Number of spurious paths with effects");

  public ThreadEffectRefiner(
      LogManager pLogger,
      GlobalRefinementStrategy pStrategy,
      Configuration pConfig,
      ARGBasedRefiner pDelegate)
      throws InvalidConfigurationException {

    super(pLogger, pStrategy, pConfig, pDelegate);
  }

  @Override
  public CounterexampleInfo
      performRefinementForPath(final ARGReachedSet pReached, final ARGPath allStatesTrace)
          throws CPAException, InterruptedException {
    totalTime.start();

    if (singleRefinementLevel) {
      strategy.initializeGlobalRefinement();
    }

    for (ARGState state : allStatesTrace.asStatesList()) {
      if (state.getAppliedFrom() != null) {
        for (ARGState projection : state.getAppliedFrom().getSecond().getProjectedFrom()) {
          numberOfPaths.inc();
          ARGPath pathToAppliedstate = ARGUtils.getOnePathTo(projection);
          CounterexampleInfo inf = delegate.performRefinementForPath(pReached, pathToAppliedstate);
          if (inf.isSpurious()) {
            numberOfSpuriousPathes.inc();
          }
        }
      }
    }

    delegatingTime.start();
    CounterexampleInfo couterexample = delegate.performRefinementForPath(pReached, allStatesTrace);
    delegatingTime.stop();

    if (couterexample.isSpurious() && singleRefinementLevel) {
      strategy.updatePrecisionAndARG();
    }
    totalTime.stop();
    return couterexample;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Stats());
    if (delegate instanceof StatisticsProvider) {
      ((StatisticsProvider) delegate).collectStatistics(pStatsCollection);
    }
  }

  private class Stats extends ThreadModularCPARefiner.Stats {

    @Override
    public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
      super.printStatistics(out, result, reached);
      StatisticsWriter w0 = writingStatisticsTo(out);
      w0.put(numberOfPaths)
          .put(numberOfSpuriousPathes);
    }

    @Override
    public String getName() {
      return "ThreadEffectRefiner";
    }
  }
}
