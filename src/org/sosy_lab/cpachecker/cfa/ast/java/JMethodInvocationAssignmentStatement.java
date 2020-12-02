// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

/**
 * This class represents an assignment with a method invocation as right hand side.
 * In the cfa, every method invocation in an expression is transformed to have its
 * own JMethodInvocationAssignmentStatement and a temporary variable to simplify analysis.
 */
public final class JMethodInvocationAssignmentStatement extends AFunctionCallAssignmentStatement
    implements JAssignment, JStatement, JMethodOrConstructorInvocation {

  private static final long serialVersionUID = -8272890940591390982L;

  public JMethodInvocationAssignmentStatement(FileLocation pFileLocation, JLeftHandSide pLeftHandSide,
      JMethodInvocationExpression pRightHandSide) {
    super(pFileLocation, pLeftHandSide, pRightHandSide);

  }

  @Override
  public JMethodInvocationExpression getFunctionCallExpression() {
    return (JMethodInvocationExpression) super.getFunctionCallExpression();
  }

  @Override
  public JLeftHandSide getLeftHandSide() {
    return (JLeftHandSide) super.getLeftHandSide();
  }

  @Override
  public JMethodInvocationExpression getRightHandSide() {
    return (JMethodInvocationExpression) super.getRightHandSide();
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

    if (!(obj instanceof JMethodInvocationAssignmentStatement)) {
      return false;
    }

    return super.equals(obj);
  }
}
