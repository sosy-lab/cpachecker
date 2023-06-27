// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.util;

import java.math.BigInteger;

/*
 * Sometimes we need to return a SMG and a SMGValue.
 */
public class OffsetAndSize {

  private final BigInteger offset;
  private final BigInteger size;

  public OffsetAndSize(BigInteger pOffset, BigInteger pSize) {
    offset = pOffset;
    size = pSize;
  }

  public BigInteger getSize() {
    return size;
  }

  public BigInteger getOffset() {
    return offset;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }
    return other instanceof OffsetAndSize otherOffsetAndSize
        && size.equals(otherOffsetAndSize.getSize())
        && offset.equals(otherOffsetAndSize.getOffset());
  }

  @Override
  public int hashCode() {
    int max = Math.max(offset.intValue(), size.intValue());
    int min = Math.min(offset.intValue(), size.intValue());
    return Math.abs(max * max) + Math.abs(min);
  }
}
