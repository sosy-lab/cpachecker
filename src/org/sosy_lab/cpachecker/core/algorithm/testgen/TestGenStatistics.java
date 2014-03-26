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
package org.sosy_lab.cpachecker.core.algorithm.testgen;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.EXTRACT_LOCATION;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.util.statistics.StatisticsUtils;



public class TestGenStatistics implements Statistics {

  private final Timer totalTimer = new Timer();
  private final Timer cpaAlogorithmTimer = new Timer();
  private final Timer pathCheckTimer = new Timer();
  private final Timer automatonFileGenerationTimer = new Timer();


  private volatile int cpaAlgorithmCount = 0;
  private volatile int countPathChecks = 0;
  private final boolean printAutomatonFileGenerationStats;
  private final List<Statistics> cpaAlgorithmStatistics = new ArrayList<>();
  private final List<ARGPath> testCases = new ArrayList<>();
  private CFA cfa;


  public TestGenStatistics(boolean pPrintAutomatonFileGenerationStats, CFA pCfa) {
    printAutomatonFileGenerationStats = pPrintAutomatonFileGenerationStats;
    cfa = pCfa;
  }

  @Override
  public String getName() {
    return "TestGen algorithm";
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult,
      ReachedSet pReached) {

    int locs = calculateTestedLocations();

    out.println("Number of CPA algorithm runs:         " + cpaAlgorithmCount);
    out.println("Locations covered by testcases:       " + locs);
    out.println("Testcase location coverage:           " + StatisticsUtils.toPercent(locs, cfa.getAllNodes().size()));

    if (cpaAlgorithmCount > 0) {

      out.println("Number of PathChecker querys:         " + countPathChecks);
      //out.println("Max. size of reached set before ref.: " + maxReachedSizeBeforeRefinement);
      //out.println("Max. size of reached set after ref.:  " + maxReachedSizeAfterRefinement);
      //out.println("Avg. size of reached set before ref.: " + div(totalReachedSizeBeforeRefinement, countRefinements));
      //out.println("Avg. size of reached set after ref.:  " + div(totalReachedSizeAfterRefinement, countSuccessfulRefinements));
      out.println("");
      out.println("Average time CPA algorithm runs:      " + cpaAlogorithmTimer.getAvgTime().formatAs(TimeUnit.SECONDS));
      out.println("Max time for CPA algorithm runs:      " + cpaAlogorithmTimer.getMaxTime().formatAs(TimeUnit.SECONDS));
      out.println("Time for CPA Algorithm runs:          " + cpaAlogorithmTimer);
      out.println("Time for PathChecker querys:          " + pathCheckTimer);
      out.println("Total time for TestGen algorithm:     " + totalTimer);
      if (printAutomatonFileGenerationStats) {
        out.println("Time next automaton file generation:  " + automatonFileGenerationTimer);
      }

      out.println();

      for (int i = 0; i < cpaAlgorithmStatistics.size(); i++) {
        out.println("-> Statistics for CPA algorithm run #" + i + ":");
        out.println();
        cpaAlgorithmStatistics.get(i).printStatistics(out, pResult, pReached);
        out.println();
      }

    }
  }

  private void collectStatisticsForAlgorithm(Algorithm pAlgorithm) {
    if(pAlgorithm instanceof StatisticsProvider) {
      StatisticsProvider sProvider = (StatisticsProvider) pAlgorithm;
      sProvider.collectStatistics(cpaAlgorithmStatistics);
    }
  }

  public void addTestCase(ARGPath pARGPath) {
    testCases.add(pARGPath);
  }

  public void beforeAutomationFileGeneration() {
    automatonFileGenerationTimer.start();
  }

  public void afterAutomatonFileGeneration() {
    automatonFileGenerationTimer.stop();
  }

  public void beforePathCheck() {
    countPathChecks++;
    pathCheckTimer.start();

  }

  public void afterPathCheck() {
    pathCheckTimer.stop();

  }

  public void beforeCpaAlgortihm() {
    cpaAlgorithmCount++;
    cpaAlogorithmTimer.start();
  }

  public void afterCpaAlgortihm(Algorithm pAlgorithm) {
    cpaAlogorithmTimer.stop();
    collectStatisticsForAlgorithm(pAlgorithm);
  }

  private int calculateTestedLocations() {

    Set<CFANode> locations = new HashSet<>();
    for (ARGPath path : testCases) {
      Set<CFANode> currentLocs = from(path.getStateSet()).transform(EXTRACT_LOCATION).filter(notNull()).toSet();
      locations.addAll(currentLocs);
    }

    return locations.size();

  }

  Timer getTotalTimer() {
    return totalTimer;
  }
}