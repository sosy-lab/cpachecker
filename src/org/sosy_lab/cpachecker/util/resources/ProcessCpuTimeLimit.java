// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.resources;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.concurrent.TimeUnit;
import javax.management.JMException;
import org.sosy_lab.common.time.TimeSpan;

/** A limit that measures the CPU time used by the current process (if available on this JVM). */
public class ProcessCpuTimeLimit implements ResourceLimit {

  private final int processorCount = Runtime.getRuntime().availableProcessors();

  private final long duration;
  private long endTime = -1;

  private ProcessCpuTimeLimit(long pLimit, TimeUnit pUnit) {
    checkArgument(pLimit > 0);
    duration = TimeUnit.NANOSECONDS.convert(pLimit, pUnit);
  }

  public static ProcessCpuTimeLimit create(TimeSpan timeSpan) throws JMException {
    return create(timeSpan.asNanos(), TimeUnit.NANOSECONDS);
  }

  public static ProcessCpuTimeLimit create(long limit, TimeUnit unit) throws JMException {
    ProcessCpuTime.read(); // do a read to detect whether it works or throws exception
    return new ProcessCpuTimeLimit(limit, unit);
  }

  @Override
  public void start(Thread pThread) {
    checkState(endTime == -1);
    endTime = getCurrentValue() + duration;
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
    checkState(endTime != -1);
    return pCurrentValue >= endTime;
  }

  @Override
  public long nanoSecondsToNextCheck(long pCurrentValue) {
    checkState(endTime != -1);
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
