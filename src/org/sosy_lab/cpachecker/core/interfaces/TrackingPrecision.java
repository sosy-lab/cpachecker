// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

public interface TrackingPrecision extends Precision {

  /**
   * If an immutable precision tracks all possibly tracked elements, this method returns true. False
   * else.
   *
   * @implSpec this should only every be overwritten if a precision can guarantee that it only ever
   *     returns true for methods that determine whether something is tracked like isTracking().
   */
  default boolean isAlwaysTracking() {
    return false;
  }
}
