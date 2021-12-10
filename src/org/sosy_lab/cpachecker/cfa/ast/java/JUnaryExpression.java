// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 *
 * This class represents the prefix expression AST node type.
 *
 * PrefixExpression:
 *   PrefixOperator Expression
 *
 *  This class does not represent increments or decrements, they are transformed
 *  to {@link JBinaryExpression}.
 *
 */
public final class JUnaryExpression extends AUnaryExpression implements JExpression {

  private static final long serialVersionUID = 7677831398229521071L;

  public JUnaryExpression(FileLocation pFileLocation, JType pType, JExpression pOperand, UnaryOperator pOperator) {
    super(pFileLocation, pType, pOperand, pOperator);

  }

  @Override
  public JType getExpressionType() {
    return (JType) super.getExpressionType();
  }

  @Override
  public JExpression getOperand() {
    return (JExpression) super.getOperand();
  }

  @Override
  public UnaryOperator getOperator() {
    return (UnaryOperator) super.getOperator();
  }

  @Override
  public <R, X extends Exception> R accept(JExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  public enum UnaryOperator implements AUnaryExpression.AUnaryOperator {
    NOT      ("!"),
    PLUS        ("+"),
    COMPLEMENT        ("~"),
    MINUS("-")
    ;

    private final String op;

    UnaryOperator(String pOp) {
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

    if (!(obj instanceof JUnaryExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}
