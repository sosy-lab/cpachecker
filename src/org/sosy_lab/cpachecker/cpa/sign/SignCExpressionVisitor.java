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
import java.util.Map;

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
import org.sosy_lab.cpachecker.cpa.sign.SignState.SIGN;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;

import com.google.common.base.Optional;


public class SignCExpressionVisitor
  extends DefaultCExpressionVisitor<Optional<SIGN>, UnrecognizedCodeException>
  implements CRightHandSideVisitor<Optional<SIGN>, UnrecognizedCodeException> {

  private CFAEdge edgeOfExpr;

  private Map<String, SIGN> signMap;

  public SignCExpressionVisitor(CFAEdge pEdgeOfExpr, Map<String, SIGN> pSignMap) {
    edgeOfExpr = pEdgeOfExpr;
    signMap = pSignMap;
  }

  @Override
  public Optional<SIGN> visit(CFunctionCallExpression pIastFunctionCallExpression) throws UnrecognizedCodeException {
    return null;
  }

  @Override
  protected Optional<SIGN> visitDefault(CExpression pExp) throws UnrecognizedCodeException {
    throw new UnrecognizedCodeException("unsupported code found", edgeOfExpr);
  }

  @Override
  public Optional<SIGN> visit(CIdExpression pIastIdExpression) throws UnrecognizedCodeException {
    return Optional.fromNullable(signMap.get(pIastIdExpression.getName()));
  }

  @Override
  public Optional<SIGN> visit(CBinaryExpression pIastBinaryExpression) throws UnrecognizedCodeException {
    Optional<SIGN> left = pIastBinaryExpression.getOperand1().accept(this);
    Optional<SIGN> right = pIastBinaryExpression.getOperand2().accept(this);
    switch(pIastBinaryExpression.getOperator()) {
    case PLUS:
    case MINUS:
    case MULTIPLY:
    case DIVIDE:
      return evaluateBinaryExpr(left, pIastBinaryExpression.getOperator(), right);
    default:
      throw new UnsupportedCCodeException(
          "Not supported", edgeOfExpr,
          pIastBinaryExpression);
    }
  }

  @Override
  public Optional<SIGN> visit(CFloatLiteralExpression pIastFloatLiteralExpression) throws UnrecognizedCodeException {
    BigDecimal value = pIastFloatLiteralExpression.getValue();
    int cResult = value.compareTo(BigDecimal.ZERO);
    if(cResult == 1) {
      return Optional.of(SIGN.PLUS);
    } else if (cResult == -1) {
      return Optional.of(SIGN.MINUS);
    }
    return Optional.of(SIGN.ZERO);
  }

  @Override
  public Optional<SIGN> visit(CIntegerLiteralExpression pIastIntegerLiteralExpression) throws UnrecognizedCodeException {
    BigInteger value = pIastIntegerLiteralExpression.getValue();
    int cResult = value.compareTo(BigInteger.ZERO);
    if(cResult == 1) {
      return Optional.of(SIGN.PLUS);
    } else if (cResult == -1) {
      return Optional.of(SIGN.MINUS);
    }
    return Optional.of(SIGN.ZERO);
  }

  @Override
  public Optional<SIGN> visit(CUnaryExpression pIastUnaryExpression) throws UnrecognizedCodeException {
    Optional<SIGN> operandSign = pIastUnaryExpression.getOperand().accept(this);
    switch(pIastUnaryExpression.getOperator()) {
    case PLUS:
    case MINUS:
      return evaluateUnaryExpr(pIastUnaryExpression.getOperator(), operandSign);
    default:
      throw new UnsupportedCCodeException(
          "Not supported", edgeOfExpr,
          pIastUnaryExpression);
    }
  }

  private static Optional<SIGN> evaluateUnaryExpr(UnaryOperator operator, Optional<SIGN> operand) {
    if(!operand.isPresent()) {
      return Optional.absent();
    }
    if(operator == UnaryOperator.PLUS && operand.get() == SIGN.MINUS
        || operator == UnaryOperator.MINUS && operand.get() == SIGN.PLUS) {
      return Optional.of(SIGN.MINUS);
    }
    return Optional.of(SIGN.PLUS);
  }

  private static Optional<SIGN> evaluateBinaryExpr(Optional<SIGN> left, BinaryOperator operator, Optional<SIGN> right) {
    boolean multOrDiv = operator == BinaryOperator.MULTIPLY || operator == BinaryOperator.DIVIDE;
    /*
     *  0 * x => 0
     *  x * 0 => 0
     *  0 / x => 0
     *  x / 0 => NOT DEFINED
     *  0 _ 0 => 0
     */
    if(multOrDiv && left.isPresent() && left.get() == SIGN.ZERO
        || operator == BinaryOperator.MULTIPLY && right.isPresent() && right.get() == SIGN.ZERO
        || right.isPresent() && right.get() == SIGN.ZERO && left.isPresent() && left.get() == SIGN.ZERO) {
        return Optional.of(SIGN.ZERO);
    }

    /*
     * ? _ ? => ?
     * + + - => ?
     * - + + => ?
     * - - - => ?
     * + - + => ?
     */
    if((!left.isPresent() || !right.isPresent())
        || operator == BinaryOperator.PLUS && left.get() == SIGN.PLUS && right.get() == SIGN.MINUS
        || operator == BinaryOperator.PLUS && left.get() == SIGN.MINUS && right.get() == SIGN.PLUS
        || operator == BinaryOperator.MINUS && left.get() == SIGN.MINUS && right.get() == SIGN.MINUS
        || operator == BinaryOperator.MINUS && left.get() == SIGN.PLUS && right.get() == SIGN.PLUS) {
      return Optional.absent();
    }

    /*
     * - + - => -
     * - - + => -
     * - * + => -
     * + * - => -
     * - / + => -
     * + / - => -
     */
    if(operator == BinaryOperator.PLUS && left.get() == SIGN.MINUS && right.get() == SIGN.MINUS
        || operator == BinaryOperator.MINUS && left.get() == SIGN.MINUS && right.get() == SIGN.PLUS
        || multOrDiv && left.get() == SIGN.MINUS && right.get() == SIGN.PLUS
        || multOrDiv && left.get() == SIGN.PLUS && right.get() == SIGN.MINUS) {
      return Optional.of(SIGN.MINUS);
    }
    return Optional.of(SIGN.PLUS);
  }
}
