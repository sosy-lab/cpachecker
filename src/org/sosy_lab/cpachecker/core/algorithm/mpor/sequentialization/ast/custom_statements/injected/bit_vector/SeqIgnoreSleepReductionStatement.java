// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.bit_vector;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.goto_labels.SeqGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control.SeqIfStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control.SingleControlStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation.BitVectorEvaluationExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqIgnoreSleepReductionStatement implements SeqInjectedBitVectorStatement {

  private final CIdExpression roundMaxVariable;

  private final BitVectorEvaluationExpression bitVectorEvaluationExpression;

  private final SeqBlockLabelStatement nextLabel;

  private final ImmutableList<SeqInjectedStatement> reductionAssumptions;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  public SeqIgnoreSleepReductionStatement(
      CIdExpression pRoundMaxVariable,
      BitVectorEvaluationExpression pBitVectorEvaluationExpression,
      SeqBlockLabelStatement pNextLabel,
      ImmutableList<SeqInjectedStatement> pReductionAssumptions,
      CBinaryExpressionBuilder pBinaryExpressionBuilder) {

    roundMaxVariable = pRoundMaxVariable;
    bitVectorEvaluationExpression = pBitVectorEvaluationExpression;
    nextLabel = pNextLabel;
    reductionAssumptions = pReductionAssumptions;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    if (reductionAssumptions.isEmpty()) {
      return buildStringWithoutAssumptions();
    }
    return buildStringWithAssumptions();
  }

  private String buildStringWithAssumptions() throws UnrecognizedCodeException {
    // create string
    StringBuilder statement = new StringBuilder();
    statement.append(buildStringWithoutAssumptions());
    statement.append(
        SeqStringUtil.appendCurlyBracketLeft(SingleControlStatementType.ELSE.getKeyword()));
    statement.append(SeqSyntax.NEWLINE);
    for (SeqInjectedStatement reductionAssumption : reductionAssumptions) {
      statement.append(reductionAssumption.toASTString());
    }
    statement.append(SeqSyntax.NEWLINE);
    statement.append(SeqSyntax.CURLY_BRACKET_RIGHT);
    return statement.toString();
  }

  private String buildStringWithoutAssumptions() throws UnrecognizedCodeException {
    // create necessary expressions and statements
    CBinaryExpression roundMaxEqualsZeroExpression =
        binaryExpressionBuilder.buildBinaryExpression(
            roundMaxVariable, SeqIntegerLiteralExpressions.INT_0, BinaryOperator.EQUALS);
    // negate the evaluation expression
    CExpression ifExpression = bitVectorEvaluationExpression.negate();
    SeqGotoStatement gotoNext = new SeqGotoStatement(nextLabel);
    SeqIfStatement ifStatement =
        new SeqIfStatement(ifExpression, ImmutableList.of(gotoNext.toASTString()));
    // create string
    return Joiner.on(SeqSyntax.NEWLINE)
        .join(
            SingleControlStatementType.IF.buildControlFlowPrefix(roundMaxEqualsZeroExpression),
            ifStatement.toASTString(),
            SeqSyntax.CURLY_BRACKET_RIGHT);
  }

  public SeqIgnoreSleepReductionStatement cloneWithGotoLabelNumber(int pLabelNumber) {
    return new SeqIgnoreSleepReductionStatement(
        roundMaxVariable,
        bitVectorEvaluationExpression,
        nextLabel.cloneWithLabelNumber(pLabelNumber),
        reductionAssumptions,
        binaryExpressionBuilder);
  }

  public SeqIgnoreSleepReductionStatement cloneWithReductionAssumptions(
      ImmutableList<SeqInjectedStatement> pReductionAssumptions) {

    return new SeqIgnoreSleepReductionStatement(
        roundMaxVariable,
        bitVectorEvaluationExpression,
        nextLabel,
        pReductionAssumptions,
        binaryExpressionBuilder);
  }

  // Getters =======================================================================================

  public ImmutableList<SeqInjectedStatement> getReductionAssumptions() {
    return reductionAssumptions;
  }
}
