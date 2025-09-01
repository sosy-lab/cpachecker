// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

/**
 * Represents the set of qualifiers of a type, e.g., its const/volatile flags. Other qualifiers are
 * not yet implemented but may be added in the future.
 *
 * <p>More information can be found in C11 § 6.7.3.
 */
public enum CTypeQualifiers {
  NONE(false, false, false),
  ATOMIC(true, false, false),
  CONST(false, true, false),
  VOLATILE(false, false, true),
  ATOMIC_CONST(true, true, false),
  ATOMIC_VOLATILE(true, false, true),
  CONST_VOLATILE(false, true, true),
  ATOMIC_CONST_VOLATILE(true, true, true),
  ;

  private final boolean containsAtomic;
  private final boolean containsConst;
  private final boolean containsVolatile;

  private CTypeQualifiers(boolean pAtomic, boolean pConst, boolean pVolatile) {
    containsAtomic = pAtomic;
    containsConst = pConst;
    containsVolatile = pVolatile;
  }

  public static CTypeQualifiers create(boolean pAtomic, boolean pConst, boolean pVolatile) {
    if (pAtomic) {
      if (pConst) {
        return pVolatile ? ATOMIC_CONST_VOLATILE : ATOMIC_CONST;
      } else {
        return pVolatile ? ATOMIC_VOLATILE : ATOMIC;
      }
    } else {
      if (pConst) {
        return pVolatile ? CONST_VOLATILE : CONST;
      } else {
        return pVolatile ? VOLATILE : NONE;
      }
    }
  }

  /** Create an instance that combines all qualifiers from both inputs. */
  public static CTypeQualifiers union(CTypeQualifiers a, CTypeQualifiers b) {
    return create(
        a.containsAtomic || b.containsAtomic,
        a.containsConst || b.containsConst,
        a.containsVolatile || b.containsVolatile);
  }

  /** Return whether this instance represents no qualifiers, e.g., is equal to {@link #NONE}. */
  public boolean isEmpty() {
    return this == NONE;
  }

  public boolean containsAtomic() {
    return containsAtomic;
  }

  public boolean containsConst() {
    return containsConst;
  }

  public boolean containsVolatile() {
    return containsVolatile;
  }

  public boolean containsAllOf(CTypeQualifiers other) {
    return union(this, other).equals(this);
  }

  /**
   * Return a string representation of this set of qualifiers that can be used as part of a type.
   * This includes a trailing space such that it can be directly prefixed to a base type.
   */
  String toASTStringPrefix() {
    return switch (this) {
      case NONE -> "";
      case ATOMIC -> "_Atomic ";
      case CONST -> "const ";
      case VOLATILE -> "volatile ";
      case ATOMIC_CONST -> "_Atomic const ";
      case ATOMIC_VOLATILE -> "_Atomic volatile ";
      case CONST_VOLATILE -> "const volatile ";
      case ATOMIC_CONST_VOLATILE -> "_Atomic const volatile ";
    };
  }

  /** Returns an instance without atomic but all other qualifiers keeping their value. */
  public CTypeQualifiers withoutAtomic() {
    return withAtomicSetTo(false);
  }

  /** Returns an instance with atomic but all other qualifiers keeping their value. */
  public CTypeQualifiers withAtomic() {
    return withAtomicSetTo(true);
  }

  /** Returns an instance with atomic as given but all other qualifiers keeping their value. */
  public CTypeQualifiers withAtomicSetTo(boolean pNewAtomic) {
    return create(pNewAtomic, containsConst, containsVolatile);
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
    return create(containsAtomic, pNewConst, containsVolatile);
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
    return create(containsAtomic, containsConst, pNewVolatile);
  }
}
