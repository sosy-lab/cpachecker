/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;

public class SMGEdgeHasValue extends SMGEdge {
  final private CType type;
  final private BigInteger offset;
  private int hashcode;

  public SMGEdgeHasValue(CType pType, BigInteger pOffset, SMGObject pObject, int pValue) {
    super(pValue, pObject);
    type = pType;
    offset = pOffset;
  }

  public SMGEdgeHasValue(int pSizeInBits, BigInteger pOffset, SMGObject pObject, int pValue) {
    super(pValue, pObject);
    type = AnonymousTypes.createTypeWithLength(pSizeInBits);
    offset = pOffset;
  }

  @Override
  public String toString() {
    return "sizeof(" + type.toASTString("foo") + ")b @ " + object.getLabel() + "+" + offset + "b has value " + value;
  }

  public BigInteger getOffset() {
    return offset;
  }

  public CType getType() {
    return type;
  }

  public int getSizeInBits(MachineModel pMachineModel) {
    if (!(type instanceof CVoidType) && type.isIncomplete()) {
      throw new IllegalArgumentException("Cannot compute size of " + object.getLabel() + " with "
          + "incomplete type " + type);
    }
    return pMachineModel.getBitSizeof(type);
  }

  @Override
  public boolean isConsistentWith(SMGEdge other) {
    if (! (other instanceof SMGEdgeHasValue)) {
      return false;
    }

    if ((object == other.object) &&
        (Objects.equals(offset, ((SMGEdgeHasValue)other).offset)) &&
        (type == ((SMGEdgeHasValue)other).type)) {
      return (value == other.value);
    }

    return true;
  }

  public boolean overlapsWith(SMGEdgeHasValue other, MachineModel pModel) {
    if (object != other.object) {
      throw new IllegalArgumentException("Call of overlapsWith() on Has-Value edges pair not originating from the same object");
    }

    BigInteger otStart = other.getOffset();

    BigInteger otEnd = otStart.add(BigInteger.valueOf(pModel.getBitSizeof(other.getType())));

    return overlapsWith(otStart, otEnd, pModel);
  }

  public boolean overlapsWith(BigInteger pOtStart, BigInteger pOtEnd, MachineModel pModel) {

    BigInteger myStart = offset;

    BigInteger myEnd = myStart.add(BigInteger.valueOf(pModel.getBitSizeof(type)));

    if (myStart.compareTo(pOtStart) < 0) {
      return (myEnd.compareTo(pOtStart) > 0);

    } else if (pOtStart.compareTo(myStart) < 0) {
      return (pOtEnd.compareTo(myStart) > 0);
    }

    // Start offsets are equal, always overlap
    return true;
  }

  public boolean isCompatibleField(SMGEdgeHasValue other) {
    return type.equals(other.type) && (offset == other.offset);
  }

  public boolean isCompatibleFieldOnSameObject(SMGEdgeHasValue other, MachineModel pModel) {
    return pModel.getBitSizeof(type) == pModel.getBitSizeof(other.type) && (offset.equals(other.offset)) && object == other.object;
  }

  @Override
  public int hashCode() {
    if (hashcode == 0) {
      hashcode = 31 * super.hashCode() + Objects.hash(type, offset);
    }
    return hashcode;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof SMGEdgeHasValue)) {
      return false;
    }
    SMGEdgeHasValue other = (SMGEdgeHasValue) obj;
    return super.equals(obj)
        && Objects.equals(offset, other.offset)
        && type.getCanonicalType().equals(other.type.getCanonicalType());
  }

  public static class SMGEdgeHasValueComparator
      implements Serializable, Comparator<SMGEdgeHasValue> {

    private static final long serialVersionUID = 1L;

      @Override
      public int compare(SMGEdgeHasValue o1, SMGEdgeHasValue o2) {
        int result = Integer.compare(o1.getObject().getId(), o2.getObject().getId());
        if (result == 0) {
          result = o1.offset.compareTo(o2.offset);
          if (result == 0) {
            result = Integer.compare(o1.getValue(), o2.getValue());
            if (result == 0) {
              result = o1.type.getCanonicalType().toString().compareTo(o2.type.getCanonicalType().toString());
            }
          }
        }
        return result;
      }
    }
}