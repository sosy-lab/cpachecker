/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.sl;

import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayRangeDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;

/**
 * Keeps the separation logic heap up-to-date.
 */
public class SLVisitor implements CAstNodeVisitor<Boolean, Exception> {

  private final SLVisitorDelegate delegate;
  private boolean onRightHandSide;
  private CPointerExpression topLvlPtrExp;
  private boolean isAssignment;

  public SLVisitor(SLVisitorDelegate pDelegate) {
    delegate = pDelegate;
    onRightHandSide = false;
    topLvlPtrExp = null;
    isAssignment = false;
  }

  @Override
  public Boolean visit(CArrayDesignator pArrayDesignator) throws Exception {
    throw new UnsupportedOperationException(
        CArrayDesignator.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public Boolean visit(CArrayRangeDesignator pArrayRangeDesignator) throws Exception {
    throw new UnsupportedOperationException(
        CArrayRangeDesignator.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public Boolean visit(CFieldDesignator pFieldDesignator) throws Exception {
    throw new UnsupportedOperationException(
        CFieldDesignator.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public Boolean visit(CInitializerExpression pInitializerExpression) throws Exception {
    return pInitializerExpression.getExpression().accept(this);
  }

  @Override
  public Boolean visit(CInitializerList pInitializerList) throws Exception {
    boolean isTarget = false;
    for (CInitializer i : pInitializerList.getInitializers()) {
      isTarget |= i.accept(this);
    }
    return isTarget;
  }

  @Override
  public Boolean visit(CDesignatedInitializer pCStructInitializerPart) throws Exception {
    throw new UnsupportedOperationException(
        CDesignatedInitializer.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public Boolean visit(CFunctionCallExpression pIastFunctionCallExpression) throws Exception {
    return false;
  }

  @Override
  public Boolean visit(CBinaryExpression pIastBinaryExpression) throws Exception {
    return pIastBinaryExpression.getOperand1().accept(this)
        || pIastBinaryExpression.getOperand2().accept(this);
  }

  @Override
  public Boolean visit(CCastExpression pIastCastExpression) throws Exception {
    throw new UnsupportedOperationException(
        CCastExpression.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public Boolean visit(CCharLiteralExpression pIastCharLiteralExpression) throws Exception {
    throw new UnsupportedOperationException(
        CCharLiteralExpression.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public Boolean visit(CFloatLiteralExpression pIastFloatLiteralExpression) throws Exception {
    throw new UnsupportedOperationException(
        CFloatLiteralExpression.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public Boolean visit(CIntegerLiteralExpression pIastIntegerLiteralExpression) throws Exception {
    return false;
  }

  @Override
  public Boolean visit(CStringLiteralExpression pIastStringLiteralExpression) throws Exception {
    throw new UnsupportedOperationException(
        CStringLiteralExpression.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public Boolean visit(CTypeIdExpression pIastTypeIdExpression) throws Exception {
    throw new UnsupportedOperationException(
        CTypeIdExpression.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public Boolean visit(CUnaryExpression pIastUnaryExpression) throws Exception {
    return pIastUnaryExpression.getOperand().accept(this);
  }

  @Override
  public Boolean visit(CImaginaryLiteralExpression PIastLiteralExpression) throws Exception {
    throw new UnsupportedOperationException(
        CImaginaryLiteralExpression.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public Boolean visit(CAddressOfLabelExpression pAddressOfLabelExpression) throws Exception {
    throw new UnsupportedOperationException(
        CAddressOfLabelExpression.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public Boolean visit(CArraySubscriptExpression pIastArraySubscriptExpression) throws Exception {
    throw new UnsupportedOperationException(
        CArraySubscriptExpression.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public Boolean visit(CFieldReference pIastFieldReference) throws Exception {
    throw new UnsupportedOperationException(
        CFieldReference.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public Boolean visit(CIdExpression pIastIdExpression) throws Exception {
    // boolean isOnHeap = delegate.isAllocated(pIastIdExpression, pIastIdExpression.getName());
    return false;
  }

  @Override
  public Boolean visit(CPointerExpression pPointerExpression) throws Exception {
    boolean isTarget = false;
    CExpression operand = pPointerExpression.getOperand();
    isTarget = !delegate.isAllocated(operand);

    if (topLvlPtrExp == null) {
      topLvlPtrExp = pPointerExpression;
    }

    // if (operand instanceof CIdExpression) {
    // CIdExpression e = (CIdExpression) operand;
    // isTarget = !delegate.isAllocated(e.getName());
    //
    // } else {
    // throw new NotImplementedException();
    // }

    return isTarget;
  }

  @Override
  public Boolean visit(CComplexCastExpression pComplexCastExpression) throws Exception {
    throw new UnsupportedOperationException(
        CComplexCastExpression.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public Boolean visit(CFunctionDeclaration pDecl) throws Exception {
    delegate.setFunctionScope(pDecl.getName());
    boolean isTarget = false;
    for (CParameterDeclaration dec : pDecl.getParameters()) {
      isTarget |= dec.accept(this);
    }
    return isTarget;
  }

  @Override
  public Boolean visit(CComplexTypeDeclaration pDecl) throws Exception {
    throw new UnsupportedOperationException(
        CComplexTypeDeclaration.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public Boolean visit(CTypeDefDeclaration pDecl) throws Exception {
    throw new UnsupportedOperationException(
        CTypeDefDeclaration.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public Boolean visit(CVariableDeclaration pDecl) throws Exception {
    // final CIdExpression e = new CIdExpression(pDecl.getFileLocation(), pDecl);
    // delegate.addVarToHeap(e, pDecl.getName());
    boolean isTarget = false;
    CInitializer i = pDecl.getInitializer();
    if (i != null) {
      isTarget = acceptOnRightHandSide(i);
    }
    return isTarget;
  }

  @Override
  public Boolean visit(CParameterDeclaration pDecl) throws Exception {
    return false;
  }

  @Override
  public Boolean visit(CEnumerator pDecl) throws Exception {
    throw new UnsupportedOperationException(
        CEnumerator.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public Boolean visit(CExpressionStatement pIastExpressionStatement) throws Exception {
    return pIastExpressionStatement.getExpression().accept(this);
  }

  @Override
  public Boolean visit(CExpressionAssignmentStatement pIastExpressionAssignmentStatement)
      throws Exception {
    topLvlPtrExp = null;
    isAssignment = true;
    final CRightHandSide rhSide = pIastExpressionAssignmentStatement.getRightHandSide();
    final boolean rightIsTarget = acceptOnRightHandSide(rhSide);

    final CLeftHandSide lhSide = pIastExpressionAssignmentStatement.getLeftHandSide();
    final boolean leftIsTarget = lhSide.accept(this);

    if (lhSide instanceof CPointerExpression) {
      CPointerExpression e = (CPointerExpression) lhSide;
      CExpression o = e.getOperand();
      if (o instanceof CIdExpression) {
        CIdExpression cid = (CIdExpression) o;
        delegate.updateHeap(cid.getName(), (CExpression) rhSide);
      }

    }

    return leftIsTarget || rightIsTarget;
  }

  @Override
  public Boolean visit(CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement)
      throws Exception {
    topLvlPtrExp = null;
    isAssignment = true;
    final CFunctionCallExpression fctExp = pIastFunctionCallAssignmentStatement.getRightHandSide();
    final boolean rightIsTarget = acceptOnRightHandSide(fctExp);
    final CIdExpression fctNameExp = (CIdExpression) fctExp.getFunctionNameExpression();

    final CLeftHandSide lhSide = pIastFunctionCallAssignmentStatement.getLeftHandSide();
    final boolean leftIsTarget = lhSide.accept(this);


    if (fctNameExp.getName().equals("malloc")) {
      final String varName = ((CIdExpression) lhSide).getName();
      CExpression allocationSize = fctExp.getParameterExpressions().get(0);
      delegate.addToHeap(varName);
    }


    return leftIsTarget || rightIsTarget;
  }

  @Override
  public Boolean visit(CFunctionCallStatement pIastFunctionCallStatement) throws Exception {
    throw new UnsupportedOperationException(
        CFunctionCallStatement.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public Boolean visit(CReturnStatement pNode) throws Exception {
    throw new UnsupportedOperationException(
        CReturnStatement.class.getSimpleName() + "is not implemented yet.");
  }

  public interface SLVisitorDelegate {
    public void handleMalloc(String pVarName, CExpression pAllocationSize);

    public void addToHeap(String pVarName);

    public void updateHeap(String pVarName, CExpression pExp);

    public void setFunctionScope(String scope);

    public boolean isAllocated(CExpression pExp);
  }

  private boolean acceptOnRightHandSide(CRightHandSide pExp) throws Exception {
    onRightHandSide = true;
    boolean isTarget = pExp.accept(this);
    onRightHandSide = false;
    return isTarget;
  }

  private boolean acceptOnRightHandSide(CInitializer pExp) throws Exception {
    onRightHandSide = true;
    boolean isTarget = pExp.accept(this);
    onRightHandSide = false;
    return isTarget;
  }
}

