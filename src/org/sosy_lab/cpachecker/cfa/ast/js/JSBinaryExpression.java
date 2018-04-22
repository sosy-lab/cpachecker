/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cfa.ast.js;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.js.JSType;

public class JSBinaryExpression extends ABinaryExpression implements JSExpression {

  private static final long serialVersionUID = 7759096923080779112L;
  private final JSType calculationType;

  public JSBinaryExpression(
      final FileLocation pFileLocation,
      final JSType pExpressionType,
      final JSType pCalculationType,
      final JSExpression pOperand1,
      final JSExpression pOperand2,
      final BinaryOperator pOperator) {
    super(pFileLocation, pExpressionType, pOperand1, pOperand2, pOperator);
    calculationType = pCalculationType;
  }

  @Override
  public <R, X extends Exception> R accept(final JSExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(final JSAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public JSType getExpressionType() {
    return (JSType) super.getExpressionType();
  }

  /**
   * This method returns the type for the 'calculation' of this binary expression.
   *
   * <p>This is not the type of the 'result' of this binary expression. The result-type is returned
   * from getType().
   *
   * <p>Before the calculation, if necessary, both operand should be casted to the calculation-type.
   * In most cases this is a widening.
   *
   * <p>Then the operation is performed in this type. This may cause an overflow, if the
   * calculation-type is not big enough.
   *
   * <p>After the calculation, if necessary, the result of the binary operation should be casted to
   * the result-type.
   */
  public JSType getCalculationType() {
    return calculationType;
  }

  @Override
  public JSExpression getOperand1() {
    return (JSExpression) super.getOperand1();
  }

  @Override
  public JSExpression getOperand2() {
    return (JSExpression) super.getOperand2();
  }

  @Override
  public BinaryOperator getOperator() {
    return (BinaryOperator) super.getOperator();
  }

  public enum BinaryOperator implements ABinaryOperator {
    AND("&"),
    CONDITIONAL_AND("&&"),
    CONDITIONAL_OR("||"),
    DIVIDE("/"),
    EQUALS("=="),
    EQUAL_EQUAL_EQUAL("==="),
    GREATER(">"),
    GREATER_EQUALS(">="),
    IN("in"),
    INSTANCEOF("instanceof"),
    LEFT_SHIFT("<<"),
    LESS("<"),
    LESS_EQUALS("<="),
    MINUS("-"),
    NOT_EQUAL_EQUAL("!=="),
    NOT_EQUALS("!="),
    OR("|"),
    PLUS("+"),
    REMAINDER("%"),
    RIGHT_SHIFT_SIGNED(">>"),
    RIGHT_SHIFT_UNSIGNED(">>>"),
    TIMES("*"),
    XOR("^"),
    ;

    private final String op;

    BinaryOperator(final String pOp) {
      op = pOp;
    }

    /** Returns the string representation of this operator (e.g. "*", "+"). */
    @Override
    public String getOperator() {
      return op;
    }

    public boolean isLogicalOperator() {
      switch (this) {
        case AND:
        case DIVIDE:
        case LEFT_SHIFT:
        case MINUS:
        case OR:
        case PLUS:
        case REMAINDER:
        case RIGHT_SHIFT_SIGNED:
        case RIGHT_SHIFT_UNSIGNED:
        case TIMES:
        case XOR:
          return false;
        case CONDITIONAL_AND:
        case CONDITIONAL_OR:
        case EQUALS:
        case EQUAL_EQUAL_EQUAL:
        case GREATER:
        case GREATER_EQUALS:
        case IN:
        case INSTANCEOF:
        case LESS:
        case LESS_EQUALS:
        case NOT_EQUAL_EQUAL:
        case NOT_EQUALS:
          return true;
        default:
          throw new AssertionError("Unhandled case statement");
      }
    }

    public boolean isEqualityOperator() {
      switch (this) {
        case AND:
        case DIVIDE:
        case LEFT_SHIFT:
        case MINUS:
        case OR:
        case PLUS:
        case REMAINDER:
        case RIGHT_SHIFT_SIGNED:
        case RIGHT_SHIFT_UNSIGNED:
        case TIMES:
        case XOR:
        case CONDITIONAL_AND:
        case CONDITIONAL_OR:
        case GREATER:
        case GREATER_EQUALS:
        case IN:
        case INSTANCEOF:
        case LESS:
        case LESS_EQUALS:
          return false;
        case EQUALS:
        case EQUAL_EQUAL_EQUAL:
        case NOT_EQUAL_EQUAL:
        case NOT_EQUALS:
          return true;
        default:
          throw new AssertionError("Unhandled case statement");
      }
    }

    public boolean isMultiplicativeOperator() {
      switch (this) {
        case AND:
        case LEFT_SHIFT:
        case MINUS:
        case OR:
        case PLUS:
        case RIGHT_SHIFT_SIGNED:
        case RIGHT_SHIFT_UNSIGNED:
        case XOR:
        case CONDITIONAL_AND:
        case CONDITIONAL_OR:
        case GREATER:
        case GREATER_EQUALS:
        case IN:
        case INSTANCEOF:
        case LESS:
        case LESS_EQUALS:
        case EQUALS:
        case EQUAL_EQUAL_EQUAL:
        case NOT_EQUAL_EQUAL:
        case NOT_EQUALS:
          return false;
        case TIMES:
        case DIVIDE:
        case REMAINDER:
          return true;
        default:
          throw new AssertionError("Unhandled case statement");
      }
    }

    public boolean isAdditiveOperator() {
      switch (this) {
        case AND:
        case LEFT_SHIFT:
        case OR:
        case RIGHT_SHIFT_SIGNED:
        case RIGHT_SHIFT_UNSIGNED:
        case XOR:
        case CONDITIONAL_AND:
        case CONDITIONAL_OR:
        case GREATER:
        case GREATER_EQUALS:
        case IN:
        case INSTANCEOF:
        case LESS:
        case LESS_EQUALS:
        case EQUALS:
        case EQUAL_EQUAL_EQUAL:
        case NOT_EQUAL_EQUAL:
        case NOT_EQUALS:
        case TIMES:
        case DIVIDE:
        case REMAINDER:
          return false;
        case MINUS:
        case PLUS:
          return true;
        default:
          throw new AssertionError("Unhandled case statement");
      }
    }

    public boolean isBitwiseShiftOperator() {
      switch (this) {
        case AND:
        case OR:
        case XOR:
        case CONDITIONAL_AND:
        case CONDITIONAL_OR:
        case GREATER:
        case GREATER_EQUALS:
        case IN:
        case INSTANCEOF:
        case LESS:
        case LESS_EQUALS:
        case EQUALS:
        case EQUAL_EQUAL_EQUAL:
        case NOT_EQUAL_EQUAL:
        case NOT_EQUALS:
        case TIMES:
        case DIVIDE:
        case REMAINDER:
        case MINUS:
        case PLUS:
          return false;
        case LEFT_SHIFT:
        case RIGHT_SHIFT_SIGNED:
        case RIGHT_SHIFT_UNSIGNED:
          return true;
        default:
          throw new AssertionError("Unhandled case statement");
      }
    }

    public BinaryOperator getOppositLogicalOperator() {
      assert isLogicalOperator();
      switch (this) {
        case LESS_EQUALS:
          return GREATER;
        case LESS:
          return GREATER_EQUALS;
        case GREATER_EQUALS:
          return LESS;
        case GREATER:
          return LESS_EQUALS;
        case EQUALS:
          return NOT_EQUALS;
        case NOT_EQUALS:
          return EQUALS;
        case EQUAL_EQUAL_EQUAL:
          return NOT_EQUAL_EQUAL;
        case NOT_EQUAL_EQUAL:
          return EQUAL_EQUAL_EQUAL;
        default:
          return this;
      }
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(calculationType);
    return result * prime + super.hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof JSBinaryExpression)) {
      return false;
    }

    final JSBinaryExpression other = (JSBinaryExpression) obj;

    return Objects.equals(other.calculationType, calculationType) && super.equals(obj);
  }
}
