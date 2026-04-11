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

public record LocalVariableDeclarationSubstitute(
    CIdExpression expression, Optional<MPORSubstitutionTracker> tracker) {

  @Override
  public int hashCode() {
    return Objects.hash(expression, tracker);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther
            instanceof
            LocalVariableDeclarationSubstitute(
                CIdExpression pExpression,
                Optional<MPORSubstitutionTracker> pTracker)
        && expression.equals(pExpression)
        && tracker.equals(pTracker);
  }
}
