/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.cpalien;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class SMGEdgeHasValue extends SMGEdge {
  final private CType type;
  final private int offset;

  final private CSimpleType dummyChar = new CSimpleType(false, false, CBasicType.CHAR, false, false, true, false, false, false, false);
  final private CSimpleType dummyInt = new CSimpleType(false, false, CBasicType.INT, true, false, false, true, false, false, false);

  public SMGEdgeHasValue(CType pType, int pOffset, SMGObject pObject, int pValue) {
    super(pValue, pObject);
    type = pType;
    offset = pOffset;
  }

  public SMGEdgeHasValue(int pSizeInBytes, int pOffset, SMGObject pObject, int pValue) {
    super(pValue, pObject);
    CIntegerLiteralExpression arrayLen = new CIntegerLiteralExpression(null, dummyInt, BigInteger.valueOf(pSizeInBytes));
    type = new CArrayType(false, false, dummyChar, arrayLen);
    offset = pOffset;
  }

  @Override
  public String toString() {
    return "sizeof(" + type.toASTString("foo") + ")b @ " + object.getLabel() + "+" + offset + "b has value " + value;
  }

  public int getOffset() {
    return offset;
  }

  public CType getType() {
    return type;
  }

  public int getSizeInBytes(MachineModel pMachineModel) {
    return pMachineModel.getSizeof(type);
  }

  @Override
  public boolean isConsistentWith(SMGEdge other) {
    if (! (other instanceof SMGEdgeHasValue)) {
      return false;
    }

    if ((this.object == other.object) &&
        (this.offset == ((SMGEdgeHasValue)other).offset) &&
        (this.type == ((SMGEdgeHasValue)other).type)) {
      return (this.value == other.value);
    }

    return true;
  }

  public boolean overlapsWith(SMGEdgeHasValue other, MachineModel pModel) {
    if (this.object != other.object) {
      throw new IllegalArgumentException("Call of overlapsWith() on Has-Value edges pair not originating from the same object");
    }

    int otStart = other.getOffset();

    int otEnd = otStart + pModel.getSizeof(other.getType());

    return overlapsWith(otStart, otEnd, pModel);
  }

  public boolean overlapsWith(int pOtStart, int pOtEnd, MachineModel pModel) {

    int myStart = offset;

    int myEnd = myStart + pModel.getSizeof(type);

    if (myStart < pOtStart) {
      return (myEnd > pOtStart);

    } else if (pOtStart < myStart) {
      return (pOtEnd > myStart);
    }

    // Start offsets are equal, always overlap
    return true;
  }

  // TODO: Fix compatibility detection based on type, not on the type size
  public boolean isCompatibleField(SMGEdgeHasValue other, MachineModel pModel) {
    // return (this.type.equals(other.type)) && (this.offset == other.offset);
    return pModel.getSizeof(type) == pModel.getSizeof(other.type) && (this.offset == other.offset);
  }

  public boolean isCompatibleFieldOnSameObject(SMGEdgeHasValue other, MachineModel pModel) {
    // return (this.type.equals(other.type)) && (this.offset == other.offset) && (this.object == other.object);
    return pModel.getSizeof(type) == pModel.getSizeof(other.type) && (this.offset == other.offset) && this.object == other.object;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + offset;
    result = prime * result + ((type == null) ? 0 : type.getCanonicalType().hashCode());
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    SMGEdgeHasValue other = (SMGEdgeHasValue) obj;
    if (offset != other.offset) {
      return false;
    }
    if (type == null) {
      if (other.type != null) {
        return false;
      }
    } else if (!type.getCanonicalType().equals(other.type.getCanonicalType())) {
      return false;
    }
    return true;
  }
}

class SMGEdgeHasValueFilter {

  public static SMGEdgeHasValueFilter objectFilter(SMGObject pObject) {
    SMGEdgeHasValueFilter filter = new SMGEdgeHasValueFilter();
    filter.filterByObject(pObject);

    return filter;
  }

  private SMGObject object = null;

  private Integer value = null;
  private boolean valueComplement = false;
  private Integer offset = null;
  private CType type = null;

  public void filterByObject(SMGObject pObject) {
    object = pObject;
  }

  public void filterHavingValue(Integer pValue) {
    value = pValue;
    valueComplement = false;
  }

  public void filterNotHavingValue(Integer pValue) {
    value = pValue;
    valueComplement = true;
  }

  public void filterAtOffset(Integer pOffset) {
    offset = pOffset;
  }

  public void filterByType(CType pType) {
    type = pType;
  }

  public boolean holdsFor(SMGEdgeHasValue pEdge) {
    if (object != null && object != pEdge.getObject()) {
      return false;
    }

    if (value != null) {
      if (valueComplement && pEdge.getValue() == value) {
        return false;
      }
      else if ( (!valueComplement) && pEdge.getValue() != value) {
        return false;
      }
    }

    if (offset != null && offset != pEdge.getOffset()) {
      return false;
    }

    if (type != null && ! type.getCanonicalType().equals(pEdge.getType().getCanonicalType())) {
      return false;
    }

    return true;
  }

  public Set<SMGEdgeHasValue> filterSet(Set<SMGEdgeHasValue> pEdges) {
    Set<SMGEdgeHasValue> returnSet = new HashSet<>();
    for (SMGEdgeHasValue edge : pEdges) {
      if (this.holdsFor(edge)) {
        returnSet.add(edge);
      }
    }
    return Collections.unmodifiableSet(returnSet);
  }
}