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

import com.google.common.annotations.VisibleForTesting;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.TypeUtils;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

/**
 * SMGs have two kind of edges: {@link SMGEdgeHasValue} and {@link SMGEdgePointsTo}. {@link
 * SMGEdgeHasValue}s lead from {@link SMGObject}s to {@link SMGValue}s. {@link SMGEdgeHasValue}s are
 * labelled by the offset and type of the field in which the value is stored within an object.
 */
public class SMGEdgeHasValue extends SMGEdge implements Comparable<SMGEdgeHasValue> {

  final private CType type;
  final private int sizeInBits;

  /**
   * @param pType type of the object's memory starting at offset.
   * @param pOffset the offset relative to the start of the source object, i.e. ZERO represents an
   *     direct access, a positive number accessed within or after the object and is used for
   *     array-element or struct-member access.
   * @param pObject the target object pointed to.
   * @param pValue the value that points to some object.
   */
  public SMGEdgeHasValue(MachineModel pMachineModel, CType pType, long pOffset, SMGObject pObject, SMGValue pValue) {
    super(pValue, pObject, pOffset);
    type = pType;
    sizeInBits = pMachineModel.getSizeofInBits(type).intValueExact();
  }

  public SMGEdgeHasValue(int pSizeInBits, long pOffset, SMGObject pObject, SMGValue pValue) {
    super(pValue, pObject, pOffset);
    type = TypeUtils.createTypeWithLength(pSizeInBits);
    sizeInBits = pSizeInBits;
  }

  @Override
  public String toString() {
    return String.format(
        "Type \"%s\" with size %db @ %s+%db has value %s",
        type.toASTString(""), sizeInBits, object.getLabel(), getOffset(), value);
  }

  public CType getType() {
    return type;
  }

  public int getSizeInBits() {
    return sizeInBits;
  }

  @Override
  public boolean isConsistentWith(SMGEdge other) {
    if (! (other instanceof SMGEdgeHasValue)) {
      return false;
    }

    if (object == other.object
        && getOffset() == other.getOffset()
        && type == ((SMGEdgeHasValue) other).type) {
      return value.equals(other.value);
    }

    return true;
  }

  public boolean overlapsWith(SMGEdgeHasValue other, MachineModel pModel) {
    if (object != other.object) {
      throw new IllegalArgumentException("Call of overlapsWith() on Has-Value edges pair not originating from the same object");
    }

    long otStart = other.getOffset();

    long otEnd = otStart + pModel.getSizeofInBits(other.getType()).longValueExact();

    return overlapsWith(otStart, otEnd, pModel);
  }

  public boolean overlapsWith(long pOtStart, long pOtEnd, MachineModel pModel) {

    long myStart = getOffset();

    long myEnd = myStart + pModel.getSizeofInBits(type).longValueExact();

    if (myStart < pOtStart) {
      return (myEnd > pOtStart);

    } else if (pOtStart < myStart) {
      return (pOtEnd > myStart);
    }

    // Start offsets are equal, always overlap
    return true;
  }

  @VisibleForTesting
  public boolean isCompatibleField(SMGEdgeHasValue other) {
    return type.equals(other.type) && (getOffset() == other.getOffset());
  }

  public boolean isCompatibleFieldOnSameObject(SMGEdgeHasValue other) {
    if (other.getValue().isZero()) {
      return
          getOffset() + getSizeInBits() <= other.getOffset() + other.getSizeInBits()
            && getOffset() >= other.getOffset()
            && object == other.object;
    } else {
      return
          getSizeInBits() == other.getSizeInBits()
              && getOffset() == other.getOffset()
              && object == other.object;
    }
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + type.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SMGEdgeHasValue)) {
      return false;
    }
    SMGEdgeHasValue other = (SMGEdgeHasValue) obj;
    return super.equals(obj)
        && type.getCanonicalType().equals(other.type.getCanonicalType());
  }

  @Override
  public int compareTo(SMGEdgeHasValue o) {
    int result = object.compareTo(o.object);
    if (result != 0) {
      return result;
    }
    result = Long.compare(getOffset(), o.getOffset());
    if (result != 0) {
      return result;
    }
    result = value.compareTo(o.value);
    if (result != 0) {
      return result;
    }
    result = Long.compare(sizeInBits, o.sizeInBits);
    if (result != 0) {
      return result;
    }
    if (type.getCanonicalType().equals(o.type.getCanonicalType())) {
      return 0;
    } else {
      if (type.canBeAssignedFrom(o.type)) {
        return -1;
      }
      if (o.type.canBeAssignedFrom(type)) {
        return 1;
      }
      // FIXME: require some comparison of types
      return -1;
    }
  }
}