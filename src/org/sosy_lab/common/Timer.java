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

/**
 * This Class represents a timer like a chronograph. It can be started and
 * stopped several times. It can return the sum, the average and the maximum 
 * of those intervals.
 * 
 * @author Karlheinz Friedberger
 */
public class Timer {

  private long startTime         = 0;
  private long sumTime           = 0;
  private long maxTime           = 0;
  private int  numberOfIntervals = 0;

  /** start the timer if it is not running, else start after stopping */
  public void start() {
    if (!isRunning()) {
      this.stop();
    }
    startTime = System.currentTimeMillis();
    // one more interval is started
    numberOfIntervals++;
  }

  /**
   * Stop the timer if it is not running, else do nothing.
   * Returns the number of milliseconds since start() was called.
   * Returns 0 if timer was not running. 
   */
  public long stop() {
    long endTime = System.currentTimeMillis();

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

  /**
   * returns the sum of all intervals. if timer is running, it returns the sum
   * of the intervals plus the time since the timer has been started
   */
  public long getSumTime() {
    if (isRunning()) {
      return sumTime + System.currentTimeMillis() - startTime;
    } else {
      return sumTime;
    }
  }

  public long getMaxTime() {
    return maxTime;
  }

  public int getNumberOfIntervals() {
    return numberOfIntervals;
  }

  /**
   * returns the average of all intervals. if timer is running, it returns the average
   * of the intervals plus one interval with the time since the timer has been started
   */
  public long getAvgTime() {
    return getSumTime() / numberOfIntervals;
  }

  /**some methods for printing*/
  public String printMaxTime() {
    return formatTime(getMaxTime());
  }

  public String printAvgTime() {
    return formatTime(getAvgTime());
  }

  @Override
  public String toString() {
    return formatTime(getSumTime());
  }

  /**
   * formats a given time in milliseconds into a String with the format "12345.123s"
   * 
   * @param time
   * @return formated String
   */
  public static String formatTime(long time) {
    return String.format("%5d.%03ds", time / 1000, time % 1000);
  }

  /** returns if the timer is running */
  private boolean isRunning() {
    return (startTime != 0);
  }
}
