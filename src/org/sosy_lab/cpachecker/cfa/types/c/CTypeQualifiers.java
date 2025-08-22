// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

/**
 * Represents the qualifiers of a type, e.g., its const/volatile flags. Other qualifiers are not yet
 * implemented but may be added in the future.
 *
 * <p>More information can be found in C11 § 6.7.3.
 */
public enum CTypeQualifiers {
  NONE(false, false),
  CONST(true, false),
  VOLATILE(false, true),
  CONST_VOLATILE(true, true),
  ;

  private final boolean isConst;
  private final boolean isVolatile;

  private CTypeQualifiers(boolean pConst, boolean pVolatile) {
    isConst = pConst;
    isVolatile = pVolatile;
  }

  public static CTypeQualifiers create(boolean pConst, boolean pVolatile) {
    if (pConst) {
      return pVolatile ? CONST_VOLATILE : CONST;
    } else {
      return pVolatile ? VOLATILE : NONE;
    }
  }

  public boolean isConst() {
    return isConst;
  }

  public boolean isVolatile() {
    return isVolatile;
  }

  /** Returns an instance without const but all other qualifiers keeping their value. */
  public CTypeQualifiers withoutConst() {
    return withConstSetTo(false);
  }

  /** Returns an instance with const but all other qualifiers keeping their value. */
  public CTypeQualifiers withConst() {
    return withConstSetTo(true);
  }

  /** Returns an instance with const as given but all other qualifiers keeping their value. */
  public CTypeQualifiers withConstSetTo(boolean pNewConst) {
    return create(pNewConst, isVolatile);
  }

  /** Returns an instance without volatile but all other qualifiers keeping their value. */
  public CTypeQualifiers withoutVolatile() {
    return withVolatileSetTo(false);
  }

  /** Returns an instance with volatile but all other qualifiers keeping their value. */
  public CTypeQualifiers withVolatile() {
    return withVolatileSetTo(true);
  }

  /** Returns an instance with volatile as given but all other qualifiers keeping their value. */
  public CTypeQualifiers withVolatileSetTo(boolean pNewVolatile) {
    return create(isConst, pNewVolatile);
  }
}
