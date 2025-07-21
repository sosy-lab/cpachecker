// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.unsequenced;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;

public record ExpressionAnalysisSummary(
    ImmutableSet<SideEffectInfo> sideEffects,
    ImmutableSet<CBinaryExpression> unsequencedBinaryExprs,
    ImmutableMap<CRightHandSide, ImmutableSet<SideEffectInfo>> sideEffectsPerSubExpr) {

  public static ExpressionAnalysisSummary empty() {
    return new ExpressionAnalysisSummary(ImmutableSet.of(), ImmutableSet.of(), ImmutableMap.of());
  }

  /** Convert mutable collections into an immutable summary. */
  public static ExpressionAnalysisSummary of(
      Set<SideEffectInfo> sideEffects,
      Set<CBinaryExpression> unsequencedBinaryExprs,
      Map<CRightHandSide, Set<SideEffectInfo>> sideEffectsPerSubExpr) {
    return new ExpressionAnalysisSummary(
        ImmutableSet.copyOf(sideEffects),
        ImmutableSet.copyOf(unsequencedBinaryExprs),
        UnseqUtils.toImmutableSideEffectsMap(sideEffectsPerSubExpr));
  }

  @Override
  public String toString() {
    return String.format("[Effects=%s, UnsequencedExprs=%s]", sideEffects, unsequencedBinaryExprs);
  }
}
