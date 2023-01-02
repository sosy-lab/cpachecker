// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.statistics;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.IdentityHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.common.time.Timer;

/**
 * This class provides a way to manage several sub-timers that can be used in their own threads. The
 * values of sub-timers are summed up, when time values of the manager are queried.
 *
 * <p>WARNING: While this class was intended to be thread-safe, it is NOT! There are at least the
 * following problems:
 *
 * <ul>
 *   <li>Methods like {@link #getSumTime()} can read and return invalid (half-updated) values from
 *       timers because they access timer fields with type long without atomicity guarantees.
 *   <li>Methods like {@link #getSumTime()} can return wrong values because there is at least one
 *       race condition when a timer is stopped by another thread while the aggregation method is
 *       called. This can lead to a timer interval being counted twice or not at all in the returned
 *       result, and thus to non-monotonic results from {@link #getSumTime()}.
 *   <li>{@link #getAvgTime()} can return wrong values because the average is not computed
 *       atomically with respect to timer starts/stops.
 * </ul>
 *
 * <p>Furthermore, the pieces of this class that are thread-safe rely on the current implementation
 * details of {@link Timer}, which are not documented and not guaranteed (that class explicitly is
 * documented as not thread-safe, but still used here).
 *
 * <p>However, creation of new timers is safe, and the behavior of this class should also be correct
 * if all of the above problems are avoided by the code using this class. This means the following
 * sequence of actions probably SHOULD be safe:
 *
 * <ol>
 *   <li>Creation of {@link ThreadSafeTimerContainer} in one thread.
 *   <li>Calls to {@link #getNewTimer()} by other threads.
 *   <li>Each returned {@link Timer} consistently being used in a single-threaded manner.
 *   <li>No intermediate calls to other methods of this class.
 *   <li>All timer-using threads being stopped or otherwise ceasing to use their timer instances.
 *   <li>Only now may the current thread access the results of this instance.
 * </ol>
 */
public final class ThreadSafeTimerContainer extends AbstractStatValue {

  /**
   * This map contains all usable timers.
   *
   * <p>We use WeakReferences to avoid memory leak when deleting timers. WeakReference allows us to
   * access the wrapped Timer before GC.
   */
  @GuardedBy("activeTimers")
  private final IdentityHashMap<WeakReference<TimerWrapper>, Timer> activeTimers =
      new IdentityHashMap<>();

  private final ReferenceQueue<TimerWrapper> referenceQueue = new ReferenceQueue<>();

  /** We assume one common unit for all sub-timers. */
  private final TimeUnit unit;

  /**
   * The sum of times of all intervals. This field should be accessed through {@link #sumTime()} to
   * account for a currently running interval.
   */
  @GuardedBy("activeTimers")
  private long sumTime = 0;

  /** The maximal time of all intervals. */
  @GuardedBy("activeTimers")
  private long maxTime = 0;

  /**
   * The number of intervals. This field should be accessed through {@link #getNumberOfIntervals()}
   * to account for a currently running interval.
   */
  @GuardedBy("activeTimers")
  private int numberOfIntervals = 0;

  @Deprecated // not actually thread-safe
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
  @GuardedBy("activeTimers")
  private void closeTimer(Timer timer) {
    // TODO A running timer here means that some component did not stop its timer before becoming
    // garbage collected. This is a bug in the component - we should warn about this.

    // Do not remove or move the following call, or otherwise make sure to call timer.isRunning().
    // Otherwise there is no "happens-before" relationship between the actions of the timer-using
    // thread and our thread (but the volatile field "running" in Timer's current implementation
    // adds one).
    timer.stopIfRunning();

    sumTime += convert(timer.getSumTime());
    maxTime = Math.max(maxTime, convert(timer.getMaxTime()));
    numberOfIntervals += timer.getNumberOfIntervals();
  }

  private long convert(TimeSpan time) {
    // we assume the same unit for all sub-timers.
    return time.getSaturated(unit);
  }

  @GuardedBy("activeTimers")
  private long eval(Function<Timer, Long> f, BiFunction<Long, Long, Long> acc) {
    long currentInterval = 0;
    for (Timer timer : activeTimers.values()) {
      // This method call makes ThreadSafeTimerContainer less racy - DO NOT REMOVE!
      // Timer is explicitly not thread-safe and this class attempts but fails to use Timer safely
      // from multiple threads (cf. the following explanation:
      // https://gitlab.com/sosy-lab/software/cpachecker/-/commit/1c2b2ed5c4a801d4f03a47344d36433ed3023100#note_647501985
      // In particular, there is no guarantee that f.apply(timer) will correctly see the most recent
      // values that timer stores internally. However, it happens that the current Timer
      // implementation uses a volatile field for isRunning(), and that most actions inside Timer
      // (like start()/stop()) "happen-before" a write to that volatile field. So if we read that
      // field here, we establish "happens-before" with the Timer actions (as long as Timer does not
      // change its implementation).
      @SuppressWarnings("unused")
      boolean doNotRemove = timer.isRunning();

      // FIXME Reads of long values are not atomic.
      // FIXME Here is a race (with timers being started/stopped).

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

    // FIXME Here is a race (with new timers created, timers being started/stopped).s

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
