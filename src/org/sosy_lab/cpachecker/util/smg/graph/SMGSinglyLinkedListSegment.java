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

public class SMGSinglyLinkedListSegment extends SMGObject {

  private final int minLength;
  private final BigInteger headOffset;
  private final BigInteger nextOffset;

  // Track the equality cache used in the latest creation of this abstraction.
  // Can be used to argue about whether Values are equal or identical and need
  //   to be copied or replicated when materializing this abstraction.
  private EqualityCache<Value> relevantEqualities;

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
    relevantEqualities = EqualityCache.of();
  }

  public SMGSinglyLinkedListSegment(
      int pNestingLevel,
      BigInteger pSize,
      BigInteger pOffset,
      BigInteger pHeadOffset,
      BigInteger pNextOffset,
      int pMinLength,
      EqualityCache<Value> pRelevantEqualities) {
    super(pNestingLevel, pSize, pOffset);
    minLength = pMinLength;
    headOffset = pHeadOffset;
    nextOffset = pNextOffset;
    relevantEqualities = pRelevantEqualities;
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

  /**
   * Copies the object, but the new object has a new id. So size etc. will match, but never the ID!
   *
   * @param objectToCopy obj to copy.
   * @return a new object with the same size etc. as the old.
   */
  public static SMGObject of(SMGSinglyLinkedListSegment objectToCopy) {
    Preconditions.checkArgument(objectToCopy.isSLL());
    return new SMGSinglyLinkedListSegment(
        objectToCopy.getNestingLevel(),
        objectToCopy.getSize(),
        objectToCopy.getOffset(),
        objectToCopy.headOffset,
        objectToCopy.nextOffset,
        objectToCopy.minLength,
        objectToCopy.relevantEqualities);
  }

  @Override
  public SMGObject copyWithNewLevel(int newLevel) {
    Preconditions.checkArgument(newLevel >= 0);
    return new SMGSinglyLinkedListSegment(
        newLevel, getSize(), getOffset(), headOffset, nextOffset, minLength, relevantEqualities);
  }

  public SMGSinglyLinkedListSegment copyWithNewMinimumLength(int newMinimumLength) {
    Preconditions.checkArgument(newMinimumLength >= 0);
    return new SMGSinglyLinkedListSegment(
        getNestingLevel(),
        getSize(),
        getOffset(),
        headOffset,
        nextOffset,
        newMinimumLength,
        relevantEqualities);
  }

  public SMGSinglyLinkedListSegment copyWithNewRelevantEqualities(
      EqualityCache<Value> pRelevantEqualities) {
    return new SMGSinglyLinkedListSegment(
        getNestingLevel(),
        getSize(),
        getOffset(),
        headOffset,
        nextOffset,
        minLength,
        pRelevantEqualities);
  }

  /**
   * Creates a new linked list segment with the length decremented but minimal 0.
   *
   * @return a new linked list segment with length max(current length - 1, 0)
   */
  public SMGObject decrementLengthAndCopy() {
    return new SMGSinglyLinkedListSegment(
        getNestingLevel(),
        getSize(),
        getOffset(),
        headOffset,
        nextOffset,
        Integer.max(getMinLength() - 1, 0),
        relevantEqualities);
  }

  @Override
  public SMGObject freshCopy() {
    return new SMGSinglyLinkedListSegment(
        getNestingLevel(),
        getSize(),
        getOffset(),
        headOffset,
        nextOffset,
        minLength,
        relevantEqualities);
  }

  @Override
  public String toString() {
    return minLength + "+SLL " + super.hashCode();
  }

  @Override
  public boolean isSLL() {
    return true;
  }

  public EqualityCache<Value> getRelevantEqualities() {
    return relevantEqualities;
  }
}
