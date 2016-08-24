/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.composite;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class CompositePrecisionAdjustment implements PrecisionAdjustment {
  private final ImmutableList<PrecisionAdjustment> precisionAdjustments;
  private final ImmutableList<Function<AbstractState, AbstractState>> stateProjectionFunctions;

  CompositePrecisionAdjustment(ImmutableList<PrecisionAdjustment> precisionAdjustments) {
    this.precisionAdjustments = precisionAdjustments;

    ImmutableList.Builder<Function<AbstractState, AbstractState>> stateProjectionFunctions =
        ImmutableList.builder();
    for (int i = 0; i < precisionAdjustments.size(); i++) {
      stateProjectionFunctions.add(getStateProjectionFunction(i));
    }
    this.stateProjectionFunctions = stateProjectionFunctions.build();
  }

  private Function<AbstractState, AbstractState> getStateProjectionFunction(int i) {
    return compState -> ((CompositeState) compState).get(i);
  }

  /**
   * @see PrecisionAdjustment#prec
   */
  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pElement,
      Precision pPrecision,
      UnmodifiableReachedSet pElements,
      Function<AbstractState, AbstractState> projection,
      AbstractState fullState)
      throws CPAException, InterruptedException {

    CompositeState comp = (CompositeState) pElement;
    CompositePrecision prec = (CompositePrecision) pPrecision;
    assert (comp.getWrappedStates().size() == prec.getWrappedPrecisions().size());
    int dim = comp.getWrappedStates().size();

    ImmutableList.Builder<AbstractState> outElements = ImmutableList.builder();
    ImmutableList.Builder<Precision> outPrecisions = ImmutableList.builder();

    boolean modified = false;
    Action action = Action.CONTINUE;

    for (int i = 0; i < dim; ++i) {
      PrecisionAdjustment precisionAdjustment = precisionAdjustments.get(i);
      AbstractState oldElement = comp.get(i);
      Precision oldPrecision = prec.get(i);
      Optional<PrecisionAdjustmentResult> out = precisionAdjustment.prec(
          oldElement, oldPrecision, pElements,
          Functions.compose(stateProjectionFunctions.get(i), projection),
          fullState
      );

      if (!out.isPresent()) {
        return Optional.empty();
      }

      PrecisionAdjustmentResult inner = out.get();

      AbstractState newElement = inner.abstractState();
      Precision newPrecision = inner.precision();
      if (inner.action() == Action.BREAK) {
        action = Action.BREAK;
      }

      if ((newElement != oldElement) || (newPrecision != oldPrecision)) {
        // something has changed
        modified = true;
      }
      outElements.add(newElement);
      outPrecisions.add(newPrecision);
    }

    CompositeState outElement = modified ? new CompositeState(outElements.build()) : comp;
    CompositePrecision outPrecision =
        modified ? new CompositePrecision(outPrecisions.build()) : prec;
    Optional<CompositeState> outElementStrengthened = callStrengthen(outElement, outPrecision);
    if (!outElementStrengthened.isPresent()) {
      return Optional.empty();
    }
    outElement = outElementStrengthened.get();

    PrecisionAdjustmentResult out =
        PrecisionAdjustmentResult.create(outElement, outPrecision, action);

    return Optional.of(out);
  }

  /**
   * Call {@link #strengthen(AbstractState, Precision, List)} on contained precision adjustments.
   * Returns identity if all of the strengthening operations are identities.
   */
  private Optional<CompositeState> callStrengthen(
      CompositeState pCompositeState, CompositePrecision pCompositePrecision)
      throws CPAException, InterruptedException {
    List<AbstractState> wrappedStates = pCompositeState.getWrappedStates();
    List<Precision> wrappedPrecisions = pCompositePrecision.getWrappedPrecisions();
    int dim = wrappedStates.size();
    ImmutableList.Builder<AbstractState> newElements = ImmutableList.builder();

    boolean modified = false;
    for (int i = 0; i < dim; i++) {
      PrecisionAdjustment precisionAdjustment = precisionAdjustments.get(i);
      AbstractState oldElement = wrappedStates.get(i);
      Precision oldPrecision = wrappedPrecisions.get(i);
      Optional<? extends AbstractState> out =
          precisionAdjustment.strengthen(
              oldElement,
              oldPrecision,
              Stream.concat(
                      wrappedStates.subList(0, i).stream(),
                      wrappedStates.subList(i + 1, dim).stream())
                  .collect(Collectors.toList()));
      if (!out.isPresent()) {
        return Optional.empty();
      }
      AbstractState unwrapped = out.get();
      if (unwrapped != oldElement) {
        modified = true;
      }
      newElements.add(unwrapped);
    }
    CompositeState outState = modified ? new CompositeState(newElements.build()) : pCompositeState;
    return Optional.of(outState);
  }
}
