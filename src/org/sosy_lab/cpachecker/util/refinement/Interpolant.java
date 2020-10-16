// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.refinement;

import java.util.Set;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public interface Interpolant<S, T> {

  /** Reconstructs some valid information, e.g., an abstract state, from the interpolant. */
  S reconstructState();

  /** Returns the size of the interpolant. For statistics. */
  int getSize();

  /** Returns the memory locations this interpolant uses. */
  Set<MemoryLocation> getMemoryLocations();

  /** Whether the interpolant is always satisfied. */
  boolean isTrue();

  /** Whether the interpolant is never satisfied. */
  boolean isFalse();

  default boolean isTrivial() {
    return isFalse() || isTrue();
  }

  /** Merge two interpolants to get more precise information. */
  T join(T otherInterpolant);
}
