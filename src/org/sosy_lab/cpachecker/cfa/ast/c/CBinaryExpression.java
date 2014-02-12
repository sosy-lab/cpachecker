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
package org.sosy_lab.cpachecker.cfa.ast.c;

import java.util.Objects;

import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class CBinaryExpression extends ABinaryExpression implements CExpression {

  private final CType calculationType;

  public CBinaryExpression(final FileLocation pFileLocation,
                              final CType pExpressionType,
                              final CType pCalculationType,
                              final CExpression pOperand1,
                              final CExpression pOperand2,
                              final BinaryOperator pOperator) {
    super(pFileLocation, pExpressionType, pOperand1, pOperand2, pOperator);
    calculationType = pCalculationType;
  }

  @Override
  public <R, X extends Exception> R accept(CExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public CType getExpressionType() {
    return (CType) super.getExpressionType();
  }

  /**
   * This method returns the type for the 'calculation' of this binary expression.
   *
   * This is not the type of the 'result' of this binary expression.
   * The result-type is returned from getExpressionType().
   * <p>
   * Before the calculation, if necessary,
   * both operand should be casted to the calculation-type.
   * In most cases this is a widening.
   *
   * Then the operation is performed in this type.
   * This may cause an overflow, if the calculation-type is not big enough.
   *
   * After the calculation, if necessary,
   * the result of the binary operation should be casted to the result-type.
   */
  public CType getCalculationType() {
    return calculationType;
  }

  @Override
  public CExpression getOperand1() {
    return (CExpression) super.getOperand1();
  }

  @Override
  public CExpression getOperand2() {
    return (CExpression)super.getOperand2();
  }

  @Override
  public BinaryOperator getOperator() {
    return (BinaryOperator) super.getOperator();
  }

  public static enum BinaryOperator implements ABinaryExpression.ABinaryOperator {
    MULTIPLY      ("*"),
    DIVIDE        ("/"),
    MODULO        ("%"),
    PLUS          ("+"),
    MINUS         ("-"),
    SHIFT_LEFT    ("<<"),
    SHIFT_RIGHT   (">>"),
    LESS_THAN     ("<"),
    GREATER_THAN  (">"),
    LESS_EQUAL    ("<="),
    GREATER_EQUAL (">="),
    BINARY_AND    ("&"),
    BINARY_XOR    ("^"),
    BINARY_OR     ("|"),
    @Deprecated // unused, does not occur in the AST
    LOGICAL_AND   ("&&"),
    @Deprecated // unused, does not occur in the AST
    LOGICAL_OR    ("||"),
    EQUALS        ("=="),
    NOT_EQUALS    ("!="),
    ;

    private final String op;

    private BinaryOperator(String pOp) {
      op = pOp;
    }

    /**
     * Returns the string representation of this operator (e.g. "*", "+").
     */
    @Override
    public String getOperator() {
      return op;
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
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CBinaryExpression)) {
      return false;
    }

    final CBinaryExpression other = (CBinaryExpression) obj;

    return Objects.equals(other.calculationType, calculationType) && super.equals(obj);
  }
}
