// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util.value;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;

/**
 * This represents either a unknown value of a certain type, a address to a SMGObject/offset pair, a
 * concrete value that may be numeric (BigInt or BigDecimal) or boolean in nature. TODO: this is
 * obviously not good enough, just better than before. I will replace this!
 */
public class CValue implements Comparable<CValue> {

  private static final CValue unknownCValue = new CValue();

  // true for unknown values; all other values are null in this case!
  private final boolean unknownValue;

  // Address + offset of the address; null both if no address
  private final @Nullable SMGObject address;
  private final @Nullable BigInteger offset;

  // The value if its a non decimal value; null for address/unknown etc.
  private final @Nullable BigInteger value;

  // The value if its a decimal value; null for address/unknown/value etc.
  private final @Nullable BigDecimal decValue;

  private CValue(BigInteger pValue) {
    value = pValue;
    address = null;
    unknownValue = false;
    offset = null;
    decValue = null;
  }

  private CValue() {
    value = null;
    address = null;
    unknownValue = true;
    offset = null;
    decValue = null;
  }

  private CValue(SMGObject pAdressTarget, BigInteger pOffset) {
    value = null;
    address = pAdressTarget;
    unknownValue = false;
    offset = pOffset;
    decValue = null;
  }

  private CValue(BigDecimal decimalValue) {
    value = null;
    address = null;
    unknownValue = false;
    offset = null;
    decValue = decimalValue;
  }

  public static CValue zero() {
    return valueOf(BigInteger.ZERO);
  }

  public static CValue valueOf(BigInteger val) {
    return new CValue(val);
  }

  public static CValue valueOf(BigDecimal val) {
    return new CValue(val);
  }

  // TODO: Bool = int; 0 = false, else true

  @Override
  public int compareTo(CValue other) {
    if (unknownValue) {
      return other.isUnknown() ? 0 : -1;
    } else if (isAddressValue()) {
      if (other.isAddressValue()) {
        int compareAddresses = address.compareTo(other.getAddress());
        if (compareAddresses == 0) {
          return offset.compareTo(other.getOffset());
        }
        return compareAddresses;
      }
    } else if (isDecimalValue()) {
      if (other.isDecimalValue()) {
        return decValue.compareTo(other.getDecimalValue());
      }
    } else {
      return value.compareTo(other.value);
    }
    return -1;
  }

  public boolean isAddressValue() {
    return address != null && offset != null;
  }

  public BigDecimal getDecimalValue() {
    return decValue;
  }

  public boolean isDecimalValue() {
    return decValue == null;
  }

  @Nullable
  public SMGObject getAddress() {
    return address;
  }

  @Nullable
  public BigInteger getOffset() {
    return offset;
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
    return unknownValue;
  }

  public static CValue getUnknownValue() {
    return unknownCValue;
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
      return CValue.unknownCValue;
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
