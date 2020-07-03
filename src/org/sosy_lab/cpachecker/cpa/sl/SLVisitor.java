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

import java.util.List;
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
import org.sosy_lab.cpachecker.cpa.sl.SLState.SLStateError;

/**
 * Keeps the separation logic heap up-to-date.
 */
public class SLVisitor implements CAstNodeVisitor<SLStateError, Exception> {

  private final SLHeapDelegate heapDelegate;
  private CLeftHandSide curLHS;
  private CRightHandSide curRHS;

  public SLVisitor(SLHeapDelegate pMemDelegate) {
    heapDelegate = pMemDelegate;
  }

  @Override
  public SLStateError visit(CArrayDesignator pArrayDesignator) throws Exception {
    throw new UnsupportedOperationException(
        CArrayDesignator.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public SLStateError visit(CArrayRangeDesignator pArrayRangeDesignator) throws Exception {
    throw new UnsupportedOperationException(
        CArrayRangeDesignator.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public SLStateError visit(CFieldDesignator pFieldDesignator) throws Exception {
    throw new UnsupportedOperationException(
        CFieldDesignator.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public SLStateError visit(CInitializerExpression pInitializerExpression) throws Exception {
    return pInitializerExpression.getExpression().accept(this);
  }

  @Override
  public SLStateError visit(CInitializerList pInitializerList) throws Exception {
    for (CInitializer i : pInitializerList.getInitializers()) {
      SLStateError error = i.accept(this);
      if(error != null) {
        return error;
      }
    }
    return null;
  }

  @Override
  public SLStateError visit(CDesignatedInitializer pCStructInitializerPart) throws Exception {
    throw new UnsupportedOperationException(
        CDesignatedInitializer.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public SLStateError visit(CFunctionCallExpression pIastFunctionCallExpression) throws Exception {
    CIdExpression fctExp = (CIdExpression) pIastFunctionCallExpression.getFunctionNameExpression();
    final List<CExpression> params = pIastFunctionCallExpression.getParameterExpressions();
    for (CExpression param : params) {
      SLStateError error = param.accept(this);
      if (error != null) {
        return error;
      }
    }

    switch (SLHeapFunction.get(fctExp.getName())) {
      case MALLOC:
        if (curLHS == null) {
          return SLStateError.MEMORY_LEAK;
        }
        heapDelegate.handleMalloc(curLHS, params.get(0));
        break;

      case CALLOC:
        if (curLHS == null) {
          return SLStateError.MEMORY_LEAK;
        }
        heapDelegate.handleCalloc(curLHS, params.get(0), params.get(1));
        break;

      case REALLOC:
        if (curLHS == null) {
          return SLStateError.MEMORY_LEAK;
        }
        return heapDelegate.handleRealloc(curLHS, params.get(0), params.get(1));

      case FREE:
        return heapDelegate.handleFree(params.get(0));

      case ALLOCA:
        heapDelegate.handleAlloca(pIastFunctionCallExpression, params.get(0));
        break;
      default:
        break;
    }
    return null;
  }

  @Override
  public SLStateError visit(CBinaryExpression pIastBinaryExpression) throws Exception {
    SLStateError error = pIastBinaryExpression.getOperand1().accept(this);
    if (error != null) {
      return error;
    }
    return pIastBinaryExpression.getOperand2().accept(this);
  }

  @Override
  public SLStateError visit(CCastExpression pIastCastExpression) throws Exception {
    return pIastCastExpression.getOperand().accept(this);
  }

  @Override
  public SLStateError visit(CCharLiteralExpression pIastCharLiteralExpression) throws Exception {
    return null;
  }

  @Override
  public SLStateError visit(CFloatLiteralExpression pIastFloatLiteralExpression) throws Exception {
    throw new UnsupportedOperationException(
        CCastExpression.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public SLStateError visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
      throws Exception {
    return null;
  }

  @Override
  public SLStateError visit(CStringLiteralExpression pIastStringLiteralExpression)
      throws Exception {
    return null;
  }

  @Override
  public SLStateError visit(CTypeIdExpression pIastTypeIdExpression) throws Exception {
    throw new UnsupportedOperationException(
        CTypeIdExpression.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public SLStateError visit(CUnaryExpression pIastUnaryExpression) throws Exception {
    CExpression operand = pIastUnaryExpression.getOperand();
    return operand.accept(this);
  }

  @Override
  public SLStateError visit(CImaginaryLiteralExpression PIastLiteralExpression) throws Exception {
    throw new UnsupportedOperationException(
        CImaginaryLiteralExpression.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public SLStateError visit(CAddressOfLabelExpression pAddressOfLabelExpression) throws Exception {
    throw new UnsupportedOperationException(
        CAddressOfLabelExpression.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public SLStateError visit(CArraySubscriptExpression pIastArraySubscriptExpression)
      throws Exception {
    CExpression subscriptExp = pIastArraySubscriptExpression.getSubscriptExpression();
    SLStateError error = subscriptExp.accept(this);
    if (error != null) {
      return error;
    }
    CExpression arrayExp = pIastArraySubscriptExpression.getArrayExpression();
    if (curLHS == pIastArraySubscriptExpression) {
      return heapDelegate.handleDereferenceAssignment(arrayExp, subscriptExp, curRHS);
    } else {
      return heapDelegate.handleDereference(arrayExp, subscriptExp);
    }
  }

  @Override
  public SLStateError visit(CFieldReference pIastFieldReference) throws Exception {
    if (pIastFieldReference.isPointerDereference()) {
      CExpression e = pIastFieldReference.getFieldOwner();
      if (curLHS == pIastFieldReference) {
        return heapDelegate.handleDereferenceAssignment(e, null, curRHS);
      } else {
        return heapDelegate.handleDereference(e);
      }

    }
    return null;
  }

  @Override
  public SLStateError visit(CIdExpression pIastIdExpression) throws Exception {
    return null;
  }

  @Override
  public SLStateError visit(CPointerExpression pPointerExpression) throws Exception {
    CExpression operand = pPointerExpression.getOperand();
    SLStateError error = operand.accept(this);
    if (error != null) {
      return error;
    }
    if (curLHS == pPointerExpression) { // is assignment?
      return heapDelegate.handleDereferenceAssignment(operand, null, curRHS);
    } else {
      return heapDelegate.handleDereference(operand, null);
    }
  }

  @Override
  public SLStateError visit(CComplexCastExpression pComplexCastExpression) throws Exception {
    throw new UnsupportedOperationException(
        CComplexCastExpression.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public SLStateError visit(CFunctionDeclaration pDecl) throws Exception {
    for (CParameterDeclaration dec : pDecl.getParameters()) {
      SLStateError error = dec.accept(this);
      if (error != null) {
        return error;
      }
    }
    return null;
  }

  @Override
  public SLStateError visit(CComplexTypeDeclaration pDecl) throws Exception {
    // throw new UnsupportedOperationException(
    // CComplexTypeDeclaration.class.getSimpleName() + "is not implemented yet.");
    return null;
  }

  @Override
  public SLStateError visit(CTypeDefDeclaration pDecl) throws Exception {
    return null;
  }

  @Override
  public SLStateError visit(CVariableDeclaration pDecl) throws Exception {
    curLHS = new CIdExpression(pDecl.getFileLocation(), pDecl);

    CInitializer i = pDecl.getInitializer();
    SLStateError error = i != null ? i.accept(this) : null;
    heapDelegate.handleDeclaration(pDecl);
    curLHS = null;
    return error;
  }

  @Override
  public SLStateError visit(CParameterDeclaration pDecl) throws Exception {
    return null;
  }

  @Override
  public SLStateError visit(CEnumerator pDecl) throws Exception {
    throw new UnsupportedOperationException(
        CEnumerator.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public SLStateError visit(CExpressionStatement pIastExpressionStatement) throws Exception {
    return pIastExpressionStatement.getExpression().accept(this);
  }

  @Override
  public SLStateError visit(CExpressionAssignmentStatement pIastExpressionAssignmentStatement)
      throws Exception {
    curLHS = pIastExpressionAssignmentStatement.getLeftHandSide();
    curRHS = pIastExpressionAssignmentStatement.getRightHandSide();
    SLStateError error = curRHS.accept(this);
    if(error == null) {
      error = curLHS.accept(this);
    }
    curLHS = null;
    curRHS = null;
    return error;
  }

  @Override
  public SLStateError visit(CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement)
      throws Exception {
    curLHS = pIastFunctionCallAssignmentStatement.getLeftHandSide();
    curRHS = pIastFunctionCallAssignmentStatement.getRightHandSide();
    SLStateError error = curRHS.accept(this);
    if (error == null) {
      error = curLHS.accept(this);
    }
    curLHS = null;
    curRHS = null;
    return error;
  }

  @Override
  public SLStateError visit(CFunctionCallStatement pIastFunctionCallStatement) throws Exception {
    return pIastFunctionCallStatement.getFunctionCallExpression().accept(this);
  }

  @Override
  public SLStateError visit(CReturnStatement pNode) throws Exception {
    throw new UnsupportedOperationException(
        CReturnStatement.class.getSimpleName() + "is not implemented yet.");
  }
}

