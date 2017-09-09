/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cpa.interval.NumberInterface;
import org.sosy_lab.cpachecker.cpa.interval.UnifyAnalysisState;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;


public class SignCExpressionVisitor
  extends DefaultCExpressionVisitor<NumberInterface, UnrecognizedCodeException>
  implements CRightHandSideVisitor<NumberInterface, UnrecognizedCodeException> {

  private CFAEdge edgeOfExpr;

  private UnifyAnalysisState state;

  private SignTransferRelation transferRel;

  public SignCExpressionVisitor(CFAEdge pEdgeOfExpr, UnifyAnalysisState pState, SignTransferRelation pTransferRel) {
    edgeOfExpr = pEdgeOfExpr;
    state = pState;
    transferRel = pTransferRel;
  }

  @Override
  public NumberInterface visit(CFunctionCallExpression pIastFunctionCallExpression) throws UnrecognizedCodeException {
    // TODO possibly treat typedef types differently
    // e.g. x = non_det() where non_det is extern, unknown function allways assume returns any value
    if (pIastFunctionCallExpression.getExpressionType() instanceof CSimpleType
        || pIastFunctionCallExpression.getExpressionType() instanceof CTypedefType
        || pIastFunctionCallExpression.getExpressionType() instanceof CPointerType) { return new CreatorSIGN().factoryMethod(7); }
    throw new UnrecognizedCodeException("unsupported code found", edgeOfExpr);
  }

  @Override
  protected NumberInterface visitDefault(CExpression pExp) throws UnrecognizedCodeException {
    throw new UnrecognizedCodeException("unsupported code found", edgeOfExpr);
  }

  @Override
  public NumberInterface visit(CCastExpression e) throws UnrecognizedCodeException {
    return e.getOperand().accept(this); // TODO correct?
  }

  @Override
  public NumberInterface visit(CFieldReference e) throws UnrecognizedCodeException {
    return state.getSignForVariable(transferRel.getScopedVariableName(e));
  }

  @Override
  public NumberInterface visit(CArraySubscriptExpression e) throws UnrecognizedCodeException {
    // TODO possibly may become preciser
    return new CreatorSIGN().factoryMethod(7);
  }

  @Override
  public NumberInterface visit(CPointerExpression e) throws UnrecognizedCodeException {
    // TODO possibly may become preciser
    return new CreatorSIGN().factoryMethod(7);
  }

  @Override
  public NumberInterface visit(CIdExpression pIastIdExpression) throws UnrecognizedCodeException {
    return state.getSignForVariable(transferRel.getScopedVariableName(pIastIdExpression));
  }

  @Override
  public NumberInterface visit(CBinaryExpression pIastBinaryExpression) throws UnrecognizedCodeException {
      NumberInterface left = pIastBinaryExpression.getOperand1().accept(this);
      NumberInterface right = pIastBinaryExpression.getOperand2().accept(this);
    Set<NumberInterface> leftAtomSigns = left.split();
    Set<NumberInterface> rightAtomSigns = right.split();
    NumberInterface result = new CreatorSIGN().factoryMethod(0);
    for (List<NumberInterface> signCombi : Sets.cartesianProduct(ImmutableList.of(leftAtomSigns, rightAtomSigns))) {
      result = result.combineWith(evaluateExpression(signCombi.get(0), pIastBinaryExpression, signCombi.get(1)));
    }
    return result;
  }

  private NumberInterface evaluateExpression(NumberInterface pLeft, CBinaryExpression pExp, NumberInterface pRight) throws UnsupportedCCodeException {
    NumberInterface result = new CreatorSIGN().factoryMethod(0);
    switch (pExp.getOperator()) {
    case PLUS:
      result = evaluatePlusOperator(pLeft, pExp.getOperand1(), pRight, pExp.getOperand2());
      break;
    case MINUS:
      result = evaluateMinusOperator(pLeft, pRight, pExp.getOperand2());
      break;
    case MULTIPLY:
      result = pLeft.evaluateMulOperator(pRight);
      break;
    case DIVIDE:
        if (pRight.getNumber().equals(4)){// == SIGN.ZERO) {
            transferRel.logger.log(Level.WARNING, "Possibly dividing by zero", edgeOfExpr);
            return new CreatorSIGN().factoryMethod(7);//SIGN.ALL;
          }
      result = pLeft.evaluateDivideOperator(pRight);
      break;
    case MODULO:
      result = pLeft.evaluateModuloOperator(pRight);
      break;
    case BINARY_AND:
      result = pLeft.evaluateAndOperator(pRight);
      break;
    case LESS_EQUAL:
      result = pLeft.evaluateLessEqualOperator(pRight);
      break;
    case GREATER_EQUAL:
      result = pRight.evaluateLessEqualOperator(pLeft);
      break;
    case LESS_THAN:
      result = pLeft.evaluateLessOperator(pRight);
      break;
    case GREATER_THAN:
      result = pRight.evaluateLessOperator(pLeft);
      break;
    case EQUALS:
      result = pLeft.evaluateEqualOperator(pRight);
      break;
    default:
      throw new UnsupportedCCodeException(
          "Not supported", edgeOfExpr);
    }
    return result;
  }


  @Override
  public NumberInterface visit(CFloatLiteralExpression pIastFloatLiteralExpression) throws UnrecognizedCodeException {
    BigDecimal value = pIastFloatLiteralExpression.getValue();
    int cResult = value.compareTo(BigDecimal.ZERO);
    if (cResult == 1) {
      return new CreatorSIGN().factoryMethod(1);//SIGN.PLUS;
    } else if (cResult == -1) {
      return new CreatorSIGN().factoryMethod(2);//SIGN.MINUS;
    }
    return new CreatorSIGN().factoryMethod(4);//SIGN.ZERO;
  }

  @Override
  public NumberInterface visit(CIntegerLiteralExpression pIastIntegerLiteralExpression) throws UnrecognizedCodeException {
    BigInteger value = pIastIntegerLiteralExpression.getValue();
    int cResult = value.compareTo(BigInteger.ZERO);
    if (cResult == 1) {
      return new CreatorSIGN().factoryMethod(1);//SIGN.PLUS;
    } else if (cResult == -1) {
      return new CreatorSIGN().factoryMethod(2);//SIGN.MINUS;
    }
    return new CreatorSIGN().factoryMethod(4);//SIGN.ZERO;
  }

  @Override
  public NumberInterface visit(CStringLiteralExpression e) throws UnrecognizedCodeException {
    return new CreatorSIGN().factoryMethod(7);//SIGN.ALL;
  }

  @Override
  public NumberInterface visit(CCharLiteralExpression e) throws UnrecognizedCodeException {
    return new CreatorSIGN().factoryMethod(7);//SIGN.ALL;
  }

  @Override
  public NumberInterface visit(CUnaryExpression pIastUnaryExpression) throws UnrecognizedCodeException {
    switch (pIastUnaryExpression.getOperator()) {
    case MINUS:
      NumberInterface result = new CreatorSIGN().factoryMethod(0);//SIGN.EMPTY;
      NumberInterface operandSign = pIastUnaryExpression.getOperand().accept(this);
      for (NumberInterface atomSign : operandSign.split()) {
        result = result.combineWith(evaluateUnaryExpression(pIastUnaryExpression.getOperator(), atomSign));
      }
      return result;
    default:
      throw new UnsupportedCCodeException(
          "Not supported", edgeOfExpr,
          pIastUnaryExpression);
    }
  }

  private static NumberInterface evaluateUnaryExpression(UnaryOperator operator, NumberInterface operand) {
    if (operand.getNumber().equals(4)){// == SIGN.ZERO) {
      return new CreatorSIGN().factoryMethod(4);//SIGN.ZERO;
    }
    if (operator == UnaryOperator.MINUS && operand.getNumber().equals(1)){// == SIGN.PLUS) {
      return new CreatorSIGN().factoryMethod(2);//SIGN.MINUS;
    }
    return new CreatorSIGN().factoryMethod(1);//SIGN.MINUS;
  }

  private NumberInterface evaluatePlusOperator(NumberInterface pLeft, CExpression pLeftExp, NumberInterface pRight, CExpression pRightExp) {
    // Special case: - + 1 => -0, 1 + - => -0
    if ((pLeft.getNumber().equals(2)// == SIGN.MINUS
            && (pRightExp instanceof CIntegerLiteralExpression)
            && ((CIntegerLiteralExpression) pRightExp).getValue().equals(BigInteger.ONE))
        || ((pLeftExp instanceof CIntegerLiteralExpression)
            && ((CIntegerLiteralExpression) pLeftExp).getValue().equals(BigInteger.ONE)
            && pRight.getNumber().equals(2))){// == SIGN.MINUS)) {
      return new CreatorSIGN().factoryMethod(6);//SIGN.MINUS0;
    }
    // Special case: +0 + 1 => +, 1 + +0 => +
    if ((pLeft.getNumber().equals(5)// == SIGN.PLUS0
            && (pRightExp instanceof CIntegerLiteralExpression)
            && ((CIntegerLiteralExpression) pRightExp).getValue().equals(BigInteger.ONE))
        || ((pLeftExp instanceof CIntegerLiteralExpression)
            && ((CIntegerLiteralExpression) pLeftExp).getValue().equals(BigInteger.ONE)
            && pRight.getNumber().equals(5))){// == SIGN.PLUS0)) {
      return new CreatorSIGN().factoryMethod(1);//SIGN.PLUS;
    }
    NumberInterface leftToRightResult = pLeft.evaluateNonCommutativePlusOperator(pRight);
    NumberInterface rightToLeftResult = pRight.evaluateNonCommutativePlusOperator(pLeft);
    return leftToRightResult.combineWith(rightToLeftResult);
  }

//  private SIGN evaluateNonCommutativePlusOperator(SIGN pLeft, SIGN pRight) {
//    if (pRight == SIGN.ZERO) {
//      return pLeft;
//    }
//    if (pLeft == SIGN.PLUS && pRight == SIGN.MINUS) {
//      return SIGN.ALL;
//    }
//    if (pLeft == SIGN.MINUS && pRight == SIGN.MINUS) {
//      return SIGN.MINUS;
//    }
//    if (pLeft == SIGN.PLUS && pRight == SIGN.PLUS) {
//      return SIGN.PLUS;
//    }
//    return SIGN.EMPTY;
//  }

  private NumberInterface evaluateMinusOperator(NumberInterface pLeft, NumberInterface pRight, CExpression pRightExp) {
    // Special case: + - 1 => +0
    if (pLeft.getNumber().equals(1)// == SIGN.PLUS
            && (pRightExp instanceof CIntegerLiteralExpression) && ((CIntegerLiteralExpression)pRightExp).getValue().equals(BigInteger.ONE)) {
      return new CreatorSIGN().factoryMethod(5);//SIGN.PLUS0;
    }
    // Special case: -0 - 1 => -
    if (pLeft.getNumber().equals(6)// == SIGN.MINUS0
            && (pRightExp instanceof CIntegerLiteralExpression) && ((CIntegerLiteralExpression)pRightExp).getValue().equals(BigInteger.ONE)) {
      return new CreatorSIGN().factoryMethod(2);//SIGN.MINUS;
    }
    if (pRight.getNumber().equals(4)){// == SIGN.ZERO) {
      return pLeft;
    }
    if(pLeft.getNumber().equals(4)){// == SIGN.ZERO) {
      switch(pRight.getNumber().intValue()) {
      case 1://PLUS
          return new CreatorSIGN().factoryMethod(2);//SIGN.MINUS;
        case 2://MINUS
          return new CreatorSIGN().factoryMethod(1);//SIGN.PLUS;
        case 5://PLUS0
          return new CreatorSIGN().factoryMethod(6);//SIGN.MINUS0;
        case 6://MINUS0
          return new CreatorSIGN().factoryMethod(5);//SIGN.PLUS0;
      default:
        return pRight;
      }
    }
//    if (pLeft == SIGN.PLUS && pRight == SIGN.MINUS) {
//      return SIGN.PLUS;
//    }
    if(pLeft.getNumber().equals(1) && pRight.getNumber().equals(2)){
        return new CreatorSIGN().factoryMethod(1);
    }
//    if (pLeft == SIGN.MINUS && pRight == SIGN.PLUS) {
//      return SIGN.MINUS;
//    }
    if(pLeft.getNumber().equals(2) && pRight.getNumber().equals(1)){
        return new CreatorSIGN().factoryMethod(2);
    }
    return new CreatorSIGN().factoryMethod(7);//SIGN.ALL;
  }

//  private SIGN evaluateMulOperator(SIGN pLeft, SIGN pRight) {
//    SIGN leftToRightResult = evaluateNonCommutativeMulOperator(pLeft, pRight);
//    SIGN rightToLeftResult = evaluateNonCommutativeMulOperator(pRight, pLeft);
//    return leftToRightResult.combineWith(rightToLeftResult);
//  }
//
//  private SIGN evaluateNonCommutativeMulOperator(SIGN left, SIGN right) {
//    if (right == SIGN.ZERO) {
//      return SIGN.ZERO;
//    }
//    if (left == SIGN.PLUS && right == SIGN.MINUS) {
//      return SIGN.MINUS;
//    }
//    if ((left == SIGN.PLUS && right == SIGN.PLUS) || (left == SIGN.MINUS && right == SIGN.MINUS)) {
//      return SIGN.PLUS;
//    }
//    return SIGN.EMPTY;
//  }
//
//  private SIGN evaluateDivideOperator(SIGN left, SIGN right) {
//    if (right == SIGN.ZERO) {
//      transferRel.logger.log(Level.WARNING, "Possibly dividing by zero", edgeOfExpr);
//      return SIGN.ALL;
//    }
//    return evaluateMulOperator(left, right);
//  }
//
//  private SIGN evaluateModuloOperator(SIGN pLeft, SIGN pRight) {
//    if (pLeft == SIGN.ZERO) {
//      return SIGN.ZERO;
//    }
//    if (pLeft == SIGN.PLUS && (pRight == SIGN.PLUS || pRight == SIGN.MINUS)) {
//      return SIGN.PLUS0;
//    }
//    if (pLeft == SIGN.MINUS && (pRight == SIGN.MINUS || pRight == SIGN.PLUS)) {
//      return SIGN.MINUS0;
//    }
//    return SIGN.ALL;
//  }
//
//
//  // assumes that indicator bit for negative numbers is 1
//  private SIGN evaluateAndOperator(SIGN left, SIGN right) {
//    if (left == SIGN.ZERO || right == SIGN.ZERO) {
//      return SIGN.ZERO;
//    }
//    if (left == SIGN.PLUS || right == SIGN.PLUS) {
//      return SIGN.PLUS0;
//    }
//    if (left == SIGN.MINUS && right == SIGN.MINUS) {
//      return SIGN.MINUS0;
//    }
//    return SIGN.EMPTY;
//  }
//
//  private SIGN evaluateLessOperator(SIGN pLeft, SIGN pRight) {
//    if (pLeft == SIGN.EMPTY || pRight == SIGN.EMPTY) { return SIGN.EMPTY; }
//    switch (pLeft) {
//      case PLUS:
//        if (SIGN.MINUS0.covers(pRight)) {
//          return SIGN.ZERO;
//        }
//        break;
//      case MINUS:
//        if (SIGN.PLUS0.covers(pRight)) {
//          return SIGN.ZERO;
//        }
//        break;
//      case ZERO:
//        if (SIGN.MINUS0.covers(pRight)) {
//          return SIGN.ZERO;
//        }
//        if(pRight == SIGN.ZERO) {
//          return SIGN.PLUSMINUS;
//        }
//        break;
//      case PLUS0:
//        if(pRight == SIGN.MINUS) {
//          return SIGN.ZERO;
//        }
//        if(pRight == SIGN.ZERO) {
//          return SIGN.PLUSMINUS;
//        }
//        break;
//      case MINUS0:
//        if(pRight == SIGN.PLUS) {
//          return SIGN.PLUSMINUS;
//        }
//        break;
//      default:
//        break;
//    }
//    return SIGN.ALL;
//  }
//
//  private SIGN evaluateLessEqualOperator(SIGN pLeft, SIGN pRight) {
//    if (pLeft == SIGN.EMPTY || pRight == SIGN.EMPTY) { return SIGN.EMPTY; }
//    switch (pLeft) {
//      case PLUS:
//        if (SIGN.MINUS0.covers(pRight)) {
//          return SIGN.ZERO;
//        }
//        break;
//      case MINUS:
//        if (SIGN.PLUS0.covers(pRight)) {
//          return SIGN.ZERO;
//        }
//        break;
//      case ZERO:
//        if (SIGN.PLUS0.covers(pRight)) {
//          return SIGN.PLUSMINUS;
//        }
//        if(pRight == SIGN.MINUS) {
//          return SIGN.ZERO;
//        }
//        break;
//      case PLUS0:
//        if(pRight == SIGN.MINUS) {
//          return SIGN.ZERO;
//        }
//        break;
//      case MINUS0:
//        if(pRight == SIGN.PLUS) {
//          return SIGN.PLUSMINUS;
//        }
//        break;
//      default:
//        break;
//    }
//    return SIGN.ALL;
//  }
//
//  private SIGN evaluateEqualOperator(SIGN pLeft, SIGN pRight) {
//    if(pLeft==SIGN.EMPTY || pRight == SIGN.EMPTY) {
//      return SIGN.EMPTY;
//    }
//    if(pLeft==SIGN.ZERO && pRight == SIGN.ZERO) {
//      return SIGN.PLUSMINUS;
//    }
//    return SIGN.ALL;
//  }
}
