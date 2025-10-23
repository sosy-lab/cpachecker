// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.bit_vector;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqFunctionCallExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control.SingleControlStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.SeqAssumptionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation.BitVectorEvaluationExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqConflictOrderStatement implements SeqInjectedStatement {

  private final MPORThread activeThread;

  private final BitVectorEvaluationExpression lastBitVectorEvaluation;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  public SeqConflictOrderStatement(
      MPORThread pActiveThread,
      BitVectorEvaluationExpression pLastBitVectorEvaluation,
      CBinaryExpressionBuilder pBinaryExpressionBuilder) {

    checkArgument(
        !pActiveThread.isMain(), "cannot build SeqConflictOrderStatement for main thread");
    activeThread = pActiveThread;
    lastBitVectorEvaluation = pLastBitVectorEvaluation;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    ImmutableList.Builder<String> lines = ImmutableList.builder();
    // last_thread < n
    CBinaryExpression lastThreadLessThanThreadId =
        binaryExpressionBuilder.buildBinaryExpression(
            SeqIdExpressions.LAST_THREAD,
            SeqExpressionBuilder.buildIntegerLiteralExpression(activeThread.getId()),
            BinaryOperator.LESS_THAN);
    // if (last_thread < n)
    lines.add(SingleControlStatementType.IF.buildControlFlowPrefix(lastThreadLessThanThreadId));
    if (lastBitVectorEvaluation.isEmpty()) {
      // if the evaluation is empty, it results in assume(0) i.e. abort()
      lines.add(SeqFunctionCallExpressions.ABORT.toASTString());
    } else {
      // assume(*conflict*) i.e. continue in thread n only if it is not in conflict with last_thread
      lines.add(
          SeqAssumptionBuilder.buildAssumption(lastBitVectorEvaluation.toCExpression())
              .toASTString());
    }
    lines.add(SeqSyntax.CURLY_BRACKET_RIGHT);
    return SeqStringUtil.joinWithNewlines(lines.build());
  }
}
