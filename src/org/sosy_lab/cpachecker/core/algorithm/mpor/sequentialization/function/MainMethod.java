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
  public String createString() {
    StringBuilder switchCases = new StringBuilder();

    boolean firstEntry = true;
    for (var entry : loopCases.entrySet()) {
      Value threadId = new Value(Integer.toString(entry.getKey().id));
      IfExpr ifExpr = new IfExpr(new BooleanExpr(SeqVars.nextThread, SeqOperator.EQUAL, threadId));
      if (firstEntry) {
        firstEntry = false;
        switchCases
            .append(ifExpr.createString())
            .append(SeqSyntax.SPACE)
            .append(SeqSyntax.CURLY_BRACKET_LEFT);
      } else {
        ElseIfExpr elseIfExpr = new ElseIfExpr(ifExpr);
        switchCases.append(SeqUtil.wrapInCurlyOutwards(elseIfExpr));
      }
      switchCases.append(SeqSyntax.NEWLINE);
      ImmutableList.Builder<String> cases = ImmutableList.builder();
      for (SeqLoopCase loopCase : entry.getValue()) {
        cases.add(loopCase.createString());
      }
      ArrayElement arrayElem = new ArrayElement(SeqVars.pcs, threadId);
      SwitchCaseExpr switchCaseExpr = new SwitchCaseExpr(arrayElem, cases.build());
      switchCases
          .append(switchCaseExpr.createString())
          .append(SeqSyntax.NEWLINE)
          .append(SeqSyntax.NEWLINE);
    }

    return getSignature().createString()
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_LEFT
        + SeqSyntax.NEWLINE
        + declareNumThreads.createString()
        + SeqSyntax.NEWLINE
        + declarePcs.createString()
        + SeqSyntax.NEWLINE
        + declareExecute.createString()
        + SeqSyntax.NEWLINE
        + whileExecute.createString()
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_LEFT
        + SeqSyntax.NEWLINE
        + declareNextThread.createString()
        + SeqSyntax.NEWLINE
        + assumeNextThread.createString()
        + SeqSyntax.NEWLINE
        + exitPcCheck.createString()
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_LEFT
        + SeqSyntax.NEWLINE
        + executeUpdate.createString()
        + SeqSyntax.NEWLINE
        + SeqToken.CONTINUE
        + SeqSyntax.SEMICOLON
        + SeqSyntax.NEWLINE
        + SeqSyntax.CURLY_BRACKET_RIGHT
        + SeqSyntax.NEWLINE
        + switchCases
        + SeqSyntax.CURLY_BRACKET_RIGHT
        + SeqSyntax.NEWLINE
        + SeqSyntax.CURLY_BRACKET_RIGHT
        + SeqSyntax.NEWLINE
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
    Builder<SeqExpression> rParams = ImmutableList.builder();
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
