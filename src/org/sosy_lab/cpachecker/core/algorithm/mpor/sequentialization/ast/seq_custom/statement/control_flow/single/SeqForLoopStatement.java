// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.single;

import static com.google.common.base.Preconditions.checkArgument;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqForLoopStatement implements SeqLoopStatement {

  private final CIdExpression loopCounter;

  private final int iterations;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  /**
   * Creates a for loop header of the form {@code for (int i = 0; i < pIterations; i = i + 1)} where
   * {@code i} is {@code pLoopCounter}.
   */
  public SeqForLoopStatement(
      CIdExpression pLoopCounter,
      int pIterations,
      CBinaryExpressionBuilder pBinaryExpressionBuilder) {

    checkArgument(
        pLoopCounter.getDeclaration() instanceof CVariableDeclaration,
        "pLoopCounters declaration must be CVariableDeclaration");
    loopCounter = pLoopCounter;
    iterations = pIterations;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    StringBuilder loopHeader = new StringBuilder();
    loopHeader.append(loopCounter.getDeclaration()).append(SeqSyntax.SPACE);
    CIntegerLiteralExpression iterationExpression =
        SeqExpressionBuilder.buildIntegerLiteralExpression(iterations);
    CBinaryExpression loopCondition =
        binaryExpressionBuilder.buildBinaryExpression(
            loopCounter, iterationExpression, BinaryOperator.LESS_THAN);
    // add additional semicolon, the binary expression doesn't include it
    loopHeader
        .append(loopCondition.toASTString())
        .append(SeqSyntax.SEMICOLON)
        .append(SeqSyntax.SPACE);
    CExpressionAssignmentStatement iterationIncrement =
        SeqStatementBuilder.buildIncrementStatement(loopCounter, binaryExpressionBuilder);
    String iterationIncrementString = iterationIncrement.toASTString();
    // strip semicolon suffix from assignment (there is no AST without the semicolon)
    String incrementWithoutSemicolon =
        iterationIncrementString.substring(0, iterationIncrementString.length() - 1);
    loopHeader.append(incrementWithoutSemicolon);
    return SeqToken._for + SeqSyntax.SPACE + SeqStringUtil.wrapInBrackets(loopHeader.toString());
  }
}
