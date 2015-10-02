/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.statistics;

import javax.management.JMException;

import org.sosy_lab.cpachecker.util.resources.ProcessCpuTime;
import org.sosy_lab.cpachecker.util.statistics.interfaces.TimeMeasurementListener;

import com.google.common.base.Preconditions;


public class StatCpuTime implements TimeMeasurementListener {

  public static class NoTimeMeasurement extends Exception {
    private static final long serialVersionUID = 8708086213456270100L;

    public NoTimeMeasurement(String pMessage) {
      super(pMessage);
    }
  }

  public static class StatCpuTimer implements AutoCloseable {

    private long startCpuTimeMillis;
    private final long startWallTimeMillis;

    private final TimeMeasurementListener addTo;
    private final boolean active;

    private long getCurrentCpuTimeMillis() throws JMException {
      return (long) (ProcessCpuTime.read() / 1e6);
    }

    StatCpuTimer(TimeMeasurementListener pAddTo) {
      this.addTo = pAddTo;

      // CPU time
      try {
        this.startCpuTimeMillis = getCurrentCpuTimeMillis();
      } catch (JMException e) {
        this.startCpuTimeMillis = -1;
      }

      // Wall time
      this.startWallTimeMillis = System.currentTimeMillis();

      this.active = true;
    }

    public void stop() {
      Preconditions.checkState(active);

      long stopWallTimeMillis = System.currentTimeMillis();

      long stopCpuTimeMillis;
      try {
        stopCpuTimeMillis = getCurrentCpuTimeMillis();
      } catch (JMException e) {
        stopCpuTimeMillis = -1;
      }

      final long spentCpuTimeMSecs = stopCpuTimeMillis - startCpuTimeMillis;
      final long spentWallTimeMSecs = stopWallTimeMillis - startWallTimeMillis;

      addTo.onMeasurementResult(spentCpuTimeMSecs, spentWallTimeMSecs);
    }

    @Override
    public void close() {
      stop();
    }
  }

  private long cpuTimeSumMsec = 0;
  private long wallTimeSumMsec = 0;

  public StatCpuTimer start() {
    return new StatCpuTimer(this);
  }

  private synchronized void incrementByMSecs(long pSpentCpuTimeMSecs, long pSpentWallTimeMSecs) {
    if (pSpentCpuTimeMSecs < 0) {
      this.cpuTimeSumMsec = Long.MIN_VALUE;
    } else {
      this.cpuTimeSumMsec += pSpentCpuTimeMSecs;
    }

    this.wallTimeSumMsec += pSpentWallTimeMSecs;
  }

  public long getCpuTimeSumMilliSecs()
      throws NoTimeMeasurement {

    if (cpuTimeSumMsec < 0) {
      throw new NoTimeMeasurement("No time measurement available!");
    }

    return cpuTimeSumMsec;
  }

  public long getWallTimeSumMsec() {
    return wallTimeSumMsec;
  }

  @Override
  public String toString() {
    try {
      long time = getCpuTimeSumMilliSecs();
      return String.format("%.4f", time / 1000.0); // Print seconds
    } catch (NoTimeMeasurement e) {
      return "NA";
    }
  }

  @Override
  public void onMeasurementResult(long pSpentCpuTimeMSecs, long pSpentWallTimeMSecs) {
    incrementByMSecs(pSpentCpuTimeMSecs, pSpentWallTimeMSecs);
  }

}
