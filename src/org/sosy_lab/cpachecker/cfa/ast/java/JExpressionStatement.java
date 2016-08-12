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
