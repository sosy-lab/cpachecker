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
package org.sosy_lab.cpachecker.core.algorithm.mpa;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatistics;
import org.sosy_lab.cpachecker.util.statistics.StatCpuTime;
import org.sosy_lab.cpachecker.util.statistics.StatCpuTime.NoTimeMeasurement;

import java.io.PrintStream;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

class DecompositionStatistics extends AbstractStatistics {

  int numberOfRestarts = 0;
  int numberOfPartitionExhaustions = 0;
  final StatCpuTime pureAnalysisTime = new StatCpuTime();
  final Set<Property> consideredProperties = Sets.newLinkedHashSet();
  final List<Integer> reachedStates = Lists.newArrayList();
  final List<Integer> reachedStatesWithFixpoint = Lists.newArrayList();
  final List<Integer> reachedStatesForRelPropsWithFixpoint = Lists.newArrayList();

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
    super.printStatistics(pOut, pResult, pReached);

    put(pOut, 0, "Number of restarts", numberOfRestarts);
    put(pOut, 0, "Number of exhaustions", numberOfPartitionExhaustions);
    put(pOut, 0, "Number of analysis runs", pureAnalysisTime.getIntervals());
    pOut.println("");

    try {
      put(pOut, 0, "Min. analysis CPU time", pureAnalysisTime.getMinCpuTimeSum().formatAs(TimeUnit.SECONDS));
      put(pOut, 0, "Max. analysis CPU time", pureAnalysisTime.getMaxCpuTimeSum().formatAs(TimeUnit.SECONDS));
      put(pOut, 0, "Avg. analysis CPU time", pureAnalysisTime.getAvgCpuTimeSum().formatAs(TimeUnit.SECONDS));
      put(pOut, 0, "(Total) Single analysis CPU time", pureAnalysisTime.getCpuTimeSum().formatAs(TimeUnit.SECONDS));
      pOut.println("");
    } catch (NoTimeMeasurement e) {
    }

    // Statistics on the reached-sets
    final String fpOnly = "(fix-points only)";
    final String fpRelPropsOnly = "(fix-points with relevant props. only)";
    final String fpAlso = "(exhausted only)";

    put(pOut, 0, "Statistics on the set 'reached' " + fpOnly);
    printStatisticsOnReachedStates(pOut, 1, fpOnly, reachedStatesWithFixpoint);
    pOut.println("");

    put(pOut, 0, "Statistics on the set 'reached' " + fpAlso);
    printStatisticsOnReachedStates(pOut, 1, fpAlso, reachedStates);
    pOut.println("");

    put(pOut, 0, "Statistics on the set 'reached' " + fpRelPropsOnly);
    printStatisticsOnReachedStates(pOut, 1, fpRelPropsOnly, reachedStatesForRelPropsWithFixpoint);
    pOut.println("");

    PropertyStats.INSTANCE.printStatistics(pOut, pResult, pReached);
  }

  private void printStatisticsOnReachedStates(PrintStream pOut, int pLevel, String pStatPostfix,
      List<Integer> pReachedStates) {

      int maxStates = Integer.MIN_VALUE;
      int minStates = Integer.MAX_VALUE;
      int totalStates = 0;
      int setCount = 0;

      for (Integer numStates: pReachedStates) {
        setCount = setCount + 1;
        totalStates = totalStates + numStates;
        maxStates = Math.max(maxStates, numStates);
        minStates = Math.min(minStates, numStates);
      }

      if (setCount > 0) {
        int setRange = maxStates - minStates;
        int avgStates = totalStates / setCount;
        put(pOut, pLevel, "Number of sets" + " " + pStatPostfix, setCount);
        put(pOut, pLevel, "Max. states reached" + " " + pStatPostfix, maxStates);
        put(pOut, pLevel, "Min. states reached" + " " + pStatPostfix, minStates);
        put(pOut, pLevel, "Range of states reached" + " " + pStatPostfix, setRange);
        put(pOut, pLevel, "Avg. states reached" + " " + pStatPostfix, avgStates);
      }
  }

}
