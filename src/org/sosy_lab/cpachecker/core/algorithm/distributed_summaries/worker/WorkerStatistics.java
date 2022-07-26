// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import java.io.PrintStream;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.StatTimerSum;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class WorkerStatistics implements Statistics {

  final StatCounter forwardAnalysis = new StatCounter("Number of triggered forward analyses");
  final StatCounter backwardAnalysis = new StatCounter("Number of triggered backward analyses");
  final StatCounter sentMessages = new StatCounter("Number of sent messages");

  final StatTimerSum proceedForwardTime = new StatTimerSum("Time for proceed forward");
  final StatTimerSum proceedBackwardTime = new StatTimerSum("Time for proceed backward");
  final StatTimerSum proceedCombineTime = new StatTimerSum("Time for combine");
  final StatTimerSum proceedSerializeTime = new StatTimerSum("Time for serialize");
  final StatTimerSum proceedDeserializeTime = new StatTimerSum("Time for deserialize");
  final StatTimerSum forwardTimer = new StatTimerSum("Time for forward analysis");
  final StatTimerSum backwardTimer = new StatTimerSum("Time for backward analysis");
  final StatTimerSum faultLocalizationTime = new StatTimerSum("Time for fault localization");

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    StatisticsWriter.writingStatisticsTo(out)
        .put(forwardAnalysis)
        .put(backwardAnalysis)
        .put(sentMessages)
        .put(proceedForwardTime)
        .put(proceedBackwardTime)
        .put(proceedCombineTime)
        .put(proceedSerializeTime)
        .put(proceedDeserializeTime)
        .put(forwardTimer)
        .put(backwardTimer)
        .put(faultLocalizationTime);
  }

  @Override
  public String getName() {
    return "Block Worker";
  }
}
