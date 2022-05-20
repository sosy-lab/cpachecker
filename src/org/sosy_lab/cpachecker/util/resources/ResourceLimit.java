// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.resources;

/**
 * An interface for representing different forms of resource limits
 * (Walltime, Calendar time, CPU-time per process, CPU-time per thread,
 * memory, etc.).
 *
 * Instances of this class are to be used with {@link ResourceLimitChecker}.
 * The interface is designed such that typical implementations can avoid
 * any mutable state and need to due potentially expensive measurements only
 * in {@link #getCurrentValue()}.
 *
 * Methods of this interface should never throw an exception.
 */
public interface ResourceLimit {

  /**
   * Measure the current value of the resource as needed for this implementation.
   * @return An arbitrary value (won't be interpreted).
   */
  long getCurrentValue();

  /**
   * Check whether a given value (that was returned by {@link #getCurrentValue()}
   * means that the limit has been exceeded and we should stop.
   * The limit won't be asked again after this method returned true once.
   *
   * For performance, this method should not actually do any measurements
   * and only interpret the given value.
   *
   * Usually, this method checks whether the current value is greater or equal
   * than some stored value that specifies the limit.
   *
   * @param currentValue A value previously returned by {@link #getCurrentValue()}
   * @return True if the limit has been exceeded.
   */
  boolean isExceeded(long currentValue);

  /**
   * Check how much time can elapse before we should bother checking this limit again
   * after a call to {@link #getCurrentValue()} returned the given value.
   * This can be used by limits that can estimate how much (wall) time is left
   * at minimum before the limit can exceed. Limits that can not do so
   * may simply return 0 (indicating that they need to check again as soon
   * as possible).
   *
   * For performance, this method should not actually do any measurements
   * and only interpret the given value.
   *
   * Note that the caller is not forced to respect this value,
   * it may choose to ask the limit more often or more rarely
   * (if the higher load or the higher imprecision is acceptable).
   *
   * @param currentValue A value previously returned by {@link #getCurrentValue()}
   * @return A time in nanoseconds from now on during which the caller
   * does not need to bother calling {@link #getCurrentValue()} and {@link #isExceeded(long)}.
   */
  long nanoSecondsToNextCheck(long currentValue);

  /**
   * Return a human-readable representation of this limit that can be presented to the user.
   * @return A non-null string.
   */
  String getName();
}