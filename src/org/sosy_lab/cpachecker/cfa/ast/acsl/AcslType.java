// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import static org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUtils.anyPermutationOf;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;

public sealed interface AcslType extends Type
    permits AcslCType, AcslFunctionType, AcslLogicType, AcslPredicateType, AcslSetType {

  private static boolean canBePromotedToInteger(AcslCType pCType) {
    return pCType.getType() instanceof CSimpleType pSimpleType
        && !pSimpleType.hasComplexSpecifier()
        && !pSimpleType.hasImaginarySpecifier()
        && pSimpleType.getType().isIntegerType();
  }

  private static boolean canBePromotedToReal(AcslCType pCType) {
    return pCType.getType() instanceof CSimpleType pSimpleType
        && !pSimpleType.hasComplexSpecifier()
        && !pSimpleType.hasImaginarySpecifier()
        && pSimpleType.getType().isFloatingPointType();
  }

  static AcslType mostGeneralType(AcslType pType1, AcslType pType2) {

    if (pType1 == pType2) {
      return pType1;
    } else if (anyPermutationOf(
        (x, y) -> x == AcslBuiltinLogicType.REAL && y == AcslBuiltinLogicType.INTEGER,
        pType1,
        pType2)) {
      return AcslBuiltinLogicType.REAL;
    } else if (anyPermutationOf(
        (x, y) ->
            x == AcslBuiltinLogicType.INTEGER
                && y instanceof AcslCType pCType
                && canBePromotedToInteger(pCType),
        pType1,
        pType2)) {
      // We are dealing with a some type of int, so we cast it up to an Integer, as in §2.2.3 of the
      // ACSL spec
      return AcslBuiltinLogicType.INTEGER;
    } else if (anyPermutationOf(
        (x, y) ->
            x == AcslBuiltinLogicType.REAL
                && y instanceof AcslCType pCType
                && canBePromotedToReal(pCType),
        pType1,
        pType2)) {
      // We are dealing with a some type of float, so we cast it up to an Real, as in §2.2.3 of
      // the ACSL spec
      return AcslBuiltinLogicType.REAL;
    }

    throw new AssertionError(
        "finding the most general type is not implemented for " + pType1 + " and " + pType2);
  }
}
