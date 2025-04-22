// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.Type;

public final class AcslOldExpression extends AbstractExpression implements AcslExpression {

  @Serial
  private static final long serialVersionUID = -81455023251276L;

  private AcslExpression expression;

  public AcslOldExpression(FileLocation pLocation, AcslExpression pExpression) {
    super(pLocation, pExpression.getExpressionType());
    checkNotNull(pExpression);
    expression = pExpression;
  }

  @Override
  public <R, X extends Exception> R accept(AcslExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public Type getExpressionType() {
    return expression.getExpressionType();
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public FileLocation getFileLocation() {
    return super.getFileLocation();
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "\\old(" + expression.toASTString(pAAstNodeRepresentation) + ")";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toASTString(pAAstNodeRepresentation);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 3;
    result = prime * result + super.hashCode();
    result = prime * result + expression.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslOldExpression other
        && super.equals(obj)
        && expression.equals(other.expression);
  }
}
