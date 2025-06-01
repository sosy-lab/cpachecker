// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.unsequenced;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;

public class ExpressionAnalysisSummary {

  private final Set<SideEffectInfo> sideEffects = new HashSet<>();
  private final Set<CBinaryExpression> unsequencedBinaryExprs = new HashSet<>();
  private final Map<CRightHandSide, Set<SideEffectInfo>> sideEffectsPerSubExpr =
      new HashMap<>(); // tracking side effects per argument

  public static ExpressionAnalysisSummary empty() {
    return new ExpressionAnalysisSummary();
  }

  public Set<SideEffectInfo> getSideEffects() {
    return sideEffects;
  }

  public Set<CBinaryExpression> getUnsequencedBinaryExprs() {
    return unsequencedBinaryExprs;
  }

  public void addSideEffect(SideEffectInfo effect) {
    sideEffects.add(effect);
  }

  public void addSideEffects(Set<SideEffectInfo> effects) {
    sideEffects.addAll(effects);
  }

  public void addUnsequencedBinaryExpr(CBinaryExpression expr) {
    unsequencedBinaryExprs.add(expr);
  }

  public void addUnsequencedBinaryExprs(Set<CBinaryExpression> exprs) {
    unsequencedBinaryExprs.addAll(exprs);
  }

  public Map<CRightHandSide, Set<SideEffectInfo>> getSideEffectsPerSubExpr() {
    return sideEffectsPerSubExpr;
  }

  /** Add side effects for arguments */
  public void addSideEffectsForSubExprs(
      Map<CRightHandSide, Set<SideEffectInfo>> pEffectsPerSubExpr) {
    for (Map.Entry<CRightHandSide, Set<SideEffectInfo>> entry : pEffectsPerSubExpr.entrySet()) {
      CRightHandSide expr = entry.getKey();
      Set<SideEffectInfo> effects = entry.getValue();
      if (!effects.isEmpty()) {
        sideEffectsPerSubExpr.computeIfAbsent(expr, k -> new HashSet<>()).addAll(effects);
      }
    }
  }

  @Override
  public String toString() {
    return String.format("[Effects=%s, UnsequencedExprs=%s]", sideEffects, unsequencedBinaryExprs);
  }
}
