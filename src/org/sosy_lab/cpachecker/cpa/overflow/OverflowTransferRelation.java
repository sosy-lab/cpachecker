// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.overflow;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.ArithmeticOverflowAssumptionBuilder;

public class OverflowTransferRelation extends SingleEdgeTransferRelation {

  private final CBinaryExpressionBuilder expressionBuilder;
  private final ArithmeticOverflowAssumptionBuilder noOverflowAssumptionBuilder;

  public OverflowTransferRelation(
      ArithmeticOverflowAssumptionBuilder pNoOverflowAssumptionBuilder,
      CBinaryExpressionBuilder pExpressionBuilder) {
    expressionBuilder = pExpressionBuilder;
    noOverflowAssumptionBuilder = pNoOverflowAssumptionBuilder;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    OverflowState prev = (OverflowState) state;

    if (prev.hasOverflow()) {

      // Once we have an overflow there is no need to continue.
      return ImmutableList.of();
    }

    boolean nextHasOverflow = prev.nextHasOverflow();
    int leavingEdgesOfNextState = cfaEdge.getSuccessor().getNumLeavingEdges();
    Set<CExpression> assumptions;
    ImmutableList.Builder<OverflowState> outStates = ImmutableList.builder();

    if (leavingEdgesOfNextState == 0) {
      return ImmutableList.of(new OverflowState(ImmutableSet.of(), nextHasOverflow, prev));
    }

    for (int i = 0; i < leavingEdgesOfNextState; i++) {
      assumptions =
          noOverflowAssumptionBuilder.assumptionsForEdge(cfaEdge.getSuccessor().getLeavingEdge(i));

      if (assumptions.isEmpty()) {
        outStates.add(new OverflowState(ImmutableSet.of(), nextHasOverflow, prev));
        continue;
      }

      for (CExpression assumption : assumptions) {
        outStates.add(new OverflowState(ImmutableSet.of(mkNot(assumption)), true, prev));
      }

      // No overflows <=> all assumptions hold.
      outStates.add(new OverflowState(assumptions, nextHasOverflow, prev));
    }

    return outStates.build();
  }

  private CExpression mkNot(CExpression arg) {
    try {
      return expressionBuilder.negateExpressionAndSimplify(arg);
    } catch (UnrecognizedCodeException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState state,
      Iterable<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision precision)
      throws CPATransferException, InterruptedException {
    OverflowState overflowState = (OverflowState) state;
    return Collections.singleton(overflowState);
  }
}
