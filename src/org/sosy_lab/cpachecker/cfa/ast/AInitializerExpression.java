// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import java.io.Serial;
import java.util.Objects;

public abstract class AInitializerExpression extends AbstractInitializer {

  @Serial private static final long serialVersionUID = 1634403757452835794L;
  private final AExpression expression;

  protected AInitializerExpression(FileLocation pFileLocation, final AExpression pExpression) {
    super(pFileLocation);
    expression = pExpression;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return expression.toASTString(pAAstNodeRepresentation);
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

    return obj instanceof AInitializerExpression other
        && super.equals(obj)
        && Objects.equals(other.expression, expression);
  }
}
