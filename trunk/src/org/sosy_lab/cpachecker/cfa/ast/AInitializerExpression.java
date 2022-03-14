// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import java.util.Objects;

public abstract class AInitializerExpression extends AbstractInitializer {

  private static final long serialVersionUID = 1634403757452835794L;
  private final AExpression expression;

  protected AInitializerExpression(FileLocation pFileLocation, final AExpression pExpression) {
    super(pFileLocation);
    expression = pExpression;
  }

  @Override
  public String toASTString(boolean pQualified) {
    return expression.toASTString(pQualified);
  }

  public AExpression getExpression() {
    return expression;
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

    if (!(obj instanceof AInitializerExpression) || !super.equals(obj)) {
      return false;
    }

    AInitializerExpression other = (AInitializerExpression) obj;

    return Objects.equals(other.expression, expression);
  }
}
