// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.interleaving;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqElement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.Value;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.Variable;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.ArrayExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.ArrayInitExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.AssignExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.BooleanExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.DeclareExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.FunctionCallExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.LoopExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.NegationExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.SwitchCaseExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.VariableExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqDataType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqValue;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

/**
 * A class representing a non-deterministic interleaving of global access edges. A {@link
 * Interleaving} is created if at least one pair of global accesses does not commute.
 */
public class Interleaving implements SeqElement {

  private static final Variable numThreads = new Variable(SeqToken.NUM_THREADS);

  private static final Variable nextThread = new Variable(SeqToken.NEXT_THREAD);

  private static final Variable executed = new Variable(SeqToken.EXECUTED);

  private static final ArrayExpr arrayExpr = new ArrayExpr(executed, Optional.of(numThreads));

  private static final ArrayInitExpr arrayInitExpr =
      new ArrayInitExpr(SeqDataType.BOOL, arrayExpr, new Value(SeqValue.FALSE));

  private static final LoopExpr loopExpr = new LoopExpr(initAnyFalseCall());

  private static final DeclareExpr declareExpr =
      new DeclareExpr(
          new VariableExpr(SeqDataType.INT, nextThread),
          new FunctionCallExpr(SeqToken.NON_DET, Optional.empty()));

  private static final FunctionCallExpr assumeNextThreadExpr =
      new FunctionCallExpr(SeqToken.ASSUME, Optional.of(initAssumeNextThread()));

  private static final FunctionCallExpr assumeNotExecutedExpr =
      new FunctionCallExpr(SeqToken.ASSUME, Optional.of(initAssumeNotExecuted()));

  private static final AssignExpr assignExpr = new AssignExpr(arrayExpr, new Value(SeqValue.TRUE));

  private static final String preSwitchCase =
      arrayInitExpr.createString()
          + SeqSyntax.NEWLINE
          + loopExpr.createString()
          + SeqSyntax.SPACE
          + SeqSyntax.CURLY_BRACKET_LEFT
          + SeqSyntax.NEWLINE
          + SeqSyntax.TAB
          + declareExpr.createString()
          + SeqSyntax.NEWLINE
          + SeqSyntax.TAB
          + assumeNextThreadExpr.createString()
          + SeqSyntax.SEMICOLON
          + SeqSyntax.NEWLINE
          + SeqSyntax.TAB
          + assumeNotExecutedExpr.createString()
          + SeqSyntax.SEMICOLON
          + SeqSyntax.NEWLINE
          + SeqSyntax.TAB
          + assignExpr.createString()
          + SeqSyntax.NEWLINE
          + SeqSyntax.TAB;

  private static final String postSwitchCase = SeqSyntax.NEWLINE + SeqSyntax.CURLY_BRACKET_RIGHT;

  public Interleaving(ImmutableMap<MPORThread, CFAEdge> pGlobalAccesses) {
    switchCaseExpr = new SwitchCaseExpr(nextThread, pGlobalAccesses);
  }

  // TODO create goto statements in the switch case after an edge is executed
  //  e.g. interleave abc and execute a to reach a', then the next state will interleave a'bc
  private final SwitchCaseExpr switchCaseExpr;

  private static FunctionCallExpr initAnyFalseCall() {
    ImmutableList.Builder<SeqExpression> parameters = ImmutableList.builder();
    parameters.add(executed);
    parameters.add(numThreads);
    return new FunctionCallExpr(SeqToken.ANY_FALSE, Optional.of(parameters.build()));
  }

  private static ImmutableList<SeqExpression> initAssumeNextThread() {
    ImmutableList.Builder<SeqExpression> rAssumeCondition = ImmutableList.builder();
    rAssumeCondition.add(
        new BooleanExpr(
            new BooleanExpr(new Value(SeqValue.ZERO), SeqOperator.LESS_OR_EQUAL, nextThread),
            SeqOperator.AND,
            new BooleanExpr(nextThread, SeqOperator.LESS, numThreads)));
    return rAssumeCondition.build();
  }

  private static ImmutableList<SeqExpression> initAssumeNotExecuted() {
    ImmutableList.Builder<SeqExpression> rAssumeCondition = ImmutableList.builder();
    rAssumeCondition.add(new NegationExpr(arrayExpr));
    return rAssumeCondition.build();
  }

  @Override
  public String createString() {
    return preSwitchCase + switchCaseExpr.createString() + postSwitchCase;
  }
}
