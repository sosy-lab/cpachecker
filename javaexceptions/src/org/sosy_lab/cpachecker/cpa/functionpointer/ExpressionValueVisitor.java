// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.functionpointer;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.FunctionPointerTarget;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.NamedFunctionTarget;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.UnknownTarget;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

class ExpressionValueVisitor
    extends DefaultCExpressionVisitor<FunctionPointerTarget, UnrecognizedCodeException>
    implements CRightHandSideVisitor<FunctionPointerTarget, UnrecognizedCodeException> {

  private final FunctionPointerState.Builder state;
  private final FunctionPointerTarget targetForInvalidPointers;

  ExpressionValueVisitor(
      FunctionPointerState.Builder pElement, FunctionPointerTarget pTargetForInvalidPointers) {
    state = pElement;
    targetForInvalidPointers = pTargetForInvalidPointers;
  }

  @Override
  public FunctionPointerTarget visit(CArraySubscriptExpression pE)
      throws UnrecognizedCodeException {
    if (pE.getSubscriptExpression() instanceof CIntegerLiteralExpression
        && pE.getArrayExpression() instanceof CIdExpression) {

      return state.getTarget(FunctionPointerTransferRelation.arrayElementVariable(pE));
    }
    return super.visit(pE);
  }

  @Override
  public FunctionPointerTarget visit(CUnaryExpression pE) {
    if (pE.getOperator() == UnaryOperator.AMPER && pE.getOperand() instanceof CIdExpression) {
      return extractFunctionId((CIdExpression) pE.getOperand());
    }
    return visitDefault(pE);
  }

  @Override
  public FunctionPointerTarget visit(CPointerExpression pE) {
    if (pE.getOperand() instanceof CIdExpression) {
      return extractFunctionId((CIdExpression) pE.getOperand());
    }
    return visitDefault(pE);
  }

  private FunctionPointerTarget extractFunctionId(CIdExpression operand) {
    if ((operand.getDeclaration() != null
            && operand.getDeclaration().getType() instanceof CFunctionType)
        || (operand.getExpressionType() instanceof CFunctionType)) {
      return new NamedFunctionTarget(operand.getName());
    }
    if (operand.getExpressionType() instanceof CPointerType) {
      CPointerType t = (CPointerType) operand.getExpressionType();
      if (t.getType() instanceof CFunctionType) {
        return state.getTarget(operand.getDeclaration().getQualifiedName());
      }
    }
    return visitDefault(operand);
  }

  @Override
  public FunctionPointerTarget visit(CIdExpression pE) {
    if (pE.getDeclaration() instanceof CFunctionDeclaration
        || pE.getExpressionType() instanceof CFunctionType) {
      return new NamedFunctionTarget(pE.getName());
    }

    return state.getTarget(pE.getDeclaration().getQualifiedName());
  }

  @Override
  public FunctionPointerTarget visit(CCastExpression pE) throws UnrecognizedCodeException {
    return pE.getOperand().accept(this);
  }

  @Override
  public FunctionPointerTarget visit(CComplexCastExpression pE) throws UnrecognizedCodeException {
    // evaluation of complex numbers is not supported by now
    return UnknownTarget.getInstance();
  }

  @Override
  protected FunctionPointerTarget visitDefault(CExpression pExp) {
    return UnknownTarget.getInstance();
  }

  @Override
  public FunctionPointerTarget visit(CFunctionCallExpression pIastFunctionCallExpression) {
    return UnknownTarget.getInstance();
  }

  @Override
  public FunctionPointerTarget visit(CCharLiteralExpression pE) {
    return targetForInvalidPointers;
  }

  @Override
  public FunctionPointerTarget visit(CFloatLiteralExpression pE) {
    return targetForInvalidPointers;
  }

  @Override
  public FunctionPointerTarget visit(CIntegerLiteralExpression pE) {
    return targetForInvalidPointers;
  }

  @Override
  public FunctionPointerTarget visit(CStringLiteralExpression pE) {
    return targetForInvalidPointers;
  }

  @Override
  public FunctionPointerTarget visit(CImaginaryLiteralExpression pE) {
    return targetForInvalidPointers;
  }
}
