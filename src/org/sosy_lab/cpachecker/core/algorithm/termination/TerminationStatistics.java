/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.termination;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsUtils.valueWithPercentage;

import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

import java.io.PrintStream;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;


public class TerminationStatistics implements Statistics {

  private int totalLoops;

  private final Timer totalTime = new Timer();

  private final Timer loopTime = new Timer();

  private final Timer recursionTime = new Timer();

  private final Timer safetyAnalysisTime = new Timer();

  private final Timer lassoTime = new Timer();

  private final Timer lassoConstructionTime = new Timer();

  private final Timer lassoNonTerminationTime = new Timer();

  private final Timer lassoTerminationTime = new Timer();

  private final AtomicInteger totalLassos = new AtomicInteger();

  private final AtomicInteger maxSafetyAnalysisRuns = new AtomicInteger();

  private final AtomicInteger safetyAnalysisRunsCurrentLoop = new AtomicInteger();

  private final AtomicInteger maxLassosPerLoop = new AtomicInteger();

  private final AtomicInteger lassosCurrentLoop = new AtomicInteger();

  private final AtomicInteger maxLassosPerIteration = new AtomicInteger();

  private final AtomicInteger lassosCurrentIteration = new AtomicInteger();

  public TerminationStatistics(int pTotalNumberOfLoops) {
    totalLoops = pTotalNumberOfLoops;
  }

  void algorithmStarted() {
    totalTime.start();
  }

  void algorithmFinished() {
    totalTime.stop();
    safetyAnalysisTime.stopIfRunning();
    lassoTime.stopIfRunning();
    loopTime.stopIfRunning();
  }

  void analysisOfLoopStarted() {
    loopTime.start();
  }

  void analysisOfLoopFinished() {
    loopTime.stop();
    recursionTime.stopIfRunning();
    safetyAnalysisTime.stopIfRunning();
    lassoTime.stopIfRunning();
    lassoConstructionTime.stopIfRunning();
    lassoNonTerminationTime.stopIfRunning();
    lassoTerminationTime.stopIfRunning();
    maxLassosPerLoop.accumulateAndGet(lassosCurrentLoop.getAndSet(0), Math::max);
    maxSafetyAnalysisRuns.accumulateAndGet(safetyAnalysisRunsCurrentLoop.getAndSet(0), Math::max);
  }

  void analysisOfRecursionStarted() {
    recursionTime.start();
  }

  void analysisOfRecursionFinished() {
    recursionTime.stop();
  }

  void safetyAnalysisStarted() {
    safetyAnalysisRunsCurrentLoop.incrementAndGet();
    safetyAnalysisTime.start();
  }

  void safetyAnalysisFinished() {
    safetyAnalysisTime.stop();
  }

  public void analysisOfLassosStarted() {
    lassoTime.start();
  }

  public void analysisOfLassosFinished() {
    lassoTime.stop();
    lassoConstructionTime.stopIfRunning();
    lassoNonTerminationTime.stopIfRunning();
    lassoTerminationTime.stopIfRunning();
    maxLassosPerIteration.accumulateAndGet(lassosCurrentIteration.getAndSet(0), Math::max);
  }

  public void lassoConstructionStarted() {
    lassoConstructionTime.start();
  }

  public void lassoConstructionFinished() {
    lassoConstructionTime.stop();
  }

  public void nonTerminationAnalysisOfLassoStarted() {
    lassoNonTerminationTime.start();
  }

  public void nonTerminationAnalysisOfLassoFinished() {
    lassoNonTerminationTime.stop();
  }

  public void terminationAnalysisOfLassoStarted() {
    lassoTerminationTime.start();
  }

  public void terminationAnalysisOfLassoFinished() {
    lassoTerminationTime.stop();
  }

  public void lassosConstructed(int numberOfLassos) {
    totalLassos.addAndGet(numberOfLassos);
    lassosCurrentLoop.addAndGet(numberOfLassos);
    lassosCurrentIteration.addAndGet(numberOfLassos);
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
    pOut.println("Total time :                                        " + totalTime);
    pOut.println("Time for recursion analysis:                        " + recursionTime);
    pOut.println();

    int loops = loopTime.getNumberOfIntervals();
    pOut.println("Number of analysed loops:                               " + valueWithPercentage(loops, totalLoops));
    pOut.println("Total time for loop analysis:                       " + loopTime);
    pOut.println("  Avg time per loop analysis:                       " + format(loopTime.getAvgTime()));
    pOut.println("  Max time per loop analysis:                       " + format(loopTime.getMaxTime()));
    pOut.println();

    pOut.println("Number of safety analysis runs:                     " + format(safetyAnalysisTime.getNumberOfIntervals()));
    pOut.println("Total time for safety analysis:                     " + safetyAnalysisTime);
    pOut.println("  Avg time per safety analysis run:                 " + format(safetyAnalysisTime.getAvgTime()));
    pOut.println("  Max time per safety analysis run:                 " + format(safetyAnalysisTime.getMaxTime()));
    pOut.println();

    int iterations = lassoTime.getNumberOfIntervals();
    int lassos = totalLassos.get();
    pOut.println("Number of analysed lassos:                          " + format(lassos));
    if (loops > 0) {
      pOut.println("  Avg number of lassos per loop:                    " + div(lassos, loops));
    }
    pOut.println("  Max number of lassos per loop:                    " + format(maxLassosPerLoop.get()));
    if (loops > 0) {
      pOut.println("  Avg number of lassos per iteration:               " + div(lassos, iterations));
    }
    pOut.println("  Max number of lassos per iteration:               " + format(maxLassosPerIteration.get()));
    pOut.println();

    pOut.println("Total time for lassos analysis:                     " + lassoTime);
    pOut.println("  Avg time per iteration:                           " + format(lassoTime.getAvgTime()));
    pOut.println("  Max time per iteration:                           " + format(lassoTime.getMaxTime()));
    pOut.println("  Time for lassos construction:                     " + lassoConstructionTime);
    pOut.println("    Avg time for lasso construction per iteration:  " + format(lassoConstructionTime.getAvgTime()));
    pOut.println("    Max time for lasso construction per iteration:  " + format(lassoConstructionTime.getMaxTime()));
    pOut.println("  Total time for non-termination analysis:          " + lassoNonTerminationTime);
    pOut.println("    Avg time for non-termination analysis per lasso:" + format(lassoNonTerminationTime.getAvgTime()));
    pOut.println("    Max time for non-termination analysis per lasso:" + format(lassoNonTerminationTime.getMaxTime()));
    pOut.println("  Total time for termination analysis:              " + lassoTerminationTime);
    pOut.println("    Avg time for termination analysis per lasso:    " + format(lassoTerminationTime.getAvgTime()));
    pOut.println("    Max time for termination analysis per lasso:    " + format(lassoTerminationTime.getMaxTime()));
  }

  @Override
  public @Nullable String getName() {
    return "Termination Algorithm";
  }

  private static String format(TimeSpan pTimeSpan) {
    return pTimeSpan.formatAs(SECONDS);
  }

  private static String format(int value) {
    return String.format(Locale.ROOT, "%5d", value);
  }

  private static String div(double val, double full) {
    return String.format(Locale.ROOT, "%8.2f", val/full);
  }
}
