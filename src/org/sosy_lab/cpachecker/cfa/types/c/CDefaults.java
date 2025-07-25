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
import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue;

public final class CDefaults {

  private CDefaults() {}

  public static CInitializer forType(MachineModel pMachineModel, CType type, FileLocation fileLoc) {
    checkNotNull(pMachineModel);
    checkNotNull(type);
    checkNotNull(fileLoc);
    // Get default value of a type for initializations
    // according to C standard §6.7.9 (10)
    type = type.getCanonicalType();
    if (type instanceof CPointerType) {
      return initializerFor(signedIntZero(fileLoc), fileLoc);

    } else if (type instanceof CSimpleType) {
      CBasicType basicType = ((CSimpleType) type).getType();
      return switch (basicType) {
        case CHAR -> initializerFor(new CCharLiteralExpression(fileLoc, type, '\0'), fileLoc);
        case FLOAT, DOUBLE, FLOAT128 ->
            initializerFor(
                new CFloatLiteralExpression(
                    fileLoc,
                    pMachineModel,
                    type,
                    FloatValue.zero(FloatValue.Format.fromCType(pMachineModel, type))),
                fileLoc);
        case UNSPECIFIED, BOOL, INT128, INT ->
            initializerFor(new CIntegerLiteralExpression(fileLoc, type, BigInteger.ZERO), fileLoc);
      };

    } else if (type instanceof CEnumType) {
      // enum declaration: enum e { ... } var;
      return initializerFor(signedIntZero(fileLoc), fileLoc);

    } else if (type instanceof CElaboratedType) {
      if (((CElaboratedType) type).getKind() == ComplexTypeKind.ENUM) {
        // enum declaration: enum e var;
        return initializerFor(signedIntZero(fileLoc), fileLoc);

      } else {
        // struct or union that is incompletely defined,
        // cannot produce an initializer
        throw new IllegalArgumentException(
            "Cannot produce initializer for incompletely defined type " + type);
      }

    } else if (type instanceof CCompositeType) {
      // struct or union
      return emptyAggregate(fileLoc);

    } else if (type instanceof CArrayType) {
      return emptyAggregate(fileLoc);

    } else if (type instanceof CBitFieldType) {
      return forType(pMachineModel, ((CBitFieldType) type).getType(), fileLoc);

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
