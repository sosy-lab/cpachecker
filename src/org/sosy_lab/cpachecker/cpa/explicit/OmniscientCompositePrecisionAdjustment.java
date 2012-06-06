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

import java.io.PrintStream;
import java.util.Collection;
import java.util.Map;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSetView;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecisionAdjustment;
import org.sosy_lab.cpachecker.cpa.conditions.path.AssignmentsInPathCondition.AssignmentsInPathConditionState;
import org.sosy_lab.cpachecker.cpa.conditions.path.AssignmentsInPathCondition.UniqueAssignmentsInPathConditionState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;

public class OmniscientCompositePrecisionAdjustment extends CompositePrecisionAdjustment implements StatisticsProvider {

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

  // statistics
  final Timer totalEnforcePathThreshold       = new Timer();
  final Timer totalEnforceReachedSetThreshold = new Timer();
  final Timer totalComposite = new Timer();
  final Timer total = new Timer();
  private Statistics stats = null;
  private boolean modified = false;

  public OmniscientCompositePrecisionAdjustment(ImmutableList<PrecisionAdjustment> precisionAdjustments) {
    super(precisionAdjustments);

    stats = new Statistics() {

      @Override
      public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
        pOut.println("total time:                 " + OmniscientCompositePrecisionAdjustment.this.total.getSumTime());
        pOut.println("total time for composite:   " + OmniscientCompositePrecisionAdjustment.this.totalComposite.getSumTime());
        pOut.println("total time for reached set: " + OmniscientCompositePrecisionAdjustment.this.totalEnforceReachedSetThreshold.getSumTime());
        pOut.println("total time for path:        " + OmniscientCompositePrecisionAdjustment.this.totalEnforcePathThreshold.getSumTime());
      }

      @Override
      public String getName() {
        return "OmniscientCompositePrecisionAdjustment Stats";
      }
    };
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment#prec(org.sosy_lab.cpachecker.core.interfaces.AbstractState, org.sosy_lab.cpachecker.core.interfaces.Precision, java.util.Collection)
   */
  @Override
  public Triple<AbstractState, Precision, Action> prec(AbstractState pElement,
                                               Precision pPrecision,
                                               UnmodifiableReachedSet pElements) throws CPAException {
    total.start();
    modified = false;

    CompositeState composite    = (CompositeState)pElement;
    CompositePrecision precision  = (CompositePrecision)pPrecision;
    assert (composite.getWrappedElements().size() == precision.getPrecisions().size());

    int indexOfExplicitState = getIndexOfExplicitState(composite);
    if(indexOfExplicitState == -1) {
      throw new CPAException("The OmniscientCompositePrecisionAdjustment needs an ExplicitState");
    }

    ImmutableList.Builder<AbstractState> outElements  = ImmutableList.builder();
    ImmutableList.Builder<Precision> outPrecisions      = ImmutableList.builder();

    Action action = Action.CONTINUE;

    for (int i = 0, size = composite.getWrappedElements().size(); i < size; ++i) {
      UnmodifiableReachedSet slice = new UnmodifiableReachedSetView(pElements, stateProjectionFunctions.get(i), precisionProjectionFunctions.get(i));
      PrecisionAdjustment precisionAdjustment = precisionAdjustments.get(i);
      AbstractState oldElement = composite.get(i);
      Precision oldPrecision = precision.get(i);

      // enforce thresholds for explicit element, by incorporating information from reached set and path condition element
      if(i == indexOfExplicitState) {
        ExplicitState explicit                  = (ExplicitState)oldElement;
        ExplicitPrecision explicitPrecision       = (ExplicitPrecision)oldPrecision;

        LocationState location                  = AbstractStates.extractElementByType(composite, LocationState.class);
        AssignmentsInPathConditionState assigns = AbstractStates.extractElementByType(composite, AssignmentsInPathConditionState.class);

        totalEnforceReachedSetThreshold.start();
        ExplicitState newElement = enforceReachedSetThreshold(explicit, explicitPrecision, slice.getReached(location.getLocationNode()));
        totalEnforceReachedSetThreshold.stop();

        totalEnforcePathThreshold.start();
        Pair<ExplicitState, ExplicitPrecision> result = enforcePathThreshold(newElement, explicitPrecision, assigns);
        totalEnforcePathThreshold.stop();

        outElements.add(result.getFirst());
        outPrecisions.add(result.getSecond());
      }
      else {
        totalComposite.start();

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

        totalComposite.stop();
      }
    }

    AbstractState outElement = modified ? new CompositeState(outElements.build())     : pElement;
    Precision outPrecision     = modified ? new CompositePrecision(outPrecisions.build()) : pPrecision;

    total.stop();

    return Triple.of(outElement, outPrecision, action);
  }

  private ExplicitState enforceReachedSetThreshold(ExplicitState element, ExplicitPrecision precision, Collection<AbstractState> reachedSetAtLocation) {
    // if an actual meaningful threshold is set
    if(precision.getReachedSetThresholds().defaultThreshold != -1) {
      // create the mapping from variable name to the number of different values this variable has
      HashMultimap<String, Long> valueMapping = createMappingFromReachedSet(reachedSetAtLocation);

      // forget the value for all variables that exceed their threshold
      for(String variable : valueMapping.keySet()) {
        if(precision.getReachedSetThresholds().exceeds(variable, valueMapping.get(variable).size())) {
          precision.getReachedSetThresholds().setExceeded(variable);
          element.forget(variable);
        }
      }
    }

    return element;
  }

  private Pair<ExplicitState, ExplicitPrecision> enforcePathThreshold(ExplicitState element, ExplicitPrecision precision, AssignmentsInPathConditionState assigns) {
    if(assigns != null) {
      if(assigns instanceof UniqueAssignmentsInPathConditionState) {
        UniqueAssignmentsInPathConditionState unique = (UniqueAssignmentsInPathConditionState)assigns;
        unique.addAssignment(element);
      }

      // forget the value for all variables that exceed their threshold
      for(Map.Entry<String, Integer> entry : assigns.getAssignmentCounts().entrySet()) {
        if(precision.getPathThresholds().exceeds(entry.getKey(), entry.getValue())) {
          //System.out.println((assigns instanceof AllAssignmentsInPathConditionState) ? "non-" : "" +
              //"unique path: forgetting var " + entry.getKey());

          // the path threshold precision is path sensitive, therefore, mutating a clone is mandatory
          if(modified == false) {
            precision = new ExplicitPrecision(precision);
            modified = true;
          }

          precision.getReachedSetThresholds().setExceeded(entry.getKey());
          element.forget(entry.getKey());
        }
      }
    }

    return Pair.of(element, precision);
  }

  private int getIndexOfExplicitState(CompositeState composite) {
    for (int i = 0; i < composite.getWrappedElements().size(); ++i) {
      if(composite.get(i) instanceof ExplicitState) {
        return i;
      }
    }

    return -1;
  }

  /**
   * This method creates the map which tracks how many different values are stored for a variable, based on the elements in the reached set.
   *
   * @param reachedSetAtLocation the collection of AbstractStates in the reached set that refer to the current location
   */
  private HashMultimap<String, Long> createMappingFromReachedSet(Collection<AbstractState> reachedSetAtLocation) {
    HashMultimap<String, Long> valueMapping = HashMultimap.create();

    for(AbstractState element : reachedSetAtLocation) {
      for(Map.Entry<String, Long> entry : ((ExplicitState)element).getConstantsMap().entrySet()) {
        valueMapping.put(entry.getKey(), entry.getValue());
      }
    }

    return valueMapping;
  }
}
