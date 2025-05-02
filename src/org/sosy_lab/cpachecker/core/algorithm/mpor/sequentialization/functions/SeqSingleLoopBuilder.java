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
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqSwitchStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCodeUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqSingleLoopBuilder {

  protected static ImmutableList<LineOfCode> buildSingleLoopSwitchStatements(
      MPOROptions pOptions,
      PcVariables pPcVariables,
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder) {

    ImmutableList.Builder<LineOfCode> rSwitches = ImmutableList.builder();
    int i = 0;
    for (var entry : pCaseClauses.entrySet()) {
      MPORThread thread = entry.getKey();
      CIntegerLiteralExpression threadId =
          SeqExpressionBuilder.buildIntegerLiteralExpression(thread.id);
      try {
        CBinaryExpression nextThreadEqualsThreadId =
            pBinaryExpressionBuilder.buildBinaryExpression(
                SeqIdExpression.NEXT_THREAD, threadId, BinaryOperator.EQUALS);
        // first switch case: use "if", otherwise "else if"
        SeqControlFlowStatementType statementType =
            i == 0 ? SeqControlFlowStatementType.IF : SeqControlFlowStatementType.ELSE_IF;
        SeqControlFlowStatement statement =
            new SeqControlFlowStatement(nextThreadEqualsThreadId, statementType);
        rSwitches.add(
            LineOfCode.of(
                2,
                i == 0
                    ? SeqStringUtil.appendOpeningCurly(statement.toASTString())
                    : SeqStringUtil.wrapInCurlyOutwards(statement.toASTString())));
      } catch (UnrecognizedCodeException e) {
        throw new RuntimeException(e);
      }
      rSwitches.addAll(
          buildSingleLoopSwitchStatement(pOptions, pPcVariables, thread, entry.getValue(), 3));
      i++;
    }
    rSwitches.add(LineOfCode.of(2, SeqSyntax.CURLY_BRACKET_RIGHT));
    return rSwitches.build();
  }

  private static ImmutableList<LineOfCode> buildSingleLoopSwitchStatement(
      MPOROptions pOptions,
      PcVariables pPcVariables,
      MPORThread pThread,
      ImmutableList<SeqCaseClause> pCaseClauses,
      int pTabs) {

    CExpression pcExpression = pPcVariables.get(pThread.id);
    SeqSwitchStatement switchStatement =
        new SeqSwitchStatement(pOptions, pcExpression, pCaseClauses, pTabs);
    return LineOfCodeUtil.buildLinesOfCode(switchStatement.toASTString());
  }
}
