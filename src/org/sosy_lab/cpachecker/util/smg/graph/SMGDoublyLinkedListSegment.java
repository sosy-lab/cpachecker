// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.graph;

import com.google.common.base.Preconditions;
import java.math.BigInteger;

public class SMGDoublyLinkedListSegment extends SMGSinglyLinkedListSegment {

  private final BigInteger prevOffset;

  public SMGDoublyLinkedListSegment(
      int pNestingLevel,
      BigInteger pSize,
      BigInteger pOffset,
      BigInteger pHeadOffset,
      BigInteger pNextOffset,
      BigInteger pPrevOffset,
      int pMinLength) {
    super(pNestingLevel, pSize, pOffset, pHeadOffset, pNextOffset, pMinLength);
    prevOffset = pPrevOffset;
  }

  public BigInteger getPrevOffset() {
    return prevOffset;
  }

  @Override
  public boolean equals(Object pOther) {
    return super.equals(pOther);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public SMGObject copyWithNewLevel(int newLevel) {
    Preconditions.checkArgument(newLevel >= 0);
    return new SMGDoublyLinkedListSegment(
        newLevel,
        getSize(),
        getOffset(),
        getHeadOffset(),
        getNextOffset(),
        prevOffset,
        getMinLength());
  }

  @Override
  public SMGObject freshCopy() {
    return new SMGDoublyLinkedListSegment(
        getNestingLevel(),
        getSize(),
        getOffset(),
        getHeadOffset(),
        getNextOffset(),
        prevOffset,
        getMinLength());
  }

  @Override
  public SMGObject decrementLengthAndCopy() {
    return new SMGDoublyLinkedListSegment(
        getNestingLevel(),
        getSize(),
        getOffset(),
        getHeadOffset(),
        getNextOffset(),
        prevOffset,
        Integer.max(getMinLength() - 1, 0));
  }

  @Override
  public String toString() {
    return getMinLength() + "+DLL " + super.hashCode();
  }

  @Override
  public boolean isSLL() {
    return false;
  }
}
