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

public class NestedTimer {

  /**
   * The time of starting the timer.
   * Volatile to make {@link #isRunning()} thread-safe.
   */
  private volatile long outerStartTime = 0;

  /** The sum of times of all intervals up to the last call to stopOuter(). */
  private long innerSumTime           = 0;
  private long outerSumTime           = 0;
//private long totalSumTime           = 0; // not necessary, is equal to sum of inner and outer

  /** Volatile to make {@link #isRunning()} thread-safe. */
  private volatile Timer innerTimer = null;

  /** The maximal time of all intervals. */
  private long innerMaxTime           = 0;
  private long outerMaxTime           = 0;
  private long totalMaxTime           = 0;

  /** The number of intervals. */
  private int  totalNumberOfIntervals = 0;

  /** Start the timer. If it was running before, the timer is stopped and then
   * started again. */
  public final void startOuter() {
    checkState(!isRunning());
    assert innerTimer == null;

    outerStartTime = System.currentTimeMillis();

    innerTimer = new Timer();

    totalNumberOfIntervals++;
  }

  public final void startBoth() {
    startOuter();

    innerTimer = new Timer(outerStartTime);
  }

  /**
   * Stop the timer if it is running.
   * Return the number of milliseconds since start() was called.
   *
   * @return time of stopped interval */
  public final long stopOuter() {
    checkState(isOuterRunning());

    return stopOuter0(System.currentTimeMillis());
  }

  private long stopOuter0(long endTime) {
    // calculate total time
    long totalIntervallTime = endTime - outerStartTime;
    totalMaxTime = Math.max(totalIntervallTime, totalMaxTime);

    long currentInnerSumTime = innerTimer.getSumTime();

    // calculate outer time
    long outerIntervallTime = totalIntervallTime - currentInnerSumTime;
    outerSumTime += outerIntervallTime;
    outerMaxTime = Math.max(outerIntervallTime, outerMaxTime);

    // update inner times
    innerSumTime += currentInnerSumTime;
    innerMaxTime = Math.max(currentInnerSumTime, innerMaxTime);

    // reset
    outerStartTime = 0;
    innerTimer = null;

    return outerIntervallTime;
  }

  public final long stopBoth() {
    checkState(innerTimer.isRunning());
    assert isRunning();

    long endTime = System.currentTimeMillis();

    // calculate total time
    long totalIntervallTime = endTime - outerStartTime;

    innerTimer.stop(endTime);
    stopOuter0(endTime);

    return totalIntervallTime;
  }

  private long getCurrentOuterIntervalTime() {
    if (!isRunning()) {
      return 0;
    }
    return getCurrentTotalIntervalTime() - innerTimer.getSumTime();
  }

  private long getCurrentTotalIntervalTime() {
    if (!isRunning()) {
      return 0;

    } else {
      long currentTime = System.currentTimeMillis();
      long currentTotal = currentTime - outerStartTime;

      return currentTotal;
    }
  }

  public Timer getInnerTimer() {
    checkState(isRunning());
    return innerTimer;
  }

  /** Return the sum of all intervals. If timer is running, return the sum
   * of the intervals plus the time since the timer has been started.
   *
   * @return sum of times of all intervals */
  public final long getOuterSumTime() {
    return outerSumTime + getCurrentOuterIntervalTime();
  }

  public final long getInnerSumTime() {
    long result = innerSumTime;
    if (isRunning()) {
      result += innerTimer.getSumTime();
    }
    return result;
  }

  public final long getTotalSumTime() {
    return outerSumTime + innerSumTime + getCurrentTotalIntervalTime();
  }

  /** Return the maximal time of all intervals. If timer is running,
   * the currently running interval will be ignored.
   *
   * @return maximal time */
  public final long getOuterMaxTime() {
    return outerMaxTime;
  }

  public final long getInnerMaxTime() {
    return innerMaxTime;
  }

  public final long getTotalMaxTime() {
    return totalMaxTime;
  }

  /** Return the number of intervals. If timer is running,
   * the currently running interval will be ignored.
   *
   * @return number of intervals */
  public final int getNumberOfIntervals() {
    return totalNumberOfIntervals;
  }

  /** Return the average of all intervals. If timer is running, return the
   * average of the intervals plus one interval with the time since the
   * timer has been started.
   *
   * @return average time */
  public final long getOuterAvgTime() {
    return getOuterSumTime() / totalNumberOfIntervals;
  }

  public final long getInnerAvgTime() {
    return getInnerSumTime() / totalNumberOfIntervals;
  }

  public final long getTotalAvgTime() {
    return getTotalSumTime() / totalNumberOfIntervals;
  }

  /** Return a String with the sum time of the intervals.
   *
   * @return formated String */
  public final String printOuterSumTime() {
    return formatTime(getOuterSumTime());
  }

  public final String printInnerSumTime() {
    return formatTime(getInnerSumTime());
  }

  public final String printTotalSumTime() {
    return formatTime(getTotalSumTime());
  }

  /** Return a String with the maximal time of the intervals.
   *
   * @return formated String */
  public final String printOuterMaxTime() {
    return formatTime(getOuterMaxTime());
  }

  public final String printInnerMaxTime() {
    return formatTime(getInnerMaxTime());
  }

  public final String printTotalMaxTime() {
    return formatTime(getTotalMaxTime());
  }

  /** Return a String with the average time of the intervals.
   *
   * @return formated String */
  public final String printOuterAvgTime() {
    return formatTime(getOuterAvgTime());
  }

  public final String printInnerAvgTime() {
    return formatTime(getInnerAvgTime());
  }

  public final String printTotalAvgTime() {
    return formatTime(getTotalAvgTime());
  }

  /** Return a String with the sum of the times of all intervals.
   *
   * @return formated String */
  @Override
  public final String toString() {
    return formatTime(getTotalSumTime());
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
  public boolean isOuterRunning() {
    return isRunning() && !innerTimer.isRunning();
  }

  public boolean isRunning() {
    return (outerStartTime != 0);
  }
}
