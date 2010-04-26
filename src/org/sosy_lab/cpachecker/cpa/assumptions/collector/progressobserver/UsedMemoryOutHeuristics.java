/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package org.sosy_lab.cpachecker.cpa.assumptions.collector.progressobserver;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.core.LogManager;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

/**
 * @author g.theoduloz
 */
public class UsedMemoryOutHeuristics
  implements StopHeuristics<TrivialStopHeuristicsData>
{
  private final MemoryMXBean mxBean;
  private final long threshold;
  
  public UsedMemoryOutHeuristics(Configuration config, LogManager logger) {
    mxBean = ManagementFactory.getMemoryMXBean();
    threshold = Long.parseLong(config.getProperty("threshold", "-1").trim());
  }

  @Override
  public TrivialStopHeuristicsData collectData(StopHeuristicsData pData, ReachedHeuristicsDataSetView pReached) {
    return (TrivialStopHeuristicsData) pData;
  }

  @Override
  public TrivialStopHeuristicsData getBottom() {
    return TrivialStopHeuristicsData.BOTTOM;
  }

  @Override
  public TrivialStopHeuristicsData getInitialData(CFANode pNode) {
    return TrivialStopHeuristicsData.TOP;
  }

  @Override
  public TrivialStopHeuristicsData getTop() {
    return TrivialStopHeuristicsData.TOP;
  }

  @Override
  public TrivialStopHeuristicsData processEdge(StopHeuristicsData pData, CFAEdge pEdge) {
    if (pData == TrivialStopHeuristicsData.BOTTOM)
      return TrivialStopHeuristicsData.BOTTOM;
    
    long usedMemory = mxBean.getHeapMemoryUsage().getUsed();
    if (usedMemory > threshold)
      return TrivialStopHeuristicsData.BOTTOM;
    else
      return TrivialStopHeuristicsData.TOP;
  }

}
