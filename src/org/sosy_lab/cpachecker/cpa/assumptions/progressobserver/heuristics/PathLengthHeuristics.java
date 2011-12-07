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

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsConsumer;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.ReachedHeuristicsDataSetView;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.StopHeuristics;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.StopHeuristicsData;

/**
 * This heuristic keeps track of the number of nodes on a path.
 * If the given threshold is exceed, it returns bottom and the assumption
 * collector algorithm is notified.
 */
public class PathLengthHeuristics implements StopHeuristics<PathLengthHeuristicsData>, StatisticsProvider{

  class PathLengthHeuristicsStatistics implements Statistics {

    @Override
    public String getName() {
      return "Path Length Heuristics Statistics";
    }

    @Override
    public void printStatistics(PrintStream pOut, Result pResult,
        ReachedSet pReached) {
      pOut.println("Maximum length of a path: " + PathLengthHeuristicsData.maxLenghtOfPath);
      pOut.println("Threshold value:          " + PathLengthHeuristicsData.threshold);
    }
  }

  public PathLengthHeuristicsPrecision precision;
  public PathLengthHeuristicsStatistics stats;

  public PathLengthHeuristics(Configuration config, LogManager logger)
      throws InvalidConfigurationException{
    precision = new PathLengthHeuristicsPrecision(config, logger);
    stats = new PathLengthHeuristicsStatistics();
  }

  @Override
  public PathLengthHeuristicsData collectData(StopHeuristicsData pData,
      ReachedHeuristicsDataSetView pReached) {
    return (PathLengthHeuristicsData)pData;
  }

  @Override
  public PathLengthHeuristicsData getInitialData(CFANode pNode) {
    return new PathLengthHeuristicsData(1);
  }

  @Override
  public PathLengthHeuristicsData processEdge(StopHeuristicsData pData,
      CFAEdge pEdge) {
    return ((PathLengthHeuristicsData)pData).updateForEdge(pData, precision.getThreshold());
  }

  @Override
  public void collectStatistics(StatisticsConsumer statsConsumer) {
    statsConsumer.addTerminationStatistics(new Statistics[]{stats});
  }

  @Override
  public HeuristicPrecision getInitialPrecision() {
    return precision;
  }

}
