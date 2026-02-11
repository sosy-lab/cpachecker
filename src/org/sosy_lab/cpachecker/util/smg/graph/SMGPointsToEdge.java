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
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

/**
 * SMG edge pointing from an (SMG-)value to an (SMG-)object. Has a target specifier consisting of
 * SMGTargetSpecifier {first, last, all, region} and an offset.
 */
public class SMGPointsToEdge {

  private final SMGObject pointsToObject;
  private final Value offset;
  private SMGTargetSpecifier targetSpecifier;

  /**
   * Constructs a new edge that points-to an object that may be a region or a DLS.
   *
   * @param pPointsToObject - The object this edge points to (region or S/DLL).
   * @param pOffset - The offset wrt. to the base address of object. This may be negative (i.e. in a
   *     linux list).
   * @param pTargetSpecifier - The target specifier {@link SMGTargetSpecifier} {first, last, all,
   *     region}. The specifier is a region iff the object models a region. If the object is a DLS,
   *     this specifies whether it points to the first, last or all concrete regions of the object.
   */
  public SMGPointsToEdge(
      SMGObject pPointsToObject, BigInteger pOffset, SMGTargetSpecifier pTargetSpecifier) {
    Preconditions.checkNotNull(pPointsToObject);
    Preconditions.checkNotNull(pOffset);
    Preconditions.checkNotNull(pTargetSpecifier);
    pointsToObject = pPointsToObject;
    offset = new NumericValue(pOffset);
    targetSpecifier = pTargetSpecifier;
  }

  public SMGPointsToEdge(
      SMGObject pPointsToObject, Value pOffset, SMGTargetSpecifier pTargetSpecifier) {
    Preconditions.checkNotNull(pPointsToObject);
    Preconditions.checkNotNull(pOffset);
    Preconditions.checkNotNull(pTargetSpecifier);
    pointsToObject = pPointsToObject;
    offset = pOffset;
    targetSpecifier = pTargetSpecifier;
  }

  /** Returns the SMGObject this edge points to. */
  public SMGObject pointsTo() {
    return pointsToObject;
  }

  public Value getOffset() {
    return offset;
  }

  public SMGTargetSpecifier targetSpecifier() {
    return targetSpecifier;
  }

  public boolean pointsToRegion() {
    return targetSpecifier.isRegion();
  }

  public boolean pointsToFirst() {
    return targetSpecifier.isFirst();
  }

  public boolean pointsToLast() {
    return targetSpecifier.isLast();
  }

  public boolean pointsToAll() {
    return targetSpecifier.isAll();
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    return other instanceof SMGPointsToEdge otherEdge
        && otherEdge.offset.equals(offset)
        && otherEdge.pointsToObject.equals(pointsToObject)
        && targetSpecifier.equals(otherEdge.targetSpecifier);
  }

  @Override
  public int hashCode() {
    return targetSpecifier.hashCode() + 31 * pointsToObject.hashCode() + 41 * offset.hashCode();
  }

  public void setTargetSpecifier(SMGTargetSpecifier pTargetSpecifier) {
    targetSpecifier = pTargetSpecifier;
  }

  public SMGPointsToEdge copyAndSetTargetSpecifier(SMGTargetSpecifier pTargetSpecifier) {
    return new SMGPointsToEdge(pointsToObject, offset, pTargetSpecifier);
  }

  @Override
  public String toString() {
    return " -> ("
        + targetSpecifier
        + ") ["
        + (offset instanceof NumericValue numOffset ? numOffset.bigIntegerValue() : offset)
        + "] "
        + pointsToObject;
  }
}
