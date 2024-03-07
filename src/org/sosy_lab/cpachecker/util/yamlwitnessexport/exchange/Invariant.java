// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.exchange;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;

/**
 * This class represents an invariant with a formula as an {@link ExpressionTree} and a {@link
 * FileLocation}. It also contains a flag indicating whether the invariant is a loop invariant or
 * only a location invariant.
 */
public class Invariant {
  private final ExpressionTree<AExpression> formula;
  private final int line;
  private final int column;
  private final boolean isLoopInvariant;

  public Invariant(
      ExpressionTree<AExpression> pFormula, int pLine, int pColumn, boolean pIsLoopInvariant) {
    formula = Objects.requireNonNull(pFormula);
    line = pLine;
    column = pColumn;
    isLoopInvariant = pIsLoopInvariant;
  }

  public int getLine() {
    return line;
  }

  public int getColumn() {
    return column;
  }

  public boolean isLoopInvariant() {
    return isLoopInvariant;
  }

  public ExpressionTree<AExpression> getFormula() {
    return formula;
  }

  @Override
  public int hashCode() {
    int hashCode = column + 124765 * line;
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
        && other.line == line
        && other.column == column
        && other.isLoopInvariant == isLoopInvariant;
  }

  @Override
  public String toString() {
    return formula.toString();
  }
}
