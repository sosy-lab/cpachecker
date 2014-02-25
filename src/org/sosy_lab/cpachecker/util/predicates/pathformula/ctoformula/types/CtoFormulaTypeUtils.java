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
package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.types;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.MachineModel.BaseSizeofVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class CtoFormulaTypeUtils {

  public static class CtoFormulaSizeofVisitor
    extends BaseSizeofVisitor
    implements CtoFormulaTypeVisitor<Integer, IllegalArgumentException> {

    public CtoFormulaSizeofVisitor(MachineModel pModel) {
      super(pModel);
    }

    @Override
    public Integer visit(CDereferenceType pCDereferenceType) {
      // Assume Guessed size, because we can't know what we are actually dereferencing.
      CType guess = pCDereferenceType.getGuessedType();
      if (guess == null) {
        return 1;
      }

      return guess.accept(this);
    }
  }

  public static CType getCanonicalType(CType t) {
    while (t instanceof CFieldTrackType) {
      t = ((CFieldTrackType)t).getType();
    }
    return t.getCanonicalType();
  }

  public static boolean areEqualWithMatchingPointerArray(CType t1, CType t2) {
    if (t1 == null || t2 == null) {
      return t1 == t2;
    }

    t1 = getCanonicalType(t1);
    t2 = getCanonicalType(t2);

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

  private static CType dereferencedType(CType t) {

    if (t instanceof CFieldTrackType) {
      return new CFieldDereferenceTrackType(dereferencedType(((CFieldTrackType) t).getType()), t);
    }

    CType simple = t.getCanonicalType();
    if (simple instanceof CPointerType) {
      CType inner = ((CPointerType)simple).getType();
      if (inner.getCanonicalType().equals(CNumericTypes.VOID)) {
        // Enable guessing for void*
        return new CDereferenceType(false, false, t, null);
      }
      return inner;
    } else if (simple instanceof CArrayType) {
      return ((CArrayType)simple).getType();
    } else {
      return new CDereferenceType(false, false, t, null);
    }
  }


  /**
   * CFieldReferences can be direct or indirect (pointer-dereferencing).
   * This method nests the owner in a CUnaryExpression if the access is indirect.
   */
  public static CExpression getRealFieldOwner(CFieldReference fExp) {
    CExpression fieldOwner = fExp.getFieldOwner();
    if (fExp.isPointerDereference()) {
      CType dereferencedType = CtoFormulaTypeUtils.dereferencedType(fieldOwner.getExpressionType());
      assert !(dereferencedType instanceof CDereferenceType) : "We should be able to dereference!";
      fieldOwner = new CPointerExpression(null, dereferencedType, fieldOwner);
    }
    return fieldOwner;
  }

  /**
   * Checks if the given type is the result of dereferencing a non-pointer type.
   * @param pType the type to check
   * @return true if the given type is the result of dereferencing a non-pointer type.
   */
  public static boolean isDereferenceType(CType pType) {
    return
        pType instanceof CDereferenceType ||
        (pType instanceof CFieldDereferenceTrackType &&
          isDereferenceType(((CFieldDereferenceTrackType)pType).getType()));
  }

  /**
   * Gets the current guess for the given dereference-type, note
   * this method accepts only types for which isDereferenceType() returns true.
   * @param pType the dereference-type to get the guess from.
   * @return the current guess or null if there is no guess.
   */
  public static CType getGuessedType(CType pType) {
    if (pType instanceof CDereferenceType) {
      CDereferenceType ref = (CDereferenceType) pType ;
      return ref.getGuessedType();
    }

    if (pType instanceof CFieldDereferenceTrackType) {
      return getGuessedType(((CFieldDereferenceTrackType)pType).getType());
    }

    throw new IllegalArgumentException("No DereferenceType!");
  }

  /**
   * Sets the given guess on the given dereference-type.
   * Note this method doesn't change the parameters, but instead returns a fresh instance.
   * getGuessedType should return null for setGuessedType to work!
   * @param pType the type to set the guess from
   * @param set the type to set as guess.
   * @return the new type with the new guess set.
   */
  public static CType setGuessedType(CType pType, CType set) {
    if (pType instanceof CDereferenceType) {
      CDereferenceType ref = (CDereferenceType) pType ;
      return ref.withGuess(set);
    }

    if (pType instanceof CFieldDereferenceTrackType) {
      CFieldDereferenceTrackType ref = (CFieldDereferenceTrackType)pType;

      return new CFieldDereferenceTrackType(
          setGuessedType(ref.getType(), set),
          ref.getReferencingFieldType());
    }

    throw new IllegalArgumentException("No DereferenceType!");
  }
}
