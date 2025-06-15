// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.nondet_simulations;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.multi_control.MultiControlStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.multi_control.SeqMultiControlStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCodeUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class NextThreadNondeterministicSimulation {

  /** Creates the control flow statements for all threads based on {@code pClauses}. */
  static ImmutableList<LineOfCode> buildThreadSimulations(
      MPOROptions pOptions,
      PcVariables pPcVariables,
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList<SeqMultiControlStatement> innerMultiControlStatements =
        buildInnerMultiControlStatements(
            pOptions, pPcVariables, pClauses, pBinaryExpressionBuilder);
    SeqMultiControlStatement outerMultiControlStatement =
        NondeterministicSimulationUtil.buildOuterMultiControlStatement(
            pOptions, innerMultiControlStatements, pBinaryExpressionBuilder);
    return LineOfCodeUtil.buildLinesOfCode(outerMultiControlStatement.toASTString());
  }

  private static ImmutableList<SeqMultiControlStatement> buildInnerMultiControlStatements(
      MPOROptions pOptions,
      PcVariables pPcVariables,
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqMultiControlStatement> rStatements = ImmutableList.builder();
    for (var entry : pClauses.entrySet()) {
      MPORThread thread = entry.getKey();
      CLeftHandSide expression = pPcVariables.getPcLeftHandSide(thread.id);
      Optional<CFunctionCallStatement> assumption =
          NondeterministicSimulationUtil.buildNextThreadActiveAssumption(
              pOptions, pPcVariables, thread, pBinaryExpressionBuilder);
      rStatements.add(
          MultiControlStatementBuilder.buildMultiControlStatementByEncoding(
              pOptions,
              pOptions.controlEncodingStatement,
              expression,
              assumption,
              entry.getValue(),
              4,
              pBinaryExpressionBuilder));
    }
    return rStatements.build();
  }
}
