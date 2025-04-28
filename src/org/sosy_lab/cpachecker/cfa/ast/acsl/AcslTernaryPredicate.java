// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.io.Serial;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.Type;

public final class AcslTernaryPredicate implements AcslPredicate {

  @Serial private static final long serialVersionUID = 81297456675011353L;

  private final FileLocation fileLocation;
  private final AcslPredicate condition;
  private final AcslPredicate resultIfTrue;
  private final AcslPredicate resultIfFalse;

  public AcslTernaryPredicate(
      FileLocation pFileLocation,
      AcslPredicate pCondition,
      AcslPredicate pResultIfTrue,
      AcslPredicate pResultIfFalse) {
    // Currently we do not allow the return types to be different.
    // This will likely be relaxed once we have polymorphic types.
    assert Objects.equals(pResultIfFalse.getExpressionType(), pResultIfTrue.getExpressionType());
    assert Objects.equals(pResultIfFalse.getExpressionType(), AcslBuiltinLogicType.BOOLEAN);
    fileLocation = pFileLocation;
    condition = pCondition;
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
  public Type getExpressionType() {
    return resultIfTrue.getExpressionType();
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
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
    result = prime * result + Objects.hashCode(condition);
    result = prime * result + Objects.hashCode(resultIfTrue);
    result = prime * result + Objects.hashCode(resultIfFalse);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslTernaryPredicate other
        && Objects.equals(other.fileLocation, fileLocation)
        && Objects.equals(other.condition, condition)
        && Objects.equals(other.resultIfTrue, resultIfTrue)
        && Objects.equals(other.resultIfFalse, resultIfFalse);
  }
}
