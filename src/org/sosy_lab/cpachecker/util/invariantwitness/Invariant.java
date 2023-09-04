// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;

public class Invariant {
  private final ExpressionTree<AExpression> formula;
  private final FileLocation location;
  private final boolean isLoopInvariant;

  public Invariant(
      ExpressionTree<AExpression> pFormula, FileLocation pLocation, boolean pIsLoopInvariant) {
    formula = Objects.requireNonNull(pFormula);
    location = Objects.requireNonNull(pLocation);
    isLoopInvariant = pIsLoopInvariant;
  }

  public FileLocation getLocation() {
    return location;
  }

  public boolean isLoopInvariant() {
    return isLoopInvariant;
  }

  public ExpressionTree<AExpression> getFormula() {
    return formula;
  }

  @Override
  public int hashCode() {
    int hashCode = location.hashCode();
    hashCode = 31 * hashCode + formula.hashCode();
    return hashCode;
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }

    return pObj instanceof Invariant other
        && other.formula.equals(formula)
        && other.location.equals(location);
  }

  @Override
  public String toString() {
    return formula.toString();
  }
}
