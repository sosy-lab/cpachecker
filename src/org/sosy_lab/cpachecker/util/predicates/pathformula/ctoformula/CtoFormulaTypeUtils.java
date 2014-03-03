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
package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

class CtoFormulaTypeUtils {

  public static boolean areEqualWithMatchingPointerArray(CType t1, CType t2) {
    if (t1 == null || t2 == null) {
      return t1 == t2;
    }

    t1 = t1.getCanonicalType();
    t2 = t2.getCanonicalType();

    return t1.equals(t2)
        || areMatchingPointerArrayTypes(t1, t2)
        || areMatchingPointerArrayTypes(t2, t1);
  }

  private static boolean areMatchingPointerArrayTypes(CType t1, CType t2) {
    if ((t1 instanceof CPointerType) && (t2 instanceof CArrayType)) {

      CType componentType1 = ((CPointerType)t1).getType();
      CType componentType2 = ((CArrayType)t2).getType();

      return (t1.isConst() == t2.isConst())
          && (t1.isVolatile() == t2.isVolatile())
          && componentType1.equals(componentType2);
    } else {
      return false;
    }
  }

  /**
   * CFieldReferences can be direct or indirect (pointer-dereferencing).
   * This method nests the owner in a CUnaryExpression if the access is indirect.
   */
  public static CExpression getRealFieldOwner(CFieldReference fExp) throws UnrecognizedCCodeException {
    CExpression fieldOwner = fExp.getFieldOwner();
    if (fExp.isPointerDereference()) {
      CType t = fieldOwner.getExpressionType().getCanonicalType();
      if (!(t instanceof CPointerType)) {
        throw new UnrecognizedCCodeException("Can't dereference a non-pointer in a field reference", fExp);
      }
      CType dereferencedType = ((CPointerType)t).getType();
      return new CPointerExpression(fExp.getFileLocation(), dereferencedType, fieldOwner);
    }
    return fieldOwner;
  }
}
