// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.assumptions;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqDeclarations.SeqFunctionDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqVoidType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqAssumptionBuilder {

  public static CFunctionCallStatement buildAssumption(CExpression pCondition) {
    return SeqStatementBuilder.buildFunctionCallStatement(buildAssumptionExpression(pCondition));
  }

  private static CFunctionCallExpression buildAssumptionExpression(CExpression pCondition) {
    return new CFunctionCallExpression(
        FileLocation.DUMMY,
        SeqVoidType.VOID,
        SeqIdExpression.ASSUME,
        ImmutableList.of(pCondition),
        SeqFunctionDeclaration.ASSUME);
  }

  public static ImmutableList<CFunctionCallStatement> buildNextThreadAssumption(
      boolean pIsSigned,
      CIdExpression pNumThreads,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<CFunctionCallStatement> rAssumptions = ImmutableList.builder();
    // split the assumption into 2 calls so that we don't have to use logical and (&&)
    if (pIsSigned) {
      CBinaryExpression nextThreadAtLeastZero =
          pBinaryExpressionBuilder.buildBinaryExpression(
              SeqIntegerLiteralExpression.INT_0,
              SeqIdExpression.NEXT_THREAD,
              BinaryOperator.LESS_EQUAL);
      rAssumptions.add(buildAssumption(nextThreadAtLeastZero));
    }
    CBinaryExpression nextThreadLessThanNumThreads =
        pBinaryExpressionBuilder.buildBinaryExpression(
            SeqIdExpression.NEXT_THREAD, pNumThreads, BinaryOperator.LESS_THAN);
    rAssumptions.add(buildAssumption(nextThreadLessThanNumThreads));
    return rAssumptions.build();
  }

  public static Optional<CFunctionCallStatement> buildNextThreadActiveAssumption(
      MPOROptions pOptions, CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pOptions.scalarPc) {
      // scalar pc: place assume(pci != -1); directly at respective thread head
      return Optional.empty();
    }
    // pc array: single assume(pc[next_thread] != -1);
    CBinaryExpression nextThreadActive =
        pBinaryExpressionBuilder.buildBinaryExpression(
            SeqExpressionBuilder.buildPcSubscriptExpression(SeqIdExpression.NEXT_THREAD),
            SeqIntegerLiteralExpression.INT_EXIT_PC,
            BinaryOperator.NOT_EQUALS);
    CFunctionCallStatement assumeCall = buildAssumption(nextThreadActive);
    return Optional.of(assumeCall);
  }

  public static Optional<CFunctionCallStatement> buildCountGreaterZeroAssumption(
      MPOROptions pOptions, CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pOptions.nondeterminismSource.isNextThreadNondeterministic()) {
      // thread count is only used when next_thread is not used
      return Optional.empty();
    }
    // assume(cnt > 0);
    CBinaryExpression nextThreadActive =
        pBinaryExpressionBuilder.buildBinaryExpression(
            SeqIdExpression.CNT, SeqIntegerLiteralExpression.INT_0, BinaryOperator.GREATER_THAN);
    CFunctionCallStatement assumeCall = buildAssumption(nextThreadActive);
    return Optional.of(assumeCall);
  }
}
