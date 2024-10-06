// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqNameBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqVars;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.ArrayElement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.Value;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.ArrayExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.AssignExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.BooleanExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.DeclareExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.ElseIfExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.FunctionCallExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.IfExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.InitializerListExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.LoopExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.SwitchCaseExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.VariableExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.loop_case.SeqLoopCase;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqDataType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqValue;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class MainMethod implements SeqFunction {

  private static final DeclareExpr declareExecute =
      new DeclareExpr(
          false,
          new VariableExpr(Optional.of(SeqDataType.INT), SeqVars.execute),
          Optional.of(new Value(SeqValue.ONE)));

  private static final LoopExpr whileExecute = new LoopExpr(SeqVars.execute);

  private static final DeclareExpr declareNextThread =
      new DeclareExpr(
          false,
          new VariableExpr(Optional.of(SeqDataType.INT), SeqVars.nextThread),
          Optional.of(new FunctionCallExpr(SeqToken.VERIFIER_NONDET_INT, ImmutableList.of())));

  private static final FunctionCallExpr assumeNextThread =
      new FunctionCallExpr(
          SeqNameBuilder.createFuncName(SeqToken.ASSUME), assumeNextThreadParams());

  private static final AssignExpr executeUpdate =
      new AssignExpr(
          SeqVars.execute,
          new FunctionCallExpr(
              SeqNameBuilder.createFuncName(SeqToken.ANY_UNSIGNED), anyNonNegativeParams()));

  /** The thread-specific cases in the main while loop. */
  private final ImmutableMap<MPORThread, ImmutableList<SeqLoopCase>> loopCases;

  private final CBinaryExpressionBuilder binExprBuilder;

  private final DeclareExpr declareNumThreads;

  private final DeclareExpr declarePc;

  private final IfExpr exitPcCheck;

  // TODO add an ImmutableSet<CExpression> pAssumptions
  public MainMethod(
      ImmutableMap<MPORThread, ImmutableList<SeqLoopCase>> pLoopCases,
      CBinaryExpressionBuilder pBinExprBuilder)
      throws UnrecognizedCodeException {

    binExprBuilder = pBinExprBuilder;
    loopCases = pLoopCases;
    declareNumThreads =
        new DeclareExpr(
            true,
            new VariableExpr(Optional.of(SeqDataType.INT), SeqVars.numThreads),
            Optional.of(new Value(Integer.toString(pLoopCases.size()))));
    declarePc =
        new DeclareExpr(
            false,
            new VariableExpr(
                Optional.of(SeqDataType.INT),
                new ArrayExpr(SeqVars.pc, Optional.of(SeqVars.numThreads))),
            Optional.of(pcInitializerList(pLoopCases.size())));
    exitPcCheck =
        new IfExpr(
            binExprBuilder.buildBinaryExpression(
                SeqExpressions.buildPcSubscriptExpr(SeqExpressions.NEXT_THREAD),
                SeqExpressions.INT_EXIT_PC,
                BinaryOperator.EQUALS));
  }

  @Override
  public String toString() {
    StringBuilder switchCases = new StringBuilder();

    int i = 0;
    for (var entry : loopCases.entrySet()) {
      CIntegerLiteralExpression threadId = SeqExpressions.buildIntLiteralExpr(entry.getKey().id);
      IfExpr ifExpr = null;
      try {
        ifExpr =
            new IfExpr(
                binExprBuilder.buildBinaryExpression(
                    SeqExpressions.NEXT_THREAD, threadId, BinaryOperator.EQUALS));
      } catch (UnrecognizedCodeException pE) {
        throw new RuntimeException(pE);
      }
      // first switch case: use if, otherwise else-if
      if (i == 0) {
        switchCases.append(
            SeqUtil.prependTabsWithoutNewline(2, SeqUtil.appendOpeningCurly(ifExpr.toString())));
      } else {
        ElseIfExpr elseIfExpr = new ElseIfExpr(ifExpr);
        switchCases.append(
            SeqUtil.prependTabsWithoutNewline(2, SeqUtil.wrapInCurlyOutwards(elseIfExpr)));
      }
      switchCases.append(SeqSyntax.NEWLINE);
      Builder<String> cases = ImmutableList.builder();
      for (SeqLoopCase loopCase : entry.getValue()) {
        cases.add(loopCase.toString());
      }
      ArrayElement arrayElem = new ArrayElement(SeqVars.pc, threadId);
      SwitchCaseExpr switchCaseExpr = new SwitchCaseExpr(arrayElem, cases.build(), 3);
      switchCases.append(switchCaseExpr);

      // append 2 newlines, except for last switch case (1 only)
      switchCases.append(SeqUtil.repeat(SeqSyntax.NEWLINE, i == loopCases.size() - 1 ? 1 : 2));
      i++;
    }

    return getSignature().toString()
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_LEFT
        + SeqSyntax.NEWLINE
        + SeqUtil.prependTabsWithNewline(1, declareNumThreads.toString())
        + SeqUtil.prependTabsWithNewline(1, declarePc.toString())
        + SeqUtil.prependTabsWithNewline(1, declareExecute.toString())
        + SeqSyntax.NEWLINE
        + SeqUtil.prependTabsWithNewline(1, SeqUtil.appendOpeningCurly(whileExecute.toString()))
        + SeqUtil.prependTabsWithNewline(2, declareNextThread.toString())
        + SeqUtil.prependTabsWithNewline(2, assumeNextThread.toString() + SeqSyntax.SEMICOLON)
        + SeqSyntax.NEWLINE
        + SeqUtil.prependTabsWithNewline(2, SeqUtil.appendOpeningCurly(exitPcCheck.toString()))
        + SeqUtil.prependTabsWithNewline(3, executeUpdate.toString())
        + SeqUtil.prependTabsWithNewline(3, SeqToken.CONTINUE + SeqSyntax.SEMICOLON)
        + SeqUtil.prependTabsWithNewline(2, SeqSyntax.CURLY_BRACKET_RIGHT)
        + SeqSyntax.NEWLINE
        + switchCases
        + SeqUtil.prependTabsWithNewline(2, SeqSyntax.CURLY_BRACKET_RIGHT)
        + SeqUtil.prependTabsWithNewline(1, SeqSyntax.CURLY_BRACKET_RIGHT)
        // TODO need a return 0; here
        + SeqSyntax.CURLY_BRACKET_RIGHT;
  }

  @Override
  public String getReturnType() {
    return SeqDataType.INT;
  }

  @Override
  public String getName() {
    return SeqToken.MAIN;
  }

  @Override
  public ImmutableList<SeqExpression> getParameters() {
    Builder<SeqExpression> rParameters = ImmutableList.builder();
    rParameters.add(new Value(SeqDataType.VOID));
    return rParameters.build();
  }

  private InitializerListExpr pcInitializerList(int pNumThreads) {
    Builder<SeqExpression> rInitializers = ImmutableList.builder();
    for (int i = 0; i < pNumThreads; i++) {
      rInitializers.add(new Value(SeqValue.ZERO));
    }
    return new InitializerListExpr(rInitializers.build());
  }

  private static ImmutableList<SeqExpression> assumeNextThreadParams() {
    Builder<SeqExpression> rParams = ImmutableList.builder();
    rParams.add(
        new BooleanExpr(
            new BooleanExpr(
                new Value(SeqValue.ZERO), SeqOperator.LESS_OR_EQUAL, SeqVars.nextThread),
            SeqOperator.AND,
            new BooleanExpr(SeqVars.nextThread, SeqOperator.LESS, SeqVars.numThreads)));
    return rParams.build();
  }

  private static ImmutableList<SeqExpression> anyNonNegativeParams() {
    Builder<SeqExpression> rParams = ImmutableList.builder();
    rParams.add(SeqVars.pc);
    rParams.add(SeqVars.numThreads);
    return rParams.build();
  }
}
