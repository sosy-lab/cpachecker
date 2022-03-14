// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util.value;

import java.math.BigInteger;

public class CValue implements Comparable<CValue> {

  private static final CValue unknownValue = new CValue(BigInteger.valueOf(Integer.MIN_VALUE));
  private final BigInteger value;

  private CValue(BigInteger pValue) {
    value = pValue;
  }

  public static CValue zero() {
    return valueOf(BigInteger.ZERO);
  }

  public static CValue valueOf(BigInteger val) {
    return new CValue(val);
  }

  @Override
  public int compareTo(CValue other) {
    if (isUnknown()) {
      return other.isUnknown() ? 0 : -1;
    }
    return value.compareTo(other.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public boolean equals(Object pObj) {
    if (isUnknown() && pObj instanceof CValue) {
      return ((CValue) pObj).isUnknown();
    }
    return pObj instanceof CValue && value.equals(((CValue) pObj).value);
  }

  public BigInteger getExplicitValue() {
    return value;
  }

  public boolean isUnknown() {
    return unknownValue == this;
  }

  public static CValue getUnknownValue() {
    return unknownValue;
  }

  public boolean isZero() {
    return BigInteger.ZERO.equals(value);
  }

  public CValue shiftRight(int shiftBy) {
    if (isUnknown() || isZero()) {
      return this;
    }
    if (shiftBy < 0) {
      return shiftLeft(-shiftBy);
    }
    return valueOf(value.shiftRight(shiftBy));
  }

  public CValue shiftLeft(int shiftBy) {
    if (isUnknown() || isZero()) {
      return this;
    }
    if (shiftBy < 0) {
      return shiftRight(-shiftBy);
    }
    return valueOf(value.shiftRight(shiftBy));
  }

  public CValue clearBit(int n) {
    if (isUnknown() || isZero()) {
      return this;
    }
    return CValue.valueOf(getExplicitValue().clearBit(n));
  }

  public CValue concat(CValue pOverlappingBitsCValue) {
    if (isUnknown()) {
      return this;
    }
    if (isZero()) {
      return pOverlappingBitsCValue;
    }
    BigInteger otherValue = pOverlappingBitsCValue.getExplicitValue();
    BigInteger newValue = value.shiftRight(otherValue.bitLength()).add(otherValue);
    return valueOf(newValue);
  }

  public CValue add(CValue other) {
    if (isUnknown() || other.isUnknown()) {
      return CValue.unknownValue;
    }
    if (isZero()) {
      return other;
    }
    if (other.isZero()) {
      return this;
    }

    return valueOf(value.add(other.getExplicitValue()));
  }
}
