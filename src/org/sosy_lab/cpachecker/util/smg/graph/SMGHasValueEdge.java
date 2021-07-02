// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.graph;

import java.math.BigInteger;

public class SMGHasValueEdge implements SMGEdge, Comparable<SMGHasValueEdge> {

  private final SMGValue value;
  // Do not use type but type size
  private final BigInteger sizeInBits;
  private final BigInteger offset;

  public SMGHasValueEdge(SMGValue pValue, BigInteger pSizeInBits, BigInteger pOffset) {
    value = pValue;
    sizeInBits = pSizeInBits;
    offset = pOffset;
  }

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
  public int compareTo(SMGHasValueEdge pArg0) {
    return value.compareTo(pArg0.value);
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
}
