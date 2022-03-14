// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

public class SignCExpressionVisitor
    extends DefaultCExpressionVisitor<SIGN, UnrecognizedCodeException>
    implements CRightHandSideVisitor<SIGN, UnrecognizedCodeException> {

  private CFAEdge edgeOfExpr;

  private SignState state;

  private SignTransferRelation transferRel;

  public SignCExpressionVisitor(
      CFAEdge pEdgeOfExpr, SignState pState, SignTransferRelation pTransferRel) {
    edgeOfExpr = pEdgeOfExpr;
    state = pState;
    transferRel = pTransferRel;
  }

  @Override
  public SIGN visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws UnrecognizedCodeException {
    // TODO possibly treat typedef types differently
    // e.g. x = non_det() where non_det is extern, unknown function allways assume returns any value
    if (pIastFunctionCallExpression.getExpressionType() instanceof CSimpleType
        || pIastFunctionCallExpression.getExpressionType() instanceof CTypedefType
        || pIastFunctionCallExpression.getExpressionType() instanceof CPointerType) {
      return SIGN.ALL;
    }
    throw new UnrecognizedCodeException("unsupported code found", edgeOfExpr);
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
    return state.getSignForVariable(transferRel.getScopedVariableName(e));
  }

  @Override
  public SIGN visit(CArraySubscriptExpression e) throws UnrecognizedCodeException {
    // TODO possibly may become preciser
    return SIGN.ALL;
  }

  @Override
  public SIGN visit(CPointerExpression e) throws UnrecognizedCodeException {
    // TODO possibly may become preciser
    return SIGN.ALL;
  }

  @Override
  public SIGN visit(CIdExpression pIastIdExpression) throws UnrecognizedCodeException {
    return state.getSignForVariable(transferRel.getScopedVariableName(pIastIdExpression));
  }

  @Override
  public SIGN visit(CBinaryExpression pIastBinaryExpression) throws UnrecognizedCodeException {
    SIGN left = pIastBinaryExpression.getOperand1().accept(this);
    SIGN right = pIastBinaryExpression.getOperand2().accept(this);
    Set<SIGN> leftAtomSigns = left.split();
    Set<SIGN> rightAtomSigns = right.split();
    SIGN result = SIGN.EMPTY;
    for (List<SIGN> signCombi :
        Sets.cartesianProduct(ImmutableList.of(leftAtomSigns, rightAtomSigns))) {
      result =
          result.combineWith(
              evaluateExpression(signCombi.get(0), pIastBinaryExpression, signCombi.get(1)));
    }
    return result;
  }

  private SIGN evaluateExpression(SIGN pLeft, CBinaryExpression pExp, SIGN pRight)
      throws UnsupportedCodeException {
    SIGN result = SIGN.EMPTY;
    switch (pExp.getOperator()) {
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
      case MODULO:
        result = evaluateModuloOperator(pLeft, pRight);
        break;
      case BINARY_AND:
        result = evaluateAndOperator(pLeft, pRight);
        break;
      case LESS_EQUAL:
        result = evaluateLessEqualOperator(pLeft, pRight);
        break;
      case GREATER_EQUAL:
        result = evaluateLessEqualOperator(pRight, pLeft);
        break;
      case LESS_THAN:
        result = evaluateLessOperator(pLeft, pRight);
        break;
      case GREATER_THAN:
        result = evaluateLessOperator(pRight, pLeft);
        break;
      case EQUALS:
        result = evaluateEqualOperator(pLeft, pRight);
        break;
      case NOT_EQUALS:
        result = evaluateUnequalOperator(pLeft, pRight);
        break;
      case SHIFT_LEFT:
        result = evaluateLeftShiftOperator(pLeft, pRight);
        break;
      case SHIFT_RIGHT:
        result = evaluateRightShiftOperator(pLeft, pRight);
        break;

      default:
        throw new UnsupportedCodeException("Not supported", edgeOfExpr);
    }
    return result;
  }

  @Override
  public SIGN visit(CFloatLiteralExpression pIastFloatLiteralExpression)
      throws UnrecognizedCodeException {
    BigDecimal value = pIastFloatLiteralExpression.getValue();
    int cResult = value.compareTo(BigDecimal.ZERO);
    if (cResult == 1) {
      return SIGN.PLUS;
    } else if (cResult == -1) {
      return SIGN.MINUS;
    }
    return SIGN.ZERO;
  }

  @Override
  public SIGN visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
      throws UnrecognizedCodeException {
    BigInteger value = pIastIntegerLiteralExpression.getValue();
    int cResult = value.compareTo(BigInteger.ZERO);
    if (cResult == 1) {
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
  public SIGN visit(CCharLiteralExpression e) throws UnrecognizedCodeException {
    return SIGN.ALL;
  }

  @Override
  public SIGN visit(CUnaryExpression pIastUnaryExpression) throws UnrecognizedCodeException {
    switch (pIastUnaryExpression.getOperator()) {
      case MINUS:
        SIGN result = SIGN.EMPTY;
        SIGN operandSign = pIastUnaryExpression.getOperand().accept(this);
        for (SIGN atomSign : operandSign.split()) {
          result =
              result.combineWith(
                  evaluateUnaryExpression(pIastUnaryExpression.getOperator(), atomSign));
        }
        return result;
      default:
        throw new UnsupportedCodeException("Not supported", edgeOfExpr, pIastUnaryExpression);
    }
  }

  private static SIGN evaluateUnaryExpression(UnaryOperator operator, SIGN operand) {
    if (operand == SIGN.ZERO) {
      return SIGN.ZERO;
    }
    if (operator == UnaryOperator.MINUS && operand == SIGN.PLUS) {
      return SIGN.MINUS;
    }
    return SIGN.PLUS;
  }

  private SIGN evaluatePlusOperator(
      SIGN pLeft, CExpression pLeftExp, SIGN pRight, CExpression pRightExp) {
    // Special case: - + 1 => -0, 1 + - => -0
    if ((pLeft == SIGN.MINUS
            && (pRightExp instanceof CIntegerLiteralExpression)
            && ((CIntegerLiteralExpression) pRightExp).getValue().equals(BigInteger.ONE))
        || ((pLeftExp instanceof CIntegerLiteralExpression)
            && ((CIntegerLiteralExpression) pLeftExp).getValue().equals(BigInteger.ONE)
            && pRight == SIGN.MINUS)) {
      return SIGN.MINUS0;
    }
    // Special case: +0 + 1 => +, 1 + +0 => +
    if ((pLeft == SIGN.PLUS0
            && (pRightExp instanceof CIntegerLiteralExpression)
            && ((CIntegerLiteralExpression) pRightExp).getValue().equals(BigInteger.ONE))
        || ((pLeftExp instanceof CIntegerLiteralExpression)
            && ((CIntegerLiteralExpression) pLeftExp).getValue().equals(BigInteger.ONE)
            && pRight == SIGN.PLUS0)) {
      return SIGN.PLUS;
    }
    SIGN leftToRightResult = evaluateNonCommutativePlusOperator(pLeft, pRight);
    SIGN rightToLeftResult = evaluateNonCommutativePlusOperator(pRight, pLeft);
    return leftToRightResult.combineWith(rightToLeftResult);
  }

  private SIGN evaluateNonCommutativePlusOperator(SIGN pLeft, SIGN pRight) {
    if (pRight == SIGN.ZERO) {
      return pLeft;
    }
    if (pLeft == SIGN.PLUS && pRight == SIGN.MINUS) {
      return SIGN.ALL;
    }
    if (pLeft == SIGN.MINUS && pRight == SIGN.MINUS) {
      return SIGN.MINUS;
    }
    if (pLeft == SIGN.PLUS && pRight == SIGN.PLUS) {
      return SIGN.PLUS;
    }
    return SIGN.EMPTY;
  }

  private SIGN evaluateMinusOperator(SIGN pLeft, SIGN pRight, CExpression pRightExp) {
    // Special case: + - 1 => +0
    if (pLeft == SIGN.PLUS
        && (pRightExp instanceof CIntegerLiteralExpression)
        && ((CIntegerLiteralExpression) pRightExp).getValue().equals(BigInteger.ONE)) {
      return SIGN.PLUS0;
    }
    // Special case: -0 - 1 => -
    if (pLeft == SIGN.MINUS0
        && (pRightExp instanceof CIntegerLiteralExpression)
        && ((CIntegerLiteralExpression) pRightExp).getValue().equals(BigInteger.ONE)) {
      return SIGN.MINUS;
    }
    if (pRight == SIGN.ZERO) {
      return pLeft;
    }
    if (pLeft == SIGN.ZERO) {
      switch (pRight) {
        case PLUS:
          return SIGN.MINUS;
        case MINUS:
          return SIGN.PLUS;
        case PLUS0:
          return SIGN.MINUS0;
        case MINUS0:
          return SIGN.PLUS0;
        default:
          return pRight;
      }
    }
    if (pLeft == SIGN.PLUS && pRight == SIGN.MINUS) {
      return SIGN.PLUS;
    }
    if (pLeft == SIGN.MINUS && pRight == SIGN.PLUS) {
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
    if (right == SIGN.ZERO) {
      return SIGN.ZERO;
    }
    if (left == SIGN.PLUS && right == SIGN.MINUS) {
      return SIGN.MINUS;
    }
    if ((left == SIGN.PLUS && right == SIGN.PLUS) || (left == SIGN.MINUS && right == SIGN.MINUS)) {
      return SIGN.PLUS;
    }
    return SIGN.EMPTY;
  }

  private SIGN evaluateDivideOperator(SIGN left, SIGN right) {
    if (right == SIGN.ZERO) {
      transferRel.logger.log(Level.WARNING, "Possibly dividing by zero", edgeOfExpr);
      return SIGN.ALL;
    }
    return evaluateMulOperator(left, right);
  }

  private SIGN evaluateModuloOperator(SIGN pLeft, SIGN pRight) {
    if (pLeft == SIGN.ZERO) {
      return SIGN.ZERO;
    }
    if (pLeft == SIGN.PLUS && (pRight == SIGN.PLUS || pRight == SIGN.MINUS)) {
      return SIGN.PLUS0;
    }
    if (pLeft == SIGN.MINUS && (pRight == SIGN.MINUS || pRight == SIGN.PLUS)) {
      return SIGN.MINUS0;
    }
    return SIGN.ALL;
  }

  // assumes that indicator bit for negative numbers is 1
  private SIGN evaluateAndOperator(SIGN left, SIGN right) {
    if (left == SIGN.ZERO || right == SIGN.ZERO) {
      return SIGN.ZERO;
    }
    if (left == SIGN.PLUS || right == SIGN.PLUS) {
      return SIGN.PLUS0;
    }
    if (left == SIGN.MINUS && right == SIGN.MINUS) {
      return SIGN.MINUS0;
    }
    return SIGN.EMPTY;
  }

  private SIGN evaluateLessOperator(SIGN pLeft, SIGN pRight) {
    if (pLeft == SIGN.EMPTY || pRight == SIGN.EMPTY) {
      return SIGN.EMPTY;
    }
    switch (pLeft) {
      case PLUS:
        if (SIGN.MINUS0.covers(pRight)) {
          return SIGN.ZERO;
        }
        break;
      case MINUS:
        if (SIGN.PLUS0.covers(pRight)) {
          return SIGN.ZERO;
        }
        break;
      case ZERO:
        if (SIGN.MINUS0.covers(pRight)) {
          return SIGN.ZERO;
        }
        if (pRight == SIGN.ZERO) {
          return SIGN.PLUSMINUS;
        }
        break;
      case PLUS0:
        if (pRight == SIGN.MINUS) {
          return SIGN.ZERO;
        }
        if (pRight == SIGN.ZERO) {
          return SIGN.PLUSMINUS;
        }
        break;
      case MINUS0:
        if (pRight == SIGN.PLUS) {
          return SIGN.PLUSMINUS;
        }
        break;
      default:
        break;
    }
    return SIGN.ALL;
  }

  private SIGN evaluateLessEqualOperator(SIGN pLeft, SIGN pRight) {
    if (pLeft == SIGN.EMPTY || pRight == SIGN.EMPTY) {
      return SIGN.EMPTY;
    }
    switch (pLeft) {
      case PLUS:
        if (SIGN.MINUS0.covers(pRight)) {
          return SIGN.ZERO;
        }
        break;
      case MINUS:
        if (SIGN.PLUS0.covers(pRight)) {
          return SIGN.ZERO;
        }
        break;
      case ZERO:
        if (SIGN.PLUS0.covers(pRight)) {
          return SIGN.PLUSMINUS;
        }
        if (pRight == SIGN.MINUS) {
          return SIGN.ZERO;
        }
        break;
      case PLUS0:
        if (pRight == SIGN.MINUS) {
          return SIGN.ZERO;
        }
        break;
      case MINUS0:
        if (pRight == SIGN.PLUS) {
          return SIGN.PLUSMINUS;
        }
        break;
      default:
        break;
    }
    return SIGN.ALL;
  }

  private SIGN evaluateEqualOperator(SIGN pLeft, SIGN pRight) {
    if (pLeft == SIGN.EMPTY || pRight == SIGN.EMPTY) {
      return SIGN.EMPTY;
    }
    if (pLeft == SIGN.ZERO && pRight == SIGN.ZERO) {
      return SIGN.PLUSMINUS;
    }
    return SIGN.ALL;
  }

  private SIGN evaluateUnequalOperator(SIGN pLeft, SIGN pRight) {
    if (pLeft == SIGN.EMPTY || pRight == SIGN.EMPTY) {
      return SIGN.EMPTY;
    }

    if (pLeft == SIGN.PLUS) {
      if (pRight == SIGN.ZERO || pRight == SIGN.MINUS) {
        return SIGN.PLUSMINUS;
      }
    } else if (pLeft == SIGN.ZERO) {
      if (pRight == SIGN.PLUS || pRight == SIGN.MINUS) {
        return SIGN.PLUSMINUS;
      }
    } else if (pLeft == SIGN.MINUS) {
      if (pRight == SIGN.ZERO || pRight == SIGN.PLUS) {
        return SIGN.PLUSMINUS;
      }
    }

    return SIGN.ALL;
  }

  private SIGN evaluateRightShiftOperator(SIGN pLeft, SIGN pRight) {
    if (pRight == SIGN.ZERO) {
      return pLeft;
    }
    if (pRight == SIGN.PLUS) {
      if (pLeft == SIGN.PLUS) {
        return SIGN.PLUS0;
      }
      return pLeft;
    }
    return SIGN.ALL;
  }

  private SIGN evaluateLeftShiftOperator(SIGN pLeft, SIGN pRight) {
    if (pRight == SIGN.ZERO) {
      return pLeft;
    }
    return SIGN.ALL;
  }
}
