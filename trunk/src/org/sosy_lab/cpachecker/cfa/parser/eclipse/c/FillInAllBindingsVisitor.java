// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import org.checkerframework.checker.nullness.qual.Nullable;
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
import org.sosy_lab.cpachecker.exceptions.NoException;

/**
 * Visitor that fills in missing bindings of CElaboratedTypes with matching types from the scope (if
 * name and kind match, of course).
 */
class FillInAllBindingsVisitor extends DefaultCTypeVisitor<@Nullable Void, NoException> {

  private final Scope scope;
  private final ProgramDeclarations programDeclarations;

  FillInAllBindingsVisitor(Scope pScope, ProgramDeclarations pProgramDeclarations) {
    scope = pScope;
    programDeclarations = pProgramDeclarations;
  }

  @Override
  public @Nullable Void visitDefault(CType pT) {
    return null;
  }

  @Override
  public @Nullable Void visit(CArrayType pArrayType) {
    pArrayType.getType().accept(this);
    return null;
  }

  @Override
  public @Nullable Void visit(CCompositeType pCompositeType) {
    for (CCompositeTypeMemberDeclaration member : pCompositeType.getMembers()) {
      member.getType().accept(this);
    }
    return null;
  }

  @Override
  public @Nullable Void visit(CElaboratedType pElaboratedType) {
    if (pElaboratedType.getRealType() == null) {

      @Nullable CComplexType realType = scope.lookupType(pElaboratedType.getQualifiedName());
      while (realType instanceof CElaboratedType) {
        realType = ((CElaboratedType) realType).getRealType();
      }
      if (realType == null) {
        realType =
            programDeclarations.lookupType(
                pElaboratedType.getQualifiedName(), pElaboratedType.getOrigName());
      }
      if (realType != null) {
        pElaboratedType.setRealType(realType);
      }
    }
    return null;
  }

  @Override
  public @Nullable Void visit(CFunctionType pFunctionType) {
    pFunctionType.getReturnType().accept(this);
    for (CType parameter : pFunctionType.getParameters()) {
      parameter.accept(this);
    }
    return null;
  }

  @Override
  public @Nullable Void visit(CPointerType pPointerType) {
    pPointerType.getType().accept(this);
    return null;
  }

  @Override
  public @Nullable Void visit(CTypedefType pTypedefType) {
    pTypedefType.getRealType().accept(this);
    return null;
  }

  @Override
  public @Nullable Void visit(CBitFieldType pCBitFieldType) {
    pCBitFieldType.getType().accept(this);
    return null;
  }
}
