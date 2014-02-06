/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.sign;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;


public class SignCExpressionVisitor
  extends DefaultCExpressionVisitor<SIGN, UnrecognizedCodeException>
  implements CRightHandSideVisitor<SIGN, UnrecognizedCodeException> {

  private CFAEdge edgeOfExpr;

  private SignState state;

  private SignTransferRelation transferRel;

  public SignCExpressionVisitor(CFAEdge pEdgeOfExpr, SignState pState, SignTransferRelation pTransferRel) {
    edgeOfExpr = pEdgeOfExpr;
    state = pState;
    transferRel = pTransferRel;
  }

  @Override
  public SIGN visit(CFunctionCallExpression pIastFunctionCallExpression) throws UnrecognizedCodeException {
    // e.g. x = non_det() where non_det is extern, unknown function allways assume returns any value
    if(pIastFunctionCallExpression.getExpressionType() instanceof CSimpleType){
      return SIGN.ALL;
    }
    return null;
  }

  @Override
  protected SIGN visitDefault(CExpression pExp) throws UnrecognizedCodeException {
    throw new UnrecognizedCodeException("unsupported code found", edgeOfExpr);
  }

  @Override
  public SIGN visit(CCastExpression e) throws UnrecognizedCodeException {
    return e.getOperand().accept(this); // TODO correct?
  }

  @Override
  public SIGN visit(CFieldReference e) throws UnrecognizedCodeException {
    return SIGN.ALL; // TODO possibly may become preciser
  }

  @Override
  public SIGN visit(CArraySubscriptExpression e) throws UnrecognizedCodeException {
    // TODO possibly may become preciser
    return SIGN.ALL;
  }

  @Override
  public SIGN visit(CIdExpression pIastIdExpression) throws UnrecognizedCodeException {
    return state.getSignMap().getSignForVariable(transferRel.getScopedVariableName(pIastIdExpression));
  }

  @Override
  public SIGN visit(CBinaryExpression pIastBinaryExpression) throws UnrecognizedCodeException {
    SIGN left = pIastBinaryExpression.getOperand1().accept(this);
    SIGN right = pIastBinaryExpression.getOperand2().accept(this);
    Set<SIGN> leftAtomSigns = left.split();
    Set<SIGN> rightAtomSigns = right.split();
    SIGN result = SIGN.EMPTY;
    for(List<SIGN> signCombi : Sets.cartesianProduct(ImmutableList.of(leftAtomSigns, rightAtomSigns))) {
      result = result.combineWith(evaluateExpression(signCombi.get(0), pIastBinaryExpression, signCombi.get(1)));
    }
    return result;
  }

  private SIGN evaluateExpression(SIGN pLeft, CBinaryExpression pExp, SIGN pRight) throws UnsupportedCCodeException {
    SIGN result = SIGN.EMPTY;
    switch(pExp.getOperator()) {
    case PLUS:
      result = evaluatePlusOperator(pLeft, pExp.getOperand1(), pRight, pExp.getOperand2());
      break;
    case MINUS:
      result = evaluateMinusOperator(pLeft, pRight, pExp.getOperand2());
      break;
    case MULTIPLY:
      result = evaluateMulOperator(pLeft, pRight);
      break;
    case DIVIDE:
      result = evaluateDivideOperator(pLeft, pRight);
      break;
    case BINARY_AND:
      result = evaluateAndOperator(pLeft, pRight);
      break;
    default:
      throw new UnsupportedCCodeException(
          "Not supported", edgeOfExpr);
    }
    return result;
  }

  @Override
  public SIGN visit(CFloatLiteralExpression pIastFloatLiteralExpression) throws UnrecognizedCodeException {
    BigDecimal value = pIastFloatLiteralExpression.getValue();
    int cResult = value.compareTo(BigDecimal.ZERO);
    if(cResult == 1) {
      return SIGN.PLUS;
    } else if (cResult == -1) {
      return SIGN.MINUS;
    }
    return SIGN.ZERO;
  }

  @Override
  public SIGN visit(CIntegerLiteralExpression pIastIntegerLiteralExpression) throws UnrecognizedCodeException {
    BigInteger value = pIastIntegerLiteralExpression.getValue();
    int cResult = value.compareTo(BigInteger.ZERO);
    if(cResult == 1) {
      return SIGN.PLUS;
    } else if (cResult == -1) {
      return SIGN.MINUS;
    }
    return SIGN.ZERO;
  }

  @Override
  public SIGN visit(CStringLiteralExpression e) throws UnrecognizedCodeException {
    return SIGN.ALL;
  }

  @Override
  public SIGN visit(CUnaryExpression pIastUnaryExpression) throws UnrecognizedCodeException {
    switch(pIastUnaryExpression.getOperator()) {
    case PLUS:
    case MINUS:
      SIGN result = SIGN.EMPTY;
      SIGN operandSign = pIastUnaryExpression.getOperand().accept(this);
      for(SIGN atomSign : operandSign.split()) {
        result = result.combineWith(evaluateUnaryExpression(pIastUnaryExpression.getOperator(), atomSign));
      }
      return result;
    default:
      throw new UnsupportedCCodeException(
          "Not supported", edgeOfExpr,
          pIastUnaryExpression);
    }
  }

  private static SIGN evaluateUnaryExpression(UnaryOperator operator, SIGN operand) {
    if(operand == SIGN.ZERO) {
      return SIGN.ZERO;
    }
    if(operator == UnaryOperator.PLUS && operand == SIGN.MINUS
        || operator == UnaryOperator.MINUS && operand == SIGN.PLUS) {
      return SIGN.MINUS;
    }
    return SIGN.PLUS;
  }

  private SIGN evaluatePlusOperator(SIGN pLeft, CExpression pLeftExp, SIGN pRight, CExpression pRightExp) {
    // Special case: - + 1 => -0, 1 + - => -0
    if(pLeft == SIGN.MINUS && (pRightExp instanceof CIntegerLiteralExpression) && ((CIntegerLiteralExpression)pRightExp).getValue().equals(BigInteger.ONE)
        || (pLeftExp instanceof CIntegerLiteralExpression) && ((CIntegerLiteralExpression)pLeftExp).getValue().equals(BigInteger.ONE) && pRight == SIGN.MINUS) {
      return SIGN.MINUS0;
    }
    // Special case: +0 + 1 => +, 1 + +0 => +
    if(pLeft == SIGN.PLUS0 && (pRightExp instanceof CIntegerLiteralExpression) && ((CIntegerLiteralExpression)pRightExp).getValue().equals(BigInteger.ONE)
        || (pLeftExp instanceof CIntegerLiteralExpression) && ((CIntegerLiteralExpression)pLeftExp).getValue().equals(BigInteger.ONE) && pRight == SIGN.PLUS0) {
      return SIGN.PLUS;
    }
    SIGN leftToRightResult = evaluateNonCommutativePlusOperator(pLeft, pRight);
    SIGN rightToLeftResult = evaluateNonCommutativePlusOperator(pRight, pLeft);
    return leftToRightResult.combineWith(rightToLeftResult);
  }

  private SIGN evaluateNonCommutativePlusOperator(SIGN pLeft, SIGN pRight) {
    if(pRight == SIGN.ZERO) {
      return pLeft;
    }
    if(pLeft == SIGN.PLUS && pRight == SIGN.MINUS) {
      return SIGN.ALL;
    }
    if(pLeft == SIGN.MINUS && pRight == SIGN.MINUS) {
      return SIGN.MINUS;
    }
    if(pLeft == SIGN.PLUS && pRight == SIGN.PLUS) {
      return SIGN.PLUS;
    }
    return SIGN.EMPTY;
  }

  private SIGN evaluateMinusOperator(SIGN pLeft, SIGN pRight, CExpression pRightExp) {
    // Special case: + - 1 => +0
    if(pLeft == SIGN.PLUS && (pRightExp instanceof CIntegerLiteralExpression) && ((CIntegerLiteralExpression)pRightExp).getValue().equals(BigInteger.ONE)) {
      return SIGN.PLUS0;
    }
    // Special case: -0 - 1 => -
    if(pLeft == SIGN.MINUS0 && (pRightExp instanceof CIntegerLiteralExpression) && ((CIntegerLiteralExpression)pRightExp).getValue().equals(BigInteger.ONE)) {
      return SIGN.MINUS;
    }
    if(pRight == SIGN.ZERO) {
      return pLeft;
    }
    if(pLeft == SIGN.PLUS && pRight == SIGN.MINUS) {
      return SIGN.PLUS;
    }
    if(pLeft == SIGN.MINUS && pRight == SIGN.PLUS) {
      return SIGN.MINUS;
    }
    return SIGN.ALL;
  }

  private SIGN evaluateMulOperator(SIGN pLeft, SIGN pRight) {
    SIGN leftToRightResult = evaluateNonCommutativeMulOperator(pLeft, pRight);
    SIGN rightToLeftResult = evaluateNonCommutativeMulOperator(pRight, pLeft);
    return leftToRightResult.combineWith(rightToLeftResult);
  }

  private SIGN evaluateNonCommutativeMulOperator(SIGN left, SIGN right) {
    if(right == SIGN.ZERO) {
      return SIGN.ZERO;
    }
    if(left == SIGN.PLUS && right == SIGN.MINUS) {
      return SIGN.MINUS;
    }
    if(left == SIGN.PLUS && right == SIGN.PLUS
        || left == SIGN.MINUS && right == SIGN.MINUS) {
      return SIGN.PLUS;
    }
    return SIGN.EMPTY;
  }

  private SIGN evaluateDivideOperator(SIGN left, SIGN right) throws UnsupportedCCodeException {
    if(right == SIGN.ZERO) {
      transferRel.logger.log(Level.WARNING, "Possibly dividing by zero", edgeOfExpr);
      return SIGN.ALL;
    }
    return evaluateMulOperator(left, right);
  }


  // assumes that indicator bit for negative numbers is 1
  private SIGN evaluateAndOperator(SIGN left, SIGN right) {
    if(left == SIGN.ZERO || right == SIGN.ZERO) {
      return SIGN.ZERO;
    }
    if(left == SIGN.PLUS || right == SIGN.PLUS) {
      return SIGN.PLUS0;
    }
    if(left == SIGN.MINUS && right == SIGN.MINUS) {
      return SIGN.MINUS0;
    }
    return SIGN.EMPTY;
  }
}
