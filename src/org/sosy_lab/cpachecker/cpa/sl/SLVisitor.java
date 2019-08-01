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

import java.math.BigInteger;
import java.util.List;
import java.util.OptionalInt;
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
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.java_smt.api.Formula;

/**
 * Keeps the separation logic heap up-to-date.
 */
public class SLVisitor implements CAstNodeVisitor<Boolean, Exception> {

  private final SLMemoryDelegate memDelegate;
  private final SLSolverDelegate solDelegate;
  private CLeftHandSide curLHS;
  private CRightHandSide curRHS;

  public SLVisitor(SLSolverDelegate pSolDelegate, SLMemoryDelegate pMemDelegate) {
    solDelegate = pSolDelegate;
    memDelegate = pMemDelegate;
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
    CIdExpression fctExp = (CIdExpression) pIastFunctionCallExpression.getFunctionNameExpression();
    final List<CExpression> params = pIastFunctionCallExpression.getParameterExpressions();
    BigInteger length;
    Formula loc;

    switch (SLHeapFunction.get(fctExp.getName())) {
      case MALLOC:
        if (curLHS == null) {
          return true;
        }
        loc = solDelegate.getFormulaForExpression(curLHS);
        length = solDelegate.getValueForCExpression(params.get(0));
        memDelegate.handleMalloc(loc, length);
        break;

      case CALLOC:
        if (curLHS == null) {
          return true;
        }
        loc = solDelegate.getFormulaForExpression(curLHS);
        length = solDelegate.getValueForCExpression(params.get(0));
        final BigInteger size = solDelegate.getValueForCExpression(params.get(1));
        memDelegate.handleCalloc(loc, length, size);
        break;

      case REALLOC:
        if (curLHS == null) {
          return true;
        }
        loc = solDelegate.getFormulaForExpression(curLHS);
        final Formula oldLoc = solDelegate.getFormulaForExpression(params.get(0));
        length = solDelegate.getValueForCExpression(params.get(1));
        return !memDelegate.handleRealloc(loc, oldLoc, length);

      case FREE:
        loc = solDelegate.getFormulaForExpression(params.get(0));
        return !memDelegate.handleFree(solDelegate, loc);

      default:
        break;
    }
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
    // switch (pIastUnaryExpression.getOperator()) {
    // case AMPER:
    // String varName = ((CIdExpression) curLHS).getName();
    // CPointerType type = (CPointerType) curLHS.getExpressionType();
    // delegate.handleAddressOf(varName, type.getType());
    // break;
    // default:
    // break;
    // }

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
    CExpression subscriptExp = pIastArraySubscriptExpression.getSubscriptExpression();
    CExpression arrayExp = pIastArraySubscriptExpression.getArrayExpression();
    if (subscriptExp.accept(this)) {
      return true;
    }
    Formula loc = solDelegate.getFormulaForExpression(arrayExp);
    Formula offset = solDelegate.getFormulaForExpression(subscriptExp);
    return memDelegate.checkAllocation(solDelegate, loc, offset, null) == null;
  }

  @Override
  public Boolean visit(CFieldReference pIastFieldReference) throws Exception {
    throw new UnsupportedOperationException(
        CFieldReference.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public Boolean visit(CIdExpression pIastIdExpression) throws Exception {
    return false;
  }

  @Override
  public Boolean visit(CPointerExpression pPointerExpression) throws Exception {
    CExpression operand = pPointerExpression.getOperand();
    if (curLHS == pPointerExpression) {
      Formula loc = solDelegate.getFormulaForExpression(operand);
      Formula val = solDelegate.getFormulaForExpression((CExpression) curRHS);
      if (memDelegate.checkAllocation(solDelegate, loc, null, val) == null) {
        return true;
      }
    }
    return operand.accept(this);
  }

  @Override
  public Boolean visit(CComplexCastExpression pComplexCastExpression) throws Exception {
    throw new UnsupportedOperationException(
        CComplexCastExpression.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public Boolean visit(CFunctionDeclaration pDecl) throws Exception {
    for (CParameterDeclaration dec : pDecl.getParameters()) {
      if (dec.accept(this)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Boolean visit(CComplexTypeDeclaration pDecl) throws Exception {
    throw new UnsupportedOperationException(
        CComplexTypeDeclaration.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public Boolean visit(CTypeDefDeclaration pDecl) throws Exception {
    return false;
  }

  @Override
  public Boolean visit(CVariableDeclaration pDecl) throws Exception {
    BigInteger size;
    CType type;
    if (pDecl.getType() instanceof CArrayType) {
      CArrayType arrayType = (CArrayType) pDecl.getType();
      type = arrayType.getType();
      OptionalInt s = arrayType.getLengthAsInt();
      size =
          s.isPresent()
              ? BigInteger.valueOf(s.getAsInt())
              : solDelegate.getValueForCExpression(arrayType.getLength());
    } else {
      type = pDecl.getType();
      size = BigInteger.ONE;
    }
    Formula f = solDelegate.getFormulaForVariableName(pDecl.getName(), pDecl.isGlobal(), false);
    memDelegate.addToStack(f, size, type, true);
    CInitializer i = pDecl.getInitializer();
    return i != null ? i.accept(this) : false;
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
    curLHS = pIastExpressionAssignmentStatement.getLeftHandSide();
    curRHS = pIastExpressionAssignmentStatement.getRightHandSide();
    final boolean isTarget = curLHS.accept(this) || curRHS.accept(this);
    curLHS = null;
    curRHS = null;
    return isTarget;
    // checkPtrAssignment(curLHS, curRHS);
    // return leftIsTarget || rightIsTarget;
  }

  @Override
  public Boolean visit(CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement)
      throws Exception {
    curLHS = pIastFunctionCallAssignmentStatement.getLeftHandSide();
    curRHS = pIastFunctionCallAssignmentStatement.getRightHandSide();
    final boolean isTarget = curLHS.accept(this) || curRHS.accept(this);
    curLHS = null;
    curRHS = null;
    return isTarget;
  }

  @Override
  public Boolean visit(CFunctionCallStatement pIastFunctionCallStatement) throws Exception {
    return pIastFunctionCallStatement.getFunctionCallExpression().accept(this);
  }

  @Override
  public Boolean visit(CReturnStatement pNode) throws Exception {
    throw new UnsupportedOperationException(
        CReturnStatement.class.getSimpleName() + "is not implemented yet.");
  }
}

