// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
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
    public static CExpressionAssignmentStatement buildPcWrite(
        CLeftHandSide pPcLeftHandSide, int pTargetPc) {

      return build(
          pPcLeftHandSide, SeqIntegerLiteralExpression.buildIntegerLiteralExpression(pTargetPc));
    }

    public static CExpressionAssignmentStatement buildPcWrite(
        CLeftHandSide pPcLeftHandSide, CExpression pRightHandSide) {

      return build(pPcLeftHandSide, pRightHandSide);
    }

    public static CExpressionAssignmentStatement buildReturnPcWriteByTargetPc(
        CIdExpression pReturnPcVariable,
        Optional<Integer> pReturnPc,
        Optional<CExpression> pReturnPcExpression) {

      // this is XOR, not (true, true) and not (false, false)
      checkArgument(
          pReturnPc.isPresent() || pReturnPcExpression.isPresent(),
          "either pReturnPc or pReturnPcExpression must be present");
      checkArgument(
          pReturnPc.isEmpty() || pReturnPcExpression.isEmpty(),
          "either pReturnPc or pReturnPcExpression must be empty");
      return pReturnPc.isPresent()
          ? build(
              pReturnPcVariable,
              SeqIntegerLiteralExpression.buildIntegerLiteralExpression(pReturnPc.orElseThrow()))
          : build(pReturnPcVariable, pReturnPcExpression.orElseThrow());
    }
  }
}
