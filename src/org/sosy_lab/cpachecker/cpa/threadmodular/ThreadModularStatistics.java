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
package org.sosy_lab.cpachecker.cpa.threadmodular;

import java.io.PrintStream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class ThreadModularStatistics implements Statistics {

  StatTimer totalTransfer = new StatTimer("Total time for transfer");
  StatTimer wrappedTransfer = new StatTimer("Total time for wrapped transfer");
  StatTimer allApplyActions = new StatTimer("Total time for environment calculation");
  StatTimer applyOperator = new StatTimer("Total time for apply operations");
  StatTimer projectOperator = new StatTimer("Total time for project operations");
  StatCounter applyCounter = new StatCounter("Number of apply operations");
  StatCounter relevantApplyCounter = new StatCounter("Number of relevant apply operations");
  StatCounter numberOfTransitionsInThreadProduced =
      new StatCounter("Number of obtained transitions in thread");
  StatCounter numberOfProjectionsProduced = new StatCounter("Number of obtained projections");
  StatCounter numberOfTransitionsInThreadConsidered =
      new StatCounter("Number of considered transitions in thread");
  StatCounter numberOfProjectionsConsidered = new StatCounter("Number of considered projections");
  StatCounter numberOfTransitionsInEnvironmentConsidered =
      new StatCounter("Number of considered transitions in environment");
  StatCounter numberOfValuableTransitionsInEnvironement =
      new StatCounter("Number of valuable transitions in environment");

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    StatisticsWriter.writingStatisticsTo(pOut)
        .put(totalTransfer)
        .beginLevel()
        .put(wrappedTransfer)
        .put(allApplyActions)
        .beginLevel()
        .put(applyOperator)
        .put(projectOperator)
        .put(applyCounter)
        .put(relevantApplyCounter)
        .endLevel()
        .endLevel()
        .spacer()
        .put(numberOfTransitionsInThreadProduced)
        .put(numberOfProjectionsProduced)
        .put(numberOfTransitionsInThreadConsidered)
        .put(numberOfTransitionsInEnvironmentConsidered)
        .put(numberOfValuableTransitionsInEnvironement)
        .put(numberOfProjectionsConsidered);
  }

  @Override
  public @Nullable String getName() {
    return "ThreadModularCPA";
  }

}
