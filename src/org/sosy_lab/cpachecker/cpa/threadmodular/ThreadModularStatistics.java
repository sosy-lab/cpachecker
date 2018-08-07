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
package org.sosy_lab.cpachecker.cpa.threadmodular;

import java.io.PrintStream;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class ThreadModularStatistics implements Statistics {

  final StatTimer transferTimer = new StatTimer("Time for transfer relation");
  final StatTimer successorCalculationTimer =
      new StatTimer("Eexploring reached set");
  final StatTimer compatibleCheckTimer = new StatTimer("Compatible check");
  final StatTimer stopTimer = new StatTimer("Time for stop operator");
  final StatTimer stateCheckTimer = new StatTimer("Stop check of states");
  final StatTimer ioCheckTimer = new StatTimer("Stop check of inference objects");
  final StatTimer ioExtractTimer =
      new StatTimer("Extraction of inference objects");
  final StatTimer stateExtractTimer =
      new StatTimer("Extraction of states");
  final StatTimer mergeTimer = new StatTimer("Time for merge operator");
  final StatTimer stateMergeTimer = new StatTimer("Merging inference objects");
  final StatTimer ioMergeTimer = new StatTimer("Merging states");
  final StatTimer argMergeTimer = new StatTimer("ARG actions");

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(pOut);
    writer.put(transferTimer)
        .beginLevel()
        .put(successorCalculationTimer)
        .put(compatibleCheckTimer)
        .endLevel()
        .put(stopTimer)
        .beginLevel()
        .put(stateCheckTimer)
        .put(ioCheckTimer)
        .put(ioExtractTimer)
        .put(stateExtractTimer)
        .endLevel()
        .put(mergeTimer)
        .beginLevel()
        .put(stateMergeTimer)
        .put(ioMergeTimer)
        .put(argMergeTimer)
        .endLevel();

  }

  @Override
  public @Nullable String getName() {
    return "ThreadModular CPA";
  }

}
