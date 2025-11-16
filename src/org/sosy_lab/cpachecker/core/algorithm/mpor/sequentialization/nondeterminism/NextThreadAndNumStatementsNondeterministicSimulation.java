// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public record NextThreadAndNumStatementsNondeterministicSimulation(
    MPOROptions options,
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> clauses,
    GhostElements ghostElements,
    CBinaryExpressionBuilder binaryExpressionBuilder) {

  String buildThreadSimulations() throws UnrecognizedCodeException {
    StringBuilder rLines = new StringBuilder();
    ImmutableMap<CExpression, SeqMultiControlStatement> innerMultiControlStatements =
        buildInnerMultiControlStatements();
    SeqMultiControlStatement outerMultiControlStatement =
        NondeterministicSimulationUtil.buildOuterMultiControlStatement(
            options, innerMultiControlStatements, binaryExpressionBuilder);
    rLines.append(outerMultiControlStatement.toASTString());

    return rLines.toString();
  }

  String buildSingleThreadSimulation(MPORThread pThread) throws UnrecognizedCodeException {

    SeqMultiControlStatement multiControlStatement =
        buildSingleThreadMultiControlStatementWithoutCount(pThread, clauses.get(pThread));
    return multiControlStatement.toASTString();
  }

  private ImmutableMap<CExpression, SeqMultiControlStatement> buildInnerMultiControlStatements()
      throws UnrecognizedCodeException {

    ImmutableMap.Builder<CExpression, SeqMultiControlStatement> rStatements =
        ImmutableMap.builder();
    for (MPORThread thread : clauses.keySet()) {
      ImmutableList<SeqThreadStatementClause> currentClauses = clauses.get(thread);
      rStatements.put(
          SeqThreadStatementClauseUtil.getStatementExpressionByEncoding(
              options.controlEncodingThread(),
              SeqIdExpressions.NEXT_THREAD,
              thread.id(),
              binaryExpressionBuilder),
          buildSingleThreadMultiControlStatementWithoutCount(thread, currentClauses));
    }
    return rStatements.buildOrThrow();
  }

  private SeqMultiControlStatement buildSingleThreadMultiControlStatementWithoutCount(
      MPORThread pThread, ImmutableList<SeqThreadStatementClause> pClauses)
      throws UnrecognizedCodeException {

    ProgramCounterVariables pcVariables = ghostElements.getPcVariables();
    CLeftHandSide expression = pcVariables.getPcLeftHandSide(pThread.id());
    ImmutableList<CStatement> precedingStatements = buildPrecedingStatements(pThread);
    ImmutableList<SeqThreadStatementClause> currentClauses =
        buildSingleThreadClausesWithoutCount(pThread, pClauses);
    ImmutableMap<CExpression, ? extends SeqStatement> expressionClauseMap =
        SeqThreadStatementClauseUtil.mapExpressionToClause(
            options,
            pcVariables.getPcLeftHandSide(pThread.id()),
            currentClauses,
            binaryExpressionBuilder);

    return MultiControlStatementBuilder.buildMultiControlStatementByEncoding(
        options.controlEncodingStatement(),
        expression,
        precedingStatements,
        expressionClauseMap,
        binaryExpressionBuilder);
  }

  private ImmutableList<SeqThreadStatementClause> buildSingleThreadClausesWithoutCount(
      MPORThread pActiveThread, ImmutableList<SeqThreadStatementClause> pClauses)
      throws UnrecognizedCodeException {

    ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap =
        SeqThreadStatementClauseUtil.mapLabelNumberToClause(pClauses);

    ImmutableList.Builder<SeqThreadStatementClause> updatedClauses = ImmutableList.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      ImmutableList.Builder<SeqThreadStatementBlock> newBlocks = ImmutableList.builder();
      for (SeqThreadStatementBlock block : clause.getBlocks()) {
        SeqThreadStatementBlock withRoundGoto =
            NondeterministicSimulationUtil.injectRoundGotoIntoBlock(
                options, block, labelClauseMap, binaryExpressionBuilder);
        SeqThreadStatementBlock withSyncUpdate =
            NondeterministicSimulationUtil.injectSyncUpdatesIntoBlock(
                options,
                withRoundGoto,
                ghostElements.threadSyncFlags().getSyncFlag(pActiveThread),
                labelClauseMap);
        newBlocks.add(withSyncUpdate);
      }
      updatedClauses.add(clause.cloneWithBlocks(newBlocks.build()));
    }
    return updatedClauses.build();
  }

  // Helpers =======================================================================================

  /** Returns the expression for {@code round_max > 0} */
  private CBinaryExpression buildRoundMaxGreaterZero() throws UnrecognizedCodeException {
    return binaryExpressionBuilder.buildBinaryExpression(
        SeqIdExpressions.ROUND_MAX,
        SeqIntegerLiteralExpressions.INT_0,
        BinaryOperator.GREATER_THAN);
  }

  private ImmutableList<CStatement> buildPrecedingStatements(MPORThread pThread)
      throws UnrecognizedCodeException {

    Optional<CFunctionCallStatement> pcUnequalExitAssumption =
        NondeterministicSimulationUtil.tryBuildPcUnequalExitAssumption(
            options, ghostElements.getPcVariables(), pThread);
    Optional<ImmutableList<CStatement>> nextThreadStatements =
        NondeterministicSimulationUtil.buildNextThreadStatementsForThreadSimulationFunction(
            options, pThread, binaryExpressionBuilder);

    CFunctionCallAssignmentStatement roundMaxNondetAssignment =
        SeqStatementBuilder.buildNondetIntegerAssignment(options, SeqIdExpressions.ROUND_MAX);
    CFunctionCallStatement roundMaxGreaterZeroAssumption =
        SeqAssumptionBuilder.buildAssumption(buildRoundMaxGreaterZero());
    CExpressionAssignmentStatement roundReset = NondeterministicSimulationUtil.buildRoundReset();

    return MultiControlStatementBuilder.buildPrecedingStatements(
        pcUnequalExitAssumption,
        nextThreadStatements,
        Optional.of(roundMaxNondetAssignment),
        Optional.of(roundMaxGreaterZeroAssumption),
        Optional.of(roundReset));
  }
}
