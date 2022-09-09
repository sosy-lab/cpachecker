// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.graph;

import java.math.BigInteger;

public class SMGSinglyLinkedListSegment extends SMGObject {

  private final int minLength;
  private final BigInteger headOffset;
  private final BigInteger nextOffset;

  public SMGSinglyLinkedListSegment(
      int pNestingLevel,
      BigInteger pSize,
      BigInteger pOffset,
      BigInteger pHeadOffset,
      BigInteger pNextOffset,
      int pMinLength) {
    super(pNestingLevel, pSize, pOffset);
    minLength = pMinLength;
    headOffset = pHeadOffset;
    nextOffset = pNextOffset;
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
    return new SMGSinglyLinkedListSegment(
        newLevel, getSize(), getOffset(), headOffset, nextOffset, minLength);
  }

  public SMGObject decrementLengthAndCopy() {
    return new SMGSinglyLinkedListSegment(
        getNestingLevel(), getSize(), getOffset(), headOffset, nextOffset, minLength - 1);
  }

  @Override
  public SMGObject freshCopy() {
    return new SMGSinglyLinkedListSegment(
        getNestingLevel(), getSize(), getOffset(), headOffset, nextOffset, minLength);
  }

  @Override
  public String toString() {
    return minLength + "+SLL " + super.hashCode();
  }

  @Override
  public boolean isSLL() {
    return true;
  }
}
