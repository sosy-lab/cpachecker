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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqFunctionDeclarations;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;

public class SeqAssumptionBuilder {

  /**
   * Returns a {@link CFunctionCallStatement} to the assume function i.e. {@code
   * assume(pCondition);}.
   */
  public static CFunctionCallStatement buildAssumeFunctionCallStatement(CExpression pCondition) {
    CFunctionCallExpression assumeFunctionCallExpression =
        new CFunctionCallExpression(
            FileLocation.DUMMY,
            CVoidType.VOID,
            SeqIdExpressions.ASSUME,
            ImmutableList.of(pCondition),
            SeqFunctionDeclarations.ASSUME);
    return new CFunctionCallStatement(FileLocation.DUMMY, assumeFunctionCallExpression);
  }

  /**
   * Returns a {@link String} representation of an assume function call i.e. {@code
   * assume(pCondition);}.
   */
  public static String buildAssumeFunctionCallStatement(ExpressionTree<CExpression> pCondition) {
    return SeqIdExpressions.ASSUME
        + SeqStringUtil.wrapInBrackets(pCondition.toString())
        + SeqSyntax.SEMICOLON;
  }

  public static ImmutableList<CFunctionCallStatement> buildNextThreadAssumeCall(
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
      rAssumptions.add(buildAssumeFunctionCallStatement(nextThreadAtLeastZero));
    }
    CIntegerLiteralExpression numThreadsExpression =
        SeqExpressionBuilder.buildIntegerLiteralExpression(pFields.numThreads);
    // ensure that next_thread < NUM_THREADS
    CBinaryExpression nextThreadLessThanNumThreads =
        pBinaryExpressionBuilder.buildBinaryExpression(
            SeqIdExpressions.NEXT_THREAD, numThreadsExpression, BinaryOperator.LESS_THAN);
    rAssumptions.add(buildAssumeFunctionCallStatement(nextThreadLessThanNumThreads));
    return rAssumptions.build();
  }
}
