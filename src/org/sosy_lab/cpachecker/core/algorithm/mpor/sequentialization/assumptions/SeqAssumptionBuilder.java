// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.assumptions;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.CToSeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqFunctionCallExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalAndExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.function_call.SeqFunctionCallStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.function_call.SeqScalarPcAssumeStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.PcVariables;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqAssumptionBuilder {

  public static SeqFunctionCallExpression buildAssumeCall(SeqExpression pCondition) {
    return new SeqFunctionCallExpression(SeqIdExpression.ASSUME, ImmutableList.of(pCondition));
  }

  public static SeqFunctionCallExpression buildAssumeCall(CExpression pCondition) {
    return new SeqFunctionCallExpression(
        SeqIdExpression.ASSUME, ImmutableList.of(new CToSeqExpression(pCondition)));
  }

  public static SeqFunctionCallExpression buildNextThreadAssumption(
      boolean pIsSigned,
      CIdExpression pNumThreads,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return new SeqFunctionCallExpression(
        SeqIdExpression.ASSUME,
        buildNextThreadAssumptionExpression(pIsSigned, pNumThreads, pBinaryExpressionBuilder));
  }

  /** Returns the expression {@code 0 <= next_thread && next_thread < NUM_THREADS} */
  private static ImmutableList<SeqExpression> buildNextThreadAssumptionExpression(
      boolean pIsSigned,
      CIdExpression pNumThreads,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqExpression> rParameters = ImmutableList.builder();
    // next_thread < NUM_THREADS is used for both signed and unsigned
    CBinaryExpression nextThreadLessThanNumThreads =
        pBinaryExpressionBuilder.buildBinaryExpression(
            SeqIdExpression.NEXT_THREAD, pNumThreads, BinaryOperator.LESS_THAN);
    rParameters.add(
        pIsSigned
            ? new SeqLogicalAndExpression(
                pBinaryExpressionBuilder.buildBinaryExpression(
                    SeqIntegerLiteralExpression.INT_0,
                    SeqIdExpression.NEXT_THREAD,
                    BinaryOperator.LESS_EQUAL),
                nextThreadLessThanNumThreads)
            : new CToSeqExpression(nextThreadLessThanNumThreads));
    return rParameters.build();
  }

  public static Optional<SeqStatement> buildNextThreadActiveAssumption(
      MPOROptions pOptions, CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pOptions.scalarPc) {
      // scalar pc: place assume(pci != -1); directly at respective thread head
      return Optional.empty();
    }
    // pc array: single assume(pc[next_thread] != -1);
    SeqFunctionCallExpression assumeCall =
        new SeqFunctionCallExpression(
            SeqIdExpression.ASSUME,
            ImmutableList.of(
                new CToSeqExpression(
                    pBinaryExpressionBuilder.buildBinaryExpression(
                        SeqExpressionBuilder.buildPcSubscriptExpression(
                            SeqIdExpression.NEXT_THREAD),
                        SeqIntegerLiteralExpression.INT_EXIT_PC,
                        BinaryOperator.NOT_EQUALS))));
    return Optional.of(assumeCall.toFunctionCallStatement());
  }

  private static ImmutableList<SeqScalarPcAssumeStatement> buildScalarPcAssumeClauses(
      int pNumThreads, PcVariables pPcVariables, CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    // scalar pc int: switch statement with individual case i: assume(pci != -1);
    ImmutableList.Builder<SeqScalarPcAssumeStatement> rAssumeClauses = ImmutableList.builder();
    for (int i = 0; i < pNumThreads; i++) {
      Verify.verify(pPcVariables.getPcLeftHandSide(i) instanceof CIdExpression);
      SeqFunctionCallStatement assumeCall =
          new SeqFunctionCallStatement(
              new SeqFunctionCallExpression(
                  SeqIdExpression.ASSUME,
                  ImmutableList.of(
                      new CToSeqExpression(
                          pBinaryExpressionBuilder.buildBinaryExpression(
                              pPcVariables.getPcLeftHandSide(i),
                              SeqIntegerLiteralExpression.INT_EXIT_PC,
                              BinaryOperator.NOT_EQUALS)))));
      rAssumeClauses.add(new SeqScalarPcAssumeStatement(assumeCall));
    }
    return rAssumeClauses.build();
  }
}
