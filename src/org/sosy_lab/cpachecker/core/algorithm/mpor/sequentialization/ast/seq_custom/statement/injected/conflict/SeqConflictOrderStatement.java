// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.conflict;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.evaluation.BitVectorEvaluationExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqIfExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.multi_control.MultiControlStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.multi_control.SeqMultiControlStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqConflictOrderStatement implements SeqInjectedStatement {

  private final MPOROptions options;

  private final CExpression nextThreadExpression;

  private final ImmutableMap<MPORThread, BitVectorEvaluationExpression> bitVectorEvaluationPairs;

  private final PcVariables pcVariables;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  public SeqConflictOrderStatement(
      MPOROptions pOptions,
      CExpression pNextThreadExpression,
      ImmutableMap<MPORThread, BitVectorEvaluationExpression> pBitVectorEvaluationPairs,
      PcVariables pPcVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder) {

    options = pOptions;
    nextThreadExpression = pNextThreadExpression;
    bitVectorEvaluationPairs = pBitVectorEvaluationPairs;
    pcVariables = pPcVariables;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    SeqIfExpression ifLastThreadNotNextThread =
        new SeqIfExpression(
            binaryExpressionBuilder.buildBinaryExpression(
                SeqIdExpression.LAST_THREAD, nextThreadExpression, BinaryOperator.NOT_EQUALS));
    ImmutableList<SeqConflictAssumptionStatement> assumptionStatements =
        buildConflictAssumptionStatements(
            nextThreadExpression, bitVectorEvaluationPairs, pcVariables, binaryExpressionBuilder);
    SeqMultiControlStatement multiControlStatement =
        MultiControlStatementBuilder.buildMultiControlStatementByEncoding(
            options,
            options.controlEncodingThread,
            SeqIdExpression.LAST_THREAD,
            Optional.empty(),
            Optional.empty(),
            assumptionStatements,
            7,
            binaryExpressionBuilder);
    return SeqStringUtil.appendCurlyBracketRight(ifLastThreadNotNextThread.toASTString())
        + SeqSyntax.NEWLINE
        + multiControlStatement.toASTString()
        + SeqSyntax.NEWLINE
        + SeqSyntax.CURLY_BRACKET_RIGHT;
  }

  private static ImmutableList<SeqConflictAssumptionStatement> buildConflictAssumptionStatements(
      CExpression pNextThreadExpression,
      ImmutableMap<MPORThread, BitVectorEvaluationExpression> pBitVectorEvaluationPairs,
      PcVariables pPcVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqConflictAssumptionStatement> rStatements = ImmutableList.builder();
    for (var entry : pBitVectorEvaluationPairs.entrySet()) {
      MPORThread otherThread = entry.getKey();
      CLeftHandSide pcLeftHandSide = pPcVariables.getPcLeftHandSide(otherThread.id);
      CBinaryExpression assumptionExpression =
          pBinaryExpressionBuilder.buildBinaryExpression(
              pNextThreadExpression,
              SeqExpressionBuilder.buildIntegerLiteralExpression(otherThread.id),
              BinaryOperator.LESS_THAN);
      rStatements.add(
          new SeqConflictAssumptionStatement(
              pcLeftHandSide, entry.getValue(), assumptionExpression, pBinaryExpressionBuilder));
    }
    return rStatements.build();
  }
}
