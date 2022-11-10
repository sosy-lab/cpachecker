// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.graph;

import java.math.BigInteger;

/**
 * SMG edge pointing from an (SMG-)value to an (SMG-)object. Has a target specifier consisting of
 * SMGTargetSpecifier {first, last, all, region} and a offset.
 */
public class SMGPointsToEdge implements SMGEdge, Comparable<SMGPointsToEdge> {

  private final SMGObject pointsToObject;
  private final BigInteger offset;
  private SMGTargetSpecifier targetSpecifier;

  /**
   * Constructs a new edge that points-to an object that may be a region or a DLS.
   *
   * @param pPointsToObject - The object this edge points to (region or DLS).
   * @param pOffset - The offset wrt. to the base address of object. This may be negative (i.e. in a
   *     linux list).
   * @param pTargetSpecifier - The target specifier SMGTargetSpecifier {first, last, all, region}.
   *     The specifier is a region iff the object models a region. If the object is a DLS, this
   *     specifies wheter it points to the first, last or all concrete regions of the object.
   */
  public SMGPointsToEdge(
      SMGObject pPointsToObject, BigInteger pOffset, SMGTargetSpecifier pTargetSpecifier) {
    pointsToObject = pPointsToObject;
    offset = pOffset;
    targetSpecifier = pTargetSpecifier;
  }

  /**
   * @return the SMGObject this edge points to.
   */
  public SMGObject pointsTo() {
    return pointsToObject;
  }

  @Override
  public BigInteger getOffset() {
    return offset;
  }

  public SMGTargetSpecifier targetSpecifier() {
    return targetSpecifier;
  }

  @Override
  public int compareTo(SMGPointsToEdge pOther) {
    if (pointsToObject.compareTo(pOther.pointsToObject) == 0) {
      if (offset.compareTo(pOther.offset) == 0) {
        if (targetSpecifier == pOther.targetSpecifier) {
          return 0;
        } else {
          return 1;
        }
      } else {
        return offset.compareTo(pOther.offset);
      }
    } else {
      return pointsToObject.compareTo(pOther.pointsToObject);
    }
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof SMGPointsToEdge)) {
      return false;
    }
    SMGPointsToEdge otherEdge = (SMGPointsToEdge) other;
    if (otherEdge.offset.equals(offset)
        && otherEdge.pointsToObject.equals(pointsToObject)
        && targetSpecifier.equals(otherEdge.targetSpecifier)) {
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return super.hashCode() + 31 * pointsToObject.hashCode() + 41 * offset.intValue();
  }

  public void setTargetSpecifier(SMGTargetSpecifier pTargetSpecifier) {
    targetSpecifier = pTargetSpecifier;
  }

  @Override
  public String toString() {
    return " -> [" + offset + "] " + pointsToObject;
  }
}
