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

import com.google.common.base.Preconditions;
import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;
import org.sosy_lab.common.time.TimeSpan;

public class ThreadCpuTimeLimit implements ResourceLimit {

  private final long duration;
  private long endTime;
  private Thread thread;

  private ThreadCpuTimeLimit(long pLimit, TimeUnit pUnit) {
    checkArgument(pLimit > 0);
    duration = TimeUnit.NANOSECONDS.convert(pLimit, pUnit);
  }

  public static ThreadCpuTimeLimit create(TimeSpan timeSpan) {
    return new ThreadCpuTimeLimit(timeSpan.asNanos(), TimeUnit.NANOSECONDS);
  }

  @Override
  public void start(Thread pThread) {
    checkState(thread == null);
    thread = pThread;
    endTime = getCurrentValue() + duration;
  }

  @Override
  public long getCurrentValue() {
    Preconditions.checkState(thread != null);
    @SuppressWarnings("deprecation") // Replacement Thread.threadId() is only available on Java 19+
    final long threadId = thread.getId();
    return ManagementFactory.getThreadMXBean().getThreadCpuTime(threadId);
  }

  @Override
  public boolean isExceeded(long pCurrentValue) {
    Preconditions.checkState(thread != null);
    return pCurrentValue >= endTime;
  }

  @Override
  public long nanoSecondsToNextCheck(long pCurrentValue) {
    Preconditions.checkState(thread != null);
    return (endTime - pCurrentValue) / 2;
  }

  @Override
  public String getName() {
    return "Thread CPU-time limit of " + TimeUnit.NANOSECONDS.toSeconds(duration) + "s";
  }
}
