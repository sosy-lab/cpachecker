// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.composite;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

class CompositePrecisionAdjustment implements PrecisionAdjustment {
  private final ImmutableList<PrecisionAdjustment> precisionAdjustments;
  private final ImmutableList<Function<AbstractState, AbstractState>> stateProjectionFunctions;

  CompositePrecisionAdjustment(ImmutableList<PrecisionAdjustment> precisionAdjustments) {
    this.precisionAdjustments = precisionAdjustments;

    ImmutableList.Builder<Function<AbstractState, AbstractState>> stateProjections =
        ImmutableList.builder();
    for (int i = 0; i < precisionAdjustments.size(); i++) {
      stateProjections.add(getStateProjectionFunction(i));
    }
    stateProjectionFunctions = stateProjections.build();
  }

  private Function<AbstractState, AbstractState> getStateProjectionFunction(int i) {
    return compState -> ((CompositeState) compState).get(i);
  }

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
      Optional<PrecisionAdjustmentResult> out =
          precisionAdjustment.prec(
              oldElement,
              oldPrecision,
              pElements,
              Functions.compose(stateProjectionFunctions.get(i), projection),
              fullState);

      if (!out.isPresent()) {
        return Optional.empty();
      }

      PrecisionAdjustmentResult inner = out.orElseThrow();

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
    outElement = outElementStrengthened.orElseThrow();

    PrecisionAdjustmentResult out =
        PrecisionAdjustmentResult.create(outElement, outPrecision, action);

    return Optional.of(out);
  }

  /**
   * Call {@link #strengthen(AbstractState, Precision, Iterable)} on contained precision
   * adjustments. Returns identity if all of the strengthening operations are identities.
   */
  private Optional<CompositeState> callStrengthen(
      CompositeState pCompositeState, CompositePrecision pCompositePrecision)
      throws CPAException, InterruptedException {
    ImmutableList<AbstractState> wrappedStates = pCompositeState.getWrappedStates();
    ImmutableList<Precision> wrappedPrecisions = pCompositePrecision.getWrappedPrecisions();
    int dim = wrappedStates.size();
    ImmutableList.Builder<AbstractState> newElements = ImmutableList.builder();

    boolean modified = false;
    for (int i = 0; i < dim; i++) {
      PrecisionAdjustment precisionAdjustment = precisionAdjustments.get(i);
      AbstractState oldElement = wrappedStates.get(i);
      Precision oldPrecision = wrappedPrecisions.get(i);
      Iterable<AbstractState> otherStates =
          Iterables.concat(wrappedStates.subList(0, i), wrappedStates.subList(i + 1, dim));

      Optional<? extends AbstractState> out =
          precisionAdjustment.strengthen(oldElement, oldPrecision, otherStates);
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
