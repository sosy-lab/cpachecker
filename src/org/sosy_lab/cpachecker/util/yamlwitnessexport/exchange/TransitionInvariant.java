// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.exchange;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;

public class TransitionInvariant extends Invariant {

  private ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> mapPrevVarsToCurr;

  public TransitionInvariant(
      ExpressionTree<AExpression> pFormula,
      int pLine,
      int pColumn,
      String pFunction,
      boolean pIsLoopInvariant,
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> pMapCurrentVarsToPrev) {
    super(pFormula, pLine, pColumn, pFunction, pIsLoopInvariant);
    mapPrevVarsToCurr = pMapCurrentVarsToPrev;
  }

  public ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> getMapCurrentVarsToPrev() {
    return mapPrevVarsToCurr;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }

    return pObj instanceof TransitionInvariant other
        && super.equals(other)
        && other.mapPrevVarsToCurr.equals(this.mapPrevVarsToCurr);
  }
}
