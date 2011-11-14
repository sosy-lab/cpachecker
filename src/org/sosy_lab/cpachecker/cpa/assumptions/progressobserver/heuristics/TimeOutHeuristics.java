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
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.ReachedHeuristicsDataSetView;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.StopHeuristics;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.StopHeuristicsData;

public class TimeOutHeuristics implements StopHeuristics<TimeOutHeuristicsData> {

  private final TimeOutHeuristicsPrecision precision;
  private long startTime;

  private final LogManager logger;

  // necessary stuff to query the OperatingSystemMBean for the process cpu time
  private final MBeanServer mbeanServer;
  private final ObjectName osMbean;
  private static final String PROCESS_CPU_TIME = "ProcessCpuTime";

  private boolean disabled = false;

  public TimeOutHeuristics(Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    precision = new TimeOutHeuristicsPrecision(this, config);
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
  public TimeOutHeuristicsData collectData(StopHeuristicsData pData, ReachedHeuristicsDataSetView pReached) {
    return (TimeOutHeuristicsData)pData;
  }

  @Override
  public TimeOutHeuristicsData getInitialData(CFANode pNode) {
    resetStartTime();
    return new TimeOutHeuristicsData(false);
  }


  @Override
  public TimeOutHeuristicsData processEdge(StopHeuristicsData pData, CFAEdge pEdge) {
    TimeOutHeuristicsData d = (TimeOutHeuristicsData)pData;
    if (d == TimeOutHeuristicsData.BOTTOM) {
      return d;
    }

    long threshold = precision.getThreshold();
    if ((threshold >= 0)
        && (System.currentTimeMillis() > startTime + threshold)) {
      d.setThreshold(precision.getThreshold());
      return TimeOutHeuristicsData.BOTTOM;
    }

    if (disabled || precision.getHardLimitThreshold() == -1) {
      return d;
    }


    Object cputimeObject;
    try {
      cputimeObject = mbeanServer.getAttribute(osMbean, PROCESS_CPU_TIME);
    } catch (JMException e) {
      logger.logDebugException(e, "Querying process cpu time failed");
      logger.log(Level.WARNING, "Your Java VM does not support measuring the cpu time, TimeOutHeuristics.hardLimitThreshold disabled");

      disabled = true;
      return d;
    }

    if (!(cputimeObject instanceof Long)) {
      logger.log(Level.WARNING, "Invalid value received for cpu time: " + cputimeObject + ", TimeOutHeuristics.hardLimitThreshold disabled");

      disabled = true;
      return d;
    }

    long cputime = ((Long)cputimeObject) / 1000000;

    if (cputime > precision.getHardLimitThreshold()) {
      precision.setShouldForceToStop();
      d.setThreshold(precision.getHardLimitThreshold());
      return TimeOutHeuristicsData.BOTTOM;
    }

    return d;
  }

  void resetStartTime() {
    startTime = System.currentTimeMillis();
  }

  @Override
  public HeuristicPrecision getInitialPrecision() {
    return precision;
  }

}
