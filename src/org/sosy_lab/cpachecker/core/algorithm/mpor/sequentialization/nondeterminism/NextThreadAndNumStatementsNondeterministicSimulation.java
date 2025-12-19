// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control.SeqMultiControlStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

record NextThreadAndNumStatementsNondeterministicSimulation(
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
    return NondeterministicSimulationUtil.buildSingleThreadMultiControlStatement(
            options, ghostElements, pThread, clauses.get(pThread), binaryExpressionBuilder)
        .toASTString();
  }

  private ImmutableMap<CExpression, SeqMultiControlStatement> buildInnerMultiControlStatements()
      throws UnrecognizedCodeException {

    ImmutableMap.Builder<CExpression, SeqMultiControlStatement> rStatements =
        ImmutableMap.builder();
    for (MPORThread thread : clauses.keySet()) {
      SeqMultiControlStatement singleThreadSimulation =
          NondeterministicSimulationUtil.buildSingleThreadMultiControlStatement(
              options, ghostElements, thread, clauses.get(thread), binaryExpressionBuilder);
      rStatements.put(
          SeqThreadStatementClauseUtil.getStatementExpressionByEncoding(
              options.controlEncodingThread(),
              SeqIdExpressions.NEXT_THREAD,
              thread.id(),
              binaryExpressionBuilder),
          singleThreadSimulation);
    }
    return rStatements.buildOrThrow();
  }
}
