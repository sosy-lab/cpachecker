// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

/**
 * This class represents the expression statement AST node type.
 *
 * This kind of node is used to convert an expression
 * (Expression) into a statement (Statement) by wrapping it.
 *
 * ExpressionStatement:
 *   StatementExpression ;
 *
 * Note that this class is only used for side effect free expressions.
 * For assignments with side effect free right hand sides,
 * we use {@link JExpressionAssignmentStatement}.
 * For method invocations we use {@link JMethodInvocationStatement}
 * And for method assignments, we use {@link JMethodInvocationAssignmentStatement}.
 *
 */
public final class JExpressionStatement extends AExpressionStatement implements JStatement {

  private static final long serialVersionUID = -6963392437624456487L;

  public JExpressionStatement(FileLocation pFileLocation, JExpression pExpression) {
    super(pFileLocation, pExpression);
  }

  @Override
  public JExpression getExpression() {
    return (JExpression) super.getExpression();
  }

  @Override
  public <R, X extends Exception> R accept(JStatementVisitor<R, X> v) throws X {
    return v.visit(this);
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

    if (!(obj instanceof JExpressionStatement)) {
      return false;
    }

    return super.equals(obj);
  }
}
