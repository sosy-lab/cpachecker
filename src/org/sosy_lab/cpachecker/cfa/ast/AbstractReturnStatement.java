// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class AbstractReturnStatement extends AbstractAstNode implements AReturnStatement {

  private static final long serialVersionUID = 2672685167471010046L;
  private final @Nullable AExpression expression;
  private final @Nullable AAssignment assignment;

  protected AbstractReturnStatement(
      final FileLocation pFileLocation,
      final Optional<? extends AExpression> pExpression,
      final Optional<? extends AAssignment> pAssignment) {
    super(pFileLocation);
    expression = pExpression.orElse(null);
    assignment = pAssignment.orElse(null);
  }

  @Override
  public String toASTString(boolean pQualified) {
    return "return" + (expression != null ? " " + expression.toASTString(pQualified) : "") + ";";
  }

  @Override
  public Optional<? extends AExpression> getReturnValue() {
    return Optional.ofNullable(expression);
  }

  @Override
  public Optional<? extends AAssignment> asAssignment() {
    return Optional.ofNullable(assignment);
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

    if (!(obj instanceof AbstractReturnStatement) || !super.equals(obj)) {
      return false;
    }

    AbstractReturnStatement other = (AbstractReturnStatement) obj;

    return Objects.equals(other.expression, expression);
  }
}
