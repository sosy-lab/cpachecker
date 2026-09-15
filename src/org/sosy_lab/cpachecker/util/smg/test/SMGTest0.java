// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.test;

import com.google.common.base.Preconditions;
import java.math.BigInteger;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.util.smg.graph.SMGDoublyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

/**
 * Base class for writing SMG test. The purpose of the class is to offer several convenient
 * functions to initialize smg edges and nodes.
 */
public class SMGTest0 {

  protected static final BigInteger mockType2bSize = BigInteger.valueOf(16);
  protected static final BigInteger mockType4bSize = BigInteger.valueOf(32);
  protected static final BigInteger mockType8bSize = BigInteger.valueOf(64);
  protected static final BigInteger mockType16bSize = BigInteger.valueOf(128);
  protected static final BigInteger mockType32bSize = BigInteger.valueOf(256);

  protected SMGObject createRegion(BigInteger withSize, int withOffset) {
    return SMGObject.of(0, withSize, BigInteger.valueOf(withOffset));
  }

  protected SMGObject createRegion(BigInteger withSize) {
    return createRegion(withSize, 0);
  }

  protected SMGObject createRegion(int withSize) {
    return createRegion(BigInteger.valueOf(withSize));
  }

  protected SMGDoublyLinkedListSegment createDLLS(
      int withSize,
      int headOffset,
      int prevOffset,
      int prevPointerTargetOffset,
      int next,
      int nextPointerTargetOffset) {
    return createDLLS(
        0,
        withSize,
        headOffset,
        prevOffset,
        prevPointerTargetOffset,
        next,
        nextPointerTargetOffset,
        0);
  }

  protected SMGDoublyLinkedListSegment createDLLS(
      int pLevel,
      int withSize,
      int headOffset,
      int prevOffset,
      int prevPointerTargetOffset,
      int next,
      int nextPointerTargetOffset,
      int minListLength) {
    Preconditions.checkArgument(prevOffset != next);
    Preconditions.checkArgument(prevOffset < withSize);
    Preconditions.checkArgument(next < withSize);
    return new SMGDoublyLinkedListSegment(
        pLevel,
        new NumericValue(BigInteger.valueOf(withSize)),
        BigInteger.ZERO,
        BigInteger.valueOf(headOffset),
        BigInteger.valueOf(next),
        BigInteger.valueOf(nextPointerTargetOffset),
        BigInteger.valueOf(prevOffset),
        BigInteger.valueOf(prevPointerTargetOffset),
        minListLength);
  }

  protected SMGHasValueEdge createHasValueEdgeToZero(BigInteger withSize) {
    return createHasValueEdge(withSize, 0, SMGValue.zeroValue());
  }

  protected SMGHasValueEdge createHasValueEdgeToZero(BigInteger withSize, int andOffset) {
    return createHasValueEdge(withSize, andOffset, SMGValue.zeroValue());
  }

  protected SMGHasValueEdge createHasValueEdge(BigInteger withSize, SMGValue andValue) {
    return createHasValueEdge(withSize, 0, andValue);
  }

  protected SMGHasValueEdge createHasValueEdge(BigInteger withSize, int offset, SMGValue andValue) {
    return new SMGHasValueEdge(andValue, withSize, BigInteger.valueOf(offset));
  }

  protected SMGHasValueEdge createHasValueEdgeToZero(int withSize) {
    return createHasValueEdge(withSize, 0, SMGValue.zeroValue());
  }

  protected SMGHasValueEdge createHasValueEdgeToZero(int withSize, int andOffset) {
    return createHasValueEdge(withSize, andOffset, SMGValue.zeroValue());
  }

  protected SMGHasValueEdge createHasValueEdge(int withSize, int offset, SMGValue andValue) {
    return new SMGHasValueEdge(andValue, BigInteger.valueOf(offset), BigInteger.valueOf(withSize));
  }

  protected SMGPointsToEdge createPTRegionEdge(int withOffset, SMGObject andObject) {
    return createPTEdge(withOffset, SMGTargetSpecifier.IS_REGION, andObject);
  }

  protected SMGPointsToEdge createPTEdge(
      int withPointerTargetOffset, SMGTargetSpecifier targetSpecifier, SMGObject targetObject) {
    return new SMGPointsToEdge(
        targetObject, BigInteger.valueOf(withPointerTargetOffset), targetSpecifier);
  }

  protected SMGValue createValue() {
    return SMGValue.of();
  }
}
