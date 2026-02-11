// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions.SeqAssumeFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism.NondeterminismSource;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CLogicalNotExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CCompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExpressionAssignmentStatementWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CGotoStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CIfStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CLabelStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CStatementWrapper;

public class SeqInstrumentationBuilder {

  public static SeqInstrumentation buildBitVectorUpdateStatement(
      CIdExpression pBitVectorVariable, CExportExpression pValue) {

    CExpressionAssignmentStatementWrapper assignmentStatement =
        new CExpressionAssignmentStatementWrapper(pBitVectorVariable, pValue);
    return new SeqInstrumentation(SeqInstrumentationType.BIT_VECTOR_UPDATE, assignmentStatement);
  }

  public static SeqInstrumentation buildBlockLabelStatement(String pName) {
    CLabelStatement labelStatement = new CLabelStatement(pName);
    return new SeqInstrumentation(SeqInstrumentationType.BLOCK_LABEL, labelStatement);
  }

  public static SeqInstrumentation buildGotoBlockLabelStatement(
      CLabelStatement pBlockLabelStatement) {

    CGotoStatement gotoBlockLabel = new CGotoStatement(pBlockLabelStatement);
    return new SeqInstrumentation(SeqInstrumentationType.GOTO_BLOCK_LABEL, gotoBlockLabel);
  }

  public static SeqInstrumentation buildGuardedGotoStatement(
      CExpression pCondition,
      ImmutableList<CStatement> pPrecedingStatements,
      CLabelStatement pLabelStatement) {

    ImmutableList<CExportStatement> ifStatements =
        ImmutableList.<CExportStatement>builder()
            .addAll(pPrecedingStatements.stream().map(s -> new CStatementWrapper(s)).iterator())
            .add(new CGotoStatement(pLabelStatement))
            .build();
    CCompoundStatement compoundStatement = new CCompoundStatement(ifStatements);
    CIfStatement ifStatement =
        new CIfStatement(new CExpressionWrapper(pCondition), compoundStatement);
    return new SeqInstrumentation(SeqInstrumentationType.GUARDED_GOTO, ifStatement);
  }

  public static SeqInstrumentation buildIgnoreSleepReductionStatement(
      CBinaryExpression roundMaxExpression,
      CExportExpression bitVectorEvaluationExpression,
      ImmutableList<SeqInstrumentation> reductionAssumptions,
      SeqBlockLabelStatement targetGoto) {

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
      return new SeqInstrumentation(
          SeqInstrumentationType.IGNORE_SLEEP_REDUCTION, outerIfStatement);
    }

    // reduction assumptions are present -> build else branch with assumptions
    CIfStatement outerIfStatement =
        new CIfStatement(
            new CExpressionWrapper(roundMaxExpression),
            new CCompoundStatement(innerIfStatement),
            new CCompoundStatement(
                reductionAssumptions.stream()
                    .map(a -> a.statement())
                    .collect(ImmutableList.toImmutableList())));
    return new SeqInstrumentation(SeqInstrumentationType.IGNORE_SLEEP_REDUCTION, outerIfStatement);
  }

  public static SeqInstrumentation buildLastThreadUpdateStatement(
      CExpressionAssignmentStatement pLastThreadUpdate) {

    CStatementWrapper lastThreadUpdate = new CStatementWrapper(pLastThreadUpdate);
    return new SeqInstrumentation(SeqInstrumentationType.LAST_THREAD_UPDATE, lastThreadUpdate);
  }

  public static SeqInstrumentation buildLastBitVectorUpdateStatement(
      ImmutableList<CExpressionAssignmentStatement> pLastBitVectorUpdates) {

    ImmutableList.Builder<CExportStatement> exportStatements = ImmutableList.builder();
    for (CExpressionAssignmentStatement lastBitVectorUpdate : pLastBitVectorUpdates) {
      exportStatements.add(new CStatementWrapper(lastBitVectorUpdate));
    }
    return new SeqInstrumentation(
        SeqInstrumentationType.LAST_BIT_VECTOR_UPDATE,
        new CCompoundStatement(exportStatements.build()));
  }

  public static SeqInstrumentation buildProgramCounterUpdate(
      CExpressionAssignmentStatement pPcUpdate) {

    CStatementWrapper pcUpdate = new CStatementWrapper(pPcUpdate);
    return new SeqInstrumentation(SeqInstrumentationType.PROGRAM_COUNTER_UPDATE, pcUpdate);
  }

  public static SeqInstrumentation buildThreadCountUpdateStatement(
      CExpressionAssignmentStatement pThreadCountUpdate) {

    CStatementWrapper threadCountUpdate = new CStatementWrapper(pThreadCountUpdate);
    return new SeqInstrumentation(SeqInstrumentationType.THREAD_COUNT_UPDATE, threadCountUpdate);
  }

  public static SeqInstrumentation buildThreadSyncUpdateStatement(
      CIdExpression pSyncVariable, CIntegerLiteralExpression pValue) {

    CExpressionAssignmentStatement syncUpdate =
        SeqStatementBuilder.buildExpressionAssignmentStatement(pSyncVariable, pValue);
    return new SeqInstrumentation(
        SeqInstrumentationType.THREAD_SYNC_UPDATE, new CStatementWrapper(syncUpdate));
  }

  public static SeqInstrumentation buildUntilConflictReductionStatement(
      NondeterminismSource pNondeterminismSource,
      Optional<CExportExpression> pBitVectorEvaluationExpression,
      CLabelStatement pTargetGoto) {

    SeqInstrumentationType type = SeqInstrumentationType.UNTIL_CONFLICT_REDUCTION;
    // no evaluation due to no global accesses -> just goto
    if (pBitVectorEvaluationExpression.isEmpty()) {
      return new SeqInstrumentation(type, new CGotoStatement(pTargetGoto));
    }
    // for next_thread nondeterminism, we use goto instead of assume, if there is no conflict
    if (pNondeterminismSource.equals(NondeterminismSource.NEXT_THREAD)) {
      CLogicalNotExpression ifExpression = pBitVectorEvaluationExpression.orElseThrow().negate();
      CGotoStatement gotoStatement = new CGotoStatement(pTargetGoto);
      CCompoundStatement compoundStatement = new CCompoundStatement(gotoStatement);
      return new SeqInstrumentation(type, new CIfStatement(ifExpression, compoundStatement));
    }
    return new SeqInstrumentation(
        type,
        SeqAssumeFunction.buildAssumeFunctionCallStatement(
            pBitVectorEvaluationExpression.orElseThrow()));
  }
}
