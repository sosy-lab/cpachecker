// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIntegerLiteralExpression;

public class SeqStatements {

  public static class SeqExpressionAssignmentStatement {

    /** Returns {@code pLeftHandSide = pRightHandSide;}. */
    public static CExpressionAssignmentStatement build(
        CLeftHandSide pLeftHandSide, CExpression pRightHandSide) {

      return new CExpressionAssignmentStatement(FileLocation.DUMMY, pLeftHandSide, pRightHandSide);
    }

    /**
     * Returns the {@link CExpressionAssignmentStatement} of {@code pc[pThreadId] = pTargetPc;} or
     * {@code pc{pThreadId} = pTargetPc;} for scalarPc.
     */
    public static CExpressionAssignmentStatement buildPcWrite(int pThreadId, int pTargetPc) {
      return buildPcWrite(
          pThreadId, SeqIntegerLiteralExpression.buildIntegerLiteralExpression(pTargetPc));
    }

    /**
     * Returns the {@link CExpressionAssignmentStatement} of {@code pc[pThreadId] = pRightHandSide;}
     * or {@code pc{pThreadId} = pRightHandSide;} for scalarPc.
     */
    public static CExpressionAssignmentStatement buildPcWrite(
        int pThreadId, CExpression pRightHandSide) {

      return build(SeqExpressions.getPcExpression(pThreadId), pRightHandSide);
    }
  }
}
