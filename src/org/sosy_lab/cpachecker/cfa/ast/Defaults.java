/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.ast;

import java.math.BigInteger;

public class Defaults {

  private Defaults() { }

  private static IType INT_TYPE = new IASTSimpleDeclSpecifier(false, false, BasicType.INT, false, false, true, false, false, false, false);
  
  public static IASTLiteralExpression forType(IType type, IASTFileLocation fileLoc) {
    if (type instanceof IASTPointerTypeSpecifier) {
      return new IASTIntegerLiteralExpression("NULL", fileLoc, INT_TYPE, BigInteger.ZERO);
    
    } else if (type instanceof IASTSimpleDeclSpecifier) {
      BasicType basicType = ((IASTSimpleDeclSpecifier)type).getType();
      switch (basicType) {
      case CHAR:
        return new IASTCharLiteralExpression("'\\0'", fileLoc, type, '\0');
        
      case DOUBLE:
      case FLOAT:
        return new IASTLiteralExpression("0.0", fileLoc, type, IASTLiteralExpression.lk_float_constant);
        
      case INT:
        return new IASTIntegerLiteralExpression("0", fileLoc, type, BigInteger.ZERO);
      default:
        throw new AssertionError("Unknown basic type");  
      }
    
    } else if (type instanceof IASTEnumerationSpecifier) {
      // enum declaration: enum e { ... } var;
      return new IASTIntegerLiteralExpression("0", fileLoc, INT_TYPE, BigInteger.ZERO);

    } else if (type instanceof IASTElaboratedTypeSpecifier && ((IASTElaboratedTypeSpecifier)type).getKind() == IASTElaboratedTypeSpecifier.k_enum) {
      // enum declaration: enum e var;
      return new IASTIntegerLiteralExpression("0", fileLoc, INT_TYPE, BigInteger.ZERO);

    } else {
      // TODO create initializer for arrays, structs, enums
      return null;
    }
  }
  
}
