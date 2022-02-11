// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.worker;

import java.io.PrintStream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class WorkerStatistics implements Statistics {

  final StatCounter forwardAnalysis = new StatCounter("Number of triggered forward analyses");
  final StatCounter backwardAnalysis = new StatCounter("Number of triggered backward analyses");
  final StatCounter sentMessages = new StatCounter("Number of sent messages");

  @Override
  public void printStatistics(
      PrintStream out, Result result, UnmodifiableReachedSet reached) {
    StatisticsWriter.writingStatisticsTo(out)
        .put(forwardAnalysis)
        .putIfUpdatedAtLeastOnce(backwardAnalysis)
        .putIfUpdatedAtLeastOnce(sentMessages);
  }

  @Override
  public @Nullable String getName() {
    return "Block Worker";
  }
}
