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
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationFields;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control.MultiControlStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control.SeqMultiControlStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.SeqAssumptionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.ThreadSyncFlags;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class NextThreadAndNumStatementsNondeterministicSimulation {

  static String buildThreadSimulations(
      MPOROptions pOptions,
      SequentializationFields pFields,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    StringBuilder rLines = new StringBuilder();

    ImmutableMap<CExpression, SeqMultiControlStatement> innerMultiControlStatements =
        buildInnerMultiControlStatements(pOptions, pFields, pBinaryExpressionBuilder);
    SeqMultiControlStatement outerMultiControlStatement =
        NondeterministicSimulationUtil.buildOuterMultiControlStatement(
            pOptions, innerMultiControlStatements, pBinaryExpressionBuilder);
    rLines.append(outerMultiControlStatement.toASTString());

    return rLines.toString();
  }

  static String buildSingleThreadSimulation(
      MPOROptions pOptions,
      GhostElements pGhostElements,
      MPORThread pThread,
      ImmutableList<SeqThreadStatementClause> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    SeqMultiControlStatement multiControlStatement =
        buildSingleThreadMultiControlStatementWithoutCount(
            pOptions, pGhostElements, pThread, pClauses, pBinaryExpressionBuilder);
    return multiControlStatement.toASTString();
  }

  private static ImmutableMap<CExpression, SeqMultiControlStatement>
      buildInnerMultiControlStatements(
          MPOROptions pOptions,
          SequentializationFields pFields,
          CBinaryExpressionBuilder pBinaryExpressionBuilder)
          throws UnrecognizedCodeException {

    ImmutableMap.Builder<CExpression, SeqMultiControlStatement> rStatements =
        ImmutableMap.builder();
    for (MPORThread thread : pFields.clauses.keySet()) {
      ImmutableList<SeqThreadStatementClause> clauses = pFields.clauses.get(thread);
      rStatements.put(
          SeqThreadStatementClauseUtil.getStatementExpressionByEncoding(
              pOptions.controlEncodingThread(),
              SeqIdExpressions.NEXT_THREAD,
              thread.id(),
              pBinaryExpressionBuilder),
          buildSingleThreadMultiControlStatementWithoutCount(
              pOptions, pFields.ghostElements, thread, clauses, pBinaryExpressionBuilder));
    }
    return rStatements.buildOrThrow();
  }

  private static SeqMultiControlStatement buildSingleThreadMultiControlStatementWithoutCount(
      MPOROptions pOptions,
      GhostElements pGhostElements,
      MPORThread pThread,
      ImmutableList<SeqThreadStatementClause> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ProgramCounterVariables pcVariables = pGhostElements.getPcVariables();
    CLeftHandSide expression = pcVariables.getPcLeftHandSide(pThread.id());
    ImmutableList<CStatement> precedingStatements =
        buildPrecedingStatements(pOptions, pGhostElements, pThread, pBinaryExpressionBuilder);
    ImmutableList<SeqThreadStatementClause> clauses =
        buildSingleThreadClausesWithoutCount(
            pOptions,
            pThread,
            pGhostElements.threadSyncFlags(),
            pClauses,
            pBinaryExpressionBuilder);
    ImmutableMap<CExpression, ? extends SeqStatement> expressionClauseMap =
        SeqThreadStatementClauseUtil.mapExpressionToClause(
            pOptions,
            pcVariables.getPcLeftHandSide(pThread.id()),
            clauses,
            pBinaryExpressionBuilder);

    return MultiControlStatementBuilder.buildMultiControlStatementByEncoding(
        pOptions.controlEncodingStatement(),
        expression,
        precedingStatements,
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

    ImmutableList.Builder<SeqThreadStatementClause> updatedClauses = ImmutableList.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      ImmutableList.Builder<SeqThreadStatementBlock> newBlocks = ImmutableList.builder();
      for (SeqThreadStatementBlock block : clause.getBlocks()) {
        SeqThreadStatementBlock withRoundGoto =
            NondeterministicSimulationUtil.injectRoundGotoIntoBlock(
                pOptions, block, labelClauseMap, pBinaryExpressionBuilder);
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
        SeqIdExpressions.ROUND_MAX,
        SeqIntegerLiteralExpressions.INT_0,
        BinaryOperator.GREATER_THAN);
  }

  private static ImmutableList<CStatement> buildPrecedingStatements(
      MPOROptions pOptions,
      GhostElements pGhostElements,
      MPORThread pThread,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    Optional<CFunctionCallStatement> pcUnequalExitAssumption =
        NondeterministicSimulationUtil.tryBuildPcUnequalExitAssumption(
            pOptions, pGhostElements.getPcVariables(), pThread);
    Optional<ImmutableList<CStatement>> nextThreadStatements =
        NondeterministicSimulationUtil.buildNextThreadStatementsForThreadSimulationFunction(
            pOptions, pThread, pBinaryExpressionBuilder);

    CFunctionCallAssignmentStatement roundMaxNondetAssignment =
        SeqStatementBuilder.buildNondetIntegerAssignment(pOptions, SeqIdExpressions.ROUND_MAX);
    CFunctionCallStatement roundMaxGreaterZeroAssumption =
        SeqAssumptionBuilder.buildAssumption(buildRoundMaxGreaterZero(pBinaryExpressionBuilder));
    CExpressionAssignmentStatement roundReset = NondeterministicSimulationUtil.buildRoundReset();

    return MultiControlStatementBuilder.buildPrecedingStatements(
        pcUnequalExitAssumption,
        nextThreadStatements,
        Optional.of(roundMaxNondetAssignment),
        Optional.of(roundMaxGreaterZeroAssumption),
        Optional.of(roundReset));
  }
}
