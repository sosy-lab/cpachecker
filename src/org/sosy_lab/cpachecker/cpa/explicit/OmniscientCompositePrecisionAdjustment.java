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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSetView;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecisionAdjustment;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.conditions.path.AssignmentsInPathCondition.AssignmentsInPathConditionState;
import org.sosy_lab.cpachecker.cpa.conditions.path.AssignmentsInPathCondition.UniqueAssignmentsInPathConditionState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;

@Options(prefix="cpa.explicit.blk")
public class OmniscientCompositePrecisionAdjustment extends CompositePrecisionAdjustment implements StatisticsProvider {

  @Option(description="restrict abstractions to loop heads")
  private boolean alwaysAtLoops = false;

  @Option(description="restrict abstractions to function calls/returns")
  private boolean alwaysAtFunctions = false;

  // statistics
  final Timer totalEnforceAbstraction         = new Timer();
  final Timer totalEnforcePathThreshold       = new Timer();
  final Timer totalEnforceReachedSetThreshold = new Timer();
  final Timer totalComposite                  = new Timer();
  final Timer total                           = new Timer();

  private Statistics stats  = null;
  private boolean modified = false;

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

  public OmniscientCompositePrecisionAdjustment(ImmutableList<PrecisionAdjustment> precisionAdjustments, Configuration config)
      throws InvalidConfigurationException {
    super(precisionAdjustments);

    config.inject(this);

    stats = new Statistics() {
      @Override
      public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
        pOut.println("Total time:                 " + OmniscientCompositePrecisionAdjustment.this.total);
        pOut.println("Total time for composite:   " + OmniscientCompositePrecisionAdjustment.this.totalComposite);
        pOut.println("Total time for abstraction: " + OmniscientCompositePrecisionAdjustment.this.totalEnforceAbstraction);
        pOut.println("Total time for reached set: " + OmniscientCompositePrecisionAdjustment.this.totalEnforceReachedSetThreshold);
        pOut.println("Total time for path:        " + OmniscientCompositePrecisionAdjustment.this.totalEnforcePathThreshold);
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
    assert (composite.getWrappedStates().size() == precision.getPrecisions().size());

    int indexOfExplicitState = getIndexOfExplicitState(composite);
    if (indexOfExplicitState == -1) {
      throw new CPAException("The OmniscientCompositePrecisionAdjustment needs an ExplicitState");
    }

    ImmutableList.Builder<AbstractState> outElements  = ImmutableList.builder();
    ImmutableList.Builder<Precision> outPrecisions    = ImmutableList.builder();

    Action action = Action.CONTINUE;

    for (int i = 0, size = composite.getWrappedStates().size(); i < size; ++i) {
      UnmodifiableReachedSet slice = new UnmodifiableReachedSetView(pElements, stateProjectionFunctions.get(i), precisionProjectionFunctions.get(i));
      PrecisionAdjustment precisionAdjustment = precisionAdjustments.get(i);
      AbstractState oldState = composite.get(i);
      Precision oldPrecision = precision.get(i);

      // enforce thresholds for explicit element, by incorporating information from reached set and path condition element
      if (i == indexOfExplicitState) {
        ExplicitState explicitState         = (ExplicitState)oldState;
        ExplicitPrecision explicitPrecision = (ExplicitPrecision)oldPrecision;
        LocationState location              = AbstractStates.extractStateByType(composite, LocationState.class);

        // compute the abstraction for CEGAR
        totalEnforceAbstraction.start();
        explicitState = enforceAbstraction(explicitState, location, explicitPrecision);
        totalEnforceAbstraction.stop();

        // compute the abstraction for reached set thresholds
        totalEnforceReachedSetThreshold.start();
        explicitState = enforceReachedSetThreshold(explicitState, explicitPrecision, slice.getReached(location.getLocationNode()));
        totalEnforceReachedSetThreshold.stop();

        // compute the abstraction for assignment thresholds
        totalEnforcePathThreshold.start();
        AssignmentsInPathConditionState assigns       = AbstractStates.extractStateByType(composite, AssignmentsInPathConditionState.class);
        Pair<ExplicitState, ExplicitPrecision> result = enforcePathThreshold(explicitState, explicitPrecision, assigns);
        totalEnforcePathThreshold.stop();

        outElements.add(result.getFirst());
        outPrecisions.add(result.getSecond());
      } else {
        totalComposite.start();

        Triple<AbstractState, Precision, Action> result = precisionAdjustment.prec(oldState, oldPrecision, slice);
        AbstractState newElement = result.getFirst();
        Precision newPrecision = result.getSecond();

        if (result.getThird() == Action.BREAK) {
          action = Action.BREAK;
        }

        if ((newElement != oldState) || (newPrecision != oldPrecision)) {
          // something has changed
          modified = true;
        }
        outElements.add(newElement);
        outPrecisions.add(newPrecision);

        totalComposite.stop();
      }
    }

    AbstractState outElement = pElement;
    Precision outPrecision   = pPrecision;
    if (modified) {
      outElement    = new CompositeState(outElements.build());
      outPrecision  = new CompositePrecision(outPrecisions.build());
    }

    total.stop();

    return Triple.of(outElement, outPrecision, action);
  }

  /**
   * This method performs an abstraction computation on the current explicit state.
   *
   * @param location the current location
   * @param explicitState the current state
   * @param explicitPrecision the current precision
   */
  private ExplicitState enforceAbstraction(ExplicitState explicitState, LocationState location, ExplicitPrecision explicitPrecision) {
    if (abstractAtEachLocation()
        || abstractAtFunction(location)
        || abstractAtLoopHead(location)) {
      explicitPrecision.setLocation(location.getLocationNode());
      explicitState.removeAll(getVariablesToDrop(explicitState, explicitPrecision));
    }

    return explicitState;
  }

  /**
   * This method determines whether or not to abstract at each location.
   *
   * @return true, if an abstraction should be computed at each location, else false
   */
  private boolean abstractAtEachLocation() {
    return !alwaysAtFunctions && !alwaysAtLoops;
  }

  /**
   * This method determines whether or not the given location is a function entry or exit,
   * and whether or not an abstraction shall be computed or not.
   *
   * @param location the current location
   * @return true, if at the current location an abstraction shall be computed, else false
   */
  private boolean abstractAtFunction(LocationState location) {
    return alwaysAtFunctions && (location.getLocationNode() instanceof FunctionEntryNode
        || location.getLocationNode().getEnteringSummaryEdge() != null);
  }

  /**
   * This method determines whether or not the given location is a loop head,
   * and whether or not an abstraction shall be computed or not.
   *
   * @param location the current location
   * @return true, if at the current location an abstraction shall be computed, else false
   */
  private boolean abstractAtLoopHead(LocationState location) {
    return alwaysAtLoops && location.getLocationNode().isLoopStart();
  }

  /**
   * This method return the set of variables to be dropped according to the current precision.
   *
   * @param explicitState the current state
   * @param explicitPrecision the current precision
   * @return the variables to the dropped
   */
  private Collection<String> getVariablesToDrop(ExplicitState explicitState, ExplicitPrecision explicitPrecision) {
    Collection<String> toDrop = new ArrayList<>();

    for(String variableName : explicitState.getTrackedVariableNames()) {
      if(!explicitPrecision.isTracking(variableName)) {
        toDrop.add(variableName);
      }
    }

    return toDrop;
  }

  private ExplicitState enforceReachedSetThreshold(ExplicitState element, ExplicitPrecision precision, Collection<AbstractState> reachedSetAtLocation) {
    // if an actual meaningful threshold is set
    if (precision.getReachedSetThresholds().defaultThreshold != -1) {
      // create the mapping from variable name to the number of different values this variable has
      HashMultimap<String, Long> valueMapping = createMappingFromReachedSet(reachedSetAtLocation);

      // forget the value for all variables that exceed their threshold
      for (String variable : valueMapping.keySet()) {
        if (precision.getReachedSetThresholds().exceeds(variable, valueMapping.get(variable).size())) {
          precision.getReachedSetThresholds().setExceeded(variable);
          element.forget(variable);
        }
      }
    }

    return element;
  }

  private Pair<ExplicitState, ExplicitPrecision> enforcePathThreshold(ExplicitState element, ExplicitPrecision precision, AssignmentsInPathConditionState assigns) {
    if (assigns != null) {
      if (assigns instanceof UniqueAssignmentsInPathConditionState) {
        UniqueAssignmentsInPathConditionState unique = (UniqueAssignmentsInPathConditionState)assigns;
        unique.addAssignment(element);
      }

      // forget the value for all variables that exceed their threshold
      for (Map.Entry<String, Integer> entry : assigns.getAssignmentCounts().entrySet()) {
        if (precision.getPathThresholds().exceeds(entry.getKey(), entry.getValue())) {
          //System.out.println((assigns instanceof AllAssignmentsInPathConditionState) ? "non-" : "" +
          //    "unique path: forgetting var " + entry.getKey());

          // the path threshold precision is path sensitive, therefore, mutating a clone is mandatory
          if (modified == false) {
            precision = new ExplicitPrecision(precision, HashMultimap.<CFANode, String>create());
            modified = true;
          }

          precision.getPathThresholds().setExceeded(entry.getKey());
          element.forget(entry.getKey());
        }
      }
    }

    return Pair.of(element, precision);
  }

  private int getIndexOfExplicitState(CompositeState composite) {
    for (int i = 0; i < composite.getWrappedStates().size(); ++i) {
      if (composite.get(i) instanceof ExplicitState) {
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

    for (AbstractState element : reachedSetAtLocation) {
      valueMapping = ((ExplicitState)element).addToValueMapping(valueMapping);
    }

    return valueMapping;
  }
}
