// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import java.util.Objects;



public abstract class AFunctionCallStatement extends AbstractStatement implements AFunctionCall {

  private static final long serialVersionUID = 7606010817704105593L;
  private final AFunctionCallExpression functionCall;

  public AFunctionCallStatement(FileLocation pFileLocation, AFunctionCallExpression pFunctionCall) {
    super(pFileLocation);
    functionCall = pFunctionCall;
  }

  @Override
  public <R, X extends Exception> R accept(AStatementVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString(boolean pQualified) {
    return functionCall.toASTString(pQualified) + ";";
  }

  @Override
  public AFunctionCallExpression getFunctionCallExpression() {
    return functionCall;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(functionCall);
    result = prime * result + super.hashCode();
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof AFunctionCallStatement)
        || !super.equals(obj)) {
      return false;
    }

    AFunctionCallStatement other = (AFunctionCallStatement) obj;

    return Objects.equals(other.functionCall, functionCall);
  }

}