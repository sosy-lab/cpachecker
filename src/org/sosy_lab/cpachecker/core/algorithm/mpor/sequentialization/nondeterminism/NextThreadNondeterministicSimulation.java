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
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationFields;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control.MultiControlStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control.SeqMultiControlStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class NextThreadNondeterministicSimulation {

  /** Creates the control flow statements for all threads based on {@code pClauses}. */
  static String buildThreadSimulations(
      MPOROptions pOptions,
      SequentializationFields pFields,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableMap<CExpression, SeqMultiControlStatement> innerMultiControlStatements =
        buildInnerMultiControlStatements(
            pOptions,
            pFields.ghostElements.getPcVariables(),
            pFields.clauses,
            pBinaryExpressionBuilder);
    SeqMultiControlStatement outerMultiControlStatement =
        NondeterministicSimulationUtil.buildOuterMultiControlStatement(
            pOptions, innerMultiControlStatements, pBinaryExpressionBuilder);
    return outerMultiControlStatement.toASTString();
  }

  private static ImmutableMap<CExpression, SeqMultiControlStatement>
      buildInnerMultiControlStatements(
          MPOROptions pOptions,
          ProgramCounterVariables pPcVariables,
          ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
          CBinaryExpressionBuilder pBinaryExpressionBuilder)
          throws UnrecognizedCodeException {

    ImmutableMap.Builder<CExpression, SeqMultiControlStatement> rStatements =
        ImmutableMap.builder();
    for (MPORThread thread : pClauses.keySet()) {
      CExpression clauseExpression =
          SeqThreadStatementClauseUtil.getStatementExpressionByEncoding(
              pOptions.controlEncodingThread,
              SeqIdExpressions.NEXT_THREAD,
              thread.getId(),
              pBinaryExpressionBuilder);
      SeqMultiControlStatement multiControlStatement =
          buildMultiControlStatement(
              pOptions, pPcVariables, thread, pClauses.get(thread), pBinaryExpressionBuilder);
      rStatements.put(clauseExpression, multiControlStatement);
    }
    return rStatements.buildOrThrow();
  }

  private static SeqMultiControlStatement buildMultiControlStatement(
      MPOROptions pOptions,
      ProgramCounterVariables pPcVariables,
      MPORThread pThread,
      ImmutableList<SeqThreadStatementClause> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    Optional<CFunctionCallStatement> pcUnequalExitAssumption =
        NondeterministicSimulationUtil.tryBuildPcUnequalExitAssumption(
            pOptions, pPcVariables, pThread, pBinaryExpressionBuilder);
    Optional<ImmutableList<CStatement>> nextThreadStatements =
        NondeterministicSimulationUtil.buildNextThreadStatementsForThreadSimulationFunction(
            pOptions, pThread, pBinaryExpressionBuilder);
    ImmutableList<CStatement> precedingStatements =
        MultiControlStatementBuilder.buildPrecedingStatements(
            pcUnequalExitAssumption,
            nextThreadStatements,
            Optional.empty(),
            Optional.empty(),
            Optional.empty());
    CLeftHandSide expression = pPcVariables.getPcLeftHandSide(pThread.getId());
    ImmutableMap<CExpression, ? extends SeqStatement> expressionClauseMap =
        SeqThreadStatementClauseUtil.mapExpressionToClause(
            pOptions,
            pPcVariables.getPcLeftHandSide(pThread.getId()),
            pClauses,
            pBinaryExpressionBuilder);
    return MultiControlStatementBuilder.buildMultiControlStatementByEncoding(
        pOptions.controlEncodingStatement,
        expression,
        precedingStatements,
        expressionClauseMap,
        pBinaryExpressionBuilder);
  }

  static String buildSingleThreadSimulation(
      MPOROptions pOptions,
      ProgramCounterVariables pPcVariables,
      MPORThread pThread,
      ImmutableList<SeqThreadStatementClause> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    SeqMultiControlStatement multiControlStatement =
        buildMultiControlStatement(
            pOptions, pPcVariables, pThread, pClauses, pBinaryExpressionBuilder);
    return multiControlStatement.toASTString();
  }
}
