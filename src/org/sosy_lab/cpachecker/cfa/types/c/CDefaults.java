/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;

public class CDefaults {

  private CDefaults() { }

  public static CLiteralExpression forType(CType type, FileLocation fileLoc) {
    if (type instanceof CPointerType) {
      return new CIntegerLiteralExpression(fileLoc, CNumericTypes.SIGNED_INT, BigInteger.ZERO);

    } else if (type instanceof CSimpleType) {
      CBasicType basicType = ((CSimpleType)type).getType();
      switch (basicType) {
      case CHAR:
        return new CCharLiteralExpression(fileLoc, type, '\0');

      case DOUBLE:
      case FLOAT:
        return new CFloatLiteralExpression(fileLoc, type, BigDecimal.ZERO);

      case UNSPECIFIED:
      case BOOL:
      case INT:
      case VOID: // is this legitimate for "void"?
        return new CIntegerLiteralExpression(fileLoc, type, BigInteger.ZERO);

      default:
        throw new AssertionError("Unknown basic type '" + basicType + "'");
      }

    } else if (type instanceof CEnumType) {
      // enum declaration: enum e { ... } var;
      return new CIntegerLiteralExpression(fileLoc, CNumericTypes.SIGNED_INT, BigInteger.ZERO);

    } else if (type instanceof CElaboratedType && ((CElaboratedType)type).getKind() == ComplexTypeKind.ENUM) {
      // enum declaration: enum e var;
      return new CIntegerLiteralExpression(fileLoc, CNumericTypes.SIGNED_INT, BigInteger.ZERO);

    } else {
      // TODO create initializer for arrays, structs, enums
      return null;
    }
  }

}
