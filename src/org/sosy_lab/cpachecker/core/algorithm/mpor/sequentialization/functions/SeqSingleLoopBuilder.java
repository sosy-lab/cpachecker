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
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.multi.SeqBinaryIfTreeStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.multi.SeqIfElseChainStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.multi.SeqMultiControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.multi.SeqSwitchStatement;
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
          SeqMainFunctionBuilder.buildMultiControlFlowStatement(
              pOptions, pPcVariables, thread, entry.getValue(), 4, pBinaryExpressionBuilder));
    }
    return switch (pOptions.controlFlowEncoding) {
      case BINARY_IF_TREE -> {
        SeqBinaryIfTreeStatement binaryTree =
            new SeqBinaryIfTreeStatement(
                SeqIdExpression.NEXT_THREAD,
                Optional.empty(),
                controlFlow.build(),
                2,
                pBinaryExpressionBuilder);
        yield LineOfCodeUtil.buildLinesOfCode(binaryTree.toASTString());
      }
      case IF_ELSE_CHAIN -> {
        SeqIfElseChainStatement ifElseChain =
            new SeqIfElseChainStatement(
                SeqIdExpression.NEXT_THREAD,
                Sequentialization.MAIN_THREAD_ID,
                Optional.empty(),
                controlFlow.build(),
                2,
                pBinaryExpressionBuilder);
        yield LineOfCodeUtil.buildLinesOfCode(ifElseChain.toASTString());
      }
      case SWITCH_CASE -> {
        SeqSwitchStatement switchCase =
            new SeqSwitchStatement(
                pOptions, SeqIdExpression.NEXT_THREAD, Optional.empty(), controlFlow.build(), 2);
        yield LineOfCodeUtil.buildLinesOfCode(switchCase.toASTString());
      }
    };
  }
}
