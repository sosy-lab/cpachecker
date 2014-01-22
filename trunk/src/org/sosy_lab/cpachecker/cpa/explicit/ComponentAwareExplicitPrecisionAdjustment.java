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

import static com.google.common.base.Preconditions.checkState;

import java.io.PrintStream;
import java.util.Collection;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
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
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitState.MemoryLocation;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

@Options(prefix="cpa.explicit.blk")
public class ComponentAwareExplicitPrecisionAdjustment extends CompositePrecisionAdjustment implements StatisticsProvider {

  @Option(description="restrict abstractions to loop heads")
  private boolean alwaysAtLoops = false;

  @Option(description="restrict abstractions to function calls/returns")
  private boolean alwaysAtFunctions = false;

  @Option(description="restrict abstractions to assume edges")
  private boolean alwaysAtAssumes = false;

  @Option(description="restrict abstractions to join points")
  private boolean alwaysAtJoins = false;

  private final ImmutableSet<CFANode> loopHeads;

  // statistics
  final Timer totalEnforceAbstraction         = new Timer();
  final Timer totalEnforcePathThreshold       = new Timer();
  final Timer totalEnforceReachedSetThreshold = new Timer();
  final Timer totalComposite                  = new Timer();
  final Timer total                           = new Timer();
  int abstractionComputations                           = 0;

  private Statistics stats  = null;
  private boolean modified = false;

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

