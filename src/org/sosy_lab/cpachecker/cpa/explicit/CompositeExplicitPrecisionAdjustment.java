/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.explicit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSetView;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecisionAdjustment;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

import com.google.common.collect.ImmutableList;

public class CompositeExplicitPrecisionAdjustment extends CompositePrecisionAdjustment implements StatisticsProvider {

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
  }

  private boolean modified = false;

  public CompositeExplicitPrecisionAdjustment(ImmutableList<PrecisionAdjustment> precisionAdjustments) {
    super(precisionAdjustments);
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment#prec(org.sosy_lab.cpachecker.core.interfaces.AbstractState, org.sosy_lab.cpachecker.core.interfaces.Precision, java.util.Collection)
   */
  @Override
  public Triple<AbstractState, Precision, Action> prec(AbstractState pElement,
                                               Precision pPrecision,
                                               UnmodifiableReachedSet pElements) throws CPAException {
    modified = false;

    CompositeState composite    = (CompositeState)pElement;
    CompositePrecision precision  = (CompositePrecision)pPrecision;
    assert (composite.getWrappedStates().size() == precision.getPrecisions().size());

    int indexOfExplicitState = getIndexOfExplicitState(composite);
    if (indexOfExplicitState == -1) {
      throw new CPAException("The CompositeExplicitPrecisionAdjustment needs an ExplicitState");
    }

    ImmutableList.Builder<AbstractState> outElements  = ImmutableList.builder();
    ImmutableList.Builder<Precision> outPrecisions    = ImmutableList.builder();

    Action action = Action.CONTINUE;

    for (int i = 0, size = composite.getWrappedStates().size(); i < size; ++i) {
      UnmodifiableReachedSet slice = new UnmodifiableReachedSetView(pElements, stateProjectionFunctions.get(i), precisionProjectionFunctions.get(i));
      PrecisionAdjustment precisionAdjustment = precisionAdjustments.get(i);
      AbstractState oldElement = composite.get(i);
      Precision oldPrecision = precision.get(i);

      // enforce thresholds for explicit element, by incorporating information from reached set and path condition element
      if (i == indexOfExplicitState) {
        ExplicitState explicitState         = (ExplicitState)oldElement;
        ExplicitPrecision explicitPrecision = (ExplicitPrecision)oldPrecision;

        explicitPrecision.setLocation(AbstractStates.extractStateByType(composite, LocationState.class).getLocationNode());
        List<String> toDrop = new ArrayList<String>();
        for(String variableName : explicitState.getConstantsMap().keySet()) {
          if(!explicitPrecision.isTracking(variableName) && explicitState.deltaContains(variableName)) {
            toDrop.add(variableName);
          }
        }

        for(String variableName : toDrop) {
          explicitState.forget(variableName);
        }

        explicitState.resetDelta();

        outElements.add(explicitState);
        outPrecisions.add(explicitPrecision);
      }
      else {
        Triple<AbstractState, Precision, Action> result = precisionAdjustment.prec(oldElement, oldPrecision, slice);
        AbstractState newElement = result.getFirst();
        Precision newPrecision = result.getSecond();

        if (result.getThird() == Action.BREAK) {
          action = Action.BREAK;
        }

        if ((newElement != oldElement) || (newPrecision != oldPrecision)) {
          // something has changed
          modified = true;
        }
        outElements.add(newElement);
        outPrecisions.add(newPrecision);
      }
    }

    AbstractState outElement = pElement;
    Precision outPrecision   = pPrecision;
    if(modified) {
      outElement    = new CompositeState(outElements.build());
      outPrecision  = new CompositePrecision(outPrecisions.build());
    }

    return Triple.of(outElement, outPrecision, action);
  }

  private int getIndexOfExplicitState(CompositeState composite) {
    for (int i = 0; i < composite.getWrappedStates().size(); ++i) {
      if (composite.get(i) instanceof ExplicitState) {
        return i;
      }
    }

    return -1;
  }
}
