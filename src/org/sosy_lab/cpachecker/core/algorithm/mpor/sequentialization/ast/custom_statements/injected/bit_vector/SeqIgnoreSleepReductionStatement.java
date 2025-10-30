// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.bit_vector;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.goto_labels.SeqGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control.SeqBranchStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation.BitVectorEvaluationExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public record SeqIgnoreSleepReductionStatement(
    CIdExpression roundMaxVariable,
    BitVectorEvaluationExpression bitVectorEvaluationExpression,
    SeqBlockLabelStatement nextLabel,
    ImmutableList<SeqInjectedStatement> reductionAssumptions,
    CBinaryExpressionBuilder binaryExpressionBuilder)
    implements SeqInjectedBitVectorStatement {

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    // create necessary expressions and statements
    CBinaryExpression roundMaxEqualsZeroExpression =
        binaryExpressionBuilder.buildBinaryExpression(
            roundMaxVariable, SeqIntegerLiteralExpressions.INT_0, BinaryOperator.EQUALS);

    // negate the evaluation expression
    String ifExpression = bitVectorEvaluationExpression.negate();
    SeqGotoStatement gotoNext = new SeqGotoStatement(nextLabel);
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
    Builder<String> elseStatements = ImmutableList.builder();
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
}
