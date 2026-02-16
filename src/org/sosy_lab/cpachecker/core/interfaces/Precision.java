// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import java.util.Optional;

public interface Precision {

  /**
   * If an immutable precision has only a single result for all possible tracking objects, it is
   * returned inside the non-empty optional. Empty in all other cases.
   *
   * @implSpec this should only every be overwritten if a precision can guarantee that it only ever
   *     returns one result for isTracking(), e.g. if all variables are tracked, this returns
   *     Optional.of(true).
   */
  default Optional<Boolean> getStaticIsTrackingResult() {
    return Optional.empty();
  }

  /**
   * Returns true iff an immutable precision always returns the same result when asked whether
   * something is tracked or not. This result can then be acquired using {@link
   * Precision#getStaticIsTrackingResult()}. False in all other cases.
   */
  default boolean hasStaticIsTrackingResult() {
    return getStaticIsTrackingResult().isPresent();
  }
}
