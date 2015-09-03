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
package org.sosy_lab.cpachecker.util.resources;

import java.lang.management.ManagementFactory;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;


public final class ProcessCpuTime {

  private ProcessCpuTime() { }

  //necessary stuff to query the OperatingSystemMBean for the process cpu time
  private static final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
  private static final ObjectName osMbean;
  private static final String PROCESS_CPU_TIME = "ProcessCpuTime";

  static {
    try {
      osMbean = new ObjectName(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
    } catch (MalformedObjectNameException e) {
      // the name is hard-coded, so this exception should never occur
      throw new AssertionError(e);
    }
  }

  /**
   * Read the cpu time this process has consumed so far.
   * This relies on a feature of the JVM and the OS
   * that might be not available in all circumstances.
   *
   * @return A time measured in nanoseconds (positive value).
   * @throws JMException If the operation is unsupported.
   */
  public static long read() throws JMException {
    Object cputime = mbeanServer.getAttribute(osMbean, PROCESS_CPU_TIME);

    if (!(cputime instanceof Long)) {
      throw new JMException("Invalid value received for cpu time: " + cputime);
    }

    long time = (Long)cputime;
    if (time < 0) {
      // value might be -1 if unsupported
      throw new JMException("Current platform does not support reading the process cpu time");
    }
    return time;
  }
}
