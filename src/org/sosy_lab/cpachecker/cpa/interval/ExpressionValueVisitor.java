/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.interval;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

/** Visitor that get's the interval from an expression, */
class ExpressionValueVisitor extends DefaultCExpressionVisitor<Interval, UnrecognizedCCodeException>
    implements CRightHandSideVisitor<Interval, UnrecognizedCCodeException> {

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
  public Interval visit(CBinaryExpression binaryExpression) throws UnrecognizedCCodeException {
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
      case EQUALS:
        if (!interval1.intersects(interval2)) {
          return Interval.ZERO;
        } else if (interval1.getLow().equals(interval1.getHigh()) && interval1.equals(interval2)) {
          // singular interval, [5;5]==[5;5]
          return Interval.ONE;
        } else {
          return Interval.BOOLEAN_INTERVAL;
        }

      case NOT_EQUALS:
        if (!interval1.intersects(interval2)) {
          return Interval.ONE;
        } else if (interval1.getLow().equals(interval1.getHigh()) && interval1.equals(interval2)) {
          // singular interval, [5;5]!=[5;5]
          return Interval.ZERO;
        } else {
          return Interval.BOOLEAN_INTERVAL;
        }

      case GREATER_THAN:
        if (interval1.isGreaterThan(interval2)) {
          return Interval.ONE;
        } else if (interval2.isGreaterOrEqualThan(interval1)) {
          return Interval.ZERO;
        } else {
          return Interval.BOOLEAN_INTERVAL;
        }

      case GREATER_EQUAL: // a>=b == a+1>b, works only for integers
        return getLogicInterval(
            BinaryOperator.GREATER_THAN, interval1.plus(Interval.ONE), interval2);

      case LESS_THAN: // a<b == b>a
        return getLogicInterval(BinaryOperator.GREATER_THAN, interval2, interval1);

      case LESS_EQUAL: // a<=b == b+1>a, works only for integers
        return getLogicInterval(
            BinaryOperator.GREATER_THAN, interval2.plus(Interval.ONE), interval1);

      default:
        throw new AssertionError("unknown binary operator: " + operator);
    }
  }

  private static Interval getArithmeticInterval(
      BinaryOperator operator, Interval interval1, Interval interval2) {
    switch (operator) {
      case PLUS:
        return interval1.plus(interval2);
      case MINUS:
        return interval1.minus(interval2);
      case MULTIPLY:
        return interval1.times(interval2);
      case DIVIDE:
        return interval1.divide(interval2);
      case SHIFT_LEFT:
        return interval1.shiftLeft(interval2);
      case SHIFT_RIGHT:
        return interval1.shiftRight(interval2);
      case MODULO:
        return interval1.modulo(interval2);
      case BINARY_AND:
      case BINARY_OR:
      case BINARY_XOR:
        return Interval.UNBOUND;
      default:
        throw new AssertionError("unknown binary operator: " + operator);
    }
  }

  @Override
  public Interval visit(CCastExpression cast) throws UnrecognizedCCodeException {
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
  public Interval visit(CImaginaryLiteralExpression exp) throws UnrecognizedCCodeException {
    return exp.getValue().accept(this);
  }

  @Override
  public Interval visit(CIntegerLiteralExpression integerLiteral) {
    return new Interval(integerLiteral.asLong());
  }

  @Override
  public Interval visit(CIdExpression identifier) {
    if (identifier.getDeclaration() instanceof CEnumerator) {
      return new Interval(((CEnumerator) identifier.getDeclaration()).getValue());
    }

    final String variableName = identifier.getDeclaration().getQualifiedName();
    if (readableState.contains(variableName)) {
      return readableState.getInterval(variableName);
    } else {
      return Interval.UNBOUND;
    }
  }

  @Override
  public Interval visit(CUnaryExpression unaryExpression) throws UnrecognizedCCodeException {
    Interval interval = unaryExpression.getOperand().accept(this);
    switch (unaryExpression.getOperator()) {
      case MINUS:
        return interval.negate();

      case AMPER:
      case TILDE:
        return Interval.UNBOUND; // valid expression, but it's a pointer value

      default:
        throw new UnrecognizedCCodeException("unknown unary operator", cfaEdge, unaryExpression);
    }
  }
}
