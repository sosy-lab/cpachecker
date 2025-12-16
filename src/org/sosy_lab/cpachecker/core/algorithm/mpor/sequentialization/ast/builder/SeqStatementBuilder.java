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
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqStatementBuilder {

  // Increment / Decrement =========================================================================

  public static CExpressionAssignmentStatement buildIncrementStatement(
      CLeftHandSide pLeftHandSide, CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return buildExpressionAssignmentStatement(
        pLeftHandSide,
        pBinaryExpressionBuilder.buildBinaryExpression(
            pLeftHandSide, SeqIntegerLiteralExpressions.INT_1, BinaryOperator.PLUS));
  }

  public static CExpressionAssignmentStatement buildDecrementStatement(
      CLeftHandSide pLeftHandSide, CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return buildExpressionAssignmentStatement(
        pLeftHandSide,
        pBinaryExpressionBuilder.buildBinaryExpression(
            pLeftHandSide, SeqIntegerLiteralExpressions.INT_1, BinaryOperator.MINUS));
  }

  // Assignments ===================================================================================

  /** Returns {@code pLeftHandSide = pRightHandSide;}. */
  public static CExpressionAssignmentStatement buildExpressionAssignmentStatement(
      CLeftHandSide pLeftHandSide, CExpression pRightHandSide) {

    return new CExpressionAssignmentStatement(FileLocation.DUMMY, pLeftHandSide, pRightHandSide);
  }
}
