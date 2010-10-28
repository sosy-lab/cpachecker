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
package org.sosy_lab.cpachecker.cpa.assumptions.collector.progressobserver;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;


public class MemoryOutHeuristics
  implements StopHeuristics<TrivialStopHeuristicsData>
{
  private final int threshold;
  private final LogManager logger;

  public MemoryOutHeuristics(Configuration config, LogManager pLogger) {
    threshold = Integer.parseInt(config.getProperty("threshold", "-1").trim());
    logger = pLogger;
  }

  @Override
  public TrivialStopHeuristicsData collectData(StopHeuristicsData pData, ReachedHeuristicsDataSetView pReached) {
    return (TrivialStopHeuristicsData)pData;
  }

  @Override
  public TrivialStopHeuristicsData processEdge(StopHeuristicsData pData, CFAEdge pEdge)
  {
    // Negative threshold => do nothing
    if (threshold <= 0)
      return TrivialStopHeuristicsData.TOP;

    // Bottom => nothing to do, we are already out of memory
    if (pData == TrivialStopHeuristicsData.BOTTOM)
      return TrivialStopHeuristicsData.BOTTOM;

    // try this later com.sun.management.OperatingSystemMXBean
    try {
      FileInputStream fis = new FileInputStream("/proc/meminfo");
      DataInputStream dis = new DataInputStream(fis);
      BufferedReader bfr = new BufferedReader(new InputStreamReader(dis));
      String line;

      long memFree = 0;

      while((line = bfr.readLine()) != null){
        //          MemTotal:        2060840 kB
        //          MemFree:         1732952 kB
        //          Buffers:            3164 kB
        //          Cached:            58376 kB
        if(line.contains("MemTotal:")){
          continue;
        }
        else if(line.contains("MemFree:")){
          memFree = Long.valueOf(line.split("\\s+")[1]);
          break;
        }
//        else{
//          break;
//        }
      }

//    long totalFree = memTotal - (memFree + buffers + cached);

      if(memFree < threshold) {
        logger.log(Level.WARNING, "MEMORY IS OUT");
        return TrivialStopHeuristicsData.BOTTOM;
      }

      dis.close();
    } catch (Exception e1) {
      e1.printStackTrace();
    }

    return TrivialStopHeuristicsData.TOP;
  }

  @Override
  public TrivialStopHeuristicsData getInitialData(CFANode pNode) {
    return TrivialStopHeuristicsData.TOP;
  }
}