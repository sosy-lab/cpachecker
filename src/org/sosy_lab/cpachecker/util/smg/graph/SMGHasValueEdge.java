// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.graph;

import java.math.BigInteger;

/**
 * Edge from (SMG-)object to (SMG-)value. May have a offset and a type. We do not use the type
 * itself, but simply the size of the type used in bits. In essence, the object has the value of the
 * specified type at the position specified by the offset.
 */
public class SMGHasValueEdge implements SMGEdge, Comparable<SMGHasValueEdge> {

  private final SMGValue value;
  // Do not use type but type size
  private final BigInteger sizeInBits;
  private final BigInteger offset;

  /**
   * Constructs a new has-value edge with the given parameters, pointing to a value at the position
   * of the offset with the given type(size).
   *
   * @param pValue - The value this edge points to.
   * @param pSizeInBits - The size of the type used in bits.
   * @param pOffset - The offset of the value. May not be negative!
   */
  public SMGHasValueEdge(SMGValue pValue, BigInteger pOffset, BigInteger pSizeInBits) {
    value = pValue;
    sizeInBits = pSizeInBits;
    offset = pOffset;
  }

  /**
   * @return the SMGValue this edge points to.
   */
  public SMGValue hasValue() {
    return value;
  }

  public BigInteger getSizeInBits() {
    return sizeInBits;
  }

  @Override
  public BigInteger getOffset() {
    return offset;
  }

  @Override
  public int compareTo(SMGHasValueEdge pOther) {
    int valueCompare = value.compareTo(pOther.value);
    if (valueCompare == 0) {
      if (offset.compareTo(pOther.offset) == 0) {
        if (sizeInBits.compareTo(pOther.sizeInBits) == 0) {
          return 0;
        } else {
          return sizeInBits.compareTo(pOther.sizeInBits);
        }
      } else {
        return offset.compareTo(pOther.offset);
      }
    } else {
      return valueCompare;
    }
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof SMGHasValueEdge)) {
      return false;
    }
    SMGHasValueEdge otherEdge = (SMGHasValueEdge) other;
    if (otherEdge.offset.equals(offset)
        && otherEdge.value.equals(value)
        && sizeInBits.equals(otherEdge.sizeInBits)) {
      return true;
    }

    return false;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public String toString() {
    return value + " [" + offset + "; " + sizeInBits + ")";
  }
}
