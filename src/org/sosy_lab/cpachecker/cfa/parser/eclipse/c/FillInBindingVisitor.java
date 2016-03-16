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

import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.DefaultCTypeVisitor;

/**
 * Visitor that fills in missing bindings of CElaboratedTypes with a given
 * target type (if name and kind match, of course).
 */
class FillInBindingVisitor extends DefaultCTypeVisitor<Void, RuntimeException> {

  private final ComplexTypeKind kind;
  private final String name;
  private final CComplexType target;

  FillInBindingVisitor(ComplexTypeKind pKind, String pName, CComplexType pTarget) {
    kind = pKind;
    name = pName;
    target = pTarget;
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
    if (pElaboratedType.getRealType() == null
        && pElaboratedType.getKind() == kind
        && pElaboratedType.getName().equals(name)) {

      pElaboratedType.setRealType(target);
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
}