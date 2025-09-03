// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.substitution;

import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;

public class LocalVariableDeclarationSubstitute {

  public final CIdExpression expression;

  private final Optional<MPORSubstitutionTracker> tracker;

  public LocalVariableDeclarationSubstitute(
      CIdExpression pExpression, Optional<MPORSubstitutionTracker> pTracker) {

    expression = pExpression;
    tracker = pTracker;
  }

  public boolean isTrackerPresent() {
    return tracker.isPresent();
  }

  public CVariableDeclaration getSubstituteVariableDeclaration() {
    assert expression.getDeclaration() instanceof CVariableDeclaration;
    return (CVariableDeclaration) expression.getDeclaration();
  }

  public MPORSubstitutionTracker getTracker() {
    assert tracker.isPresent() : "cannot get tracker, tracker is not present";
    return tracker.orElseThrow();
  }

  @Override
  public int hashCode() {
    return Objects.hash(expression, tracker);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof LocalVariableDeclarationSubstitute other
        && expression.equals(other.expression)
        && tracker.equals(other.tracker);
  }
}
