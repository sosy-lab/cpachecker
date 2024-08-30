// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.interleaving;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqElement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.ArrayExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.ArrayInitExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.AssignExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.BooleanExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.DeclareExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.FunctionCallExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.LoopExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.NegationExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.SwitchCaseExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.data_entity.Value;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.data_entity.Variable;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqDataType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqValue;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

/**
 * A class representing a non-deterministic interleaving of global access edges. A {@link
 * SeqInterleaving} is created if at least one pair of global accesses does not commute.
 */
public class SeqInterleaving implements SeqElement {

  private final SwitchCaseExpr switchCaseExpr;

  public SeqInterleaving(ImmutableMap<MPORThread, CFAEdge> pGlobalAccesses) {
    // TODO create separate any_false method
    //  if all values in the given array are true, return true
    switchCaseExpr = new SwitchCaseExpr(nextThread, pGlobalAccesses);
  }

  private static final Variable numThreads = new Variable(SeqToken.NUM_THREADS);

  private static final Variable nextThread = new Variable(SeqToken.NEXT_THREAD);

  private static final ArrayExpr arrayExpr =
      new ArrayExpr(new Variable(SeqToken.EXECUTED), numThreads);

  private static final ArrayInitExpr arrayInitExpr =
      new ArrayInitExpr(SeqDataType.BOOL, arrayExpr, new Value(SeqValue.FALSE));

  // TODO use any_false method here
  private static final LoopExpr loopExpr = new LoopExpr(new Value(SeqValue.TRUE));

  private static final DeclareExpr declareExpr =
      new DeclareExpr(SeqDataType.INT, nextThread, new FunctionCallExpr(SeqToken.NON_DET));

  private static final FunctionCallExpr assumeNextThreadExpr =
      new FunctionCallExpr(
          SeqToken.ASSUME,
          new BooleanExpr(
              new BooleanExpr(new Value(SeqValue.ZERO), SeqOperator.LESS_OR_EQUAL, nextThread),
              SeqOperator.AND,
              new BooleanExpr(nextThread, SeqOperator.LESS, numThreads)));

  private static final FunctionCallExpr assumeNotExecutedExpr =
      new FunctionCallExpr(SeqToken.ASSUME, new NegationExpr(arrayExpr));

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

  @Override
  public String createString() {
    return preSwitchCase + switchCaseExpr.createString() + postSwitchCase;
  }
}
