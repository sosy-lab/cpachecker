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

  private final CIdExpression idExpression;

  private final Optional<MPORSubstitutionTracker> tracker;

  LocalVariableDeclarationSubstitute(
      CIdExpression pIdExpression, Optional<MPORSubstitutionTracker> pTracker) {

    idExpression = pIdExpression;
    tracker = pTracker;
  }

  CIdExpression getIdExpression() {
    return idExpression;
  }

  CVariableDeclaration getSubstituteVariableDeclaration() {
    return (CVariableDeclaration) idExpression.getDeclaration();
  }

  boolean isTrackerPresent() {
    return tracker.isPresent();
  }

  MPORSubstitutionTracker getTracker() {
    return tracker.orElseThrow();
  }

  @Override
  public int hashCode() {
    return Objects.hash(idExpression, tracker);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof LocalVariableDeclarationSubstitute other
        && idExpression.equals(other.idExpression)
        && tracker.equals(other.tracker);
  }
}
