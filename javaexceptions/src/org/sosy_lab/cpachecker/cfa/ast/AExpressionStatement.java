// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import java.util.Objects;

public abstract class AExpressionStatement extends AbstractStatement {

  private static final long serialVersionUID = 315680811122305698L;
  private final AExpression expression;

  protected AExpressionStatement(FileLocation pFileLocation, final AExpression pExpression) {
    super(pFileLocation);
    expression = pExpression;
  }

  @Override
  public String toASTString(boolean pQualified) {
    return expression.toASTString(pQualified) + ";";
  }

  public AExpression getExpression() {
    return expression;
  }

  @Override
  public <R, X extends Exception> R accept(AStatementVisitor<R, X> pV) throws X {

    return pV.visit(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(expression);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof AExpressionStatement) || !super.equals(obj)) {
      return false;
    }

    AExpressionStatement other = (AExpressionStatement) obj;

    return Objects.equals(other.expression, expression);
  }
}
