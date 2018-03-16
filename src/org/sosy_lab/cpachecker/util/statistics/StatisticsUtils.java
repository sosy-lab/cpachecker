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
package org.sosy_lab.cpachecker.util.statistics;

import com.google.common.base.Strings;
import java.io.PrintStream;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

public class StatisticsUtils {

  private static final TimeSpan STATISTICS_WARNING_TIME = TimeSpan.ofSeconds(10);

  private StatisticsUtils() { }

  public static String toPercent(double val, double full) {
    return String.format("%1.0f", val/full*100) + "%";
  }

  public static String valueWithPercentage(int value, int totalCount) {
    return value + " (" + toPercent(value, totalCount) + ")";
  }

  public static String div(double val, double full) {
    return String.format("%.2f", val / full);
  }

  public static void write(PrintStream target, int indentLevel, int outputNameColWidth,
      String name, Object value) {
    String indentation = Strings.repeat("  ", indentLevel);
    target.println(String.format("%-" + outputNameColWidth + "s %s",
                                 indentation + name + ":", value));
  }

  public static void write(PrintStream target, int indentLevel, int outputNameColWidth,
      AbstractStatValue stat) {
    write(target, indentLevel, outputNameColWidth, stat.getTitle(), stat.toString());
  }

  /**
   * This method calls {@link Statistics#printStatistics(PrintStream, Result,
   * UnmodifiableReachedSet)} but additionally prints the title and handles cases like statistics
   * that take too much resources.
   */
  public static void printStatistics(
      final Statistics pStatistics,
      final PrintStream pOut,
      final LogManager pLogger,
      final Result pResult,
      final UnmodifiableReachedSet pReached) {
    final String name = getStatisticsName(pStatistics);
    if (!Strings.isNullOrEmpty(pStatistics.getName())) {
      pOut.println();
      pOut.println(name);
      pOut.println(Strings.repeat("-", name.length()));
    }

    final Timer timer = new Timer();
    timer.start();
    try {
      pStatistics.printStatistics(pOut, pResult, pReached);
    } catch (OutOfMemoryError e) {
      pLogger.logUserException(
          Level.WARNING,
          e,
          "Out of memory while generating statistics from " + name + " and writing output files");
    }
    timer.stop();
    if (timer.getLengthOfLastInterval().compareTo(STATISTICS_WARNING_TIME) > 0) {
      pLogger.logf(Level.WARNING, "Generating statistics from %s took %s.", name, timer);
    }
  }

  /**
   * This method calls {@link Statistics#writeOutputFiles(Result, UnmodifiableReachedSet)} but
   * additionally handles cases like statistics that take too much resources.
   */
  public static void writeOutputFiles(
      Statistics statistics, LogManager logger, Result result, UnmodifiableReachedSet reached) {
    Timer timer = new Timer();
    timer.start();
    try {
      statistics.writeOutputFiles(result, reached);
    } catch (OutOfMemoryError e) {
      logger.logUserException(
          Level.WARNING,
          e,
          "Out of memory while writing output files from " + getStatisticsName(statistics));
    }
    timer.stop();
    if (timer.getLengthOfLastInterval().compareTo(STATISTICS_WARNING_TIME) > 0) {
      logger.logf(
          Level.WARNING,
          "Writing output files from %s took %s.",
          getStatisticsName(statistics),
          timer);
    }
  }

  private static String getStatisticsName(final Statistics pStatistics) {
    if (Strings.isNullOrEmpty(pStatistics.getName())) {
      return pStatistics.getClass().getName();
    } else {
      return pStatistics.getName() + " statistics";
    }
  }
}
