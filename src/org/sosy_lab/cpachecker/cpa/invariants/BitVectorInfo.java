// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants;

import com.google.common.base.Preconditions;
import java.math.BigInteger;

public class BitVectorInfo implements TypeInfo {

  private final int size;

  private final boolean signed;

  private final BigInteger minValue;

  private final BigInteger maxValue;

  private BitVectorInfo(int pSize, boolean pSigned) {
    Preconditions.checkArgument(pSize >= 0, "bit vector size must not be negative");
    size = pSize;
    signed = pSigned;
    minValue = !signed ? BigInteger.ZERO : BigInteger.valueOf(2).pow(size - 1).negate();
    maxValue =
        !signed
            ? BigInteger.valueOf(2).pow(size).subtract(BigInteger.ONE)
            : BigInteger.valueOf(2).pow(size - 1).subtract(BigInteger.ONE);
  }

  public int getSize() {
    return size;
  }

  @Override
  public boolean isSigned() {
    return signed;
  }

  @Override
  public BigInteger getMinValue() {
    return minValue;
  }

  @Override
  public BigInteger getMaxValue() {
    return maxValue;
  }

  public BitVectorInterval getRange() {
    return BitVectorInterval.of(this, minValue, maxValue);
  }

  @Override
  public String abbrev() {
    return size + (signed ? "" : "U");
  }

  @Override
  public String toString() {
    return String.format("Size: %d; Signed: %b", size, signed);
  }

  @Override
  public int hashCode() {
    return signed ? -size : size;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof BitVectorInfo other && size == other.size && signed == other.signed;
  }

  public static BitVectorInfo from(int pSize, boolean pSigned) {
    return new BitVectorInfo(pSize, pSigned);
  }

  public BitVectorInfo extend(int pExtension) {
    return new BitVectorInfo(size + pExtension, signed);
  }
}
