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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqDataType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqElement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqValue;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.ArrayExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.ArrayInitExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.AssignExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.BooleanExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.DeclareExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.FunctionCallExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.LoopExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.NegationExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.Operator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.SwitchExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.data_entity.Value;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.data_entity.Variable;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

/**
 * A class representing a non-deterministic interleaving of global access edges. A {@link
 * SeqInterleaving} is created if at least one pair of {@link SeqInterleaving#globalAccesses}.
 */
public class SeqInterleaving implements SeqElement {

  private final ImmutableMap<MPORThread, CFAEdge> globalAccesses;

  public SeqInterleaving(ImmutableMap<MPORThread, CFAEdge> pGlobalAccesses) {
    globalAccesses = pGlobalAccesses;

    Variable numThreads = new Variable(SeqToken.NUM_THREADS);
    Variable nextThread = new Variable(SeqToken.NEXT_THREAD);

    arrayExpr = new ArrayExpr(new Variable(SeqToken.EXECUTED), numThreads);
    arrayInitExpr = new ArrayInitExpr(SeqDataType.BOOL, arrayExpr, new Value(SeqValue.FALSE));
    // TODO create separate any_false method
    //  if all values in the given array are true, return true
    loopExpr = new LoopExpr(new Value(SeqValue.TRUE));
    declareExpr = new DeclareExpr(SeqDataType.INT, nextThread, new Variable(SeqToken.NON_DET));
    assumeNextThreadExpr =
        new FunctionCallExpr(
            SeqToken.ASSUME,
            new BooleanExpr(
                new BooleanExpr(new Value(SeqValue.ZERO), Operator.LESS_OR_EQUAL, nextThread),
                Operator.AND,
                new BooleanExpr(nextThread, Operator.LESS, numThreads)));
    assumeNotExecutedExpr = new FunctionCallExpr(SeqToken.ASSUME, new NegationExpr(arrayExpr));
    assignExpr = new AssignExpr(arrayExpr, new Value(SeqValue.TRUE));
    switchExpr = new SwitchExpr(nextThread, globalAccesses);
  }

  private final ArrayExpr arrayExpr;

  private final ArrayInitExpr arrayInitExpr;

  private final LoopExpr loopExpr;

  private final DeclareExpr declareExpr;

  private final FunctionCallExpr assumeNextThreadExpr;

  private final FunctionCallExpr assumeNotExecutedExpr;

  private final AssignExpr assignExpr;

  private final SwitchExpr switchExpr;

  @Override
  public String generateString() {
    return ""; // TODO
  }
}
