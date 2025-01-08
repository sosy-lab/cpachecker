// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.resources;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Preconditions;
import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;
import org.sosy_lab.common.time.TimeSpan;

public class ThreadCpuTimeLimit implements ResourceLimit {

  private final long duration;
  private long endTime;
  private Thread thread;
  private long overallUsedTime = 0;

  private ThreadCpuTimeLimit(long pLimit, TimeUnit pUnit) {
    checkArgument(pLimit > 0);
    duration = TimeUnit.NANOSECONDS.convert(pLimit, pUnit);
    endTime = -1;
    thread = null;
  }

  /**
   * This creates a not yet started {@link ThreadCpuTimeLimit} with the given {@link TimeSpan}, but
   * no thread associated. You need to set a thread via {@link #setThread(Thread)} before starting
   * this time limit!
   *
   * @param timeSpan any {@link TimeSpan} with arbitrary {@link TimeUnit}.
   * @return a not started {@link ThreadCpuTimeLimit} with a set time-span, but not thread
   *     associated.
   */
  public static ThreadCpuTimeLimit withTimeSpan(TimeSpan timeSpan) {
    return new ThreadCpuTimeLimit(timeSpan.asNanos(), TimeUnit.NANOSECONDS);
  }

  /**
   * This associates a {@link Thread} with the {@link ThreadCpuTimeLimit}, making it ready to track
   * and limit the time of the one thread associated once {@link ResourceLimitChecker#start()} is
   * called.
   *
   * @param pThread the {@link Thread} who's time is to be tracked/limited.
   */
  public synchronized void setThread(Thread pThread) {
    endTime = getCurrentThreadTime(pThread) + duration;
    thread = pThread;
  }

  @Override
  public synchronized long getCurrentValue() {
    Preconditions.checkState(thread != null);
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
    Preconditions.checkState(thread != null);
    return pCurrentValue >= endTime;
  }

  @Override
  public synchronized long nanoSecondsToNextCheck(long pCurrentValue) {
    Preconditions.checkState(thread != null);
    return (endTime - pCurrentValue) / 2;
  }

  public synchronized TimeSpan getOverallUsedTime() {
    Preconditions.checkState(thread != null);
    return TimeSpan.of(overallUsedTime, TimeUnit.NANOSECONDS);
  }

  @Override
  public String getName() {
    return "Thread CPU-time limit of " + TimeUnit.NANOSECONDS.toSeconds(duration) + "s";
  }
}
