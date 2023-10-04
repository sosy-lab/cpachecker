// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants;

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;

public interface TypeInfo {

  boolean isSigned();

  Number getMinValue();

  Number getMaxValue();

  String abbrev();

  static TypeInfo from(MachineModel pMachineModel, Type pType) {
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
        if (cType.hasKnownConstantSize()) {
          size = pMachineModel.getSizeofInBits(cType).intValueExact();
        } else {
          throw new IllegalArgumentException("Unsupported type: " + type);
        }
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
        case UNSPECIFIED:
        case VOID:
        default:
          throw new IllegalArgumentException("Unsupported type: " + type);
      }
    } else {
      throw new IllegalArgumentException("Unsupported type: " + type);
    }
    return BitVectorInfo.from(size, signed);
  }

  static boolean isSupported(Type pType) {
    Type type = pType;
    if (type instanceof CType) {
      type = ((CType) type).getCanonicalType();
    }
    if (type instanceof CType cType) {
      if (!cType.hasKnownConstantSize() || cType.isIncomplete()) {
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
        case UNSPECIFIED:
        case VOID:
        default:
          return false;
      }
    }
    return false;
  }
}
