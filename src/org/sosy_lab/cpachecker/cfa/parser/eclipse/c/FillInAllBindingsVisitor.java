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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.DefaultCTypeVisitor;

/**
 * Visitor that fills in missing bindings of CElaboratedTypes with matching
 * types from the scope (if name and kind match, of course).
 */
class FillInAllBindingsVisitor extends DefaultCTypeVisitor<Void, RuntimeException> {

  private final Scope scope;
  private final ProgramDeclarations programDeclarations;

  FillInAllBindingsVisitor(Scope pScope, ProgramDeclarations pProgramDeclarations) {
    scope = pScope;
    programDeclarations = pProgramDeclarations;

  }

  @Override
  public Void visitDefault(CType pT) {
    return null;
  }

  @Override
  public Void visit(CArrayType pArrayType) {
    pArrayType.getType().accept(this);
    return null;
  }

  @Override
  public Void visit(CCompositeType pCompositeType) {
    for (CCompositeTypeMemberDeclaration member : pCompositeType.getMembers()) {
      member.getType().accept(this);
    }
    return null;
  }

  @Override
  public Void visit(CElaboratedType pElaboratedType) {
    if (pElaboratedType.getRealType() == null) {

      CComplexType realType = scope.lookupType(pElaboratedType.getQualifiedName());
      while (realType instanceof CElaboratedType) {
        realType = ((CElaboratedType)realType).getRealType();
      }
      if (realType == null) {
        realType = programDeclarations.lookupType(pElaboratedType.getQualifiedName(), pElaboratedType.getOrigName());
      }
      if (realType != null) {
        pElaboratedType.setRealType(realType);
      }
    }
    return null;
  }

  @Override
  public Void visit(CFunctionType pFunctionType) {
    pFunctionType.getReturnType().accept(this);
    for (CType parameter : pFunctionType.getParameters()) {
      parameter.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(CPointerType pPointerType) {
    pPointerType.getType().accept(this);
    return null;
  }

  @Override
  public Void visit(CTypedefType pTypedefType) {
    pTypedefType.getRealType().accept(this);
    return null;
  }

  @Override
  public Void visit(CBitFieldType pCBitFieldType) throws RuntimeException {
    pCBitFieldType.getType().accept(this);
    return null;
  }
}