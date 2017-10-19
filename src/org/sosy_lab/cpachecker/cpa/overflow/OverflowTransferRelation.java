/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.overflow;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
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
      return Collections.emptyList();
    }

    List<CExpression> assumptions = noOverflowAssumptionBuilder.assumptionsForEdge(cfaEdge);
    if (assumptions.isEmpty()) {
      return ImmutableList.of(new OverflowState(ImmutableList.of(), false, prev));
    }

    ImmutableList.Builder<OverflowState> outStates = ImmutableList.builder();

    outStates.addAll(
        Lists.transform(
            assumptions,
            // Overflow <=> there exists a violating assumption.
            a -> new OverflowState(ImmutableList.of(mkNot(a)), true, prev)));

    // No overflows <=> all assumptions hold.
    outStates.add(new OverflowState(assumptions, false, prev));

    return outStates.build();
  }

  private CExpression mkNot(CExpression arg) {
    try {
      return expressionBuilder.negateExpressionAndSimplify(arg);
    } catch (UnrecognizedCCodeException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState state,
      List<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision precision)
      throws CPATransferException, InterruptedException {
    Optional<AbstractState> optionalPredicateState =
        otherStates.stream().filter(x -> x instanceof PredicateAbstractState).findFirst();
    if (optionalPredicateState.isPresent()) {
      PredicateAbstractState predicateState = (PredicateAbstractState) optionalPredicateState.get();
      OverflowState overflowState = (OverflowState) state;
      overflowState.updatePathFormulas(predicateState.getPathFormula());
      return Collections.singleton(overflowState);
    } else {
      return Collections.singleton(state);
    }
  }
}
