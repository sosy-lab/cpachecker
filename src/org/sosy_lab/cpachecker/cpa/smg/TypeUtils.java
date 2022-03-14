// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class TypeUtils {
  public static CType createTypeWithLength(int pSizeInBits) {
    if (pSizeInBits % 8 == 0) {
      CIntegerLiteralExpression arrayLen =
          new CIntegerLiteralExpression(
              FileLocation.DUMMY,
              CNumericTypes.UNSIGNED_LONG_INT,
              BigInteger.valueOf(pSizeInBits / 8));
      return new CArrayType(false, false, CNumericTypes.SIGNED_CHAR, arrayLen);
    } else {
      CSimpleType fieldType = CNumericTypes.SIGNED_CHAR;
      return new CBitFieldType(fieldType, pSizeInBits);
    }
  }

  public static CType getRealExpressionType(CType type) {
    return type.getCanonicalType();
  }

  public static CType getRealExpressionType(CSimpleDeclaration decl) {
    return getRealExpressionType(decl.getType());
  }

  public static CType getRealExpressionType(CRightHandSide exp) {
    return getRealExpressionType(exp.getExpressionType());
  }
}
