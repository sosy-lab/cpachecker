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
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;

public class UnseqUtils {

  private UnseqUtils() {}

  public static String replaceTmpInExpression(
      CRightHandSide expr, UnseqBehaviorAnalysisState pState) {

    String replacedExpr = expr.toASTString();

    for (Map.Entry<String, CRightHandSide> entry : pState.getTmpToOriginalExprMap().entrySet()) {
      String fullTmpName = entry.getKey();

      String shortTmpName = fullTmpName.substring(fullTmpName.lastIndexOf("::") + 2);
      String originalExpr = entry.getValue().toASTString();

      if (replacedExpr.contains(shortTmpName)) {
        replacedExpr = replacedExpr.replace(shortTmpName, originalExpr);
      }
    }

    return replacedExpr;
  }

  public static ImmutableMap<String, ImmutableSet<SideEffectInfo>> toImmutableSideEffectsMap(
      Map<String, Set<SideEffectInfo>> mutableMap) {

    ImmutableMap.Builder<String, ImmutableSet<SideEffectInfo>> builder = ImmutableMap.builder();
    for (Map.Entry<String, Set<SideEffectInfo>> entry : mutableMap.entrySet()) {
      builder.put(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
    }
    return builder.buildOrThrow();
  }
}
