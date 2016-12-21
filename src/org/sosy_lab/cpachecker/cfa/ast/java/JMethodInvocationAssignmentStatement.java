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

import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

/**
 * This class represents an assignment with a method invocation as right hand side.
 * In the cfa, every method invocation in an expression is transformed to have its
 * own JMethodInvocationAssignmentStatement and a temporary variable to simplify analysis.
 */
public final class JMethodInvocationAssignmentStatement extends AFunctionCallAssignmentStatement
    implements JAssignment, JStatement, JMethodOrConstructorInvocation {

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
