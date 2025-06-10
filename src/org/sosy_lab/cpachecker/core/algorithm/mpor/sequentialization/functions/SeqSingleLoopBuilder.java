// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.assumptions.SeqAssumptionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.SeqBinaryIfTreeStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.SeqMultiControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.SeqSwitchStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.function_call.SeqFunctionCallStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCodeUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqSingleLoopBuilder {

  /** Creates the control flow statements for all threads based on {@code pClauses}. */
  protected static ImmutableList<LineOfCode> buildSingleLoopThreadSimulationStatements(
      MPOROptions pOptions,
      PcVariables pPcVariables,
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqMultiControlFlowStatement> controlFlow = ImmutableList.builder();
    for (var entry : pClauses.entrySet()) {
      MPORThread thread = entry.getKey();
      controlFlow.add(
          buildSingleLoopMultiControlFlowStatement(
              pOptions, pPcVariables, thread, entry.getValue(), 4, pBinaryExpressionBuilder));
    }
    return switch (pOptions.controlFlowEncoding) {
      case SWITCH_CASE -> {
        SeqSwitchStatement switchStatement =
            new SeqSwitchStatement(
                pOptions, SeqIdExpression.NEXT_THREAD, Optional.empty(), controlFlow.build(), 2);
        yield LineOfCodeUtil.buildLinesOfCode(switchStatement.toASTString());
      }
      case BINARY_IF_TREE -> {
        SeqBinaryIfTreeStatement treeStatement =
            new SeqBinaryIfTreeStatement(
                SeqIdExpression.NEXT_THREAD, controlFlow.build(), 2, pBinaryExpressionBuilder);
        yield LineOfCodeUtil.buildLinesOfCode(treeStatement.toASTString());
      }
    };
  }

  /** Creates the {@link SeqMultiControlFlowStatement} for {@code pThread}. */
  private static SeqMultiControlFlowStatement buildSingleLoopMultiControlFlowStatement(
      MPOROptions pOptions,
      PcVariables pPcVariables,
      MPORThread pThread,
      ImmutableList<SeqThreadStatementClause> pClauses,
      int pTabs,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CLeftHandSide pcExpression = pPcVariables.getPcLeftHandSide(pThread.id);
    SeqFunctionCallStatement assumption =
        buildThreadActiveAssumption(pPcVariables, pThread, pBinaryExpressionBuilder);
    return switch (pOptions.controlFlowEncoding) {
      case SWITCH_CASE ->
          new SeqSwitchStatement(pOptions, pcExpression, Optional.of(assumption), pClauses, pTabs);
      case BINARY_IF_TREE ->
          new SeqBinaryIfTreeStatement(pcExpression, pClauses, pTabs, pBinaryExpressionBuilder);
    };
  }

  private static SeqFunctionCallStatement buildThreadActiveAssumption(
      PcVariables pPcVariables,
      MPORThread pThread,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CBinaryExpression threadActiveExpression =
        SeqExpressionBuilder.buildPcUnequalExitPc(
            pPcVariables.getPcLeftHandSide(pThread.id), pBinaryExpressionBuilder);
    return SeqAssumptionBuilder.buildAssumeCall(threadActiveExpression).toFunctionCallStatement();
  }
}
