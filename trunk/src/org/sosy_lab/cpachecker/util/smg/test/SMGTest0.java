// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.test;

import java.math.BigInteger;
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
      int withSize, int offset, int prevOffset, int next) {
    return createDLLS(0, withSize, offset, prevOffset, next, 0);
  }

  protected SMGDoublyLinkedListSegment createDLLS(
      int pLevel, int withSize, int offset, int prevOffset, int next, int mLength) {
    return new SMGDoublyLinkedListSegment(
        pLevel,
        BigInteger.valueOf(withSize),
        BigInteger.valueOf(offset),
        BigInteger.valueOf(prevOffset),
        BigInteger.valueOf(next),
        mLength,
        BigInteger.valueOf(offset));
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

  protected SMGHasValueEdge createHasValueEdge(int withSize, SMGValue andValue) {
    return createHasValueEdge(withSize, 0, andValue);
  }

  protected SMGHasValueEdge createHasValueEdge(int withSize, int offset, SMGValue andValue) {
    return new SMGHasValueEdge(andValue, BigInteger.valueOf(offset), BigInteger.valueOf(withSize));
  }

  protected SMGPointsToEdge createPTRegionEdge(int withOffset, SMGObject andObject) {
    return createPTEdge(withOffset, SMGTargetSpecifier.IS_REGION, andObject);
  }

  protected SMGPointsToEdge createPTDLLsEdge(int withOffset, SMGObject andObject) {
    return createPTEdge(withOffset, SMGTargetSpecifier.IS_ALL_POINTER, andObject);
  }

  protected SMGPointsToEdge createPTEdge(
      int withOffset, SMGTargetSpecifier targetSpecifier, SMGObject andObject) {
    return new SMGPointsToEdge(andObject, BigInteger.valueOf(withOffset), targetSpecifier);
  }

  protected SMGValue createValue(int withLevel) {
    return SMGValue.of(withLevel);
  }

  protected SMGValue createValue() {
    return SMGValue.of(0);
  }

  protected SMGObject createRegion(BigInteger withSize, int withOffset, String label) {
    return new LabeledObject(0, withSize, BigInteger.valueOf(withOffset), label);
  }

  protected SMGObject createRegion(BigInteger withSize, String label) {
    return createRegion(withSize, 0, label);
  }

  protected SMGObject createRegion(int withSize, String label) {
    return createRegion(BigInteger.valueOf(withSize), label);
  }

  protected SMGDoublyLinkedListSegment createDLLS(
      int withSize, int offset, int prevOffset, int next, String label) {
    return createDLLS(0, withSize, offset, prevOffset, next, 0, label);
  }

  protected SMGDoublyLinkedListSegment createDLLS(
      int pLevel, int withSize, int offset, int prevOffset, int next, int mLength, String label) {
    return new LabeledDLLS(
        pLevel,
        BigInteger.valueOf(withSize),
        BigInteger.valueOf(offset),
        BigInteger.valueOf(prevOffset),
        BigInteger.valueOf(next),
        mLength,
        BigInteger.valueOf(offset),
        label);
  }

  protected SMGValue createValue(int withLevel, String label) {
    return new LabeledValue(withLevel, label);
  }

  protected SMGValue createValue(String label) {
    return new LabeledValue(0, label);
  }
}
