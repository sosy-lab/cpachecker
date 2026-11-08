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
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslType;

public final class AcslTernaryTerm extends AbstractExpression implements AcslTerm {

  @Serial private static final long serialVersionUID = 812812375011353L;
  private final AcslPredicate condition;
  private final AcslTerm resultIfTrue;
  private final AcslTerm resultIfFalse;

  public AcslTernaryTerm(
      FileLocation pFileLocation,
      AcslPredicate pCondition,
      AcslTerm pResultIfTrue,
      AcslTerm pResultIfFalse) {
    super(pFileLocation, pResultIfTrue.getExpressionType());
    // Currently we do not allow the return types to be different.
    // This will likely be relaxed once we have polymorphic types.
    assert pResultIfFalse.getExpressionType() == pResultIfTrue.getExpressionType();
    condition = checkNotNull(pCondition);
    resultIfTrue = pResultIfTrue;
    resultIfFalse = pResultIfFalse;
  }

  public AcslPredicate getCondition() {
    return condition;
  }

  public AcslTerm getResultIfTrue() {
    return resultIfTrue;
  }

  public AcslTerm getResultIfFalse() {
    return resultIfFalse;
  }

  @Override
  public AcslType getExpressionType() {
    return (AcslType) super.getExpressionType();
  }

  @Override
  public <R, X extends Exception> R accept(AcslTermVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return condition.toASTString(pAAstNodeRepresentation)
        + " ? "
        + resultIfTrue.toASTString(pAAstNodeRepresentation)
        + " : "
        + resultIfFalse.toASTString(pAAstNodeRepresentation);
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "("
        + condition.toParenthesizedASTString(pAAstNodeRepresentation)
        + "?"
        + resultIfTrue.toParenthesizedASTString(pAAstNodeRepresentation)
        + ":"
        + resultIfFalse.toParenthesizedASTString(pAAstNodeRepresentation)
        + ")";
  }

  @Override
  public int hashCode() {
    final int prime = 35;
    int result = -1;
    result = prime * result + super.hashCode();
    result = prime * result + condition.hashCode();
    result = prime * result + resultIfTrue.hashCode();
    result = prime * result + resultIfFalse.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslTernaryTerm other
        && super.equals(other)
        && other.condition.equals(condition)
        && other.resultIfTrue.equals(resultIfTrue)
        && other.resultIfFalse.equals(resultIfFalse);
  }
}
