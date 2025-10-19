// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationFields;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.multi_control.MultiControlStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.multi_control.SeqMultiControlStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.SeqAssumptionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.ThreadSyncFlags;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class NextThreadAndNumStatementsNondeterministicSimulation {

  static ImmutableList<String> buildThreadSimulations(
      MPOROptions pOptions,
      SequentializationFields pFields,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CFunctionCallAssignmentStatement roundMaxNondetAssignment =
        NondeterministicSimulationUtil.buildRoundMaxNondetAssignment(
            pOptions, SeqIdExpression.ROUND_MAX);
    CFunctionCallStatement roundMaxGreaterZeroAssumption =
        SeqAssumptionBuilder.buildAssumption(buildRoundMaxGreaterZero(pBinaryExpressionBuilder));
    CExpressionAssignmentStatement roundReset = NondeterministicSimulationUtil.buildRoundReset();

    return buildThreadSimulations(
        pOptions,
        pFields,
        roundMaxNondetAssignment,
        roundMaxGreaterZeroAssumption,
        roundReset,
        pBinaryExpressionBuilder);
  }

  static ImmutableList<String> buildThreadSimulation(
      MPOROptions pOptions,
      GhostElements pGhostElements,
      MPORThread pThread,
      ImmutableList<SeqThreadStatementClause> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CFunctionCallAssignmentStatement roundMaxNondetAssignment =
        NondeterministicSimulationUtil.buildRoundMaxNondetAssignment(
            pOptions, SeqIdExpression.ROUND_MAX);
    CFunctionCallStatement roundMaxGreaterZeroAssumption =
        SeqAssumptionBuilder.buildAssumption(buildRoundMaxGreaterZero(pBinaryExpressionBuilder));
    CExpressionAssignmentStatement roundReset = NondeterministicSimulationUtil.buildRoundReset();

    SeqMultiControlStatement multiControlStatement =
        buildSingleThreadMultiControlStatementWithoutCount(
            pOptions,
            pGhostElements,
            pThread,
            roundMaxNondetAssignment,
            roundMaxGreaterZeroAssumption,
            roundReset,
            pClauses,
            pBinaryExpressionBuilder);
    return SeqStringUtil.splitOnNewline(multiControlStatement.toASTString());
  }

  private static ImmutableList<String> buildThreadSimulations(
      MPOROptions pOptions,
      SequentializationFields pFields,
      CFunctionCallAssignmentStatement pRoundMaxNondetAssignment,
      CFunctionCallStatement pRoundMaxGreaterZeroAssumption,
      CExpressionAssignmentStatement pRoundReset,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<String> rLines = ImmutableList.builder();

    ImmutableMap<CExpression, SeqMultiControlStatement> innerMultiControlStatements =
        buildInnerMultiControlStatements(
            pOptions,
            pFields,
            pRoundMaxNondetAssignment,
            pRoundMaxGreaterZeroAssumption,
            pRoundReset,
            pBinaryExpressionBuilder);
    SeqMultiControlStatement outerMultiControlStatement =
        NondeterministicSimulationUtil.buildOuterMultiControlStatement(
            pOptions, innerMultiControlStatements, pBinaryExpressionBuilder);
    rLines.addAll(SeqStringUtil.splitOnNewline(outerMultiControlStatement.toASTString()));

    return rLines.build();
  }

  private static ImmutableMap<CExpression, SeqMultiControlStatement>
      buildInnerMultiControlStatements(
          MPOROptions pOptions,
          SequentializationFields pFields,
          CFunctionCallAssignmentStatement pRoundMaxNondetAssignment,
          CFunctionCallStatement pRoundMaxGreaterZeroAssumption,
          CExpressionAssignmentStatement pRoundReset,
          CBinaryExpressionBuilder pBinaryExpressionBuilder)
          throws UnrecognizedCodeException {

    ImmutableMap.Builder<CExpression, SeqMultiControlStatement> rStatements =
        ImmutableMap.builder();
    for (MPORThread thread : pFields.clauses.keySet()) {
      ImmutableList<SeqThreadStatementClause> clauses = pFields.clauses.get(thread);
      rStatements.put(
          SeqThreadStatementClauseUtil.getStatementExpressionByEncoding(
              pOptions.controlEncodingThread,
              SeqIdExpression.NEXT_THREAD,
              thread.getId(),
              pBinaryExpressionBuilder),
          buildSingleThreadMultiControlStatementWithoutCount(
              pOptions,
              pFields.ghostElements,
              thread,
              pRoundMaxNondetAssignment,
              pRoundMaxGreaterZeroAssumption,
              pRoundReset,
              clauses,
              pBinaryExpressionBuilder));
    }
    return rStatements.buildOrThrow();
  }

  private static SeqMultiControlStatement buildSingleThreadMultiControlStatementWithoutCount(
      MPOROptions pOptions,
      GhostElements pGhostElements,
      MPORThread pThread,
      CFunctionCallAssignmentStatement pRoundMaxNondetAssignment,
      CFunctionCallStatement pRoundMaxGreaterZeroAssumption,
      CExpressionAssignmentStatement pRoundReset,
      ImmutableList<SeqThreadStatementClause> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList<SeqThreadStatementClause> clauses =
        buildSingleThreadClausesWithoutCount(
            pOptions,
            pThread,
            pGhostElements.getThreadSyncFlags(),
            pClauses,
            pBinaryExpressionBuilder);

    ProgramCounterVariables pcVariables = pGhostElements.getPcVariables();
    CLeftHandSide expression = pcVariables.getPcLeftHandSide(pThread.getId());
    Optional<CFunctionCallStatement> assumption =
        NondeterministicSimulationUtil.tryBuildNextThreadActiveAssumption(
            pOptions, pcVariables, pThread, pBinaryExpressionBuilder);

    ImmutableMap<CExpression, ? extends SeqStatement> expressionClauseMap =
        SeqThreadStatementClauseUtil.mapExpressionToClause(
            pOptions,
            pcVariables.getPcLeftHandSide(pThread.getId()),
            clauses,
            pBinaryExpressionBuilder);

    return MultiControlStatementBuilder.buildMultiControlStatementByEncoding(
        pOptions.controlEncodingStatement,
        expression,
        MultiControlStatementBuilder.buildPrecedingStatements(
            assumption,
            Optional.of(pRoundMaxNondetAssignment),
            Optional.of(pRoundMaxGreaterZeroAssumption),
            Optional.of(pRoundReset)),
        expressionClauseMap,
        pBinaryExpressionBuilder);
  }

  private static ImmutableList<SeqThreadStatementClause> buildSingleThreadClausesWithoutCount(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      ThreadSyncFlags pThreadSyncFlags,
      ImmutableList<SeqThreadStatementClause> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap =
        SeqThreadStatementClauseUtil.mapLabelNumberToClause(pClauses);
    CBinaryExpression roundSmallerK =
        pBinaryExpressionBuilder.buildBinaryExpression(
            SeqIdExpression.ROUND, SeqIdExpression.ROUND_MAX, BinaryOperator.LESS_THAN);
    CExpressionAssignmentStatement roundIncrement =
        SeqStatementBuilder.buildIncrementStatement(
            SeqIdExpression.ROUND, pBinaryExpressionBuilder);

    ImmutableList.Builder<SeqThreadStatementClause> updatedClauses = ImmutableList.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      ImmutableList.Builder<SeqThreadStatementBlock> newBlocks = ImmutableList.builder();
      for (SeqThreadStatementBlock block : clause.getBlocks()) {
        SeqThreadStatementBlock withRoundGoto =
            NondeterministicSimulationUtil.injectRoundGotoIntoBlock(
                pOptions, block, roundSmallerK, roundIncrement, labelClauseMap);
        SeqThreadStatementBlock withSyncUpdate =
            NondeterministicSimulationUtil.injectSyncUpdatesIntoBlock(
                pOptions,
                withRoundGoto,
                pThreadSyncFlags.getSyncFlag(pActiveThread),
                labelClauseMap);
        newBlocks.add(withSyncUpdate);
      }
      updatedClauses.add(clause.cloneWithBlocks(newBlocks.build()));
    }
    return updatedClauses.build();
  }

  // Helpers =======================================================================================

  /** Returns the expression for {@code round_max > 0} */
  private static CBinaryExpression buildRoundMaxGreaterZero(
      CBinaryExpressionBuilder pBinaryExpressionBuilder) throws UnrecognizedCodeException {

    return pBinaryExpressionBuilder.buildBinaryExpression(
        SeqIdExpression.ROUND_MAX, SeqIntegerLiteralExpression.INT_0, BinaryOperator.GREATER_THAN);
  }
}
