/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.composite2;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ForcedCoveringStopOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

class Composite2StopOperator implements StopOperator, ForcedCoveringStopOperator {

  private final ImmutableList<StopOperator> stopOperators;

  Composite2StopOperator(ImmutableList<StopOperator> stopOperators) {
    this.stopOperators = stopOperators;
  }

  @Override
  public boolean stop(AbstractState element, Collection<AbstractState> reached, Precision precision) throws CPAException, InterruptedException {
    Composite2State compositeState = (Composite2State) element;
    Composite2Precision compositePrecision = (Composite2Precision) precision;

    for (AbstractState e : reached) {
      if (stop(compositeState, (Composite2State)e, compositePrecision)) {
        return true;
      }
    }
    return false;
  }

  private boolean stop(Composite2State compositeState, Composite2State compositeReachedState, Composite2Precision compositePrecision) throws CPAException, InterruptedException {
    List<? extends AbstractState> compositeElements = compositeState.getWrappedStates();
    checkArgument(compositeElements.size() == stopOperators.size(), "State with wrong number of component states given");
    List<? extends AbstractState> compositeReachedStates = compositeReachedState.getWrappedStates();

    List<Precision> compositePrecisions = compositePrecision.getWrappedPrecisions();

    for (int idx = 0; idx < compositeElements.size(); idx++) {
      StopOperator stopOp = stopOperators.get(idx);

      AbstractState absElem1 = compositeElements.get(idx);
      AbstractState absElem2 = compositeReachedStates.get(idx);
      Precision prec = compositePrecisions.get(idx);

      if (!stopOp.stop(absElem1, Collections.singleton(absElem2), prec)) {
        return false;
      }
    }
    return true;
  }

  boolean isCoveredBy(AbstractState pElement, AbstractState pOtherElement, List<ConfigurableProgramAnalysis> cpas) throws CPAException, InterruptedException {
    Composite2State compositeState = (Composite2State)pElement;
    Composite2State compositeOtherElement = (Composite2State)pOtherElement;

    List<? extends AbstractState> componentElements = compositeState.getWrappedStates();
    List<? extends AbstractState> componentOtherElements = compositeOtherElement.getWrappedStates();

    if (componentElements.size() != cpas.size()) {
      return false;
    }

    for (int idx = 0; idx < componentElements.size(); idx++) {
      ProofChecker componentProofChecker = (ProofChecker)cpas.get(idx);

      AbstractState absElem1 = componentElements.get(idx);
      AbstractState absElem2 = componentOtherElements.get(idx);

      if (!componentProofChecker.isCoveredBy(absElem1, absElem2)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public boolean isForcedCoveringPossible(AbstractState pElement, AbstractState pReachedState, Precision pPrecision) throws CPAException, InterruptedException {

    Composite2State compositeState = (Composite2State)pElement;
    Composite2State compositeReachedState = (Composite2State)pReachedState;
    Composite2Precision compositePrecision = (Composite2Precision)pPrecision;

    List<? extends AbstractState> compositeElements = compositeState.getWrappedStates();
    List<? extends AbstractState> compositeReachedStates = compositeReachedState.getWrappedStates();
    List<Precision> compositePrecisions = compositePrecision.getWrappedPrecisions();

    for (int idx = 0; idx < compositeElements.size(); idx++) {
      StopOperator stopOp = stopOperators.get(idx);

      AbstractState wrappedState = compositeElements.get(idx);
      AbstractState wrappedReachedState = compositeReachedStates.get(idx);
      Precision prec = compositePrecisions.get(idx);

      boolean possible;
      if (stopOp instanceof ForcedCoveringStopOperator) {

        possible = ((ForcedCoveringStopOperator)stopOp).isForcedCoveringPossible(wrappedState, wrappedReachedState, prec);

      } else {
        possible = stopOp.stop(wrappedState, Collections.singleton(wrappedReachedState), prec);
      }

      if (!possible) {
        return false;
      }
    }

    return true;
  }
}
