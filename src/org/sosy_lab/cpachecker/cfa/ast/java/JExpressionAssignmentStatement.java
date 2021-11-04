// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

/**
 * This class represents the assignment expression AST node type.
 *
 *
 * Assignment:
 *   Expression = Expression
 *
 * Note that the assignment operator is always '='. All assignment expressions
 * are transformed into an assignment with '=' and a {@link JBinaryExpression}.
 *
 * Note also, that the expressions have to be side-effect free.
 *
 *
 */
public final class JExpressionAssignmentStatement extends AExpressionAssignmentStatement
    implements JAssignment, JStatement {

  private static final long serialVersionUID = 4121782081088537434L;

  public JExpressionAssignmentStatement(FileLocation pFileLocation, JLeftHandSide pLeftHandSide,
      JExpression pRightHandSide) {
    super(pFileLocation, pLeftHandSide, pRightHandSide);
  }

  @Override
  public JLeftHandSide getLeftHandSide() {
    return (JLeftHandSide) super.getLeftHandSide();
  }

  @Override
  public JExpression getRightHandSide() {
    return (JExpression) super.getRightHandSide();
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

    if (!(obj instanceof JExpressionAssignmentStatement)) {
      return false;
    }

    return super.equals(obj);
  }
}
