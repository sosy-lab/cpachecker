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

    public static CExpressionAssignmentStatement buildPcWriteByTargetPc(
        CLeftHandSide pPcLeftHandSide,
        Optional<Integer> pTargetPc,
        Optional<CIdExpression> pTargetPcExpression) {

      // this is XOR, not (true, true) and not (false, false)
      checkArgument(
          pTargetPc.isPresent() || pTargetPcExpression.isPresent(),
          "either pTargetPc or pTargetPcExpression must be present");
      checkArgument(
          pTargetPc.isEmpty() || pTargetPcExpression.isEmpty(),
          "either pTargetPc or pTargetPcExpression must be empty");
      return pTargetPc.isPresent()
          ? buildPcWrite(pPcLeftHandSide, pTargetPc.orElseThrow())
          : buildPcWrite(pPcLeftHandSide, pTargetPcExpression.orElseThrow());
    }
  }
}
