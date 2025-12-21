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
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationFields;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control.MultiControlStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control.SeqMultiControlStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

record NextThreadNondeterministicSimulation(
    MPOROptions options, SequentializationFields fields, SequentializationUtils utils) {

  String buildThreadSimulations() throws UnrecognizedCodeException {
    // the inner multi control statements choose the next statement, e.g. "pc == 1"
    ImmutableMap<CExpression, SeqMultiControlStatement> innerMultiControlStatements =
        buildInnerMultiControlStatements();
    // the outer multi control statement chooses the thread, e.g. "next_thread == 0"
    SeqMultiControlStatement outerMultiControlStatement =
        MultiControlStatementBuilder.buildMultiControlStatementByEncoding(
            options.controlEncodingThread(),
            SeqIdExpressions.NEXT_THREAD,
            // the outer multi control statement never has an assumption
            ImmutableList.of(),
            innerMultiControlStatements,
            utils.binaryExpressionBuilder());
    return outerMultiControlStatement.toASTString();
  }

  private ImmutableMap<CExpression, SeqMultiControlStatement> buildInnerMultiControlStatements()
      throws UnrecognizedCodeException {

    ImmutableMap.Builder<CExpression, SeqMultiControlStatement> rStatements =
        ImmutableMap.builder();
    for (MPORThread thread : fields.clauses.keySet()) {
      CExpression clauseExpression =
          SeqThreadStatementClauseUtil.getStatementExpressionByEncoding(
              options.controlEncodingThread(),
              SeqIdExpressions.NEXT_THREAD,
              thread.id(),
              utils.binaryExpressionBuilder());
      SeqMultiControlStatement multiControlStatement =
          NondeterministicSimulationBuilder.buildSingleThreadSimulation(
              options,
              fields.ghostElements,
              thread,
              fields.clauses.get(thread),
              utils.binaryExpressionBuilder());
      rStatements.put(clauseExpression, multiControlStatement);
    }
    return rStatements.buildOrThrow();
  }
}
