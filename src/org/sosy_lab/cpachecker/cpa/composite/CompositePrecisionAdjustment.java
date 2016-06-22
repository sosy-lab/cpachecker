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

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.logging.Level;

class CompositePrecisionAdjustment implements PrecisionAdjustment {
  private final ImmutableList<PrecisionAdjustment> precisionAdjustments;
  private final ImmutableList<Function<AbstractState, AbstractState>> stateProjectionFunctions;

  private final LogManager logger;

  CompositePrecisionAdjustment(
      ImmutableList<PrecisionAdjustment> precisionAdjustments, LogManager pLogger) {
    this.precisionAdjustments = precisionAdjustments;
    logger = pLogger;

    ImmutableList.Builder<Function<AbstractState, AbstractState>> stateProjectionFunctions =
        ImmutableList.builder();
    for (int i = 0; i < precisionAdjustments.size(); i++) {
      stateProjectionFunctions.add(getStateProjectionFunction(i));
    }
    this.stateProjectionFunctions = stateProjectionFunctions.build();
  }

  private final Function<AbstractState, AbstractState> getStateProjectionFunction(int i) {
    return compState -> ((CompositeState) compState).get(i);
  }

  /* (non-Javadoc)
   * @see PrecisionAdjustment#prec
   */
  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pElement,
      Precision pPrecision,
      UnmodifiableReachedSet pElements,
      Function<AbstractState, AbstractState> projection,
      AbstractState fullState) throws CPAException, InterruptedException {
    return prec0(pElement, pPrecision, pElements, projection, fullState, 1);
  }

  private Optional<PrecisionAdjustmentResult> prec0(
      AbstractState pElement,
      Precision pPrecision,
      UnmodifiableReachedSet pElements,
      Function<AbstractState, AbstractState> projection,
      AbstractState fullState,
      int depth)
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

    AbstractState outElement = modified ? new CompositeState(outElements.build())     : pElement;
    Precision outPrecision     = modified ? new CompositePrecision(outPrecisions.build()) : pPrecision;

    PrecisionAdjustmentResult out = PrecisionAdjustmentResult.create(outElement, outPrecision, action);

    if (!modified) {

      logger.log(Level.FINER, "Precision adjustment iteration has converged.");
      return Optional.of(out);
    } else {

      // Recursion is acceptable here as we have very small chains.
      logger.log(Level.FINER, "Starting new fixpoint iteration of precision adjustment");
      return prec0(outElement, outPrecision, pElements, projection, fullState, depth+1);
    }
  }
}
