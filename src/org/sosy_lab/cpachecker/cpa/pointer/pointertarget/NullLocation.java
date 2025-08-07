// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.pointertarget;

import static org.sosy_lab.cpachecker.cpa.pointer.util.PointerUtils.compareByType;

/**
 * Pointer target representing the abstract {@code null} location.
 *
 * <p>All instances of {@code NullLocation} are considered equal. Comparison and hashing are
 * implemented accordingly.
 */
public final class NullLocation implements PointerTarget {
  @Override
  public int compareTo(PointerTarget pOther) {
    return (pOther instanceof NullLocation) ? 0 : compareByType(this, pOther);
  }

  /**
   * Returns a string representation of the null location.
   *
   * @return {@code "null"}
   */
  @Override
  public String toString() {
    return "null";
  }

  /**
   * Returns a constant hash code for all null locations.
   *
   * @return 0
   */
  @Override
  public int hashCode() {
    return 0;
  }
}
