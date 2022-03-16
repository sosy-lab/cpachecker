// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.graph;

import java.math.BigInteger;

public class SMGDoublyLinkedListSegment extends SMGObject {

  private final int minLength;
  private final BigInteger headOffset;
  private final BigInteger nextOffset;
  private final BigInteger prevOffset;

  public SMGDoublyLinkedListSegment(
      int pNestingLevel,
      BigInteger pSize,
      BigInteger pOffset,
      BigInteger pPrevOffset,
      BigInteger pNextOffset,
      int pMinLength,
      BigInteger pHeadOffset) {
    super(pNestingLevel, pSize, pOffset);
    minLength = pMinLength;
    headOffset = pHeadOffset;
    nextOffset = pNextOffset;
    prevOffset = pPrevOffset;
  }

  public BigInteger getPrevOffset() {
    return prevOffset;
  }

  public BigInteger getNextOffset() {
    return nextOffset;
  }

  public BigInteger getHeadOffset() {
    return headOffset;
  }

  public int getMinLength() {
    return minLength;
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
    return new SMGDoublyLinkedListSegment(
        newLevel, getSize(), getOffset(), prevOffset, nextOffset, minLength, headOffset);
  }

  @Override
  public SMGObject freshCopy() {
    return new SMGDoublyLinkedListSegment(
        getNestingLevel(), getSize(), getOffset(), prevOffset, nextOffset, minLength, headOffset);
  }
}
