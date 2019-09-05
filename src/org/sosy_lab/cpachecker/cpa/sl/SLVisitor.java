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
import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.sl.SLState.SLStateError;
import org.sosy_lab.java_smt.api.Formula;

/**
 * Keeps the separation logic heap up-to-date.
 */
public class SLVisitor implements CAstNodeVisitor<SLStateError, Exception> {

  private final SLMemoryDelegate memDelegate;
  private final SLSolverDelegate solDelegate;
  private CLeftHandSide curLHS;
  private CRightHandSide curRHS;
  private final Set<CSimpleDeclaration> inScopePtrs = new HashSet<>();

  public SLVisitor(SLSolverDelegate pSolDelegate, SLMemoryDelegate pMemDelegate) {
    solDelegate = pSolDelegate;
    memDelegate = pMemDelegate;
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
    BigInteger length;
    Formula loc;

    switch (SLHeapFunction.get(fctExp.getName())) {
      case MALLOC:
        if (curLHS == null) {
          return SLStateError.UNFREED_MEMORY;
        }
        loc = solDelegate.getFormulaForExpression(curLHS, true);
        length = solDelegate.getValueForCExpression(params.get(0));
        memDelegate.handleMalloc(loc, length);
        break;

      case CALLOC:
        if (curLHS == null) {
          return SLStateError.UNFREED_MEMORY;
        }
        loc = solDelegate.getFormulaForExpression(curLHS, true);
        length = solDelegate.getValueForCExpression(params.get(0));
        final BigInteger size = solDelegate.getValueForCExpression(params.get(1));
        memDelegate.handleCalloc(loc, length, size);
        break;

      case REALLOC:
        if (curLHS == null) {
          return SLStateError.UNFREED_MEMORY;
        }
        loc = solDelegate.getFormulaForExpression(curLHS, true);
        final Formula oldLoc = solDelegate.getFormulaForExpression(params.get(0), false);
        length = solDelegate.getValueForCExpression(params.get(1));
        return memDelegate.handleRealloc(loc, oldLoc, length) ? null : SLStateError.INVALID_DEREF;

      case FREE:
        loc = solDelegate.getFormulaForExpression(params.get(0), false);
        return memDelegate.handleFree(solDelegate, loc) ? null : SLStateError.INVALID_DEREF;

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
    throw new UnsupportedOperationException(
        CStringLiteralExpression.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public SLStateError visit(CTypeIdExpression pIastTypeIdExpression) throws Exception {
    throw new UnsupportedOperationException(
        CTypeIdExpression.class.getSimpleName() + "is not implemented yet.");
  }

  @Override
  public SLStateError visit(CUnaryExpression pIastUnaryExpression) throws Exception {
    CExpression operand = pIastUnaryExpression.getOperand();
    // switch (pIastUnaryExpression.getOperator()) {
    // case AMPER:
    // Formula f = solDelegate.getFormulaForExpression(curLHS, true);
    // memDelegate.handleAddressOf(f, operand.getExpressionType());
    // break;
    // default:
    // break;
    // }
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
    Formula loc = solDelegate.getFormulaForExpression(arrayExp, false);
    Formula offset = solDelegate.getFormulaForExpression(subscriptExp, false);
    Formula val = null;
    if (curLHS == pIastArraySubscriptExpression) {
      val = solDelegate.getFormulaForExpression((CExpression) curRHS, false);
    }
    // Formula heapLoc = memDelegate.checkHeapAllocation(solDelegate, loc, offset, val);
    // if (heapLoc == null) {
    // CExpression e =
    // new CUnaryExpression(
    // FileLocation.DUMMY,
    // arrayExp.getExpressionType(),
    // arrayExp,
    // UnaryOperator.AMPER);
    // loc = solDelegate.getFormulaForExpression(e, false);
    // return memDelegate.checkStackAllocation(solDelegate, loc, offset, val) == null
    // ? SLStateErrors.INVALID_DEREF
    // : null;
    // }
    // return null;
    return memDelegate.checkAllocation(solDelegate, loc, offset, val) == null
        ? SLStateError.INVALID_DEREF
        : null;
  }

  @Override
  public SLStateError visit(CFieldReference pIastFieldReference) throws Exception {
    if (pIastFieldReference.isPointerDereference()) {
      CExpression e = pIastFieldReference.getFieldOwner();
      Formula loc = solDelegate.getFormulaForExpression(e, false);
      return memDelegate.checkAllocation(solDelegate, loc, null, null) == null
          ? SLStateError.INVALID_DEREF
          : null;
    }
    return null;
  }

  @Override
  public SLStateError visit(CIdExpression pIastIdExpression) throws Exception {
    // if (curLHS == pIastIdExpression) {
    // Formula fCurrent = solDelegate.getFormulaForExpression(pIastIdExpression, true, false);
    // Formula fNew = solDelegate.getFormulaForExpression(pIastIdExpression, true, true);
    // Formula loc = memDelegate.checkStackAllocation(solDelegate, fCurrent);
    // if (loc != null) {
    //
    // memDelegate.removeFromStack(loc);
    // memDelegate.addToStack(fNew, BigInteger.ONE, pIastIdExpression.getExpressionType(), true);
    // }
    //
    // }
    return null;
  }

  @Override
  public SLStateError visit(CPointerExpression pPointerExpression) throws Exception {
    CExpression operand = pPointerExpression.getOperand();
    SLStateError error = operand.accept(this);
    if (error != null) {
      return error;
    }
    Formula loc = solDelegate.getFormulaForExpression(operand, false);
    Formula val = null;
    if (curLHS == pPointerExpression) {
      val = solDelegate.getFormulaForExpression((CExpression) curRHS, false);
    }
    return memDelegate.checkAllocation(solDelegate, loc, null, val) == null
        ? SLStateError.INVALID_DEREF
        : null;
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
    CType type = pDecl.getType();
    if (type instanceof CArrayType || type instanceof CPointerType) {
      inScopePtrs.add(pDecl);

      if (type instanceof CArrayType) {
        CArrayType aType = (CArrayType) type;
        type = aType.asPointerType();
        OptionalInt s = aType.getLengthAsInt();
        BigInteger size =
            s.isPresent()
                ? BigInteger.valueOf(s.getAsInt())
                : solDelegate.getValueForCExpression(aType.getLength());
        Formula fArray = solDelegate.getFormulaForExpression(curLHS, true);
        memDelegate.addToStack(fArray, size, aType.getType(), true);
      }
    }

    CExpression e = SLMemoryDelegate.createSymbolicMemLoc(pDecl);
    Formula f = solDelegate.getFormulaForExpression(e, false);
    memDelegate.addToStack(f, BigInteger.ONE, type, true);

    CInitializer i = pDecl.getInitializer();
    SLStateError error = i != null ? i.accept(this) : null;
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
    SLStateError error = curLHS.accept(this);
    if(error == null) {
      error = curRHS.accept(this);
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
    SLStateError error = curLHS.accept(this);
    if (error == null) {
      error = curRHS.accept(this);
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

  public Set<CSimpleDeclaration> getInScopePtrs() {
    return inScopePtrs;
  }

  public void removePtr(CSimpleDeclaration pPtr) {
    inScopePtrs.remove(pPtr);
  }
}

