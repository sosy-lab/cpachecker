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

import static com.google.common.base.Preconditions.checkArgument;

import java.util.concurrent.TimeUnit;

import javax.management.JMException;

import org.sosy_lab.common.time.TimeSpan;

/**
 * A limit that measures the CPU time used by the current process
 * (if available on this JVM).
 */
public class ProcessCpuTimeLimit implements ResourceLimit {

  private final int processorCount = Runtime.getRuntime().availableProcessors();

  private final long duration;
  private final long endTime;

  private ProcessCpuTimeLimit(long pStart, long pLimit, TimeUnit pUnit) {
    checkArgument(pLimit > 0);
    duration = TimeUnit.NANOSECONDS.convert(pLimit, pUnit);
    endTime = pStart + duration;
  }

  public static ProcessCpuTimeLimit fromNowOn(TimeSpan timeSpan) throws JMException {
    return new ProcessCpuTimeLimit(ProcessCpuTime.read(), timeSpan.asNanos(), TimeUnit.NANOSECONDS);
  }

  public static ProcessCpuTimeLimit fromNowOn(long limit, TimeUnit unit) throws JMException {
    return new ProcessCpuTimeLimit(ProcessCpuTime.read(), limit, unit);
  }

  public static ProcessCpuTimeLimit sinceProcessStart(long time, TimeUnit unit) throws JMException {
    // Do a single read to trigger exceptions if ProcessCpuTime is not available.
    ProcessCpuTime.read();
    return new ProcessCpuTimeLimit(0, time, unit);
  }

  @Override
  public long getCurrentValue() {
    try {
      return ProcessCpuTime.read();
    } catch (JMException e) {
      return 0;
    }
  }

  @Override
  public boolean isExceeded(long pCurrentValue) {
    return pCurrentValue >= endTime;
  }

  @Override
  public long nanoSecondsToNextCheck(long pCurrentValue) {
    if (pCurrentValue == 0) {
      // reading failed suddenly, we disable this limit
      return Long.MAX_VALUE;
    }
    return (endTime - pCurrentValue) / processorCount;
  }

  @Override
  public String getName() {
    return "CPU-time limit of " + TimeUnit.NANOSECONDS.toSeconds(duration) + "s";
  }
}