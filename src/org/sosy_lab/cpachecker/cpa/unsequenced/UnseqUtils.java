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
import java.util.HashMap;
import java.util.HashSet;
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

  public static UnseqBehaviorAnalysisState replaceSideEffectBatch(
      SideEffectInfo unresolvedPointer,
      Set<SideEffectInfo> replacements,
      UnseqBehaviorAnalysisState oldState) {
    Map<String, Set<SideEffectInfo>> updated = new HashMap<>();
    for (Map.Entry<String, ImmutableSet<SideEffectInfo>> entry :
        oldState.getSideEffectsInFun().entrySet()) {
      Set<SideEffectInfo> set = new HashSet<>(entry.getValue());
      if (set.remove(unresolvedPointer)) {
        set.addAll(replacements);
      }
      updated.put(entry.getKey(), set);
    }
    return new UnseqBehaviorAnalysisState(
        UnseqUtils.toImmutableSideEffectsMap(updated),
        oldState.getCalledFunctionStack(),
        oldState.getDetectedConflicts(),
        oldState.getTmpToOriginalExprMap(),
        oldState.getLogger());
  }

  /**
   * Convert a Map<K, Set<SideEffectInfo>> into an immutable Map<K, ImmutableSet<SideEffectInfo>>.
   */
  public static <K> ImmutableMap<K, ImmutableSet<SideEffectInfo>> toImmutableSideEffectsMap(
      Map<K, Set<SideEffectInfo>> mutableMap) {
    ImmutableMap.Builder<K, ImmutableSet<SideEffectInfo>> builder = ImmutableMap.builder();
    for (Map.Entry<K, Set<SideEffectInfo>> entry : mutableMap.entrySet()) {
      builder.put(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
    }
    return builder.buildOrThrow();
  }
}
