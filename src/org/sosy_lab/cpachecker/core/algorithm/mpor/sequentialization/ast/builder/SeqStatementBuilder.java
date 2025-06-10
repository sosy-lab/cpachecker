// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.verifier_nondet.VerifierNondetFunctionType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqStatementBuilder {

  public static CExpressionAssignmentStatement buildIncrementStatement(
      CLeftHandSide pLeftHandSide, CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return buildExpressionAssignmentStatement(
        pLeftHandSide,
        pBinaryExpressionBuilder.buildBinaryExpression(
            pLeftHandSide, SeqIntegerLiteralExpression.INT_1, BinaryOperator.PLUS));
  }

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
   * Returns {@code next_thread = __VERIFIER_nondet_{u}int} with {@code uint} for unsigned, {@code
   * int} for signed.
   */
  public static CFunctionCallAssignmentStatement buildNextThreadAssignment(boolean pIsSigned) {
    return new CFunctionCallAssignmentStatement(
        FileLocation.DUMMY,
        SeqIdExpression.NEXT_THREAD,
        pIsSigned
            ? VerifierNondetFunctionType.INT.getFunctionCallExpression()
            : VerifierNondetFunctionType.UINT.getFunctionCallExpression());
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
