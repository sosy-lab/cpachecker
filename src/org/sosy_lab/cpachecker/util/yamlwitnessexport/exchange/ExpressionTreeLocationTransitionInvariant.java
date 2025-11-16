// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.exchange;

import com.google.common.collect.ImmutableMap;
import java.util.concurrent.ConcurrentMap;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ToFormulaVisitor;

public class ExpressionTreeLocationTransitionInvariant extends ExpressionTreeLocationInvariant {

  private ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> mapCurrentVarsToPrev;

  public ExpressionTreeLocationTransitionInvariant(
      String pGroupId,
      CFANode pLocation,
      ExpressionTree<AExpression> pExpressionTree,
      ConcurrentMap<ManagerKey, ToFormulaVisitor> pVisitorCache,
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> pMapCurrentVarsToPrev) {
    super(pGroupId, pLocation, pExpressionTree, pVisitorCache);
    mapCurrentVarsToPrev = pMapCurrentVarsToPrev;
  }

  public ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> getMapCurrentVarsToPrev() {
    return mapCurrentVarsToPrev;
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

    return pObj instanceof ExpressionTreeLocationTransitionInvariant other
        && super.equals(other)
        && other.mapCurrentVarsToPrev.equals(this.mapCurrentVarsToPrev);
  }
}
