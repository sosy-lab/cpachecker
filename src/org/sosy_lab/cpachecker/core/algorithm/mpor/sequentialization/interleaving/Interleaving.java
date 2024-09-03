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
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqElement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.Value;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.Variable;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.BooleanExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.DeclareExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.FunctionCallExpr;
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

  private static final DeclareExpr declareNextThread =
      new DeclareExpr(
          new VariableExpr(SeqDataType.INT, nextThread),
          new FunctionCallExpr(SeqToken.NON_DET, Optional.empty()));

  private static final FunctionCallExpr assumeNextThread =
      new FunctionCallExpr(SeqToken.ASSUME, Optional.of(initAssumeNextThread()));

  private static final String preSwitchCase =
      declareNextThread.createString()
          + SeqSyntax.NEWLINE
          + SeqSyntax.TAB
          + assumeNextThread.createString()
          + SeqSyntax.SEMICOLON
          + SeqSyntax.NEWLINE;

  private static final String postSwitchCase = SeqSyntax.NEWLINE + SeqSyntax.CURLY_BRACKET_RIGHT;

  private static ImmutableList<SeqExpression> initAssumeNextThread() {
    ImmutableList.Builder<SeqExpression> rAssumeCondition = ImmutableList.builder();
    rAssumeCondition.add(
        new BooleanExpr(
            new BooleanExpr(new Value(SeqValue.ZERO), SeqOperator.LESS_OR_EQUAL, nextThread),
            SeqOperator.AND,
            new BooleanExpr(nextThread, SeqOperator.LESS, numThreads)));
    return rAssumeCondition.build();
  }

  private final SwitchCaseExpr switchCaseExpr;

  public Interleaving(ImmutableMap<MPORThread, ImmutableSet<CFAEdge>> pGlobalAccesses) {
    ImmutableSet.Builder<String> cases = ImmutableSet.builder();
    for (var entry : pGlobalAccesses.entrySet()) {
      cases.add(generateCase(Integer.toString(entry.getKey().id), entry.getValue()));
    }
    switchCaseExpr = new SwitchCaseExpr(nextThread, cases.build());
  }

  // TODO include goto state statements (execute edges and create new states beforehand)
  private String generateCase(String pCaseNumber, ImmutableSet<CFAEdge> pEdges) {
    // no edge: no case
    if (pEdges.isEmpty()) {
      return SeqSyntax.EMPTY_STRING;

      // one edge: deterministic execution
    } else if (pEdges.size() == 1) {
      CFAEdge singleEdge = pEdges.iterator().next();
      return SeqUtil.generateCase(pCaseNumber, SeqUtil.createLineOfCode(singleEdge));

      // multiple edges: separate nondeterministic switch case
    } else {
      AssumeInterleaving assumeInterleaving = new AssumeInterleaving(pEdges);
      return assumeInterleaving.createString();
    }
  }

  @Override
  public String createString() {
    return preSwitchCase + switchCaseExpr.createString() + postSwitchCase;
  }
}
