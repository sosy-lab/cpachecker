/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.common;

import static com.google.common.base.Preconditions.checkState;

/**
 * This Class represents a timer like a chronograph. It can be started and
 * stopped several times. It can return the sum, the average, the maximum and
 * the number of those intervals.
 *
 * This class is not thread-safe.
 *
 * @author Karlheinz Friedberger
 */
public class Timer {

  /**
   * The time of starting the timer.
   * Volatile to make {@link #isRunning()} thread-safe.
   */
  private volatile long startTime         = 0;

  /** The sum of times of all intervals. */
  private long sumTime           = 0;

  /** The maximal time of all intervals. */
  private long maxTime           = 0;

  /** The number of intervals. */
  private int  numberOfIntervals = 0;

  /**
   * Create a fresh timer in the not-running state.
   */
  public Timer() {
  }

  /**
   * Create a timer in the running state, with a given start time.
   */
  Timer(long startTime) {
    checkState(startTime > 0);
    this.startTime = startTime;
  }

  /** Start the timer. If it was running before, the timer is stopped and then
   * started again. */
  public final void start() {
    if (isRunning()) {
      this.stop();
    }
    startTime = System.currentTimeMillis();
    // one more interval is started
    numberOfIntervals++;
  }

  /**
   * Stop the timer if it is running, else do nothing.
   * Return the number of milliseconds since start() was called.
   * Return 0 if timer was not running.
   *
   * @return time of stopped interval */
  public final long stop() {
    return stop(System.currentTimeMillis());
  }

  final long stop(long endTime) {
    if (isRunning()) {
      long intervallTime = endTime - startTime;
      sumTime += intervallTime;
      maxTime = Math.max(intervallTime, maxTime);

      // reset startTime for isRunning()
      startTime = 0;
      return intervallTime;
    }
    return 0;
  }

  /** Return the sum of all intervals. If timer is running, return the sum
   * of the intervals plus the time since the timer has been started.
   *
   * @return sum of times of all intervals */
  public final long getSumTime() {
    if (isRunning()) {
      return sumTime + System.currentTimeMillis() - startTime;
    } else {
      return sumTime;
    }
  }

  /** Return the maximal time of all intervals. If timer is running,
   * the currently running interval will be ignored.
   *
   * @return maximal time */
  public final long getMaxTime() {
    return maxTime;
  }

  /** Return the number of intervals. If timer is running,
   * the currently running interval will be ignored.
   *
   * @return number of intervals */
  public final int getNumberOfIntervals() {
    return numberOfIntervals;
  }

  /** Return the average of all intervals. If timer is running, return the
   * average of the intervals plus one interval with the time since the
   * timer has been started.
   *
   * @return average time */
  public final long getAvgTime() {
    return getSumTime() / numberOfIntervals;
  }

  /** Return a String with the maximal time of the intervals.
   *
   * @return formated String */
  public final String printMaxTime() {
    return formatTime(getMaxTime());
  }

  /** Return a String with the average time of the intervals.
   *
   * @return formated String */
  public final String printAvgTime() {
    return formatTime(getAvgTime());
  }

  /** Return a String with the sum of the times of all intervals.
   *
   * @return formated String */
  @Override
  public final String toString() {
    return formatTime(getSumTime());
  }

  /** Format a given time in milliseconds into a String with the format
   * "12345.123s".
   *
   * @param time time to format
   * @return formated String */
  public static String formatTime(final long time) {
    return String.format("%5d.%03ds", time / 1000, time % 1000);
  }

  /**
   * Return if the timer is running.
   * This method is thread-safe, it is guaranteed to return true if another
   * thread has called {@link #start()} and not yet called {@link #stop()}.
   *
   * @return is the timer running?
   */
  public boolean isRunning() {
    return (startTime != 0);
  }
}
