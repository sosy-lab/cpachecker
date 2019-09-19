/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.smg.graphs.edge;

import static com.google.common.base.Preconditions.checkArgument;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

/**
 * SMGs have two kind of edges: {@link SMGEdgeHasValue} and {@link SMGEdgePointsTo}. {@link
 * SMGEdgeHasValue}s lead from {@link SMGObject}s to {@link SMGValue}s. {@link SMGEdgeHasValue}s are
 * labelled by the offset and type of the field in which the value is stored within an object.
 */
public class SMGEdgeHasValue extends SMGEdge {

  private final BigInteger sizeInBits;

  /**
   * @param pOffset the offset relative to the start of the source object, i.e. ZERO represents an
   *     direct access, a positive number accessed within or after the object and is used for
   *     array-element or struct-member access.
   * @param pObject the target object pointed to.
   * @param pValue the value that points to some object.
   */
  public SMGEdgeHasValue(BigInteger pSizeInBits, long pOffset, SMGObject pObject, SMGValue pValue) {
    super(pValue, pObject, pOffset);
    sizeInBits = pSizeInBits;
  }

  public SMGEdgeHasValue(long pSizeInBits, long pOffset, SMGObject pObject, SMGValue pValue) {
    this(BigInteger.valueOf(pSizeInBits), pOffset, pObject, pValue);
  }

  @Override
  public String toString() {
    return String.format("%s+%db[%sb]->%s", object.getLabel(), getOffset(), sizeInBits, value);
  }

  public long getSizeInBits() {
    return sizeInBits.longValueExact();
  }

  @Override
  public boolean isConsistentWith(SMGEdge other) {
    if (! (other instanceof SMGEdgeHasValue)) {
      return false;
    }

    if (object == other.object
        && getOffset() == other.getOffset()
        && sizeInBits.equals(((SMGEdgeHasValue) other).sizeInBits)) {
      return value.equals(other.value);
    }

    return true;
  }

  public boolean overlapsWith(SMGEdgeHasValue other) {
    checkArgument(
        object == other.object,
        "Call of overlapsWith() on Has-Value edges pair not originating from the same object");

    long otStart = other.getOffset();
    long otEnd = otStart + other.getSizeInBits();
    return overlapsWith(otStart, otEnd);
  }

  public boolean overlapsWith(long pOtStart, long pOtEnd) {

    long myStart = getOffset();

    long myEnd = myStart + getSizeInBits();

    if (myStart < pOtStart) {
      return (myEnd > pOtStart);

    } else if (pOtStart < myStart) {
      return (pOtEnd > myStart);
    }

    // Start offsets are equal, always overlap
    return true;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + sizeInBits.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SMGEdgeHasValue)) {
      return false;
    }
    SMGEdgeHasValue other = (SMGEdgeHasValue) obj;
    return super.equals(obj) && sizeInBits.equals(other.sizeInBits);
  }
}