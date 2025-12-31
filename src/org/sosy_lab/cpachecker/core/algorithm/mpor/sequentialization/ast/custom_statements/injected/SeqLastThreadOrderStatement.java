// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control.SeqBranchStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.SeqAssumeFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation.BitVectorEvaluationExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public record SeqLastThreadOrderStatement(
    MPORThread activeThread,
    Optional<BitVectorEvaluationExpression> lastBitVectorEvaluation,
    CBinaryExpressionBuilder binaryExpressionBuilder)
    implements SeqInjectedStatement {

  public SeqLastThreadOrderStatement {
    checkArgument(
        !activeThread.isMain(), "cannot build SeqLastThreadOrderStatement for main thread");
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    // last_thread < n
    CBinaryExpression lastThreadLessThanThreadId =
        binaryExpressionBuilder.buildBinaryExpression(
            SeqIdExpressions.LAST_THREAD,
            SeqExpressionBuilder.buildIntegerLiteralExpression(activeThread.id()),
            BinaryOperator.LESS_THAN);

    // create the ifBlock i.e. call to assume / abort
    final String ifBlock;
    if (lastBitVectorEvaluation.isEmpty()) {
      // if the evaluation is empty, it results in assume(0) i.e. abort()
      ifBlock = SeqAssumeFunction.ABORT_FUNCTION_CALL_STATEMENT.toASTString();
    } else {
      // assume(*conflict*) i.e. continue in thread n only if it is not in conflict with last_thread
      ifBlock =
          SeqAssumeFunction.buildAssumeFunctionCallStatement(
              lastBitVectorEvaluation.orElseThrow().expression());
    }
    SeqBranchStatement ifStatement =
        new SeqBranchStatement(lastThreadLessThanThreadId.toASTString(), ImmutableList.of(ifBlock));

    return ifStatement.toASTString();
  }

  @Override
  public boolean isPrunedWithTargetGoto() {
    return true;
  }

  @Override
  public boolean isPrunedWithEmptyBitVectorEvaluation() {
    return false;
  }
}
