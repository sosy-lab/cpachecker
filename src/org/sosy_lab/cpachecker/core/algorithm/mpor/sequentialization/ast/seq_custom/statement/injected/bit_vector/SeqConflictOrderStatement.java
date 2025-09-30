// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.assumptions.SeqAssumptionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqIfExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
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
            SeqIdExpression.LAST_THREAD,
            SeqExpressionBuilder.buildIntegerLiteralExpression(activeThread.getId()),
            BinaryOperator.LESS_THAN);
    // if (last_thread < n)
    SeqIfExpression ifExpression = new SeqIfExpression(lastThreadLessThanThreadId);
    // assume(*conflict*) i.e. continue in thread n only if it is not in conflict with last_thread
    String assumeCall = SeqAssumptionBuilder.buildAssumption(lastBitVectorEvaluation.toASTString());
    // add all LOC
    lines.add(SeqStringUtil.appendCurlyBracketLeft(ifExpression.toASTString()));
    lines.add(assumeCall);
    lines.add(SeqSyntax.CURLY_BRACKET_RIGHT);
    return SeqStringUtil.joinWithNewlines(lines.build());
  }
}
