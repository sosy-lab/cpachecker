// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;

public final class CDefaults {

  private CDefaults() { }

  public static CInitializer forType(CType type, FileLocation fileLoc) {
    checkNotNull(fileLoc);
    // Get default value of a type for initializations
    // according to C standard §6.7.9 (10)
    type = type.getCanonicalType();
    if (type instanceof CPointerType) {
      return initializerFor(signedIntZero(fileLoc), fileLoc);

    } else if (type instanceof CSimpleType) {
      CBasicType basicType = ((CSimpleType)type).getType();
      switch (basicType) {
      case CHAR:
        return initializerFor(new CCharLiteralExpression(fileLoc, type, '\0'), fileLoc);

      case DOUBLE:
      case FLOAT128:
      case FLOAT:
        return initializerFor(new CFloatLiteralExpression(fileLoc, type, BigDecimal.ZERO), fileLoc);

      case UNSPECIFIED:
      case BOOL:
      case INT128:
      case INT:
        return initializerFor(new CIntegerLiteralExpression(fileLoc, type, BigInteger.ZERO), fileLoc);

      default:
        throw new AssertionError("Unknown basic type '" + basicType + "'");
      }

    } else if (type instanceof CEnumType) {
      // enum declaration: enum e { ... } var;
      return initializerFor(signedIntZero(fileLoc), fileLoc);

    } else if (type instanceof CElaboratedType) {
       if (((CElaboratedType)type).getKind() == ComplexTypeKind.ENUM) {
        // enum declaration: enum e var;
        return initializerFor(signedIntZero(fileLoc), fileLoc);

       } else {
         // struct or union that is incompletely defined,
         // cannot produce an initializer
         throw new IllegalArgumentException("Cannot produce initializer for incompletely defined type " + type);
       }

    } else if (type instanceof CCompositeType) {
      // struct or union
      return emptyAggregate(fileLoc);

    } else if (type instanceof CArrayType) {
      return emptyAggregate(fileLoc);

    } else if (type instanceof CBitFieldType) {
      return forType(((CBitFieldType) type).getType(), fileLoc);

    } else {
      throw new IllegalArgumentException("Type " + type + " has no default value");
    }
  }

  private static CInitializerList emptyAggregate(FileLocation fileLoc) {
    // The initializer { } (without any explizit values)
    // is equal to initializing all fields/elements with 0
    // (C standard §6.7.9 (21))
    return new CInitializerList(fileLoc, ImmutableList.of());
  }

  private static CIntegerLiteralExpression signedIntZero(FileLocation fileLoc) {
    return new CIntegerLiteralExpression(fileLoc, CNumericTypes.SIGNED_INT, BigInteger.ZERO);
  }

  private static CInitializerExpression initializerFor(CExpression exp, FileLocation fileLoc) {
    return new CInitializerExpression(fileLoc, exp);
  }
}
