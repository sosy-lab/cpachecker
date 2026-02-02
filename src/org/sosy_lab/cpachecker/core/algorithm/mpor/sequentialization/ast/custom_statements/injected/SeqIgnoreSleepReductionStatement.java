// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CLogicalNotExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CCompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CGotoStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CIfStatement;

public record SeqIgnoreSleepReductionStatement(
    CIdExpression roundMaxVariable,
    CExportExpression bitVectorEvaluationExpression,
    ImmutableList<CExportStatement> reductionAssumptions,
    CBinaryExpressionBuilder binaryExpressionBuilder,
    SeqBlockLabelStatement targetGoto)
    implements SeqInjectedStatementWithTargetGoto {

  @Override
  public ImmutableList<CExportStatement> toCExportStatements() throws UnrecognizedCodeException {
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
      return ImmutableList.of(outerIfStatement);
    }

    // reduction assumptions are present -> build else branch with assumptions
    CIfStatement outerIfStatement =
        new CIfStatement(
            new CExpressionWrapper(roundMaxEqualsZeroExpression),
            new CCompoundStatement(innerIfStatement),
            new CCompoundStatement(reductionAssumptions));
    return ImmutableList.of(outerIfStatement);
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
      ImmutableList<CExportStatement> pReductionAssumptions) {

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
