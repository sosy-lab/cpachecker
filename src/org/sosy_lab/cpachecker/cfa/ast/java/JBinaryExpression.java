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
package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 *
 * This class represents the infix expression AST node type.
 *
 * InfixExpression:
 * Expression InfixOperator Expression { InfixOperator Expression }
 *
 * Operand1 is the left operand.
 * Operand2 the right operand.
 * The possible Operators are represented by the enum {@link JBinaryExpression.BinaryOperator}
 *
 * Some expression in Java, like the postfix increment, will be transformed
 * into a infix expression in the CFA and also be represented by this class.
 *
 */
public final class JBinaryExpression extends ABinaryExpression implements JExpression {

  public JBinaryExpression(FileLocation pFileLocation, JType pType, JExpression pOperand1, JExpression pOperand2,
      BinaryOperator pOperator) {
    super(pFileLocation, pType, pOperand1, pOperand2, pOperator);

  }

  @Override
  public <R, X extends Exception> R accept(JExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public JType getExpressionType() {
    return (JType) super.getExpressionType();
  }

  @Override
  public JExpression getOperand1() {
    return (JExpression) super.getOperand1();
  }

  @Override
  public JExpression getOperand2() {
    return (JExpression)super.getOperand2();
  }

  @Override
  public BinaryOperator getOperator() {
    return (BinaryOperator) super.getOperator();
  }

  public static enum BinaryOperator implements ABinaryExpression.ABinaryOperator {
    MULTIPLY      ("*"),
    DIVIDE        ("/"),
    MODULO        ("%"),
    STRING_CONCATENATION("+"),
    PLUS          ("+"),
    MINUS         ("-"),
    SHIFT_LEFT    ("<<"),
    SHIFT_RIGHT_SIGNED   (">>"),
    SHIFT_RIGHT_UNSIGNED (">>>"),
    LESS_THAN     ("<"),
    GREATER_THAN  (">"),
    LESS_EQUAL    ("<="),
    GREATER_EQUAL (">="),
    BINARY_AND    ("&"),
    BINARY_XOR    ("^"),
    BINARY_OR     ("|"),
    LOGICAL_AND   ("&"),
    LOGICAL_OR    ("|"),
    LOGICAL_XOR   ("^"),
    CONDITIONAL_AND ("&&"),
    CONDITIONAL_OR  ("||"),
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
    int prime = 31;
    int result = 7;
    return prime * result + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof JBinaryExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}