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
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
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
    if (pType instanceof CType cType) {
      cType = cType.getCanonicalType();
      if (cType instanceof CSimpleType simpleType) {
        CBasicType basicType = simpleType.getType();
        if (basicType == CBasicType.FLOAT) {
          return FloatingPointTypeInfo.FLOAT;
        }
        if (basicType == CBasicType.DOUBLE) {
          return FloatingPointTypeInfo.DOUBLE;
        }
      }
      if (!cType.hasKnownConstantSize()) {
        throw new IllegalArgumentException("Unsupported type: " + cType);
      }
      final int size = pMachineModel.getSizeofInBits(cType).intValueExact();
      assert size >= 0;
      final boolean signed =
          (cType instanceof CSimpleType simpleType) && pMachineModel.isSigned(simpleType);
      return BitVectorInfo.from(size, signed);

    } else if (pType instanceof JSimpleType simpleType) {
      return switch (simpleType.getType()) {
        case BOOLEAN -> BitVectorInfo.from(32, false);
        case BYTE -> BitVectorInfo.from(8, true);
        case CHAR -> BitVectorInfo.from(16, false);
        case SHORT -> BitVectorInfo.from(16, true);
        case INT -> BitVectorInfo.from(32, true);
        case LONG -> BitVectorInfo.from(64, true);
        case FLOAT -> FloatingPointTypeInfo.FLOAT;
        case DOUBLE -> FloatingPointTypeInfo.DOUBLE;
        default -> throw new IllegalArgumentException("Unsupported type: " + simpleType);
      };
    } else {
      throw new IllegalArgumentException("Unsupported type: " + pType);
    }
  }

  static boolean isSupported(Type pType) {
    if (pType instanceof CType cType) {
      cType = cType.getCanonicalType();
      if (!cType.hasKnownConstantSize() || cType.isIncomplete()) {
        return false;
      }
      if (cType instanceof CSimpleType simpleType) {
        return switch (simpleType.getType()) {
          case CHAR, INT, BOOL, INT128, FLOAT, DOUBLE, FLOAT128 -> true;
          default -> false;
        };
      } else {
        return cType instanceof CPointerType
            || cType instanceof CEnumType
            || cType instanceof CBitFieldType;
      }

    } else if (pType instanceof JSimpleType simpleType) {
      return switch (simpleType.getType()) {
        case BOOLEAN, BYTE, CHAR, SHORT, INT, LONG, FLOAT, DOUBLE -> true;
        default -> false;
      };

    } else {
      return false;
    }
  }
}
