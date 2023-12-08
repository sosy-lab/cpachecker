// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.taintanalysis;

import com.google.common.collect.Sets;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;

public class TaintAnalysisUtils {






  /**
   * Collects all variables present in the given pExpression and returns them as {@link
   * CIdExpression}
   *
   * @param pExpression to collect in
   * @return the {@link CIdExpression} present
   */
  public static Set<CIdExpression> getAllVarsAsCExpr(CExpression pExpression) {

    if (pExpression instanceof CBinaryExpression bin) {
      return Sets.union(getAllVarsAsCExpr(bin.getOperand1()), getAllVarsAsCExpr(bin.getOperand2()));
    } else if (pExpression instanceof CUnaryExpression) {
      return getAllVarsAsCExpr(((CUnaryExpression) pExpression).getOperand());
    } else if (pExpression instanceof CIdExpression) {
      return Sets.newHashSet((CIdExpression) pExpression);
    }
    return Sets.newHashSet();
  }



  public static CIdExpression getCidExpressionForCVarDec(CVariableDeclaration pDec) {
    return new CIdExpression(pDec.getFileLocation(), pDec);
  }

}

