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

import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;

class RepresentabilityCTypeVisitor implements CtoFormulaTypeVisitor<Boolean, RuntimeException> {

  @Override
  public Boolean visit(CArrayType pArrayType) {
    return pArrayType.getType().accept(this);
  }

  @Override
  public Boolean visit(CCompositeType pCompositeType) {
    for (CCompositeTypeMemberDeclaration decl : pCompositeType.getMembers()) {
      if (!decl.getType().accept(this)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public Boolean visit(CElaboratedType pElaboratedType) {
    switch (pElaboratedType.getKind()) {
    case ENUM:
      return true;
    default:
      // Return true if the type is resolved and it is a representable type.
      if (pElaboratedType.getRealType() == null) {
        return false;
      } else {
        return pElaboratedType.getRealType().accept(this);
      }
    }
  }

  @Override
  public Boolean visit(CEnumType pEnumType) {
    return true;
  }

  @Override
  public Boolean visit(CFunctionType pFunctionPointerType) {
    return false;
  }

  @Override
  public Boolean visit(CPointerType pPointerType) {
    return true;
  }

  @Override
  public Boolean visit(CProblemType pProblemType) {
    return false;
  }

  @Override
  public Boolean visit(CSimpleType pSimpleType) {
    return true;
  }

  @Override
  public Boolean visit(CTypedefType pTypedefType) {
    return pTypedefType.getRealType().accept(this);
  }

  @Override
  public Boolean visit(CDereferenceType pCDereferenceType) {
    return pCDereferenceType.getType().accept(this);
  }
}