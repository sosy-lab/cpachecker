// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

/**
 * Class represents Precision as a set, in which it is possible to add elements and subtract them.
 */
public interface AdjustablePrecision extends Precision {

  /** Add other Precision to current and return a new Precision. */
  AdjustablePrecision add(AdjustablePrecision otherPrecision);

  /** Subtract other Precision from current and a return new Precision. */
  AdjustablePrecision subtract(AdjustablePrecision otherPrecision);

  /**
   * Returns true for precision p iff for any instance of the same class p':
   * p'.subtract(p).equals(p') and p'.add(p).equals(p').
   *
   * <p>The value isEmpty() does not affect on analysis operators. The only purpose is to optimize
   * application of add() and subtract() operations.
   */
  boolean isEmpty();
}