  public ComponentAwareExplicitPrecisionAdjustment(ImmutableList<PrecisionAdjustment> precisionAdjustments, Configuration config, CFA cfa)
      throws InvalidConfigurationException {
    super(precisionAdjustments);

    config.inject(this);

    if (alwaysAtLoops && cfa.getAllLoopHeads().isPresent()) {
      loopHeads = cfa.getAllLoopHeads().get();
    } else {
      loopHeads = null;
    }

    stats = new Statistics() {
      @Override
      public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
        pOut.println("Total time:                 " + ComponentAwareExplicitPrecisionAdjustment.this.total);
        pOut.println("Total time for composite:   " + ComponentAwareExplicitPrecisionAdjustment.this.totalComposite);
        pOut.println("Total time for abstraction: " + ComponentAwareExplicitPrecisionAdjustment.this.totalEnforceAbstraction);
        pOut.println("Total time for reached set: " + ComponentAwareExplicitPrecisionAdjustment.this.totalEnforceReachedSetThreshold);
        pOut.println("Total time for path:        " + ComponentAwareExplicitPrecisionAdjustment.this.totalEnforcePathThreshold);
        pOut.println("Number of abstractions:     " + ComponentAwareExplicitPrecisionAdjustment.this.abstractionComputations);
      }

      @Override
      public String getName() {
        return "ComponentAwareExplicitPrecisionAdjustment Statistics";
      }
    };
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment#prec(org.sosy_lab.cpachecker.core.interfaces.AbstractState, org.sosy_lab.cpachecker.core.interfaces.Precision, java.util.Collection)
   */
  @Override
  public Triple<AbstractState, Precision, Action> prec(AbstractState pElement,
                                               Precision pPrecision,
                                               UnmodifiableReachedSet pElements) throws CPAException, InterruptedException {
    total.start();
    modified = false;

    CompositeState composite    = (CompositeState)pElement;
    CompositePrecision precision  = (CompositePrecision)pPrecision;
    assert (composite.getWrappedStates().size() == precision.getPrecisions().size());

    int indexOfExplicitState = getIndexOfExplicitState(composite);
    if (indexOfExplicitState == -1) {
      throw new CPAException("The ComponentAwareExplicitPrecisionAdjustment needs an ExplicitState");
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
        ExplicitState explicitState                 = (ExplicitState)oldState;
        ExplicitPrecision explicitPrecision         = (ExplicitPrecision)oldPrecision;
        LocationState location                      = AbstractStates.extractStateByType(composite, LocationState.class);
        AssignmentsInPathConditionState assignments = AbstractStates.extractStateByType(composite, AssignmentsInPathConditionState.class);

        // compute the abstraction based on the explicit-value precision, unless assignment information is available
        // then, this is dealt with during enforcement of the path thresholds, see below
        if(assignments == null) {
          totalEnforceAbstraction.start();
          explicitState = enforceAbstraction(explicitState, location, explicitPrecision);
          totalEnforceAbstraction.stop();
        }

        // compute the abstraction for reached set thresholds
        totalEnforceReachedSetThreshold.start();
        explicitState = enforceReachedSetThreshold(explicitState, explicitPrecision, slice.getReached(location.getLocationNode()));
        totalEnforceReachedSetThreshold.stop();

        // compute the abstraction for assignment thresholds
        totalEnforcePathThreshold.start();
        Pair<ExplicitState, ExplicitPrecision> result = enforcePathThreshold(explicitState, explicitPrecision, assignments);
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
   * @param state the current state
   * @param precision the current precision
   */
  private ExplicitState enforceAbstraction(ExplicitState state, LocationState location, ExplicitPrecision precision) {
    if (abstractAtEachLocation()
        || abstractAtAssumes(location)
        || abstractAtJoins(location)
        || abstractAtFunction(location)
        || abstractAtLoopHead(location)) {
      state = precision.computeAbstraction(state, location.getLocationNode());
      state.clearDelta();
      abstractionComputations++;
    }

    return state;
  }

  /**
   * This method determines whether or not to abstract at each location.
   *
   * @return true, if an abstraction should be computed at each location, else false
   */
  private boolean abstractAtEachLocation() {
    return !alwaysAtAssumes && !alwaysAtJoins && !alwaysAtFunctions && !alwaysAtLoops;
  }

  /**
   * This method determines whether or not the given location is a branching,
   * and whether or not an abstraction shall be computed or not.
   *
   * @param location the current location
   * @return true, if at the current location an abstraction shall be computed, else false
   */
  private boolean abstractAtAssumes(LocationState location) {
    return alwaysAtAssumes && location.getLocationNode().getEnteringEdge(0).getEdgeType() == CFAEdgeType.AssumeEdge;
  }

  /**
   * This method determines whether or not to abstract before a join point.
   *
   * @param location the current location
   * @return true, if at the current location an abstraction shall be computed, else false
   */
  private boolean abstractAtJoins(LocationState location) {
    return alwaysAtJoins && location.getLocationNode().getNumEnteringEdges() > 1;
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
    checkState(!alwaysAtLoops || loopHeads != null);
    return alwaysAtLoops && loopHeads.contains(location.getLocationNode());
  }

  /**
   * This method abstracts variables that exceed the threshold of assignments along the current path.
   *
   * @param state the explicit state
   * @param precision the current precision
   * @param assignments the assignment information
   * @return the abstracted explicit state
   */
  private Pair<ExplicitState, ExplicitPrecision> enforcePathThreshold(ExplicitState state, ExplicitPrecision precision, AssignmentsInPathConditionState assignments) {
    if (assignments != null) {
      if (assignments instanceof UniqueAssignmentsInPathConditionState) {
        UniqueAssignmentsInPathConditionState unique = (UniqueAssignmentsInPathConditionState)assignments;
        unique.addAssignment(state);
      }

      // forget the value for all variables that exceed their threshold
      for (MemoryLocation memoryLocation: state.getDelta()) {
        if (assignments.variableExceedsSoftThreshold(memoryLocation.getAsSimpleString())) {
          if(!precision.isTracking(memoryLocation)
              || assignments.variableExceedsHardThreshold(memoryLocation.getAsSimpleString())) {
            state.forget(memoryLocation);
          }
        }
      }

      state.clearDelta();
    }

    return Pair.of(state, precision);
  }

  /**
   * This method abstracts variables that exceed the threshold of different values in a given slice of the reached set.
   *
   * @param state the explicit state
   * @param precision the current precision
   * @param reachedSetAtLocation the slice of the reached set from where to retrieve the values per variable
   * @return the abstracted explicit state
   */
  private ExplicitState enforceReachedSetThreshold(ExplicitState state, ExplicitPrecision precision, Collection<AbstractState> reachedSetAtLocation) {
    if (precision.isReachedSetThresholdActive()) {
      // create the mapping from variable name to its different values in this slice of the reached set
      Multimap<String, Long> valueMapping = createMappingFromReachedSet(reachedSetAtLocation);

      for (String variable : valueMapping.keySet()) {
        if (precision.variableExceedsReachedSetThreshold(valueMapping.get(variable).size())) {
          state.forget(variable);
        }
      }
    }

    return state;
  }

  /**
   * This method creates the map which tracks how many different values are stored for a variable, based on the elements in the reached set.
   *
   * @param reachedSetAtLocation the collection of AbstractStates in the reached set that refer to the current location
   */
  private Multimap<String, Long> createMappingFromReachedSet(Collection<AbstractState> reachedSetAtLocation) {
    Multimap<String, Long> valueMapping = HashMultimap.create();

    for (AbstractState element : reachedSetAtLocation) {
      valueMapping = ((ExplicitState)element).addToValueMapping(valueMapping);
    }

    return valueMapping;
  }

  /**
   * This helper gets the index of the explicit state within the composite state.
   *
   * @param composite the composite state in which to look for the explicit state
   * @return the index of the explicit state within the composite state
   * @throws CPAException if no explicit state could be found
   */
  private int getIndexOfExplicitState(CompositeState composite) throws CPAException {
    for (int i = 0; i < composite.getWrappedStates().size(); ++i) {
      if (composite.get(i) instanceof ExplicitState) {
        return i;
      }
    }

    throw new CPAException("Explicit-State could not be found within Composite-State.");
  }
}
