// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.memory_model;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
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

  public static final class CLeftHandSideSimpleDeclarationVisitor
      implements CLeftHandSideVisitor<CSimpleDeclaration, NoException> {

    @Override
    public CSimpleDeclaration visit(CArraySubscriptExpression pArraySubscriptExpression)
        throws NoException {

      CLeftHandSide arrayLeftHandSide =
          (CLeftHandSide) pArraySubscriptExpression.getArrayExpression();
      return arrayLeftHandSide.accept(this);
    }

    @Override
    public CSimpleDeclaration visit(CFieldReference pFieldReference) throws NoException {
      CLeftHandSide fieldOwnerLeftHandSide = (CLeftHandSide) pFieldReference.getFieldOwner();
      return fieldOwnerLeftHandSide.accept(this);
    }

    @Override
    public CSimpleDeclaration visit(CIdExpression pIdExpression) throws NoException {
      return pIdExpression.getDeclaration();
    }

    @Override
    public CSimpleDeclaration visit(CPointerExpression pPointerExpression) throws NoException {
      CLeftHandSide operandLeftHandSide = (CLeftHandSide) pPointerExpression.getOperand();
      return operandLeftHandSide.accept(this);
    }

    @Override
    public CSimpleDeclaration visit(CComplexCastExpression pComplexCastExpression)
        throws NoException {

      CLeftHandSide operandLeftHandSide = (CLeftHandSide) pComplexCastExpression.getOperand();
      return operandLeftHandSide.accept(this);
    }
  }

  public static final class CFieldReferenceVisitor
      extends DefaultCExpressionVisitor<Void, NoException> {

    private final Set<CFieldReference> fieldReferences;

    public CFieldReferenceVisitor() {
      fieldReferences = new HashSet<>();
    }

    ImmutableSet<CFieldReference> getFieldReferences() {
      return ImmutableSet.copyOf(fieldReferences);
    }

    @Override
    public Void visit(CArraySubscriptExpression pArraySubscriptExpression) {
      pArraySubscriptExpression.getSubscriptExpression().accept(this);
      return null;
    }

    @Override
    public Void visit(CFieldReference pFieldReference) {
      fieldReferences.add(pFieldReference);
      pFieldReference.getFieldOwner().accept(this);
      return null;
    }

    @Override
    public Void visit(CPointerExpression pPointerExpression) {
      pPointerExpression.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(CComplexCastExpression pComplexCastExpression) {
      pComplexCastExpression.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(CBinaryExpression pBinaryExpression) {
      pBinaryExpression.getOperand1().accept(this);
      pBinaryExpression.getOperand2().accept(this);
      return null;
    }

    @Override
    public Void visit(CCastExpression pCastExpression) {
      pCastExpression.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(CUnaryExpression pUnaryExpression) {
      pUnaryExpression.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(CIdExpression pIdExpression) {
      return null;
    }

    @Override
    protected Void visitDefault(CExpression pExpression) {
      return null;
    }
  }

  public static final class CCompositeTypeMemberDeclarationVisitor
      extends DefaultCTypeVisitor<CCompositeTypeMemberDeclaration, NoException> {

    private final CFieldReference fieldReference;

    public CCompositeTypeMemberDeclarationVisitor(CFieldReference pFieldReference) {
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
