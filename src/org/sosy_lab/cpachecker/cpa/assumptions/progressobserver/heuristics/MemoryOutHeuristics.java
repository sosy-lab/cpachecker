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

import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.ProcessExecutor;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.ReachedHeuristicsDataSetView;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.StopHeuristics;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.StopHeuristicsData;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.TrivialStopHeuristicsData;
import org.sosy_lab.cpachecker.util.assumptions.HeuristicToFormula.PreventingHeuristicType;

/**
 * Checks the info from "top" command and uses the virtual memory used for Java
 * to determine whether we are about to exceed the memory limit.
 * So, this heuristic only works in unix and "top -b -n 1" command
 * should produce the results in the same order top version 3.2.8
 * (tested in Ubuntu 10.10)
 * Check that information before using this heuristic.
 */
@Options(prefix="cpa.assumptions.progressobserver.heuristics")
public class MemoryOutHeuristics
implements StopHeuristics<TrivialStopHeuristicsData>
{
  @Option(description = "threshold for heuristics of progressobserver")
  private final int threshold = -1;

  private final LogManager logger;
  private int freq = 50000; //TODO read from file
  private int noOfIterations = 0;

  // find the index of column VIRT in "top -b -n 1" command
  private final int idxOfVirt;

  public MemoryOutHeuristics(Configuration config, LogManager pLogger)
      throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;
    idxOfVirt = findIndexOfVIRTColumn();
  }

  private int findIndexOfVIRTColumn() {
    ProcessExecutor<Exception> processExecutor;
    try {
      processExecutor = new ProcessExecutor<Exception>(logger, Exception.class, new String[]{"top", "-b", "-n", "1"});
      processExecutor.join();

      for (String line: processExecutor.getOutput())
      {
        if(line.contains("VIRT")){
          String lines[] = line.split("\\s+");
          for(int i=0; i<lines.length; i++){
            if(lines[i].contains("VIRT")){
              return i;
            }
          }
          break;
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
    return -1;
  }

  @Override
  public TrivialStopHeuristicsData collectData(StopHeuristicsData pData, ReachedHeuristicsDataSetView pReached) {
    return (TrivialStopHeuristicsData)pData;
  }

  @Override
  public TrivialStopHeuristicsData processEdge(StopHeuristicsData pData, CFAEdge pEdge)
  {
    //TODO consider moving these into a thread later
    // does com.sun.management.OperatingSystemMXBean give us
    // info same with "top"?

    if(noOfIterations != freq){
      noOfIterations++;
      return TrivialStopHeuristicsData.TOP;
    }

    else{
      noOfIterations = 0;
    }

    // Negative threshold => do nothing
    if (threshold <= 0)
      return TrivialStopHeuristicsData.TOP;

    // Bottom => nothing to do, we are already out of memory
    if (pData == TrivialStopHeuristicsData.BOTTOM)
      return TrivialStopHeuristicsData.BOTTOM;

    try {

      ProcessExecutor<Exception> processExecutor = new ProcessExecutor<Exception>(logger, Exception.class, new String[]{"top", "-b", "-n", "1"});
      processExecutor.join();

      long memUsed = -2;

      for (String line: processExecutor.getOutput())
      {
        if(line.contains("java")){
          memUsed = Long.valueOf(line.split("\\s+")[idxOfVirt-1].replace("m", ""));
          break;
        }
      }

      if(memUsed > threshold) {
        logger.log(Level.WARNING, "System out of memory, terminating.");
        TrivialStopHeuristicsData.setThreshold(threshold);
        TrivialStopHeuristicsData.setPreventingHeuristicType(PreventingHeuristicType.MEMORYOUT);
        return TrivialStopHeuristicsData.BOTTOM;
      }

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