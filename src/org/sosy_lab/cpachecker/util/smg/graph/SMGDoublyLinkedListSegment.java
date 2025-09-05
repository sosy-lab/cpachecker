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
import org.sosy_lab.cpachecker.cpa.smg2.SMGState.EqualityCache;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

public class SMGDoublyLinkedListSegment extends SMGSinglyLinkedListSegment {

  private final BigInteger prevOffset;
  private final BigInteger prevPointerTargetOffset;

  public SMGDoublyLinkedListSegment(
      int pNestingLevel,
      Value pSize,
      BigInteger pOffset,
      BigInteger pHeadOffset,
      BigInteger pNextOffset,
      BigInteger pNextPointerTargetOffset,
      BigInteger pPrevOffset,
      BigInteger pPrevPointerTargetOffset,
      int pMinLength) {
    super(
        pNestingLevel,
        pSize,
        pOffset,
        pHeadOffset,
        pNextOffset,
        pNextPointerTargetOffset,
        pMinLength);
    Preconditions.checkNotNull(pPrevOffset);
    Preconditions.checkNotNull(pPrevPointerTargetOffset);
    prevOffset = pPrevOffset;
    prevPointerTargetOffset = pPrevPointerTargetOffset;
  }

  public SMGDoublyLinkedListSegment(
      int pNestingLevel,
      Value pSize,
      BigInteger pOffset,
      BigInteger pHeadOffset,
      BigInteger pNextOffset,
      BigInteger pNextPointerTargetOffset,
      BigInteger pPrevOffset,
      BigInteger pPrevPointerTargetOffset,
      int pMinLength,
      EqualityCache<Value> pRelevantEqualities) {
    super(
        pNestingLevel,
        pSize,
        pOffset,
        pHeadOffset,
        pNextOffset,
        pNextPointerTargetOffset,
        pMinLength,
        pRelevantEqualities);
    prevOffset = pPrevOffset;
    prevPointerTargetOffset = pPrevPointerTargetOffset;
  }

  public BigInteger getPrevOffset() {
    return prevOffset;
  }

  public BigInteger getPrevPointerTargetOffset() {
    return prevPointerTargetOffset;
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
        getNextPointerTargetOffset(),
        prevOffset,
        prevPointerTargetOffset,
        getMinLength(),
        getRelevantEqualities());
  }

  @Override
  public SMGObject freshCopy() {
    return new SMGDoublyLinkedListSegment(
        getNestingLevel(),
        getSize(),
        getOffset(),
        getHeadOffset(),
        getNextOffset(),
        getNextPointerTargetOffset(),
        prevOffset,
        prevPointerTargetOffset,
        getMinLength(),
        getRelevantEqualities());
  }

  @Override
  public SMGObject decrementLengthAndCopy() {
    return new SMGDoublyLinkedListSegment(
        getNestingLevel(),
        getSize(),
        getOffset(),
        getHeadOffset(),
        getNextOffset(),
        getNextPointerTargetOffset(),
        prevOffset,
        prevPointerTargetOffset,
        Integer.max(getMinLength() - 1, 0),
        getRelevantEqualities());
  }

  @Override
  public String toString() {
    return getMinLength() + "+DLL " + super.hashCode();
  }

  @Override
  public boolean isSLL() {
    return false;
  }

  @Override
  public SMGDoublyLinkedListSegment copyWithNewMinimumLength(int newMinimumLength) {
    Preconditions.checkArgument(newMinimumLength >= 0);
    return new SMGDoublyLinkedListSegment(
        getNestingLevel(),
        getSize(),
        getOffset(),
        getHeadOffset(),
        getNextOffset(),
        getNextPointerTargetOffset(),
        prevOffset,
        prevPointerTargetOffset,
        newMinimumLength,
        getRelevantEqualities());
  }

  @Override
  public SMGDoublyLinkedListSegment copyWithNewRelevantEqualities(
      EqualityCache<Value> pRelevantEqualities) {
    return new SMGDoublyLinkedListSegment(
        getNestingLevel(),
        getSize(),
        getOffset(),
        getHeadOffset(),
        getNextOffset(),
        getNextPointerTargetOffset(),
        prevOffset,
        prevPointerTargetOffset,
        getMinLength(),
        pRelevantEqualities);
  }

  /**
   * Copies the object, but the new object has a new id. So size etc. will match, but never the ID!
   *
   * @param objectToCopy obj to copy.
   * @return a new object with the same size etc. as the old.
   */
  public static SMGObject of(SMGDoublyLinkedListSegment objectToCopy) {
    return new SMGDoublyLinkedListSegment(
        objectToCopy.getNestingLevel(),
        objectToCopy.getSize(),
        objectToCopy.getOffset(),
        objectToCopy.getHeadOffset(),
        objectToCopy.getNextOffset(),
        objectToCopy.getNextPointerTargetOffset(),
        objectToCopy.prevOffset,
        objectToCopy.prevPointerTargetOffset,
        objectToCopy.getMinLength(),
        objectToCopy.getRelevantEqualities());
  }
}
