// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;

public class SeqStatementBuilder {

  /** Returns {@code pLeftHandSide = pRightHandSide;}. */
  public static CExpressionAssignmentStatement buildExpressionAssignmentStatement(
      CLeftHandSide pLeftHandSide, CExpression pRightHandSide) {

    return new CExpressionAssignmentStatement(FileLocation.DUMMY, pLeftHandSide, pRightHandSide);
  }

  public static CFunctionCallAssignmentStatement buildFunctionCallAssignmentStatement(
      CLeftHandSide pLeftHandSide, CFunctionCallExpression pFunctionCallExpression) {

    return new CFunctionCallAssignmentStatement(
        FileLocation.DUMMY, pLeftHandSide, pFunctionCallExpression);
  }

  /**
   * Returns the {@link CExpressionAssignmentStatement} of {@code pc[pThreadId] = pTargetPc;} or
   * {@code pc{pThreadId} = pTargetPc;} for scalarPc.
   */
  public static CExpressionAssignmentStatement buildPcWrite(
      CLeftHandSide pPcLeftHandSide, int pTargetPc) {

    return buildExpressionAssignmentStatement(
        pPcLeftHandSide, SeqExpressionBuilder.buildIntegerLiteralExpression(pTargetPc));
  }
}
