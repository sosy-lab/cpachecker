// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.ACastExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 *
 * This class represents a Cast expression AST node type.
 *
 * CastExpression:
 *   ( Type ) Expression
 *
 * The expression type also denotes the type the expression is casted to.
 *
 */
public final class JCastExpression extends ACastExpression implements JExpression {

  private static final long serialVersionUID = 5936546055892473701L;

  public JCastExpression(FileLocation pFileLocation, JType pCastType, JExpression pOperand) {
    super(pFileLocation, pCastType, pOperand);
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
  public JType getCastType() {
    return (JType) super.getCastType();
  }

  @Override
  public JExpression getOperand() {
    return (JExpression) super.getOperand();
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

    if (!(obj instanceof JCastExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}
