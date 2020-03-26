/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.mutation.strategy;

import com.google.common.collect.ImmutableList;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class CycleStrategy extends CompositeStrategy {
  private AbstractMutationStatistics thisCycle;

  private class CycleStatistics extends AbstractMutationStatistics {
    private final StatCounter cycles = new StatCounter("cycles");
    private final List<Pair<AbstractMutationStatistics, List<Statistics>>> cycleStats = new ArrayList<>();

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      super.printStatistics(pOut, pResult, pReached);
      StatisticsWriter w = StatisticsWriter.writingStatisticsTo(pOut);
      w.beginLevel().put(cycles);
      if (cycles.getUpdateCount() > 0) {
        for (Pair<AbstractMutationStatistics, List<Statistics>> p : cycleStats) {
          p.getFirst().printStatistics(pOut, pResult, pReached);
          p.getSecond().forEach(s -> s.printStatistics(pOut, pResult, pReached));
        }
      }
      w.endLevel();
    }
  }

  public CycleStrategy(LogManager pLogger) {
    super(
        pLogger,
        ImmutableList.of(
            // First, remove statements if possible
            new StatementNodeStrategy(pLogger, 5, 1),
            new DummyStrategy(pLogger),
            // Second, remove AssumeEdges if possible
            new SimpleAssumeEdgeStrategy(pLogger, 5, 1),
            new DummyStrategy(pLogger),
            // Then remove blank edges
            new BlankNodeStrategy(pLogger, 5, 0),
            new DummyStrategy(pLogger)));
    stats = new CycleStatistics();
  }

  @Override
  public boolean mutate(ParseResult pParseResult) {
    if (stats.rounds.getUpdateCount() == 0) {
      ((CycleStatistics) stats).cycles.inc();
      logger.logf(Level.INFO, "Starting cycle 1");
      thisCycle = createThisCycle();
    }
    if (super.mutate(pParseResult)) {
      thisCycle.rounds.inc();
      return true;
    }
    goNextCycle();
    return super.mutate(pParseResult);
  }

  private void goNextCycle() {
    List<Statistics> subStrStats = new ArrayList<>();
    for (AbstractCFAMutationStrategy strategy : strategiesList) {
      strategy.collectStatistics(subStrStats);
    }
    ((CycleStatistics) stats).cycleStats.add(Pair.of(thisCycle, subStrStats));
    ((CycleStatistics) stats).cycles.inc();
    thisCycle = createThisCycle();
    thisCycle.rounds.inc();
    logger.logf(Level.INFO, "Starting cycle %d", ((CycleStatistics) stats).cycles.getValue());

    strategies = strategiesList.iterator();
    currentStrategy = strategies.next();
  }

  private AbstractMutationStatistics createThisCycle() {
    return new AbstractMutationStatistics() {
      protected final int cycle = (int) ((CycleStatistics) stats).cycles.getValue();

      @Override
      public String toString() {
        return super.toString() + " " + cycle;
      }
    };
  }

  @Override
  public void rollback(ParseResult pParseResult) {
    thisCycle.rollbacks.inc();
    super.rollback(pParseResult);
  }

  @Override
  public String toString() {
    return super.toString() + ", " + ((CycleStatistics) stats).cycles.getValue() + " cycles";
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    List<Statistics> subStrStats = new ArrayList<>();
    for (AbstractCFAMutationStrategy strategy : strategiesList) {
      strategy.collectStatistics(subStrStats);
    }
    ((CycleStatistics) stats).cycleStats.add(Pair.of(thisCycle, subStrStats));

    pStatsCollection.add(stats);
    stats = new CycleStatistics();
  }
}
