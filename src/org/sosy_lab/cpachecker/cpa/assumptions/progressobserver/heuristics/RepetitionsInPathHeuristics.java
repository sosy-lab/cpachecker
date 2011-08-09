/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.heuristics;

import java.io.PrintStream;
import java.util.Collection;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.ReachedHeuristicsDataSetView;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.StopHeuristics;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.StopHeuristicsData;

public class RepetitionsInPathHeuristics implements StopHeuristics<RepetitionsInPathHeuristicsData>, StatisticsProvider {

  class RepetitionsInPathHeuristicsStatistics implements Statistics {

    @Override
    public String getName() {
      return "Repetitions In Path Heuristics Statistics";
    }

    @Override
    public void printStatistics(PrintStream pOut, Result pResult,
        ReachedSet pReached) {
      pOut.println("Maximum number of repetitions: " + RepetitionsInPathHeuristicsData.maxNoOfRepetitions);
      pOut.println("Threshold value:               " + RepetitionsInPathHeuristicsData.threshold);
    }
  }

  RepetitionsInPathHeuristicsPrecision precision;
  RepetitionsInPathHeuristicsStatistics stats;

  public RepetitionsInPathHeuristics(Configuration config, LogManager logger)
      throws InvalidConfigurationException
  {
    precision = new RepetitionsInPathHeuristicsPrecision(config, logger);
    stats = new RepetitionsInPathHeuristicsStatistics();
  }

  @Override
  public RepetitionsInPathHeuristicsData getInitialData(CFANode pNode) {
    return new RepetitionsInPathHeuristicsData();
  }

  @Override
  public RepetitionsInPathHeuristicsData collectData(StopHeuristicsData pData,
      ReachedHeuristicsDataSetView pReached) {
    return (RepetitionsInPathHeuristicsData)pData;
  }

  @Override
  public RepetitionsInPathHeuristicsData processEdge(StopHeuristicsData pData,
      CFAEdge pEdge) {
    return ((RepetitionsInPathHeuristicsData)pData).updateForEdge(pEdge, precision.getThresholdFunction());
  }

  @Override
  public HeuristicPrecision getPrecision() {
    return precision;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

}
