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
package org.sosy_lab.cpachecker.cfa.types.c;

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

import com.google.common.collect.ImmutableList;

public class CDefaults {

  private CDefaults() { }

  public static CInitializer forType(CType type, FileLocation fileLoc) {
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
      case FLOAT:
        return initializerFor(new CFloatLiteralExpression(fileLoc, type, BigDecimal.ZERO), fileLoc);

      case UNSPECIFIED:
      case BOOL:
      case INT:
      case VOID: // is this legitimate for "void"?
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

    } else {
      throw new IllegalArgumentException("Type " + type + " has no default value");
    }
  }

  private static CInitializerList emptyAggregate(FileLocation fileLoc) {
    // The initializer { } (without any explizit values)
    // is equal to initializing all fields/elements with 0
    // (C standard §6.7.9 (21))
    return new CInitializerList(fileLoc, ImmutableList.<CInitializer>of());
  }

  private static CIntegerLiteralExpression signedIntZero(FileLocation fileLoc) {
    return new CIntegerLiteralExpression(fileLoc, CNumericTypes.SIGNED_INT, BigInteger.ZERO);
  }

  private static CInitializerExpression initializerFor(CExpression exp, FileLocation fileLoc) {
    return new CInitializerExpression(fileLoc, exp);
  }
}
