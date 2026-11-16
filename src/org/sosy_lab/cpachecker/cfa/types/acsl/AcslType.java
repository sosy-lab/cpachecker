// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.acsl;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;

public sealed interface AcslType extends Type
    permits AcslCType,
        AcslFunctionType,
        AcslLogicType,
        AcslPointerType,
        AcslPredicateType,
        AcslSetType {

  /** Whether the values of the type are integers, e.g., for integer or a C integer type. */
  static boolean isIntegral(AcslType pType) {
    return pType == AcslBuiltinLogicType.INTEGER
        || (pType instanceof AcslCType cType
            && (cType.getType().getCanonicalType() instanceof CEnumType
                || (cType.getType().getCanonicalType() instanceof CSimpleType simpleType
                    && !simpleType.hasComplexSpecifier()
                    && !simpleType.hasImaginarySpecifier()
                    && simpleType.getType().isIntegerType())));
  }

  /** Whether the values of the type are numbers, e.g., for real or a C floating type. */
  static boolean isNumeric(AcslType pType) {
    return isIntegral(pType)
        || pType == AcslBuiltinLogicType.REAL
        || (pType instanceof AcslCType cType
            && cType.getType().getCanonicalType() instanceof CSimpleType simpleType
            && !simpleType.hasComplexSpecifier()
            && !simpleType.hasImaginarySpecifier()
            && simpleType.getType().isFloatingPointType());
  }

  /** Whether the values of the type are truth values, i.e., for boolean or _Bool. */
  static boolean isBoolean(AcslType pType) {
    return pType == AcslBuiltinLogicType.BOOLEAN
        || (pType instanceof AcslCType cType
            && cType.getType().getCanonicalType() instanceof CSimpleType simpleType
            && simpleType.getType() == CBasicType.BOOL);
  }

  /** Whether the values of the type are pointers, which includes C arrays. */
  static boolean isPointer(AcslType pType) {
    return pType instanceof AcslPointerType
        || (pType instanceof AcslCType cType
            && (cType.getType().getCanonicalType() instanceof CPointerType
                || cType.getType().getCanonicalType() instanceof CArrayType));
  }

  /** Whether a value of one of the types can be used for the other one, maybe after promotion. */
  static boolean areCompatible(AcslType pType1, AcslType pType2) {
    return pType1.equals(pType2)
        || pType1 instanceof AcslPolymorphicType
        || pType2 instanceof AcslPolymorphicType
        || (isNumeric(pType1) && isNumeric(pType2))
        || (isBoolean(pType1) && isBoolean(pType2))
        || (isPointer(pType1) && isPointer(pType2));
  }

  /**
   * Returns the type to which both types are promoted, as in §2.2.3 of the ACSL standard, e.g.,
   * real for integer and a C floating type.
   */
  static AcslType mostGeneralType(AcslType pType1, AcslType pType2) {
    if (pType1.equals(pType2)) {
      return pType1;
    } else if (isNumeric(pType1) && isNumeric(pType2)) {
      return isIntegral(pType1) && isIntegral(pType2)
          ? AcslBuiltinLogicType.INTEGER
          : AcslBuiltinLogicType.REAL;
    } else if (isBoolean(pType1) && isBoolean(pType2)) {
      return AcslBuiltinLogicType.BOOLEAN;
    }
    throw new IllegalArgumentException(
        "The types " + pType1 + " and " + pType2 + " have no common type");
  }
}
