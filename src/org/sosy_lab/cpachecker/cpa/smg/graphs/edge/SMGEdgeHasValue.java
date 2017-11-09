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

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.AnonymousTypes;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

/**
 * SMGs have two kind of edges: {@link SMGEdgeHasValue} and {@link SMGEdgePointsTo}. {@link
 * SMGEdgeHasValue}s lead from {@link SMGObject}s to {@link SMGValue}s. {@link SMGEdgeHasValue}s are
 * labelled by the offset and type of the field in which the value is stored within an object.
 */
public class SMGEdgeHasValue extends SMGEdge {

  final private CType type;

  public SMGEdgeHasValue(CType pType, long pOffset, SMGObject pObject, int pValue) {
    super(pValue, pObject, pOffset);
    type = pType;
  }

  public SMGEdgeHasValue(int pSizeInBits, long pOffset, SMGObject pObject, int pValue) {
    super(pValue, pObject, pOffset);
    type = AnonymousTypes.createTypeWithLength(pSizeInBits);
  }

  @Override
  public String toString() {
    return "sizeof("
        + type.toASTString("foo")
        + ")b @ "
        + object.getLabel()
        + "+"
        + getOffset()
        + "b has value "
        + value;
  }

  public CType getType() {
    return type;
  }

  public int getSizeInBits(MachineModel pMachineModel) {
    return pMachineModel.getSizeofInBits(type);
  }

  @Override
  public boolean isConsistentWith(SMGEdge other) {
    if (! (other instanceof SMGEdgeHasValue)) {
      return false;
    }

    if (object == other.object
        && getOffset() == ((SMGEdgeHasValue) other).getOffset()
        && type == ((SMGEdgeHasValue) other).type) {
      return value == other.value;
    }

    return true;
  }

  public boolean overlapsWith(SMGEdgeHasValue other, MachineModel pModel) {
    if (object != other.object) {
      throw new IllegalArgumentException("Call of overlapsWith() on Has-Value edges pair not originating from the same object");
    }

    long otStart = other.getOffset();

    long otEnd = otStart + pModel.getSizeofInBits(other.getType());

    return overlapsWith(otStart, otEnd, pModel);
  }

  public boolean overlapsWith(long pOtStart, long pOtEnd, MachineModel pModel) {

    long myStart = getOffset();

    long myEnd = myStart + pModel.getSizeofInBits(type);

    if (myStart < pOtStart) {
      return (myEnd > pOtStart);

    } else if (pOtStart < myStart) {
      return (pOtEnd > myStart);
    }

    // Start offsets are equal, always overlap
    return true;
  }

  public boolean isCompatibleField(SMGEdgeHasValue other) {
    return type.equals(other.type) && (getOffset() == other.getOffset());
  }

  public boolean isCompatibleFieldOnSameObject(SMGEdgeHasValue other, MachineModel pModel) {
    return pModel.getSizeofInBits(type) == pModel.getSizeofInBits(other.type)
        && (getOffset() == other.getOffset())
        && object == other.object;
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
}