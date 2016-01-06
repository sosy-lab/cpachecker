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

import java.util.concurrent.TimeUnit;

import javax.management.JMException;

import org.sosy_lab.common.time.TimeSpan;
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

  private long maxCpuTimeSumMsec = Long.MIN_VALUE;
  private long minCpuTimeSumMsec = Long.MAX_VALUE;
  private long cpuTimeSumMsec = 0;
  private long wallTimeSumMsec = 0;
  private long intervals = 0;

  public StatCpuTimer start() {
    return new StatCpuTimer(this);
  }

  public long getIntervals() {
    return intervals;
  }

  private synchronized void incrementByMSecs(long pSpentCpuTimeMSecs, long pSpentWallTimeMSecs) {

    intervals += 1;

    if (pSpentCpuTimeMSecs < 0) {
      maxCpuTimeSumMsec = Long.MIN_VALUE;
      minCpuTimeSumMsec = Long.MAX_VALUE;
      cpuTimeSumMsec = Long.MIN_VALUE;
    } else {
      cpuTimeSumMsec += pSpentCpuTimeMSecs;
      minCpuTimeSumMsec = Math.min(minCpuTimeSumMsec, pSpentCpuTimeMSecs);
      maxCpuTimeSumMsec = Math.max(maxCpuTimeSumMsec, pSpentCpuTimeMSecs);
    }

    wallTimeSumMsec += pSpentWallTimeMSecs;
  }

  public TimeSpan getCpuTimeSum()
      throws NoTimeMeasurement {

    if (cpuTimeSumMsec < 0) {
      throw new NoTimeMeasurement("No time measurement available!");
    }

    return TimeSpan.ofMillis(cpuTimeSumMsec);
  }

  public TimeSpan getMinCpuTimeSum()
      throws NoTimeMeasurement {

    if (minCpuTimeSumMsec == Long.MAX_VALUE) {
      throw new NoTimeMeasurement("No time measurement available!");
    }

    return TimeSpan.ofMillis(minCpuTimeSumMsec);
  }

  public TimeSpan getMaxCpuTimeSum()
      throws NoTimeMeasurement {

    if (maxCpuTimeSumMsec == Long.MIN_VALUE) {
      throw new NoTimeMeasurement("No time measurement available!");
    }

    return TimeSpan.ofMillis(maxCpuTimeSumMsec);
  }

  public TimeSpan getAvgCpuTimeSum()
      throws NoTimeMeasurement {

    if (intervals == 0) {
      throw new NoTimeMeasurement("No time measurement available!");
    }

    return TimeSpan.ofMillis(cpuTimeSumMsec / intervals);
  }

  public long getWallTimeSumMsec() {
    return wallTimeSumMsec;
  }

  @Override
  public String toString() {
    try {
      TimeSpan time = getCpuTimeSum();
      return time.formatAs(TimeUnit.SECONDS);
    } catch (NoTimeMeasurement e) {
      return "NA";
    }
  }

  @Override
  public void onMeasurementResult(long pSpentCpuTimeMSecs, long pSpentWallTimeMSecs) {
    incrementByMSecs(pSpentCpuTimeMSecs, pSpentWallTimeMSecs);
  }

}
