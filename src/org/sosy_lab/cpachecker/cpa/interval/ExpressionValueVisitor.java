// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interval;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CEnumerator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/** Visitor that gets the interval from an expression, */
class ExpressionValueVisitor extends DefaultCExpressionVisitor<Interval, UnrecognizedCodeException>
    implements CRightHandSideVisitor<Interval, UnrecognizedCodeException> {

  private final IntervalAnalysisState readableState;

  private final CFAEdge cfaEdge;

  public ExpressionValueVisitor(IntervalAnalysisState pState, CFAEdge edge) {
    readableState = pState;
    cfaEdge = edge;
  }

  @Override
  protected Interval visitDefault(CExpression expression) {
    return Interval.UNBOUND;
  }

  @Override
  public Interval visit(CBinaryExpression binaryExpression) throws UnrecognizedCodeException {
    Interval interval1 = binaryExpression.getOperand1().accept(this);
    Interval interval2 = binaryExpression.getOperand2().accept(this);

    if (interval1 == null || interval2 == null) {
      return Interval.UNBOUND;
    }

    BinaryOperator operator = binaryExpression.getOperator();
    if (operator.isLogicalOperator()) {
      return getLogicInterval(operator, interval1, interval2);
    } else {
      return getArithmeticInterval(operator, interval1, interval2);
    }
  }

  private static Interval getLogicInterval(
      BinaryOperator operator, Interval interval1, Interval interval2) {
    switch (operator) {
      case EQUALS -> {
        if (!interval1.intersects(interval2)) {
          return Interval.ZERO;
        } else if (interval1.getLow().equals(interval1.getHigh()) && interval1.equals(interval2)) {
          // singular interval, [5;5]==[5;5]
          return Interval.ONE;
        } else {
          return Interval.BOOLEAN_INTERVAL;
        }
      }
      case NOT_EQUALS -> {
        if (!interval1.intersects(interval2)) {
          return Interval.ONE;
        } else if (interval1.getLow().equals(interval1.getHigh()) && interval1.equals(interval2)) {
          // singular interval, [5;5]!=[5;5]
          return Interval.ZERO;
        } else {
          return Interval.BOOLEAN_INTERVAL;
        }
      }
      case GREATER_THAN -> {
        if (interval1.isGreaterThan(interval2)) {
          return Interval.ONE;
        } else if (interval2.isGreaterOrEqualThan(interval1)) {
          return Interval.ZERO;
        } else {
          return Interval.BOOLEAN_INTERVAL;
        }
      }
      case GREATER_EQUAL -> {
        // a>=b == a+1>b, works only for integers
        return getLogicInterval(
            BinaryOperator.GREATER_THAN, interval1.plus(Interval.ONE), interval2);
      }
      case LESS_THAN -> {
        // a<b == b>a
        return getLogicInterval(BinaryOperator.GREATER_THAN, interval2, interval1);
      }
      case LESS_EQUAL -> {
        // a<=b == b+1>a, works only for integers
        return getLogicInterval(
            BinaryOperator.GREATER_THAN, interval2.plus(Interval.ONE), interval1);
      }
      default -> throw new AssertionError("unknown binary operator: " + operator);
    }
  }

  private static Interval getArithmeticInterval(
      BinaryOperator operator, Interval interval1, Interval interval2) {
    return switch (operator) {
      case PLUS -> interval1.plus(interval2);
      case MINUS -> interval1.minus(interval2);
      case MULTIPLY -> interval1.times(interval2);
      case DIVIDE -> interval1.divide(interval2);
      case SHIFT_LEFT -> interval1.shiftLeft(interval2);
      case SHIFT_RIGHT -> interval1.shiftRight(interval2);
      case MODULO -> interval1.modulo(interval2);
      case BINARY_AND, BINARY_OR, BINARY_XOR -> Interval.UNBOUND;
      default -> throw new AssertionError("unknown binary operator: " + operator);
    };
  }

  @Override
  public Interval visit(CCastExpression cast) throws UnrecognizedCodeException {
    return cast.getOperand().accept(this);
  }

  @Override
  public Interval visit(CFunctionCallExpression functionCall) {
    return Interval.UNBOUND;
  }

  @Override
  public Interval visit(CCharLiteralExpression charLiteral) {
    return new Interval((long) charLiteral.getCharacter());
  }

  @Override
  public Interval visit(CImaginaryLiteralExpression exp) throws UnrecognizedCodeException {
    return exp.getValue().accept(this);
  }

  @Override
  public Interval visit(CIntegerLiteralExpression integerLiteral) {
    BigInteger value = integerLiteral.getValue();
    return getIntervalFor(value);
  }

  @Override
  public Interval visit(CIdExpression identifier) {
    if (identifier.getDeclaration() instanceof CEnumerator cEnumerator) {
      BigInteger enumConstant = cEnumerator.getValue();
      return getIntervalFor(enumConstant);
    }

    final String variableName = identifier.getDeclaration().getQualifiedName();
    if (readableState.contains(variableName)) {
      return readableState.getInterval(variableName);
    } else {
      return Interval.UNBOUND;
    }
  }

  @Override
  public Interval visit(CUnaryExpression unaryExpression) throws UnrecognizedCodeException {
    Interval interval = unaryExpression.getOperand().accept(this);
    return switch (unaryExpression.getOperator()) {
      case MINUS -> interval.negate();
      case AMPER, TILDE -> Interval.UNBOUND;
      default ->
          throw new UnrecognizedCodeException("unknown unary operator", cfaEdge, unaryExpression);
    };
  }

  private Interval getIntervalFor(BigInteger value) {
    // TODO handle values that are bigger than MAX_LONG.
    try {
      long longValue = value.longValueExact();
      return new Interval(longValue);
    } catch (ArithmeticException e) {
      return Interval.UNBOUND;
    }
  }
}
