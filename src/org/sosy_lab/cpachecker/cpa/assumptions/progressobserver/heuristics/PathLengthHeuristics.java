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

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.ReachedHeuristicsDataSetView;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.StopHeuristics;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.StopHeuristicsData;

/**
 * This heuristic keeps track of the number of nodes on a path.
 * If the given threshold is exceed, it returns bottom and the assumption
 * collector algorithm is notified.
 */
@Options(prefix="cpa.assumptions.progressobserver.heuristics.pathLengthHeuristics")
public class PathLengthHeuristics implements StopHeuristics<PathLengthHeuristicsData>{

  @Option(description = "threshold for heuristics of progressobserver")
  private int threshold = -1;

  public PathLengthHeuristics(Configuration config, LogManager logger)
      throws InvalidConfigurationException{
    config.inject(this);
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
    return ((PathLengthHeuristicsData)pData).updateForEdge(pData, threshold);
  }

}