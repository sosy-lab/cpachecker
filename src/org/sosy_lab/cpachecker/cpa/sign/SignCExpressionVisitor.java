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

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
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
    return null;
  }

  @Override
  protected SIGN visitDefault(CExpression pExp) throws UnrecognizedCodeException {
    throw new UnrecognizedCodeException("unsupported code found", edgeOfExpr);
  }

  @Override
  public SIGN visit(CIdExpression pIastIdExpression) throws UnrecognizedCodeException {
    SIGN s = state.getSignMap().getSignForVariable(transferRel.getScopedVariableName(pIastIdExpression));
    return s;
  }

  @Override
  public SIGN visit(CBinaryExpression pIastBinaryExpression) throws UnrecognizedCodeException {
    SIGN left = pIastBinaryExpression.getOperand1().accept(this);
    SIGN right = pIastBinaryExpression.getOperand2().accept(this);
    switch(pIastBinaryExpression.getOperator()) {
    case PLUS:
    case MINUS:
    case MULTIPLY:
    case DIVIDE:
      Set<SIGN> leftAtomSigns = left.split();
      Set<SIGN> rightAtomSigns = right.split();
      SIGN result = SIGN.EMPTY;
      for(List<SIGN> signCombi : Sets.cartesianProduct(ImmutableList.of(leftAtomSigns, rightAtomSigns))) {
        result = result.combineWith(evaluateBinaryExpr(signCombi.get(0), pIastBinaryExpression.getOperator(), signCombi.get(1)));
      }
      return result;
    default:
      throw new UnsupportedCCodeException(
          "Not supported", edgeOfExpr,
          pIastBinaryExpression);
    }
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
  public SIGN visit(CUnaryExpression pIastUnaryExpression) throws UnrecognizedCodeException {
    switch(pIastUnaryExpression.getOperator()) {
    case PLUS:
    case MINUS:
      SIGN result = SIGN.EMPTY;
      SIGN operandSign = pIastUnaryExpression.getOperand().accept(this);
      for(SIGN atomSign : operandSign.split()) {
        result = result.combineWith(evaluateUnaryExpr(pIastUnaryExpression.getOperator(), atomSign));
      }
      return result;
    default:
      throw new UnsupportedCCodeException(
          "Not supported", edgeOfExpr,
          pIastUnaryExpression);
    }
  }

  private static SIGN evaluateUnaryExpr(UnaryOperator operator, SIGN operand) {
    if(operator == UnaryOperator.PLUS && operand == SIGN.MINUS
        || operator == UnaryOperator.MINUS && operand == SIGN.PLUS) {
      return SIGN.MINUS;
    }
    return SIGN.PLUS;
  }

  private static SIGN evaluateBinaryExpr(SIGN left, BinaryOperator operator, SIGN right) {
    boolean multOrDiv = operator == BinaryOperator.MULTIPLY || operator == BinaryOperator.DIVIDE;
    /*
     *  0 * x => 0
     *  x * 0 => 0
     *  0 / x => 0
     *  x / 0 => NOT DEFINED
     *  0 _ 0 => 0
     */
    if(multOrDiv && left.isAll() && left == SIGN.ZERO
        || operator == BinaryOperator.MULTIPLY && right.isAll() && right == SIGN.ZERO
        || right.isAll() && right == SIGN.ZERO && left.isAll() && left == SIGN.ZERO) {
        return SIGN.ZERO;
    }

    /*
     * ? _ ? => ?
     * ? _ _ => ?
     * _ _ ? => ?
     * + + - => ?
     * - + + => ?
     * - - - => ?
     * + - + => ?
     */
    if((left.isAll() || right.isAll())
        || operator == BinaryOperator.PLUS && left == SIGN.PLUS && right == SIGN.MINUS
        || operator == BinaryOperator.PLUS && left == SIGN.MINUS && right == SIGN.PLUS
        || operator == BinaryOperator.MINUS && left == SIGN.MINUS && right == SIGN.MINUS
        || operator == BinaryOperator.MINUS && left == SIGN.PLUS && right == SIGN.PLUS) {
      return SIGN.ALL;
    }

    /*
     * - + - => -
     * - - + => -
     * - * + => -
     * + * - => -
     * - / + => -
     * + / - => -
     */
    if(operator == BinaryOperator.PLUS && left == SIGN.MINUS && right == SIGN.MINUS
        || operator == BinaryOperator.MINUS && left == SIGN.MINUS && right == SIGN.PLUS
        || multOrDiv && left == SIGN.MINUS && right == SIGN.PLUS
        || multOrDiv && left == SIGN.PLUS && right == SIGN.MINUS) {
      return SIGN.MINUS;
    }
    return SIGN.PLUS;
  }
}
