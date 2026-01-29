// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExpressionTree;
import org.sosy_lab.cpachecker.util.cwriter.export.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.CGotoStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CIfStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CLogicalNotExpression;

public record SeqIgnoreSleepReductionStatement(
    CIdExpression roundMaxVariable,
    CExpressionTree bitVectorEvaluationExpression,
    ImmutableList<SeqInjectedStatement> reductionAssumptions,
    CBinaryExpressionBuilder binaryExpressionBuilder,
    SeqBlockLabelStatement targetGoto)
    implements SeqInjectedStatementWithTargetGoto {

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    // create necessary expressions and statements
    CBinaryExpression roundMaxEqualsZeroExpression =
        binaryExpressionBuilder.buildBinaryExpression(
            roundMaxVariable, SeqIntegerLiteralExpressions.INT_0, BinaryOperator.EQUALS);

    // negate the evaluation expression
    CLogicalNotExpression ifExpression = bitVectorEvaluationExpression.negate();
    CGotoStatement gotoNext = new CGotoStatement(targetGoto.toCLabelStatement());
    CCompoundStatement compoundStatement = new CCompoundStatement(gotoNext);
    CIfStatement innerIfStatement = new CIfStatement(ifExpression, compoundStatement);

    if (reductionAssumptions.isEmpty()) {
      // no reduction assumptions -> just return outer if statement
      CIfStatement outerIfStatement =
          new CIfStatement(
              new CExpressionWrapper(roundMaxEqualsZeroExpression),
              new CCompoundStatement(innerIfStatement));
      return outerIfStatement.toASTString();
    }

    // reduction assumptions are present -> build else branch with assumptions
    ImmutableList.Builder<CExportStatement> elseStatements = ImmutableList.builder();
    for (SeqInjectedStatement reductionAssumption : reductionAssumptions) {
      elseStatements.add(reductionAssumption);
    }
    CIfStatement outerIfStatement =
        new CIfStatement(
            new CExpressionWrapper(roundMaxEqualsZeroExpression),
            new CCompoundStatement(innerIfStatement),
            new CCompoundStatement(elseStatements.build()));
    return outerIfStatement.toASTString(pAAstNodeRepresentation);
  }

  @Override
  public SeqInjectedStatementWithTargetGoto withTargetNumber(int pTargetNumber) {
    return new SeqIgnoreSleepReductionStatement(
        roundMaxVariable,
        bitVectorEvaluationExpression,
        reductionAssumptions,
        binaryExpressionBuilder,
        targetGoto.withLabelNumber(pTargetNumber));
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

  @Override
  public boolean isPrunedWithTargetGoto() {
    return true;
  }

  @Override
  public boolean isPrunedWithEmptyBitVectorEvaluation() {
    return false;
  }
}
