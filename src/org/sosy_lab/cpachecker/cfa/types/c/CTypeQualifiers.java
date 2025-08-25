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

  /** Create an instance that combines all qualifiers from both inputs. */
  public static CTypeQualifiers union(CTypeQualifiers a, CTypeQualifiers b) {
    return create(a.isConst || b.isConst, a.isVolatile || b.isVolatile);
  }

  public boolean isConst() {
    return isConst;
  }

  public boolean isVolatile() {
    return isVolatile;
  }

  /**
   * Return a string representation of this set of qualifiers that can be used as part of a type.
   * This includes a trailing space such that it can be directly prefixed to a base type.
   */
  String toASTStringPrefix() {
    return switch (this) {
      case NONE -> "";
      case CONST -> "const ";
      case VOLATILE -> "volatile ";
      case CONST_VOLATILE -> "const volatile ";
    };
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
