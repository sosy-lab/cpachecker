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
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control.MultiControlStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control.SeqMultiControlStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control.SeqBranchStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

record NextThreadNondeterministicSimulation(
    MPOROptions options,
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> clauses,
    GhostElements ghostElements,
    CBinaryExpressionBuilder binaryExpressionBuilder) {

  /** Creates the control flow statements for all threads based on {@code clauses}. */
  String buildThreadSimulations() throws UnrecognizedCodeException {

    ImmutableMap<CExpression, SeqMultiControlStatement> innerMultiControlStatements =
        buildInnerMultiControlStatements();
    SeqMultiControlStatement outerMultiControlStatement =
        NondeterministicSimulationUtil.buildOuterMultiControlStatement(
            options, innerMultiControlStatements, binaryExpressionBuilder);
    return outerMultiControlStatement.toASTString();
  }

  private ImmutableMap<CExpression, SeqMultiControlStatement> buildInnerMultiControlStatements()
      throws UnrecognizedCodeException {

    ImmutableMap.Builder<CExpression, SeqMultiControlStatement> rStatements =
        ImmutableMap.builder();
    for (MPORThread thread : clauses.keySet()) {
      CExpression clauseExpression =
          SeqThreadStatementClauseUtil.getStatementExpressionByEncoding(
              options.controlEncodingThread(),
              SeqIdExpressions.NEXT_THREAD,
              thread.id(),
              binaryExpressionBuilder);
      SeqMultiControlStatement multiControlStatement =
          buildMultiControlStatement(thread, clauses.get(thread));
      rStatements.put(clauseExpression, multiControlStatement);
    }
    return rStatements.buildOrThrow();
  }

  private SeqMultiControlStatement buildMultiControlStatement(
      MPORThread pThread, ImmutableList<SeqThreadStatementClause> pClauses)
      throws UnrecognizedCodeException {

    ProgramCounterVariables pcVariables = ghostElements.getPcVariables();
    Optional<SeqBranchStatement> pcUnequalExitAssumption =
        NondeterministicSimulationUtil.tryBuildPcUnequalExitAssumption(
            options, pcVariables, pThread);
    Optional<ImmutableList<String>> nextThreadStatements =
        NondeterministicSimulationUtil.buildNextThreadStatementsForThreadSimulationFunction(
            options, pThread, binaryExpressionBuilder);
    ImmutableList<String> precedingStatements =
        MultiControlStatementBuilder.buildPrecedingStatements(
            pcUnequalExitAssumption,
            nextThreadStatements,
            Optional.empty(),
            Optional.empty(),
            Optional.empty());
    CLeftHandSide expression = pcVariables.getPcLeftHandSide(pThread.id());
    ImmutableMap<CExpression, ? extends SeqStatement> expressionClauseMap =
        SeqThreadStatementClauseUtil.mapExpressionToClause(
            options,
            pcVariables.getPcLeftHandSide(pThread.id()),
            pClauses,
            binaryExpressionBuilder);
    return MultiControlStatementBuilder.buildMultiControlStatementByEncoding(
        options.controlEncodingStatement(),
        expression,
        precedingStatements,
        expressionClauseMap,
        binaryExpressionBuilder);
  }

  String buildSingleThreadSimulation(MPORThread pThread) throws UnrecognizedCodeException {

    SeqMultiControlStatement multiControlStatement =
        buildMultiControlStatement(pThread, clauses.get(pThread));
    return multiControlStatement.toASTString();
  }
}
