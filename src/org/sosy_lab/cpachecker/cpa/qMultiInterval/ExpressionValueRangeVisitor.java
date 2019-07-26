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
package org.sosy_lab.cpachecker.cpa.qMultiInterval;

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
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.Variable;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/** Visitor that get's the interval from an expression, */
class ExpressionValueRangeVisitor
    extends DefaultCExpressionVisitor<Range, UnrecognizedCodeException>
    implements CRightHandSideVisitor<Range, UnrecognizedCodeException> {

  private final MultiIntervalState readableState;
  private final CFAEdge cfaEdge;

  /**
   * Constructor for the Visitor which takes a state to visit and an edge TO this state
   *
   * @param pState state to visit
   * @param edge edge To this state
   */
  public ExpressionValueRangeVisitor(MultiIntervalState pState, CFAEdge edge) {
    readableState = pState;
    cfaEdge = edge;
  }

  /** Default return of the visitor is an unbound Interval */
  @Override
  protected Range visitDefault(CExpression expression) {
    return new Range(IntervalExt.UNBOUND);
  }

  /** Standard visitung method */
  @Override
  public Range visit(CBinaryExpression binaryExpression) throws UnrecognizedCodeException {
    Range interval1 = binaryExpression.getOperand1().accept(this);
    Range interval2 = binaryExpression.getOperand2().accept(this);

    if (interval1 == null || interval2 == null) {
      return new Range(IntervalExt.UNBOUND);
    }

    BinaryOperator operator = binaryExpression.getOperator();
    if (operator.isLogicalOperator()) {
      Log.Log2("Logical Operator " + interval1 + " " + operator + " " + interval2);
      return getLogicInterval(operator, interval1, interval2);
    } else {
      /*  Log.Log2(
      "Arithmetic Operator "
          + binaryExpression
          + ":"
          + interval1
          + " "
          + operator
          + " "
          + interval2);*/
      return getArithmeticInterval(operator, interval1, interval2);
    }
  }

  /**
   * Method for returning a Logic interval ([0,0]for false [1,1]for true or [0,1] for dont know)
   *
   * @param operator the BinaryOperator
   * @param interval1 first Interval
   * @param interval2 second Interval
   * @return the resulting logic interval e.g. [0,5] EQUALS [0,10] --> [0,1] ||| [5,5] EQUALS [5,5]
   *     --> [1] |||[0,5] EQUALS [6,10] --> [0]
   */
  private static Range getLogicInterval(BinaryOperator operator, Range interval1, Range interval2) {
    switch (operator) {
      case EQUALS:
        if (!interval1.intersects(interval2)) {
          return Range.ZERO;
        } else if (interval1.getLow().equals(interval1.getHigh()) && interval1.equals(interval2)) {
          // singular interval, [5;5]==[5;5]
          return Range.ONE;
        } else {
          return Range.BOOLEAN_INTERVAL;
        }

      case NOT_EQUALS:
        if (!interval1.intersects(interval2)) {
          return Range.ONE;
        } else if (interval1.getLow().equals(interval1.getHigh()) && interval1.equals(interval2)) {
          // singular interval, [5;5]!=[5;5]
          return Range.ZERO;
        } else {
          return Range.BOOLEAN_INTERVAL;
        }

      case GREATER_THAN:
        if (interval1.getIn().isGreaterThan(interval2.getIn())) {
          return Range.ONE;
        } else if (interval2.getIn().isGreaterOrEqualThan(interval1.getIn())) {
          return Range.ZERO;
        } else {
          return Range.BOOLEAN_INTERVAL;
        }

      case GREATER_EQUAL: // a>=b == a+1>b, works only for integers
        return getLogicInterval(
            BinaryOperator.GREATER_THAN, interval1.plus(IntervalExt.ONE), interval2);

      case LESS_THAN: // a<b == b>a
        return getLogicInterval(BinaryOperator.GREATER_THAN, interval2, interval1);

      case LESS_EQUAL: // a<=b == b+1>a, works only for integers
        return getLogicInterval(
            BinaryOperator.GREATER_THAN, interval2.plus(IntervalExt.ONE), interval1);

      default:
        throw new AssertionError("unknown binary operator: " + operator);
    }
  }

  /**
   * Method for calculating an arithmetic interval
   *
   * @param operator the used operator to use
   * @param interval1 the first interval
   * @param interval2 the second interval
   * @return the resulting interval from applying the operator to both intervals
   */
  private static Range getArithmeticInterval(
      BinaryOperator operator, Range interval1, Range interval2) {
    switch (operator) {
      case PLUS:
        return interval1.plus(interval2);
      case MINUS:
        return interval1.minus(interval2);
      case MULTIPLY:
        return interval1.times(interval2);
      case DIVIDE:
        return interval1.divide(interval2);
        /*case SHIFT_LEFT:
          return interval1.shiftLeft(interval2);
        case SHIFT_RIGHT:
          return interval1.shiftRight(interval2);
        case MODULO:
          return interval1.modulo(interval2);
        case BINARY_AND:
        case BINARY_OR:
        case BINARY_XOR:
          return Interval.UNBOUND;*/
      default:
        throw new AssertionError("currently unknown operator: " + operator);
    }
  }

  @Override
  public Range visit(CCastExpression cast) throws UnrecognizedCodeException {
    return cast.getOperand().accept(this);
  }

  @Override
  public Range visit(CFunctionCallExpression functionCall) {
    return new Range(IntervalExt.UNBOUND);
  }

  @Override
  public Range visit(CCharLiteralExpression charLiteral) {
    throw new AssertionError("currently unknown Expression: " + charLiteral);
    // return new Interval((long) charLiteral.getCharacter());
  }

  @Override
  public Range visit(CImaginaryLiteralExpression exp) throws UnrecognizedCodeException {
    return exp.getValue().accept(this);
  }

  @Override
  public Range visit(CIntegerLiteralExpression integerLiteral) {
    return new Range(integerLiteral.asLong());
  }

  @Override
  public Range visit(CIdExpression identifier) {
    if (identifier.getDeclaration() instanceof CEnumerator) {
      return new Range(((CEnumerator) identifier.getDeclaration()).getValue());
    }

    final String variableName = identifier.getDeclaration().getQualifiedName();
    if (readableState.contains(variableName)) {
      return readableState.getRange(new Variable(variableName));
    } else {
      return new Range(IntervalExt.UNBOUND);
    }
  }

  @Override
  public Range visit(CUnaryExpression unaryExpression) throws UnrecognizedCodeException {
    Range interval = unaryExpression.getOperand().accept(this);
    switch (unaryExpression.getOperator()) {
      case MINUS:
        return interval.negate();

        /*case AMPER:
              case TILDE:
                return Interval.UNBOUND; // valid expression, but it's a pointer value
        */
      default:
        throw new UnrecognizedCodeException("unknown unary operator", cfaEdge, unaryExpression);
    }
  }
}
