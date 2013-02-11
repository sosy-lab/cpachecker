/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.ctoformulahelper;

import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * We use this type to be able to track the type of aliases
 * (For example we could create a formula like *l = *r
 * when the right side is actually no pointer then we use CDereferenceType(typeof(r)))
 */
public final class CDereferenceType extends CtoFormulaCType {

  private final CType type;
  private final boolean   isConst;
  private final boolean   isVolatile;
  private final CType guessedType;

  public CDereferenceType(final boolean pConst, final boolean pVolatile,
      final CType pType, CType pGuessedType) {
    isConst = pConst;
    isVolatile = pVolatile;
    type = pType;
    guessedType = pGuessedType;
  }

  public CType getGuessedType() {
    return guessedType;
  }

  public CDereferenceType withGuess(CType guessedType){
    if (this.guessedType != null) {
      throw new IllegalArgumentException("Type was already guessed!");
    }

    return new CDereferenceType(isConst, isVolatile, type, guessedType);
  }


  @Override
  public boolean isConst() {
    return isConst;
  }

  @Override
  public boolean isVolatile() {
    return isVolatile;
  }

  public CType getType() {
    return type;
  }

  @Override
  public String toString() {
    String decl;

    decl = "!(" + type.toString() + ")";


    return (isConst() ? "const " : "")
        + (isVolatile() ? "volatile " : "")
        + decl;
  }

  @Override
  public String toASTString(String pDeclarator) {
    // ugly hack but it works:
    // We need to insert the "*" between the type and the name (e.g. "int *var").
    String decl;

    if (type instanceof CArrayType) {
      decl = type.toASTString("(!" + pDeclarator + ")");
    } else {
      decl = type.toASTString("!" + pDeclarator);
    }

    return (isConst() ? "const " : "")
        + (isVolatile() ? "volatile " : "")
        + decl;
  }

  @Override
  public <R, X extends Exception> R accept(CtoFormulaTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }
}
