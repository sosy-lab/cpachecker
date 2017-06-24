/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.invariants;

import com.google.common.base.Preconditions;
import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.MachineModel.BaseSizeofVisitor;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;

public class BitVectorInfo implements TypeInfo {

  private final int size;

  private final boolean signed;

  private final BigInteger minValue;

  private final BigInteger maxValue;

  private BitVectorInfo(int pSize, boolean pSigned) {
    Preconditions.checkArgument(pSize >= 0, "bit vector size must not be negative");
    size = pSize;
    signed = pSigned;
    minValue = !signed ? BigInteger.ZERO : BigInteger.valueOf(2).pow(size - 1).negate();
    maxValue = !signed ? BigInteger.valueOf(2).pow(size).subtract(BigInteger.ONE) : BigInteger.valueOf(2).pow(size - 1).subtract(BigInteger.ONE);
  }

  public int getSize() {
    return size;
  }

  @Override
  public boolean isSigned() {
    return signed;
  }

  @Override
  public BigInteger getMinValue() {
    return minValue;
  }

  @Override
  public BigInteger getMaxValue() {
    return maxValue;
  }

  public BitVectorInterval getRange() {
    return BitVectorInterval.of(this, minValue, maxValue);
  }

  @Override
  public String abbrev() {
    return size + (signed ? "" : "U");
  }

  @Override
  public String toString() {
    return String.format("Size: %d; Signed: %b", size, signed);
  }

  @Override
  public int hashCode() {
    return signed ? -size : size;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther instanceof BitVectorInfo) {
      BitVectorInfo other = (BitVectorInfo) pOther;
      return size == other.size && signed == other.signed;
    }
    return false;
  }

  public static BitVectorInfo from(int pSize, boolean pSigned) {
    return new BitVectorInfo(pSize, pSigned);
  }

  public static TypeInfo from(MachineModel pMachineModel, Type pType) {
    Type type = pType;
    if (type instanceof CType) {
      type = ((CType) type).getCanonicalType();
    }
    final int size;
    final boolean signed;
    if (type instanceof CType) {
      boolean isBitField = false;
      int bitFieldSize = 0;
      if (type instanceof CBitFieldType) {
        isBitField = true;
        CBitFieldType bitFieldType = (CBitFieldType) type;
        type = bitFieldType.getType();
        bitFieldSize = bitFieldType.getBitFieldSize();
      }
      if (type instanceof CSimpleType) {
        CBasicType basicType = ((CSimpleType) type).getType();
        if (basicType == CBasicType.FLOAT) {
          return FloatingPointTypeInfo.FLOAT;
        }
        if (basicType == CBasicType.DOUBLE) {
          return FloatingPointTypeInfo.DOUBLE;
        }
      }
      CType cType = (CType) type;
      if (isBitField) {
        size = bitFieldSize;
      } else {
        int sizeInChars = 0;
        if (!cType.isIncomplete()) {
          sizeInChars = cType.accept(new BaseSizeofVisitor(pMachineModel));
        }
        if (sizeInChars == 0) {
          sizeInChars = pMachineModel.getSizeofPtr();
        }
        size = sizeInChars * pMachineModel.getSizeofCharInBits();
      }
      assert size >= 0;
      signed = (type instanceof CSimpleType) && pMachineModel.isSigned((CSimpleType) type);
    } else if (type instanceof JSimpleType) {
      switch (((JSimpleType) type).getType()) {
      case BOOLEAN:
        size = 32;
        signed = false;
        break;
      case BYTE:
        size = 8;
        signed = true;
        break;
      case CHAR:
        size = 16;
        signed = false;
        break;
      case SHORT:
        size = 16;
        signed = true;
        break;
      case INT:
        size = 32;
        signed = true;
        break;
      case LONG:
        size = 64;
        signed = true;
        break;
        case FLOAT:
          return FloatingPointTypeInfo.FLOAT;
        case DOUBLE:
          return FloatingPointTypeInfo.DOUBLE;
      case NULL:
      case UNSPECIFIED:
      case VOID:
      default:
        throw new IllegalArgumentException("Unsupported type: " + type);
      }
    } else {
      throw new IllegalArgumentException("Unsupported type: " + type);
    }
    return from(size, signed);
  }

  public static boolean isSupported(Type pType) {
    Type type = pType;
    if (type instanceof CType) {
      type = ((CType) type).getCanonicalType();
    }
    if (type instanceof CType) {
      if (((CType) type).isIncomplete()) {
        return false;
      }
      if (!(type instanceof CSimpleType)) {
        return type instanceof CPointerType;
      }
      switch (((CSimpleType) type).getType()) {
      case CHAR:
      case INT:
        return true;
      case FLOAT:
      case DOUBLE:
      case UNSPECIFIED:
      default:
        return false;
      }
    }
    if (type instanceof JSimpleType) {
      switch (((JSimpleType) type).getType()) {
      case BOOLEAN:
      case BYTE:
      case CHAR:
      case SHORT:
      case INT:
      case LONG:
        return true;
      case FLOAT:
      case DOUBLE:
      case NULL:
      case UNSPECIFIED:
      case VOID:
      default:
        return false;
      }
    }
    return false;
  }

  public BitVectorInfo extend(int pExtension) {
    return new BitVectorInfo(size + pExtension, signed);
  }

}
