// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.statistics;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.common.time.Timer;

// TODO move this class into sosy-lab-commons?

/**
 * This class provides a way to manage several sub-timers that can be used in their own threads. The
 * values of sub-timers are summed up, when time values of the manager are queried.
 */
public class ThreadSafeTimerContainer extends AbstractStatValue {

  /**
   * This map contains all usable timers.
   *
   * <p>We use WeakReferences to avoid memory leak when deleting timers. WeakReference allows us to
   * access the wrapped Timer before GC.
   */
  private final Map<WeakReference<TimerWrapper>, Timer> activeTimers = new IdentityHashMap<>();

  private final ReferenceQueue<TimerWrapper> referenceQueue = new ReferenceQueue<>();

  /** We assume one common unit for all sub-timers. */
  private final TimeUnit unit;

  /**
   * The sum of times of all intervals. This field should be accessed through {@link #sumTime()} to
   * account for a currently running interval.
   */
  private long sumTime = 0;

  /** The maximal time of all intervals. */
  private long maxTime = 0;

  /**
   * The number of intervals. This field should be accessed through {@link #getNumberOfIntervals()}
   * to account for a currently running interval.
   */
  private int numberOfIntervals = 0;

  public ThreadSafeTimerContainer(String title) {
    super(StatKind.SUM, title);
    unit = new Timer().getMaxTime().getUnit();
  }

  @Override
  public int getUpdateCount() {
    return getNumberOfIntervals();
  }

  public TimerWrapper getNewTimer() {
    cleanupReferences();
    Timer timer = new Timer();
    TimerWrapper wrapper = new TimerWrapper(timer);
    assert unit == timer.getSumTime().getUnit() : "sub-timers should use same unit";
    synchronized (activeTimers) {
      activeTimers.put(new WeakReference<>(wrapper, referenceQueue), timer);
    }
    return wrapper;
  }

  private void cleanupReferences() {
    Reference<? extends TimerWrapper> ref;
    while ((ref = referenceQueue.poll()) != null) {
      synchronized (activeTimers) {
        closeTimer(activeTimers.remove(ref));
      }
    }
  }

  /** Stop the given Timer and collect its values. */
  private void closeTimer(Timer timer) {
    timer.stopIfRunning();
    sumTime += convert(timer.getSumTime());
    maxTime = Math.max(maxTime, convert(timer.getMaxTime()));
    numberOfIntervals += timer.getNumberOfIntervals();
  }

  private long convert(TimeSpan time) {
    // we assume the same unit for all sub-timers.
    return time.getSaturated(unit);
  }

  private long eval(Function<Timer, Long> f, BiFunction<Long, Long, Long> acc) {
    long currentInterval = 0;
    for (Timer timer : activeTimers.values()) {
      currentInterval = acc.apply(currentInterval, f.apply(timer));
    }
    return currentInterval;
  }

  /*
   * Return the sum of all intervals. If timers are running, the current intervals are also counted
   * (up to the current time). If no timer was started, this method returns 0.
   */
  public TimeSpan getSumTime() {
    cleanupReferences();
    return export(sumTime());
  }

  long sumTime() {
    synchronized (activeTimers) {
      return sumTime + eval(t -> convert(t.getSumTime()), Math::addExact);
    }
  }

  /**
   * Return the maximal time of all intervals. If timers are running, the current intervals are also
   * counted (up to the current time). If no timer was started, this method returns 0.
   */
  public TimeSpan getMaxTime() {
    cleanupReferences();
    synchronized (activeTimers) {
      return export(Math.max(maxTime, eval(t -> convert(t.getMaxTime()), Math::max)));
    }
  }

  /**
   * Return the number of intervals. If timers are running, the current intervals are also counted.
   * If no timer was started, this method returns 0.
   */
  public int getNumberOfIntervals() {
    cleanupReferences();
    synchronized (activeTimers) {
      return (int) (numberOfIntervals + eval(t -> (long) t.getNumberOfIntervals(), Math::addExact));
    }
  }

  /**
   * Return the average of all intervals. If timers are running, the current intervals are also
   * counted (up to the current time). If no timer started, this method returns 0.
   */
  public TimeSpan getAvgTime() {
    cleanupReferences();
    int currentNumberOfIntervals = getNumberOfIntervals();
    if (currentNumberOfIntervals == 0) {
      // prevent divide by zero
      return export(0);
    }
    return export(sumTime() / currentNumberOfIntervals);
  }

  private TimeSpan export(long time) {
    return TimeSpan.of(time, unit);
  }

  @Override
  public String toString() {
    cleanupReferences();
    return getSumTime().formatAs(TimeUnit.SECONDS);
  }

  /** Syntax sugar method: pretty-format the timer output into a string in seconds. */
  public String prettyFormat() {
    cleanupReferences();
    TimeUnit t = TimeUnit.SECONDS;
    return String.format(
        "%s (Max: %s), (Avg: %s), (#intervals = %s)",
        getSumTime().formatAs(t),
        getMaxTime().formatAs(t),
        getAvgTime().formatAs(t),
        getNumberOfIntervals());
  }

  /** A small wrapper to keep a reference on the timer and provide a limited view. */
  public static class TimerWrapper {
    private final Timer timer;

    TimerWrapper(Timer pTimer) {
      timer = pTimer;
    }

    public void start() {
      timer.start();
    }

    public void stop() {
      timer.stop();
    }

    public void stopIfRunning() {
      timer.stopIfRunning();
    }

    public boolean isRunning() {
      return timer.isRunning();
    }

    public TimeSpan getLengthOfLastInterval() {
      return timer.getLengthOfLastInterval();
    }
  }

}
