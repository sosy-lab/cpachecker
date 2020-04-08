/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.overflow2;

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

public class OverflowTransferRelation2 extends SingleEdgeTransferRelation {

  private final CBinaryExpressionBuilder expressionBuilder;
  private final ArithmeticOverflowAssumptionBuilder noOverflowAssumptionBuilder;

  public OverflowTransferRelation2(
      ArithmeticOverflowAssumptionBuilder pNoOverflowAssumptionBuilder,
      CBinaryExpressionBuilder pExpressionBuilder) {
    expressionBuilder = pExpressionBuilder;
    noOverflowAssumptionBuilder = pNoOverflowAssumptionBuilder;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    OverflowState2
        prev = (OverflowState2) state;

    if (prev.hasOverflow()) {

      // Once we have an overflow there is no need to continue.
      return ImmutableList.of();
    }

    int leavingEdgesOfNextState = cfaEdge.getSuccessor().getNumLeavingEdges();
    Set<CExpression> assumptions;
    ImmutableList.Builder<OverflowState2> outStates = ImmutableList.builder();

    if(leavingEdgesOfNextState == 0) {
      return ImmutableList.of(new OverflowState2(ImmutableSet.of(), prev.nextHasOverflow(), prev.nextHasOverflow(), prev));
    }

    for (int i = 0; i < leavingEdgesOfNextState; i++) {
      assumptions = noOverflowAssumptionBuilder.assumptionsForEdge(cfaEdge.getSuccessor().getLeavingEdge(i));

      if (assumptions.isEmpty()) {
         outStates.add(new OverflowState2(ImmutableSet.of(), prev.nextHasOverflow(), prev.nextHasOverflow(), prev));
         continue;
      }

      for (CExpression assumption : assumptions) {
        outStates.add(new OverflowState2(ImmutableSet.of(mkNot(assumption)), prev.nextHasOverflow(), true, prev));
      }

      // No overflows <=> all assumptions hold.
      outStates.add(new OverflowState2(assumptions, prev.nextHasOverflow(), prev.nextHasOverflow(), prev));
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
      OverflowState2
          overflowState = (OverflowState2) state;
      overflowState.updateStatesForPreconditions(otherStates);
      return Collections.singleton(overflowState);
  }
}
