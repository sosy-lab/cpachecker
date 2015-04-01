/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.conditions.global;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.logging.Level;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.defaults.SimplePrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.resources.ProcessCpuTime;


class GlobalConditionsSimplePrecisionAdjustment extends SimplePrecisionAdjustment {

  private final LogManager logger;

  private final GlobalConditionsThresholds thresholds;

  //necessary stuff to query the OperatingSystemMBean for the process cpu time
  private final MBeanServer mbeanServer;
  private final ObjectName osMbean;
  private static final String MEMORY_SIZE = "CommittedVirtualMemorySize";

  private final MemoryMXBean memory;

  private boolean cpuTimeDisabled = false;
  private boolean processMemoryDisabled = false;

  GlobalConditionsSimplePrecisionAdjustment(LogManager pLogger, GlobalConditionsThresholds pThresholds) {
    logger = pLogger;
    thresholds = pThresholds;

    mbeanServer = ManagementFactory.getPlatformMBeanServer();
    memory = ManagementFactory.getMemoryMXBean();

    try {
      osMbean = new ObjectName(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
    } catch (MalformedObjectNameException e) {
      // the name is hard-coded, so this exception should never occur
      throw new AssertionError(e);
    }
  }

  @Override
  public Action prec(AbstractState pElement, Precision pPrecision) throws CPAException {

    if (checkWallTime()) {
      logger.log(Level.WARNING, "Wall time threshold reached, terminating.");
      return Action.BREAK;
    }

    if (checkCpuTime()) {
      logger.log(Level.WARNING, "Cpu time threshold reached, terminating.");
      return Action.BREAK;
    }

    if (checkHeapMemory()) {
      logger.log(Level.WARNING, "Java heap memory threshold reached, terminating.");
      return Action.BREAK;
    }

    if (checkProcessMemory()) {
      logger.log(Level.WARNING, "Process memory threshold reached, terminating.");
      return Action.BREAK;
    }

    return Action.CONTINUE;
  }


  private boolean checkWallTime() {
    return (System.currentTimeMillis() > thresholds.getWallTimeThreshold());
  }

  private boolean checkCpuTime() {
    if (cpuTimeDisabled) {
      return false;
    }
    long threshold = thresholds.getCpuTimeThreshold();
    if (threshold < 0) {
      return false;
    }

    long cputimeNanos;
    try {
      cputimeNanos = ProcessCpuTime.read();
    } catch (JMException e) {
      logger.logDebugException(e, "Querying cpu time failed");
      logger.log(Level.WARNING, "Your Java VM does not support measuring the cpu time, cpu time threshold disabled");

      cpuTimeDisabled = true;
      return false;
    }

    long cputime = cputimeNanos / (1000*1000);

    return (cputime > threshold);
  }

  private boolean checkHeapMemory() {
    long threshold = thresholds.getHeapMemoryThreshold();
    if (threshold < 0) {
      return false;
    }

    return ((memory.getHeapMemoryUsage().getUsed() / (1000*1000)) > threshold);
  }

  private boolean checkProcessMemory() {
    if (processMemoryDisabled) {
      return false;
    }
    long threshold = thresholds.getProcessMemoryThreshold();
    if (threshold < 0) {
      return false;
    }

    Object memUsedObject;
    try {
      memUsedObject = mbeanServer.getAttribute(osMbean, MEMORY_SIZE);
    } catch (JMException e) {
      logger.logDebugException(e, "Querying memory size failed");
      logger.log(Level.WARNING, "Your Java VM does not support measuring the memory size, process memory threshold disabled");

      processMemoryDisabled = true;
      return false;
    }

    if (!(memUsedObject instanceof Long)) {
      logger.log(Level.WARNING, "Invalid value received for memory size: " + memUsedObject + ", process memory threshold disabled");

      processMemoryDisabled = true;
      return false;
    }

    long memUsed = ((Long)memUsedObject) / (1000*1000);

    return (memUsed > threshold);
  }
}
