// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.unsequenced;

import java.util.Map;
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
}
