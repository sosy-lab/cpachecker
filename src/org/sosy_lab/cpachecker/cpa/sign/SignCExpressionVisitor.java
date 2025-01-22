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
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue;

public class SignCExpressionVisitor
    extends DefaultCExpressionVisitor<Sign, UnrecognizedCodeException>
    implements CRightHandSideVisitor<Sign, UnrecognizedCodeException> {

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
  public Sign visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws UnrecognizedCodeException {
    // TODO possibly treat typedef types differently
    // e.g. x = non_det() where non_det is extern, unknown function allways assume returns any value
    if (pIastFunctionCallExpression.getExpressionType() instanceof CSimpleType
        || pIastFunctionCallExpression.getExpressionType() instanceof CTypedefType
        || pIastFunctionCallExpression.getExpressionType() instanceof CPointerType) {
      return Sign.ALL;
    }
    throw new UnrecognizedCodeException("unsupported code found", edgeOfExpr);
  }

  @Override
  protected Sign visitDefault(CExpression pExp) throws UnrecognizedCodeException {
    throw new UnrecognizedCodeException("unsupported code found", edgeOfExpr);
  }

  @Override
  public Sign visit(CCastExpression e) throws UnrecognizedCodeException {
    return e.getOperand().accept(this); // TODO correct?
  }

  @Override
  public Sign visit(CFieldReference e) throws UnrecognizedCodeException {
    return state.getSignForVariable(transferRel.getScopedVariableName(e));
  }

  @Override
  public Sign visit(CArraySubscriptExpression e) throws UnrecognizedCodeException {
    // TODO possibly may become preciser
    return Sign.ALL;
  }

  @Override
  public Sign visit(CPointerExpression e) throws UnrecognizedCodeException {
    // TODO possibly may become preciser
    return Sign.ALL;
  }

  @Override
  public Sign visit(CIdExpression pIastIdExpression) throws UnrecognizedCodeException {
    return state.getSignForVariable(transferRel.getScopedVariableName(pIastIdExpression));
  }

  @Override
  public Sign visit(CBinaryExpression pIastBinaryExpression) throws UnrecognizedCodeException {
    Sign left = pIastBinaryExpression.getOperand1().accept(this);
    Sign right = pIastBinaryExpression.getOperand2().accept(this);
    Set<Sign> leftAtomSigns = left.split();
    Set<Sign> rightAtomSigns = right.split();
    Sign result = Sign.EMPTY;
    for (List<Sign> signCombi :
        Sets.cartesianProduct(ImmutableList.of(leftAtomSigns, rightAtomSigns))) {
      result =
          result.combineWith(
              evaluateExpression(signCombi.get(0), pIastBinaryExpression, signCombi.get(1)));
    }
    return result;
  }

  private Sign evaluateExpression(Sign pLeft, CBinaryExpression pExp, Sign pRight)
      throws UnsupportedCodeException {
    Sign result =
        switch (pExp.getOperator()) {
          case PLUS -> evaluatePlusOperator(pLeft, pExp.getOperand1(), pRight, pExp.getOperand2());
          case MINUS -> evaluateMinusOperator(pLeft, pRight, pExp.getOperand2());
          case MULTIPLY -> evaluateMulOperator(pLeft, pRight);
          case DIVIDE -> evaluateDivideOperator(pLeft, pRight);
          case MODULO -> evaluateModuloOperator(pLeft, pRight);
          case BINARY_AND -> evaluateAndOperator(pLeft, pRight);
          case LESS_EQUAL -> evaluateLessEqualOperator(pLeft, pRight);
          case GREATER_EQUAL -> evaluateLessEqualOperator(pRight, pLeft);
          case LESS_THAN -> evaluateLessOperator(pLeft, pRight);
          case GREATER_THAN -> evaluateLessOperator(pRight, pLeft);
          case EQUALS -> evaluateEqualOperator(pLeft, pRight);
          case NOT_EQUALS -> evaluateUnequalOperator(pLeft, pRight);
          case SHIFT_LEFT -> evaluateLeftShiftOperator(pLeft, pRight);
          case SHIFT_RIGHT -> evaluateRightShiftOperator(pLeft, pRight);
          default -> throw new UnsupportedCodeException("Not supported", edgeOfExpr);
        };
    return result;
  }

  @Override
  public Sign visit(CFloatLiteralExpression pIastFloatLiteralExpression)
      throws UnrecognizedCodeException {
    FloatValue value = pIastFloatLiteralExpression.getValue();
    if (value.isZero()) {
      return Sign.ZERO;
    } else {
      return value.isNegative() ? Sign.MINUS : Sign.PLUS;
    }
  }

  @Override
  public Sign visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
      throws UnrecognizedCodeException {
    BigInteger value = pIastIntegerLiteralExpression.getValue();
    int cResult = value.compareTo(BigInteger.ZERO);
    if (cResult == 1) {
      return Sign.PLUS;
    } else if (cResult == -1) {
      return Sign.MINUS;
    }
    return Sign.ZERO;
  }

  @Override
  public Sign visit(CStringLiteralExpression e) throws UnrecognizedCodeException {
    return Sign.ALL;
  }

  @Override
  public Sign visit(CCharLiteralExpression e) throws UnrecognizedCodeException {
    return Sign.ALL;
  }

  @Override
  public Sign visit(CUnaryExpression pIastUnaryExpression) throws UnrecognizedCodeException {
    switch (pIastUnaryExpression.getOperator()) {
      case MINUS:
        Sign result = Sign.EMPTY;
        Sign operandSign = pIastUnaryExpression.getOperand().accept(this);
        for (Sign atomSign : operandSign.split()) {
          result =
              result.combineWith(
                  evaluateUnaryExpression(pIastUnaryExpression.getOperator(), atomSign));
        }
        return result;
      default:
        throw new UnsupportedCodeException("Not supported", edgeOfExpr, pIastUnaryExpression);
    }
  }

  private static Sign evaluateUnaryExpression(UnaryOperator operator, Sign operand) {
    if (operand == Sign.ZERO) {
      return Sign.ZERO;
    }
    if (operator == UnaryOperator.MINUS && operand == Sign.PLUS) {
      return Sign.MINUS;
    }
    return Sign.PLUS;
  }

  private Sign evaluatePlusOperator(
      Sign pLeft, CExpression pLeftExp, Sign pRight, CExpression pRightExp) {
    // Special case: - + 1 => -0, 1 + - => -0
    if ((pLeft == Sign.MINUS
            && (pRightExp instanceof CIntegerLiteralExpression)
            && ((CIntegerLiteralExpression) pRightExp).getValue().equals(BigInteger.ONE))
        || ((pLeftExp instanceof CIntegerLiteralExpression)
            && ((CIntegerLiteralExpression) pLeftExp).getValue().equals(BigInteger.ONE)
            && pRight == Sign.MINUS)) {
      return Sign.MINUS0;
    }
    // Special case: +0 + 1 => +, 1 + +0 => +
    if ((pLeft == Sign.PLUS0
            && (pRightExp instanceof CIntegerLiteralExpression)
            && ((CIntegerLiteralExpression) pRightExp).getValue().equals(BigInteger.ONE))
        || ((pLeftExp instanceof CIntegerLiteralExpression)
            && ((CIntegerLiteralExpression) pLeftExp).getValue().equals(BigInteger.ONE)
            && pRight == Sign.PLUS0)) {
      return Sign.PLUS;
    }
    Sign leftToRightResult = evaluateNonCommutativePlusOperator(pLeft, pRight);
    Sign rightToLeftResult = evaluateNonCommutativePlusOperator(pRight, pLeft);
    return leftToRightResult.combineWith(rightToLeftResult);
  }

  private Sign evaluateNonCommutativePlusOperator(Sign pLeft, Sign pRight) {
    if (pRight == Sign.ZERO) {
      return pLeft;
    }
    if (pLeft == Sign.PLUS && pRight == Sign.MINUS) {
      return Sign.ALL;
    }
    if (pLeft == Sign.MINUS && pRight == Sign.MINUS) {
      return Sign.MINUS;
    }
    if (pLeft == Sign.PLUS && pRight == Sign.PLUS) {
      return Sign.PLUS;
    }
    return Sign.EMPTY;
  }

  private Sign evaluateMinusOperator(Sign pLeft, Sign pRight, CExpression pRightExp) {
    // Special case: + - 1 => +0
    if (pLeft == Sign.PLUS
        && (pRightExp instanceof CIntegerLiteralExpression)
        && ((CIntegerLiteralExpression) pRightExp).getValue().equals(BigInteger.ONE)) {
      return Sign.PLUS0;
    }
    // Special case: -0 - 1 => -
    if (pLeft == Sign.MINUS0
        && (pRightExp instanceof CIntegerLiteralExpression)
        && ((CIntegerLiteralExpression) pRightExp).getValue().equals(BigInteger.ONE)) {
      return Sign.MINUS;
    }
    if (pRight == Sign.ZERO) {
      return pLeft;
    }
    if (pLeft == Sign.ZERO) {
      return switch (pRight) {
        case PLUS -> Sign.MINUS;
        case MINUS -> Sign.PLUS;
        case PLUS0 -> Sign.MINUS0;
        case MINUS0 -> Sign.PLUS0;
        default -> pRight;
      };
    }
    if (pLeft == Sign.PLUS && pRight == Sign.MINUS) {
      return Sign.PLUS;
    }
    if (pLeft == Sign.MINUS && pRight == Sign.PLUS) {
      return Sign.MINUS;
    }
    return Sign.ALL;
  }

  private Sign evaluateMulOperator(Sign pLeft, Sign pRight) {
    Sign leftToRightResult = evaluateNonCommutativeMulOperator(pLeft, pRight);
    Sign rightToLeftResult = evaluateNonCommutativeMulOperator(pRight, pLeft);
    return leftToRightResult.combineWith(rightToLeftResult);
  }

  private Sign evaluateNonCommutativeMulOperator(Sign left, Sign right) {
    if (right == Sign.ZERO) {
      return Sign.ZERO;
    }
    if (left == Sign.PLUS && right == Sign.MINUS) {
      return Sign.MINUS;
    }
    if ((left == Sign.PLUS && right == Sign.PLUS) || (left == Sign.MINUS && right == Sign.MINUS)) {
      return Sign.PLUS;
    }
    return Sign.EMPTY;
  }

  private Sign evaluateDivideOperator(Sign left, Sign right) {
    if (right == Sign.ZERO) {
      transferRel.logger.log(Level.WARNING, "Possibly dividing by zero", edgeOfExpr);
      return Sign.ALL;
    }
    return evaluateMulOperator(left, right);
  }

  private Sign evaluateModuloOperator(Sign pLeft, Sign pRight) {
    if (pLeft == Sign.ZERO) {
      return Sign.ZERO;
    }
    if (pLeft == Sign.PLUS && (pRight == Sign.PLUS || pRight == Sign.MINUS)) {
      return Sign.PLUS0;
    }
    if (pLeft == Sign.MINUS && (pRight == Sign.MINUS || pRight == Sign.PLUS)) {
      return Sign.MINUS0;
    }
    return Sign.ALL;
  }

  // assumes that indicator bit for negative numbers is 1
  private Sign evaluateAndOperator(Sign left, Sign right) {
    if (left == Sign.ZERO || right == Sign.ZERO) {
      return Sign.ZERO;
    }
    if (left == Sign.PLUS || right == Sign.PLUS) {
      return Sign.PLUS0;
    }
    if (left == Sign.MINUS && right == Sign.MINUS) {
      return Sign.MINUS0;
    }
    return Sign.EMPTY;
  }

  private Sign evaluateLessOperator(Sign pLeft, Sign pRight) {
    if (pLeft == Sign.EMPTY || pRight == Sign.EMPTY) {
      return Sign.EMPTY;
    }
    switch (pLeft) {
      case PLUS:
        if (Sign.MINUS0.covers(pRight)) {
          return Sign.ZERO;
        }
        break;
      case MINUS:
        if (Sign.PLUS0.covers(pRight)) {
          return Sign.ZERO;
        }
        break;
      case ZERO:
        if (Sign.MINUS0.covers(pRight)) {
          return Sign.ZERO;
        }
        if (pRight == Sign.ZERO) {
          return Sign.PLUSMINUS;
        }
        break;
      case PLUS0:
        if (pRight == Sign.MINUS) {
          return Sign.ZERO;
        }
        if (pRight == Sign.ZERO) {
          return Sign.PLUSMINUS;
        }
        break;
      case MINUS0:
        if (pRight == Sign.PLUS) {
          return Sign.PLUSMINUS;
        }
        break;
      default:
        break;
    }
    return Sign.ALL;
  }

  private Sign evaluateLessEqualOperator(Sign pLeft, Sign pRight) {
    if (pLeft == Sign.EMPTY || pRight == Sign.EMPTY) {
      return Sign.EMPTY;
    }
    switch (pLeft) {
      case PLUS:
        if (Sign.MINUS0.covers(pRight)) {
          return Sign.ZERO;
        }
        break;
      case MINUS:
        if (Sign.PLUS0.covers(pRight)) {
          return Sign.ZERO;
        }
        break;
      case ZERO:
        if (Sign.PLUS0.covers(pRight)) {
          return Sign.PLUSMINUS;
        }
        if (pRight == Sign.MINUS) {
          return Sign.ZERO;
        }
        break;
      case PLUS0:
        if (pRight == Sign.MINUS) {
          return Sign.ZERO;
        }
        break;
      case MINUS0:
        if (pRight == Sign.PLUS) {
          return Sign.PLUSMINUS;
        }
        break;
      default:
        break;
    }
    return Sign.ALL;
  }

  private Sign evaluateEqualOperator(Sign pLeft, Sign pRight) {
    if (pLeft == Sign.EMPTY || pRight == Sign.EMPTY) {
      return Sign.EMPTY;
    }
    if (pLeft == Sign.ZERO && pRight == Sign.ZERO) {
      return Sign.PLUSMINUS;
    }
    return Sign.ALL;
  }

  private Sign evaluateUnequalOperator(Sign pLeft, Sign pRight) {
    if (pLeft == Sign.EMPTY || pRight == Sign.EMPTY) {
      return Sign.EMPTY;
    }

    if (pLeft == Sign.PLUS) {
      if (pRight == Sign.ZERO || pRight == Sign.MINUS) {
        return Sign.PLUSMINUS;
      }
    } else if (pLeft == Sign.ZERO) {
      if (pRight == Sign.PLUS || pRight == Sign.MINUS) {
        return Sign.PLUSMINUS;
      }
    } else if (pLeft == Sign.MINUS) {
      if (pRight == Sign.ZERO || pRight == Sign.PLUS) {
        return Sign.PLUSMINUS;
      }
    }

    return Sign.ALL;
  }

  private Sign evaluateRightShiftOperator(Sign pLeft, Sign pRight) {
    if (pRight == Sign.ZERO) {
      return pLeft;
    }
    if (pRight == Sign.PLUS) {
      if (pLeft == Sign.PLUS) {
        return Sign.PLUS0;
      }
      return pLeft;
    }
    return Sign.ALL;
  }

  private Sign evaluateLeftShiftOperator(Sign pLeft, Sign pRight) {
    if (pRight == Sign.ZERO) {
      return pLeft;
    }
    return Sign.ALL;
  }
}
