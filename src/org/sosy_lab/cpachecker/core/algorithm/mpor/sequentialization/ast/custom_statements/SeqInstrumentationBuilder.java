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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions.SeqAssumeFunctionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism.NondeterminismSource;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatementElement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.CGotoStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CIfStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CLabelStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CLogicalNotExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.CStatementWrapper;

public class SeqInstrumentationBuilder {

  public static SeqInstrumentation buildBitVectorUpdateStatement(
      CIdExpression pBitVectorVariable, CIntegerLiteralExpression pValue) {

    CExpressionAssignmentStatement assignmentStatement =
        new CExpressionAssignmentStatement(FileLocation.DUMMY, pBitVectorVariable, pValue);
    return new SeqInstrumentation(
        SeqInstrumentationType.BIT_VECTOR_UPDATE, new CStatementWrapper(assignmentStatement));
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

    ImmutableList<CCompoundStatementElement> ifStatements =
        ImmutableList.<CCompoundStatementElement>builder()
            .addAll(pPrecedingStatements.stream().map(s -> new CStatementWrapper(s)).iterator())
            .add(new CGotoStatement(pLabelStatement))
            .build();
    CCompoundStatement compoundStatement = new CCompoundStatement(ifStatements);
    CIfStatement ifStatement =
        new CIfStatement(new CExpressionWrapper(pCondition), compoundStatement);
    return new SeqInstrumentation(SeqInstrumentationType.GUARDED_GOTO, ifStatement);
  }

  public static SeqInstrumentation buildLastThreadUpdateStatement(
      CExpressionAssignmentStatement pLastThreadUpdate) {

    CStatementWrapper lastThreadUpdate = new CStatementWrapper(pLastThreadUpdate);
    return new SeqInstrumentation(SeqInstrumentationType.LAST_THREAD_UPDATE, lastThreadUpdate);
  }

  public static SeqInstrumentation buildLastBitVectorUpdateStatement(
      ImmutableList<CExpressionAssignmentStatement> pLastBitVectorUpdates) {

    ImmutableList.Builder<CCompoundStatementElement> exportStatements = ImmutableList.builder();
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
        SeqAssumeFunctionBuilder.buildAssumeFunctionCallStatement(
            pBitVectorEvaluationExpression.orElseThrow()));
  }
}
