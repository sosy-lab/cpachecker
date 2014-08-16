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

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSetView;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;

public class CompositePrecisionAdjustment implements PrecisionAdjustment {

  protected final ImmutableList<PrecisionAdjustment> precisionAdjustments;
  protected final ImmutableList<StateProjectionFunction> stateProjectionFunctions;
  protected final ImmutableList<PrecisionProjectionFunction> precisionProjectionFunctions;

  public CompositePrecisionAdjustment(ImmutableList<PrecisionAdjustment> precisionAdjustments) {
    this.precisionAdjustments = precisionAdjustments;

    ImmutableList.Builder<StateProjectionFunction> stateProjectionFunctions = ImmutableList.builder();
    ImmutableList.Builder<PrecisionProjectionFunction> precisionProjectionFunctions = ImmutableList.builder();

    for (int i = 0; i < precisionAdjustments.size(); i++) {
      stateProjectionFunctions.add(new StateProjectionFunction(i));
      precisionProjectionFunctions.add(new PrecisionProjectionFunction(i));
    }
    this.stateProjectionFunctions = stateProjectionFunctions.build();
    this.precisionProjectionFunctions = precisionProjectionFunctions.build();
  }

  protected static class StateProjectionFunction
    implements Function<AbstractState, AbstractState> {
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

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment#prec(org.sosy_lab.cpachecker.core.interfaces.AbstractState, org.sosy_lab.cpachecker.core.interfaces.Precision, java.util.Collection)
   */
  @Override
  public PrecisionAdjustmentResult prec(AbstractState pElement,
                                               Precision pPrecision,
                                               UnmodifiableReachedSet pElements) throws CPAException, InterruptedException {
    CompositeState comp = (CompositeState) pElement;
    CompositePrecision prec = (CompositePrecision) pPrecision;
    assert (comp.getWrappedStates().size() == prec.getPrecisions().size());
    int dim = comp.getWrappedStates().size();

    ImmutableList.Builder<AbstractState> outElements = ImmutableList.builder();
    ImmutableList.Builder<Precision> outPrecisions = ImmutableList.builder();

    boolean modified = false;
    Action action = Action.CONTINUE;

    for (int i = 0; i < dim; ++i) {
      UnmodifiableReachedSet slice =
        new UnmodifiableReachedSetView(pElements, stateProjectionFunctions.get(i), precisionProjectionFunctions.get(i));
      PrecisionAdjustment precisionAdjustment = precisionAdjustments.get(i);
      AbstractState oldElement = comp.get(i);
      Precision oldPrecision = prec.get(i);
      PrecisionAdjustmentResult out = precisionAdjustment.prec(oldElement, oldPrecision, slice);
      AbstractState newElement = out.abstractState();
      Precision newPrecision = out.precision();
      if (out.action() == Action.BREAK) {
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

    return PrecisionAdjustmentResult.create(outElement, outPrecision, action);
  }

}
