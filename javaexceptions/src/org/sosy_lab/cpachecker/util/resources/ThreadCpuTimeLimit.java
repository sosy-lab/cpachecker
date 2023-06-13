// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.resources;

import static com.google.common.base.Preconditions.checkArgument;

import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;
import org.sosy_lab.common.time.TimeSpan;

public class ThreadCpuTimeLimit implements ResourceLimit {

  private final long duration;
  private final long endTime;
  private final Thread thread;
  private long overallUsedTime = 0;

  private ThreadCpuTimeLimit(long pStart, long pLimit, TimeUnit pUnit, Thread pThread) {
    checkArgument(pLimit > 0);
    duration = TimeUnit.NANOSECONDS.convert(pLimit, pUnit);
    endTime = pStart + duration;
    thread = pThread;
  }

  public static ThreadCpuTimeLimit fromNowOn(TimeSpan timeSpan, Thread pThread) {
    return new ThreadCpuTimeLimit(
        getCurrentThreadTime(pThread), timeSpan.asNanos(), TimeUnit.NANOSECONDS, pThread);
  }

  public static ThreadCpuTimeLimit fromNowOn(long limit, TimeUnit unit, Thread pThread) {
    return new ThreadCpuTimeLimit(getCurrentThreadTime(pThread), limit, unit, pThread);
  }

  @Override
  public synchronized long getCurrentValue() {
    long currentValue = getCurrentThreadTime(thread);
    if (currentValue != -1) {
      overallUsedTime = currentValue;
    }
    return currentValue;
  }

  private static long getCurrentThreadTime(Thread pThread) {
    @SuppressWarnings("deprecation") // Replacement Thread.threadId() is only available on Java 19+
    final long threadId = pThread.getId();
    return ManagementFactory.getThreadMXBean().getThreadCpuTime(threadId);
  }

  @Override
  public synchronized boolean isExceeded(long pCurrentValue) {
    return pCurrentValue >= endTime;
  }

  @Override
  public synchronized long nanoSecondsToNextCheck(long pCurrentValue) {
    return (endTime - pCurrentValue) / 2;
  }

  public synchronized TimeSpan getOverallUsedTime() {
    return TimeSpan.of(overallUsedTime, TimeUnit.NANOSECONDS);
  }

  @Override
  public String getName() {
    return "Thread CPU-time limit of " + TimeUnit.NANOSECONDS.toSeconds(duration) + "s";
  }
}
