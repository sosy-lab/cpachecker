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
import com.google.common.collect.ImmutableList.Builder;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqFunctionCallExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control.SeqBranchStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.SeqAssumptionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation.BitVectorEvaluationExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public record SeqConflictOrderStatement(
    MPORThread activeThread,
    BitVectorEvaluationExpression lastBitVectorEvaluation,
    CBinaryExpressionBuilder binaryExpressionBuilder)
    implements SeqInjectedStatement {

  public SeqConflictOrderStatement {

    checkArgument(!activeThread.isMain(), "cannot build SeqConflictOrderStatement for main thread");
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    StringJoiner joiner = new StringJoiner(SeqSyntax.NEWLINE);
    // last_thread < n
    CBinaryExpression lastThreadLessThanThreadId =
        binaryExpressionBuilder.buildBinaryExpression(
            SeqIdExpressions.LAST_THREAD,
            SeqExpressionBuilder.buildIntegerLiteralExpression(activeThread.id()),
            BinaryOperator.LESS_THAN);
    // if (last_thread < n)
    Builder<String> ifBlock = ImmutableList.builder();
    if (lastBitVectorEvaluation.isEmpty()) {
      // if the evaluation is empty, it results in assume(0) i.e. abort()
      ifBlock.add(SeqFunctionCallExpressions.ABORT.toASTString());
    } else {
      // assume(*conflict*) i.e. continue in thread n only if it is not in conflict with last_thread
      ifBlock.add(SeqAssumptionBuilder.buildAssumption(lastBitVectorEvaluation));
    }
    SeqBranchStatement ifStatement =
        new SeqBranchStatement(lastThreadLessThanThreadId.toASTString(), ifBlock.build());
    joiner.add(ifStatement.toASTString());
    return joiner.toString();
  }
}
