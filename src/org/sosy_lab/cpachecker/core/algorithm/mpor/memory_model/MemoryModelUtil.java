// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.memory_model;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.DefaultCTypeVisitor;
import org.sosy_lab.cpachecker.exceptions.NoException;

public class MemoryModelUtil {

  public static final class CFieldMemberDeclarationVisitor
      extends DefaultCTypeVisitor<CCompositeTypeMemberDeclaration, NoException> {

    private final CFieldReference fieldReference;

    public CFieldMemberDeclarationVisitor(CFieldReference pFieldReference) {
      fieldReference = pFieldReference;
    }

    @Override
    public @Nullable CCompositeTypeMemberDeclaration visitDefault(CType pT) {
      return null;
    }

    @Override
    public CCompositeTypeMemberDeclaration visit(CArrayType pArrayType) {
      return pArrayType.getType().accept(this);
    }

    @Override
    public @Nullable CCompositeTypeMemberDeclaration visit(CCompositeType pCompositeType) {
      for (CCompositeTypeMemberDeclaration member : pCompositeType.getMembers()) {
        if (member.getName().equals(fieldReference.getFieldName())) {
          return member;
        }
      }
      return null;
    }

    @Override
    public @Nullable CCompositeTypeMemberDeclaration visit(CElaboratedType pElaboratedType) {
      if (pElaboratedType.getRealType() != null) {
        return pElaboratedType.getRealType().accept(this);
      }
      return null;
    }

    @Override
    public @Nullable CCompositeTypeMemberDeclaration visit(CFunctionType pFunctionType) {
      for (CType parameterType : pFunctionType.getParameters()) {
        return parameterType.accept(this);
      }
      return null;
    }

    @Override
    public CCompositeTypeMemberDeclaration visit(CPointerType pPointerType) {
      return pPointerType.getType().accept(this);
    }

    @Override
    public CCompositeTypeMemberDeclaration visit(CTypedefType pTypedefType) {
      return pTypedefType.getRealType().accept(this);
    }

    @Override
    public CCompositeTypeMemberDeclaration visit(CBitFieldType pCBitFieldType) {
      return pCBitFieldType.getType().accept(this);
    }
  }
}
