// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.resources;

/**
 * An interface for representing different forms of resource limits (Walltime, Calendar time,
 * CPU-time per process, CPU-time per thread, memory, etc.).
 *
 * <p>Instances of this class are to be used with {@link ResourceLimitChecker}. The interface is
 * designed such that typical implementations can avoid any mutable state after {@link
 * #start(Thread)} and need to due potentially expensive measurements only in {@link
 * #getCurrentMeasurementValue()}.
 *
 * <p>Classes beside {@link ResourceLimitChecker} should only create {@link ResourceLimit} instances
 * and pass then to {@link
 * ResourceLimitChecker#ResourceLimitChecker(org.sosy_lab.common.ShutdownManager, java.util.List)},
 * but never call ony of the methods of this interface.
 *
 * <p>Implementations do not need to worry about thread-safety, it is the responsibility of the
 * caller to ensure that each instance is only used from a single thread at the same time and that
 * there is a proper happens-before relationship between calls that happen on different threads.
 *
 * <p>Methods of this interface should never throw an exception (except for bugs).
 */
public interface ResourceLimit {

  /**
   * Mark the start point in time for this limit. Limit implementations typically need to store this
   * for later use in {@link #isExceeded(long)}. This method is called only once.
   *
   * @param thread The thread for which this limit is relevant (limit implementations might want to
   *     make use of this).
   */
  void start(Thread thread);

  /**
   * Measure the current value of the resource as needed for this implementation.
   *
   * @return An arbitrary value (won't be interpreted).
   */
  long getCurrentMeasurementValue();

  /**
   * Check whether a given value (that was returned by {@link #getCurrentMeasurementValue()} means
   * that the limit has been exceeded and we should stop. The limit won't be asked again after this
   * method returned true once.
   *
   * <p>For performance, this method should not actually do any measurements and only interpret the
   * given value.
   *
   * <p>Usually, this method checks whether the current value is greater or equal than some stored
   * value that specifies the limit.
   *
   * @param currentValue A value previously returned by {@link #getCurrentMeasurementValue()}
   * @return True if the limit has been exceeded.
   */
  boolean isExceeded(long currentValue);

  /**
   * Check how much time can elapse before we should bother checking this limit again after a call
   * to {@link #getCurrentMeasurementValue()} returned the given value. This can be used by limits
   * that can estimate how much (wall) time is left at minimum before the limit can exceed. Limits
   * that can not do so may simply return 0 (indicating that they need to check again as soon as
   * possible).
   *
   * <p>For performance, this method should not actually do any measurements and only interpret the
   * given value.
   *
   * <p>Note that the caller is not forced to respect this value, it may choose to ask the limit
   * more often or more rarely (if the higher load or the higher imprecision is acceptable).
   *
   * @param currentValue A value previously returned by {@link #getCurrentMeasurementValue()}
   * @return A time in nanoseconds from now on during which the caller does not need to bother
   *     calling {@link #getCurrentMeasurementValue()} and {@link #isExceeded(long)}.
   */
  long nanoSecondsToNextCheck(long currentValue);

  /**
   * Return a human-readable representation of this limit that can be presented to the user.
   *
   * @return A non-null string.
   */
  String getName();
}
