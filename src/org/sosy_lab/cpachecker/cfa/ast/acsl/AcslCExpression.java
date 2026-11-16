// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslCType;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslType;

public final class AcslCExpression extends AbstractExpression implements AcslTerm {

  @Serial private static final long serialVersionUID = -6651381884284097876L;
  private final CExpression expression;

  public AcslCExpression(FileLocation pLocation, CExpression pExpression) {
    super(pLocation, new AcslCType(pExpression.getExpressionType()));
    expression = pExpression;
  }

  public CExpression getCExpression() {
    return expression;
  }

  @Override
  public <R, X extends Exception> R accept(AcslTermVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public AcslType getExpressionType() {
    return (AcslType) super.getExpressionType();
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return expression.toASTString(pAAstNodeRepresentation);
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toASTString(pAAstNodeRepresentation);
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + expression.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslCExpression other
        && super.equals(other)
        && other.expression.equals(expression);
  }
}
