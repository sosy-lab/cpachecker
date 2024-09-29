// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqValues;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqVars;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.SeqExprBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.SwitchCaseExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.VariableExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.loop_case.SeqLoopCase;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqDataType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class MainMethod implements SeqFunction {

  private static final DeclareExpr declareExecute =
      new DeclareExpr(
          new VariableExpr(Optional.of(SeqDataType.BOOL), SeqVars.execute),
          Optional.of(SeqValues.boolTrue));

  private static final LoopExpr whileExecute = new LoopExpr(SeqVars.execute);

  private static final DeclareExpr declareNextThread =
      new DeclareExpr(
          new VariableExpr(Optional.of(SeqDataType.INT), SeqVars.nextThread),
          Optional.of(new FunctionCallExpr(SeqToken.VERIFIER_NONDET_INT, ImmutableList.of())));

  private static final FunctionCallExpr assumeNextThread =
      new FunctionCallExpr(SeqToken.ASSUME, assumeNextThreadParams());

  private static final IfExpr exitPcCheck =
      new IfExpr(
          new BooleanExpr(
              SeqExprBuilder.pcsNextThread,
              SeqOperator.EQUAL,
              new Value(Integer.toString(SeqUtil.EXIT_PC))));

  private static final AssignExpr executeUpdate =
      new AssignExpr(
          SeqVars.execute, new FunctionCallExpr(SeqToken.ANY_NON_NEGATIVE, anyNonNegativeParams()));

  /** The thread-specific cases in the main while loop. */
  private final ImmutableMap<MPORThread, ImmutableList<SeqLoopCase>> loopCases;

  private final DeclareExpr declareNumThreads;

  private final DeclareExpr declarePcs;

  public MainMethod(ImmutableMap<MPORThread, ImmutableList<SeqLoopCase>> pLoopCases) {
    loopCases = pLoopCases;
    declareNumThreads =
        new DeclareExpr(
            new VariableExpr(Optional.of(SeqDataType.INT), SeqVars.numThreads),
            Optional.of(new Value(Integer.toString(pLoopCases.size()))));
    declarePcs =
        new DeclareExpr(
            new VariableExpr(
                Optional.of(SeqDataType.INT),
                new ArrayExpr(SeqVars.pcs, Optional.of(SeqVars.numThreads))),
            Optional.of(pcsInitializerList(pLoopCases.size())));
  }

  @Override
  public String toString() {
    StringBuilder switchCases = new StringBuilder();

    int i = 0;
    for (var entry : loopCases.entrySet()) {
      Value threadId = new Value(Integer.toString(entry.getKey().id));
      IfExpr ifExpr = new IfExpr(new BooleanExpr(SeqVars.nextThread, SeqOperator.EQUAL, threadId));

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
      ImmutableList.Builder<String> cases = ImmutableList.builder();
      for (SeqLoopCase loopCase : entry.getValue()) {
        cases.add(loopCase.toString());
      }
      ArrayElement arrayElem = new ArrayElement(SeqVars.pcs, threadId);
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
        + SeqUtil.prependTabsWithNewline(1, declarePcs.toString())
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
    ImmutableList.Builder<SeqExpression> rParameters = ImmutableList.builder();
    rParameters.add(new Value(SeqToken.VOID));
    return rParameters.build();
  }

  @Override
  public FunctionSignature getSignature() {
    return new FunctionSignature(getReturnType(), new FunctionCallExpr(getName(), getParameters()));
  }

  private InitializerListExpr pcsInitializerList(int pNumThreads) {
    ImmutableList.Builder<SeqExpression> rInitializers = ImmutableList.builder();
    for (int i = 0; i < pNumThreads; i++) {
      rInitializers.add(SeqValues.zero);
    }
    return new InitializerListExpr(rInitializers.build());
  }

  private static ImmutableList<SeqExpression> assumeNextThreadParams() {
    ImmutableList.Builder<SeqExpression> rParams = ImmutableList.builder();
    rParams.add(
        new BooleanExpr(
            new BooleanExpr(SeqValues.zero, SeqOperator.LESS_OR_EQUAL, SeqVars.nextThread),
            SeqOperator.AND,
            new BooleanExpr(SeqVars.nextThread, SeqOperator.LESS, SeqVars.numThreads)));
    return rParams.build();
  }

  private static ImmutableList<SeqExpression> anyNonNegativeParams() {
    ImmutableList.Builder<SeqExpression> rParams = ImmutableList.builder();
    rParams.add(SeqVars.pcs);
    rParams.add(SeqVars.numThreads);
    return rParams.build();
  }
}
