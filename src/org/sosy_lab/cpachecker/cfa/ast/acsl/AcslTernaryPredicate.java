// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslBuiltinLogicType;

public final class AcslTernaryPredicate extends AbstractExpression implements AcslPredicate {

  @Serial private static final long serialVersionUID = 8803046908559569331L;
  private final AcslPredicate condition;
  private final AcslPredicate resultIfTrue;
  private final AcslPredicate resultIfFalse;

  public AcslTernaryPredicate(
      FileLocation pFileLocation,
      AcslPredicate pCondition,
      AcslPredicate pResultIfTrue,
      AcslPredicate pResultIfFalse) {
    super(pFileLocation, AcslBuiltinLogicType.BOOLEAN);
    // Currently we do not allow the return types to be different.
    // This will likely be relaxed once we have polymorphic types.
    checkArgument(
        pResultIfFalse.getExpressionType().equals(pResultIfTrue.getExpressionType()),
        "The branches of %s have different types",
        pCondition);
    checkArgument(
        pResultIfFalse.getExpressionType().equals(AcslBuiltinLogicType.BOOLEAN),
        "The branches of %s are no predicates",
        pCondition);
    condition = checkNotNull(pCondition);
    resultIfTrue = pResultIfTrue;
    resultIfFalse = pResultIfFalse;
  }

  public AcslPredicate getCondition() {
    return condition;
  }

  public AcslPredicate getResultIfTrue() {
    return resultIfTrue;
  }

  public AcslPredicate getResultIfFalse() {
    return resultIfFalse;
  }

  @Override
  public <R, X extends Exception> R accept(AcslPredicateVisitor<R, X> v) throws X {
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
    int result = 1;
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

    return obj instanceof AcslTernaryPredicate other
        && super.equals(other)
        && other.condition.equals(condition)
        && other.resultIfTrue.equals(resultIfTrue)
        && other.resultIfFalse.equals(resultIfFalse);
  }
}
