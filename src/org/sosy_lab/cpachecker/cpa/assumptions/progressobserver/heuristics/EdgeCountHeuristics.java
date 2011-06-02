/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.ReachedHeuristicsDataSetView;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.StopHeuristics;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.StopHeuristicsData;

import com.google.common.base.Function;
import com.google.common.base.Functions;


/**
 * @author g.theoduloz
 */
public class EdgeCountHeuristics implements StopHeuristics<EdgeCountHeuristicsData> {

  private final Function<? super CFAEdge, Integer> thresholdFunction;

  public EdgeCountHeuristics(Configuration config, LogManager logger)
  {
    // Initialise the threshold function
    int staticThreshold = Integer.parseInt(config.getProperty("threshold", "-1").trim());
    thresholdFunction = Functions.constant((staticThreshold <= 0) ? null : staticThreshold);
  }

  @Override
  public EdgeCountHeuristicsData getInitialData(CFANode node) {
    return new EdgeCountHeuristicsData(node);
  }

  @Override
  public EdgeCountHeuristicsData collectData(StopHeuristicsData pData, ReachedHeuristicsDataSetView pReached) {
    return ((EdgeCountHeuristicsData)pData).collectData(pReached);
  }

  @Override
  public EdgeCountHeuristicsData processEdge(StopHeuristicsData pData, CFAEdge pEdge) {
    return ((EdgeCountHeuristicsData)pData).updateForEdge(pEdge, thresholdFunction);
  }

}
