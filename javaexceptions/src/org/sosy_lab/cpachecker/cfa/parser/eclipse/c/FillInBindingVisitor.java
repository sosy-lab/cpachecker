// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
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
import org.sosy_lab.cpachecker.exceptions.NoException;

/**
 * Visitor that fills in missing bindings of CElaboratedTypes with a given target type (if name and
 * kind match, of course).
 */
class FillInBindingVisitor extends DefaultCTypeVisitor<Void, NoException> {

  private final ComplexTypeKind kind;
  private final String name;
  private final CComplexType target;

  FillInBindingVisitor(ComplexTypeKind pKind, String pName, CComplexType pTarget) {
    kind = pKind;
    name = pName;
    target = pTarget;
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
    if (pElaboratedType.getRealType() == null
        && pElaboratedType.getKind() == kind
        && pElaboratedType.getName().equals(name)) {

      pElaboratedType.setRealType(target);
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
