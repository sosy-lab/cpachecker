package org.sosy_lab.common;

/**
 * This Class represents a timer like a chronograph. It can be started and
 * stopped several times. It can return the sum of those intervals and the
 * maximal interval.
 */
public class Timer {

  private long startTime = 0;
  private long endTime   = 0;
  private long sumTime   = 0;
  private long maxTime   = 0;

  /** Constructor of Timer */
  public Timer() {
  }

  /** start the timer if it is not running, else start new interval */
  public void start() {
    if (!isRunning()) {
      startTime = System.currentTimeMillis();
    } else {
      // new interval
      this.stop();
      this.start();
    }
  }

  /** stop the timer if it is not running, else do nothing */
  public void stop() {
    endTime = System.currentTimeMillis();
    if (isRunning()) {
      long intervallTime = endTime - startTime;
      sumTime += intervallTime;
      maxTime = Math.max(intervallTime, maxTime);

      // reset startTime for isRunning()
      startTime = 0;
    }
  }

  /**
   * returns the sum of all intervals. if timer is running, it returns the sum
   * of the intarvals plus the time since the timer has been started
   */
  public long getSumTime() {
    if (isRunning()) {
      return System.currentTimeMillis() - startTime + sumTime;
    } else {
      return sumTime;
    }
  }

  public long getMaxTime() {
    return maxTime;
  }

  public String printMaxTime() {
    return formatTime(maxTime);
  }

  @Override
  public String toString() {
    return formatTime(getSumTime());
  }

  /**
   * formats a given time into a String with the format "12345.123s"
   * 
   * @param time
   * @return formated String
   */
  private static String formatTime(long time) {
    return String.format("%5d.%03ds", time / 1000, time % 1000);
  }

  /** returns if the timer is running */
  private boolean isRunning() {
    return (startTime != 0);
  }
}
