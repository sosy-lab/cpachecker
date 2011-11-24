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

import java.lang.management.ManagementFactory;
import java.util.logging.Level;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

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
@Options(prefix="cpa.assumptions.progressobserver.heuristics.memoryOutHeuristics")
public class MemoryOutHeuristics implements StopHeuristics<TrivialStopHeuristicsData> {

  @Option(required=true, description = "Threshold for MemoryOutHeuristics (total memory of process in MB)")
  private int threshold = -1;

  private final LogManager logger;

  // necessary stuff to query the OperatingSystemMBean
  private final MBeanServer mbeanServer;
  private final ObjectName osMbean;
  private static final String MEMORY_SIZE = "CommittedVirtualMemorySize";

  private boolean disabled = false;

    public MemoryOutHeuristics(Configuration config, LogManager pLogger)
      throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;

    mbeanServer = ManagementFactory.getPlatformMBeanServer();
    try {
      osMbean = new ObjectName("java.lang", "type", "OperatingSystem");
    } catch (MalformedObjectNameException e) {
      // the name is hard-coded, so this exception should never occur
      throw new AssertionError(e);
    }
  }

  @Override
  public TrivialStopHeuristicsData collectData(StopHeuristicsData pData, ReachedHeuristicsDataSetView pReached) {
    return (TrivialStopHeuristicsData)pData;
  }

  @Override
  public TrivialStopHeuristicsData processEdge(StopHeuristicsData pData, CFAEdge pEdge) {
    TrivialStopHeuristicsData d = (TrivialStopHeuristicsData)pData;
    if (d == TrivialStopHeuristicsData.BOTTOM) {
      return d;
    }

    if (disabled || threshold <= 0) {
      return d;
    }

    Object memUsedObject;
    try {
      memUsedObject = mbeanServer.getAttribute(osMbean, MEMORY_SIZE);
    } catch (JMException e) {
      logger.logDebugException(e, "Querying memory size failed");
      logger.log(Level.WARNING, "Your Java VM does not support measuring the memory size, MemoryOutHeuristics disabled");

      disabled = true;
      return d;
    }

    if (!(memUsedObject instanceof Long)) {
      logger.log(Level.WARNING, "Invalid value received for memory size: " + memUsedObject + ", MemoryOutHeuristics disabled");

      disabled = true;
      return d;
    }

    long memUsed = ((Long)memUsedObject) / (1024*1024);

    if (memUsed > threshold) {
      logger.log(Level.WARNING, "System out of memory, terminating.");
      TrivialStopHeuristicsData.setThreshold(threshold);
      TrivialStopHeuristicsData.setPreventingHeuristicType(PreventingHeuristicType.MEMORYOUT);
      return TrivialStopHeuristicsData.BOTTOM;
    }

    return d;
  }

  @Override
  public TrivialStopHeuristicsData getInitialData(CFANode pNode) {
    return TrivialStopHeuristicsData.TOP;
  }

  @Override
  public HeuristicPrecision getPrecision() {
    // TODO Auto-generated method stub
    return null;
  }
}