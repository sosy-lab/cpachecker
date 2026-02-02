// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CLogicalNotExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CCompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CGotoStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CIfStatement;

public record SeqIgnoreSleepReductionStatement(
    CBinaryExpression roundMaxExpression,
    CExportExpression bitVectorEvaluationExpression,
    ImmutableList<SeqInjectedStatement> reductionAssumptions,
    SeqBlockLabelStatement targetGoto)
    implements SeqInjectedStatementWithTargetGoto {

  @Override
  public ImmutableList<CExportStatement> toCExportStatements() {
    // negate the evaluation expression
    CLogicalNotExpression ifExpression = bitVectorEvaluationExpression.negate();
    CGotoStatement gotoNext = new CGotoStatement(targetGoto.toCLabelStatement());
    CCompoundStatement compoundStatement = new CCompoundStatement(gotoNext);
    CIfStatement innerIfStatement = new CIfStatement(ifExpression, compoundStatement);

    if (reductionAssumptions.isEmpty()) {
      // no reduction assumptions -> just return outer if statement
      CIfStatement outerIfStatement =
          new CIfStatement(
              new CExpressionWrapper(roundMaxExpression), new CCompoundStatement(innerIfStatement));
      return ImmutableList.of(outerIfStatement);
    }

    // reduction assumptions are present -> build else branch with assumptions
    ImmutableList.Builder<CExportStatement> exportReductionAssumptions = ImmutableList.builder();
    for (SeqInjectedStatement assumption : reductionAssumptions) {
      exportReductionAssumptions.addAll(assumption.toCExportStatements());
    }
    CIfStatement outerIfStatement =
        new CIfStatement(
            new CExpressionWrapper(roundMaxExpression),
            new CCompoundStatement(innerIfStatement),
            new CCompoundStatement(exportReductionAssumptions.build()));
    return ImmutableList.of(outerIfStatement);
  }

  @Override
  public SeqInjectedStatementWithTargetGoto withTargetNumber(int pTargetNumber) {
    return new SeqIgnoreSleepReductionStatement(
        roundMaxExpression,
        bitVectorEvaluationExpression,
        reductionAssumptions,
        targetGoto.withLabelNumber(pTargetNumber));
  }

  public SeqIgnoreSleepReductionStatement withReductionAssumptions(
      ImmutableList<SeqInjectedStatement> pReductionAssumptions) {

    return new SeqIgnoreSleepReductionStatement(
        roundMaxExpression, bitVectorEvaluationExpression, pReductionAssumptions, targetGoto);
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
