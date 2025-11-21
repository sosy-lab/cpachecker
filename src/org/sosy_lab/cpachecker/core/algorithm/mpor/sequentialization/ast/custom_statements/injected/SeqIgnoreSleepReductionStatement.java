// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.gotos.SeqGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control.SeqBranchStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation.BitVectorEvaluationExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public final class SeqIgnoreSleepReductionStatement extends SeqInjectedStatement {

  private final CIdExpression roundMaxVariable;

  private final BitVectorEvaluationExpression bitVectorEvaluationExpression;

  public final ImmutableList<SeqInjectedStatement> reductionAssumptions;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  private final SeqBlockLabelStatement targetGoto;

  public SeqIgnoreSleepReductionStatement(
      CIdExpression pRoundMaxVariable,
      BitVectorEvaluationExpression pBitVectorEvaluationExpression,
      ImmutableList<SeqInjectedStatement> pReductionAssumptions,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      SeqBlockLabelStatement pTargetGoto) {

    roundMaxVariable = pRoundMaxVariable;
    bitVectorEvaluationExpression = pBitVectorEvaluationExpression;
    reductionAssumptions = pReductionAssumptions;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
    targetGoto = pTargetGoto;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    // create necessary expressions and statements
    CBinaryExpression roundMaxEqualsZeroExpression =
        binaryExpressionBuilder.buildBinaryExpression(
            roundMaxVariable, SeqIntegerLiteralExpressions.INT_0, BinaryOperator.EQUALS);

    // negate the evaluation expression
    String ifExpression = bitVectorEvaluationExpression.toNegatedASTString();
    SeqGotoStatement gotoNext = new SeqGotoStatement(targetGoto);
    SeqBranchStatement innerIfStatement =
        new SeqBranchStatement(ifExpression, ImmutableList.of(gotoNext.toASTString()));

    if (reductionAssumptions.isEmpty()) {
      // no reduction assumptions -> just return outer if statement
      SeqBranchStatement outerIfStatement =
          new SeqBranchStatement(
              roundMaxEqualsZeroExpression.toASTString(),
              ImmutableList.of(innerIfStatement.toASTString()));
      return outerIfStatement.toASTString();
    }

    // reduction assumptions are present -> build else branch with assumptions
    ImmutableList.Builder<String> elseStatements = ImmutableList.builder();
    for (SeqInjectedStatement reductionAssumption : reductionAssumptions) {
      elseStatements.add(reductionAssumption.toASTString());
    }
    SeqBranchStatement outerIfStatement =
        new SeqBranchStatement(
            roundMaxEqualsZeroExpression.toASTString(),
            ImmutableList.of(innerIfStatement.toASTString()),
            elseStatements.build());
    return outerIfStatement.toASTString();
  }

  @Override
  public Optional<SeqBlockLabelStatement> getTargetGoto() {
    return Optional.of(targetGoto);
  }

  @Override
  public SeqIgnoreSleepReductionStatement withLabelNumber(int pLabelNumber) {
    return new SeqIgnoreSleepReductionStatement(
        roundMaxVariable,
        bitVectorEvaluationExpression,
        reductionAssumptions,
        binaryExpressionBuilder,
        targetGoto.withLabelNumber(pLabelNumber));
  }

  public SeqIgnoreSleepReductionStatement withReductionAssumptions(
      ImmutableList<SeqInjectedStatement> pReductionAssumptions) {

    return new SeqIgnoreSleepReductionStatement(
        roundMaxVariable,
        bitVectorEvaluationExpression,
        pReductionAssumptions,
        binaryExpressionBuilder,
        targetGoto);
  }
}
