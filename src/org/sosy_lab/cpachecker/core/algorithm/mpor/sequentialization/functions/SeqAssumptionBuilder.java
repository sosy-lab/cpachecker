// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationFields;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqFunctionDeclarations;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation.BitVectorEvaluationExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;

public class SeqAssumptionBuilder {

  public static String buildAssumption(BitVectorEvaluationExpression pCondition) {
    if (pCondition.binaryExpression.isPresent()) {
      return buildAssumption(pCondition.binaryExpression.orElseThrow()).toASTString();
    }
    if (pCondition.logicalExpression.isPresent()) {
      return buildAssumption(pCondition.logicalExpression.orElseThrow());
    }
    throw new IllegalArgumentException("both binaryExpression and logicalExpression are empty");
  }

  public static CFunctionCallStatement buildAssumption(CExpression pCondition) {
    return SeqStatementBuilder.buildFunctionCallStatement(buildAssumptionExpression(pCondition));
  }

  public static String buildAssumption(ExpressionTree<CExpression> pCondition) {
    return SeqIdExpressions.ASSUME
        + SeqStringUtil.wrapInBrackets(pCondition.toString())
        + SeqSyntax.SEMICOLON;
  }

  private static CFunctionCallExpression buildAssumptionExpression(CExpression pCondition) {
    return new CFunctionCallExpression(
        FileLocation.DUMMY,
        CVoidType.VOID,
        SeqIdExpressions.ASSUME,
        ImmutableList.of(pCondition),
        SeqFunctionDeclarations.ASSUME);
  }

  public static ImmutableList<CFunctionCallStatement> buildNextThreadAssumption(
      boolean pIsSigned,
      SequentializationFields pFields,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<CFunctionCallStatement> rAssumptions = ImmutableList.builder();
    // split the assumption into 2 calls so that we don't have to use logical and (&&)
    if (pIsSigned) {
      // ensure that 0 <= next_thread
      CBinaryExpression nextThreadAtLeastZero =
          pBinaryExpressionBuilder.buildBinaryExpression(
              SeqIntegerLiteralExpressions.INT_0,
              SeqIdExpressions.NEXT_THREAD,
              BinaryOperator.LESS_EQUAL);
      rAssumptions.add(buildAssumption(nextThreadAtLeastZero));
    }
    CIntegerLiteralExpression numThreadsExpression =
        SeqExpressionBuilder.buildIntegerLiteralExpression(pFields.numThreads);
    // ensure that next_thread < NUM_THREADS
    CBinaryExpression nextThreadLessThanNumThreads =
        pBinaryExpressionBuilder.buildBinaryExpression(
            SeqIdExpressions.NEXT_THREAD, numThreadsExpression, BinaryOperator.LESS_THAN);
    rAssumptions.add(buildAssumption(nextThreadLessThanNumThreads));
    return rAssumptions.build();
  }

  public static CFunctionCallStatement buildNextThreadActiveAssumption(
      CBinaryExpressionBuilder pBinaryExpressionBuilder) throws UnrecognizedCodeException {

    // pc array: single assume(pc[next_thread] != -1);
    CBinaryExpression nextThreadActive =
        pBinaryExpressionBuilder.buildBinaryExpression(
            SeqExpressionBuilder.buildPcSubscriptExpression(SeqIdExpressions.NEXT_THREAD),
            SeqIntegerLiteralExpressions.INT_EXIT_PC,
            BinaryOperator.NOT_EQUALS);
    return buildAssumption(nextThreadActive);
  }

  public static CFunctionCallStatement buildCountGreaterZeroAssumption(
      CBinaryExpressionBuilder pBinaryExpressionBuilder) throws UnrecognizedCodeException {

    // assume(cnt > 0);
    CBinaryExpression countGreaterZeroExpression =
        pBinaryExpressionBuilder.buildBinaryExpression(
            SeqIdExpressions.CNT, SeqIntegerLiteralExpressions.INT_0, BinaryOperator.GREATER_THAN);
    return buildAssumption(countGreaterZeroExpression);
  }
}
