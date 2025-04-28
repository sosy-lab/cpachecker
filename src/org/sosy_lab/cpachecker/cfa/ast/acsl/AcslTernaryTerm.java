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

public final class AcslTernaryTerm implements AcslTerm {

  @Serial private static final long serialVersionUID = 812812375011353L;

  private final AcslPredicate condition;
  private final AcslTerm resultIfTrue;
  private final AcslTerm resultIfFalse;
  private final AcslType type;
  private final FileLocation fileLocation;

  public AcslTernaryTerm(
      FileLocation pFileLocation,
      AcslPredicate pCondition,
      AcslTerm pResultIfTrue,
      AcslTerm pResultIfFalse) {
    // Currently we do not allow the return types to be different.
    // This will likely be relaxed once we have polymorphic types.
    assert pResultIfFalse.getExpressionType() == pResultIfTrue.getExpressionType();
    condition = pCondition;
    resultIfTrue = pResultIfTrue;
    resultIfFalse = pResultIfFalse;
    type = pResultIfTrue.getExpressionType();
    fileLocation = pFileLocation;
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
    return type;
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
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

    return obj instanceof AcslTernaryTerm other
        && Objects.equals(fileLocation, other.fileLocation)
        && Objects.equals(other.type, type)
        && Objects.equals(other.condition, condition)
        && Objects.equals(other.resultIfTrue, resultIfTrue)
        && Objects.equals(other.resultIfFalse, resultIfFalse);
  }
}
