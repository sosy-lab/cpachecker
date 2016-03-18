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
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class CompositePrecisionAdjustment implements PrecisionAdjustment {
  protected final ImmutableList<PrecisionAdjustment> precisionAdjustments;
  protected final ImmutableList<StateProjectionFunction> stateProjectionFunctions;
  protected final ImmutableList<PrecisionProjectionFunction> precisionProjectionFunctions;

  private final LogManager logger;

  public CompositePrecisionAdjustment(
      ImmutableList<PrecisionAdjustment> precisionAdjustments, LogManager pLogger) {
    this.precisionAdjustments = precisionAdjustments;
    logger = pLogger;

    ImmutableList.Builder<StateProjectionFunction> stateProjectionFunctions = ImmutableList.builder();
    ImmutableList.Builder<PrecisionProjectionFunction> precisionProjectionFunctions = ImmutableList.builder();

    for (int i = 0; i < precisionAdjustments.size(); i++) {
      stateProjectionFunctions.add(new StateProjectionFunction(i));
      precisionProjectionFunctions.add(new PrecisionProjectionFunction(i));
    }
    this.stateProjectionFunctions = stateProjectionFunctions.build();
    this.precisionProjectionFunctions = precisionProjectionFunctions.build();
  }

  protected static class StateProjectionFunction implements Function<AbstractState, AbstractState> {

    private final int dimension;

    public StateProjectionFunction(int d) {
      dimension = d;
    }

    @Override
    public AbstractState apply(AbstractState from) {
      return ((CompositeState)from).get(dimension);
    }
  }

  protected static class PrecisionProjectionFunction
  implements Function<Precision, Precision> {
    private final int dimension;

    public PrecisionProjectionFunction(int d) {
      dimension = d;
    }

    @Override
    public Precision apply(Precision from) {
      return ((CompositePrecision)from).get(dimension);
    }
  }


  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pElement,
      Precision pPrecision,
      UnmodifiableReachedSet pElements,
      Function<AbstractState, AbstractState> projection,
      AbstractState fullState) throws CPAException, InterruptedException {

    CompositeState comp = (CompositeState) pElement;
    CompositePrecision prec = (CompositePrecision) pPrecision;
    assert (comp.getWrappedStates().size() == prec.getPrecisions().size());
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
        return Optional.absent();
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

    CompositeState outElement = modified ? new CompositeState(outElements.build())     : comp;
    CompositePrecision outPrecision     = modified ? new CompositePrecision(outPrecisions.build()) : prec;

    outElements = ImmutableList.builder();
    outPrecisions = ImmutableList.builder();

    // TODO: call in a fixpoint??
    modified = true;

    while (modified) {
      modified = false;
      for (int i = 0; i < dim; ++i) {
        PrecisionAdjustment precisionAdjustment = precisionAdjustments.get(i);

        AbstractState oldElement = outElement.get(i);
        Precision oldPrecision = outPrecision.get(i);

        Optional<PrecisionAdjustmentResult> strengthenResult =
            precisionAdjustment.postAdjustmentStrengthen(
                oldElement,
                oldPrecision,
                Iterables.concat(
                    outElement.getWrappedStates().subList(0, i),
                    outElement.getWrappedStates()
                        .subList(i + 1, outElement.getWrappedStates().size())),
                Iterables.concat(
                    outPrecision.getPrecisions().subList(0, i),
                    outPrecision.getPrecisions()
                        .subList(i + 1, outElement.getWrappedStates().size())),
                pElements,
                Functions.compose(stateProjectionFunctions.get(i), projection),
                fullState
            );

        if (!strengthenResult.isPresent()) {
          return Optional.absent();
        }
        PrecisionAdjustmentResult inner = strengthenResult.get();

        // TODO: stop code duplication.
        if (inner.action() == Action.BREAK) {
          action = Action.BREAK;
        }
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

      outElement = modified ? new CompositeState(outElements.build())     : outElement;
      outPrecision     = modified ? new CompositePrecision(outPrecisions.build()) : outPrecision;
    }

    PrecisionAdjustmentResult out =
        PrecisionAdjustmentResult.create(outElement, outPrecision, action);
    return Optional.of(out);
  }

  @Override
  public Optional<PrecisionAdjustmentResult> postAdjustmentStrengthen(
      AbstractState result,
      Precision pPrecision,
      Iterable<AbstractState> otherStates,
      Iterable<Precision> otherPrecisions,
      UnmodifiableReachedSet states,
      Function<AbstractState, AbstractState> stateProjection,
      AbstractState resultFullState
  ) throws CPAException, InterruptedException {

    // This should never be called.
    return Optional.of(PrecisionAdjustmentResult.create(result, pPrecision, Action.CONTINUE));
  }
}
