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
package org.sosy_lab.cpachecker.cpa.value;

import static com.google.common.base.Preconditions.checkState;

import java.io.PrintStream;
import java.util.Collection;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.conditions.path.AssignmentsInPathCondition.UniqueAssignmentsInPathConditionState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

@Options(prefix="cpa.value.blk")
public class ValueAnalysisPrecisionAdjustment implements PrecisionAdjustment, StatisticsProvider {

  @Option(secure=true, description="restrict abstractions to branching points")
  private boolean alwaysAtBranch = false;

  @Option(secure=true, description="restrict abstractions to join points")
  private boolean alwaysAtJoin = false;

  @Option(secure=true, description="restrict abstractions to function calls/returns")
  private boolean alwaysAtFunction = false;

  @Option(secure=true, description="restrict abstractions to loop heads")
  private boolean alwaysAtLoop = false;

  @Option(secure=true, description="toggle liveness abstraction")
  private boolean doLivenessAbstraction = false;

  @Option(secure=true, description="restrict liveness abstractions to nodes with more than one entering and/or leaving edge")
  private boolean onlyAtNonLinearCFA = false;

  private final ImmutableSet<CFANode> loopHeads;

  // statistics
  final StatCounter abstractions    = new StatCounter("Number of abstraction computations");
  final StatTimer totalLiveness     = new StatTimer("Total time for liveness abstraction");
  final StatTimer totalAbstraction  = new StatTimer("Total time for abstraction computation");
  final StatTimer totalEnforcePath  = new StatTimer("Total time for path thresholds");

  private final Statistics statistics;

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(statistics);
  }

  private final Optional<LiveVariables> liveVariables;

  public ValueAnalysisPrecisionAdjustment(Configuration pConfig, CFA pCfa)
      throws InvalidConfigurationException {

    pConfig.inject(this);

    if (alwaysAtLoop && pCfa.getAllLoopHeads().isPresent()) {
      loopHeads = pCfa.getAllLoopHeads().get();
    } else {
      loopHeads = null;
    }

    statistics = new Statistics() {
      @Override
      public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {

        StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(pOut);
        writer.put(abstractions);
        writer.put(totalLiveness);
        writer.put(totalAbstraction);
        writer.put(totalEnforcePath);
      }

      @Override
      public String getName() {
        return ValueAnalysisPrecisionAdjustment.this.getClass().getSimpleName();
      }
    };

    liveVariables = pCfa.getLiveVariables();
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(AbstractState pState, Precision pPrecision, UnmodifiableReachedSet pStates,
      Function<AbstractState, AbstractState> projection,
      AbstractState fullState)
      throws CPAException, InterruptedException {

    return prec((ValueAnalysisState)pState,
        (VariableTrackingPrecision)pPrecision,
        AbstractStates.extractStateByType(fullState, LocationState.class),
        AbstractStates.extractStateByType(fullState, UniqueAssignmentsInPathConditionState.class));
  }

  private Optional<PrecisionAdjustmentResult> prec(ValueAnalysisState pState,
      VariableTrackingPrecision pPrecision,
      LocationState location,
      UniqueAssignmentsInPathConditionState assignments) {
    ValueAnalysisState resultState = ValueAnalysisState.copyOf(pState);

    if(doLivenessAbstraction && liveVariables.isPresent()) {
      totalLiveness.start();
      enforceLiveness(pState, location, resultState);
      totalLiveness.stop();
    }

    // compute the abstraction based on the value-analysis precision
    totalAbstraction.start();
    enforcePrecision(resultState, location, pPrecision);
    totalAbstraction.stop();

    // compute the abstraction for assignment thresholds
    if (assignments != null) {
      totalEnforcePath.start();
      enforcePathThreshold(resultState, pPrecision, assignments);
      totalEnforcePath.stop();
    }

    return Optional.of(PrecisionAdjustmentResult.create(resultState, pPrecision, Action.CONTINUE));
  }

  private void enforceLiveness(ValueAnalysisState pState, LocationState location, ValueAnalysisState resultState) {
    CFANode actNode = location.getLocationNode();

    boolean hasMoreThanOneEnteringLeavingEdge = actNode.getNumEnteringEdges() > 1 || actNode.getNumLeavingEdges() > 1;

    if (!onlyAtNonLinearCFA || hasMoreThanOneEnteringLeavingEdge) {
      boolean onlyBlankEdgesEntering = true;
      for (int i = 0; i < actNode.getNumEnteringEdges() && onlyBlankEdgesEntering; i++) {
        onlyBlankEdgesEntering = location.getLocationNode().getEnteringEdge(i) instanceof BlankEdge;
      }

      // when there are only blank edges that lead to this state, then we can
      // skip the abstraction, after a blank edge there cannot be a variable
      // less live
      if (!onlyBlankEdgesEntering) {
        for (MemoryLocation variable : pState.getTrackedMemoryLocations()) {
          if (!liveVariables.get().isVariableLive(variable.getAsSimpleString(), location.getLocationNode())) {
            resultState.forget(variable);
          }
        }
      }
    }
  }

  /**
   * This method performs an abstraction computation on the current value-analysis state.
   *
   * @param location the current location
   * @param state the current state
   * @param precision the current precision
   */
  private void enforcePrecision(ValueAnalysisState state, LocationState location, VariableTrackingPrecision precision) {
    if (abstractAtEachLocation()
        || abstractAtBranch(location)
        || abstractAtJoin(location)
        || abstractAtFunction(location)
        || abstractAtLoop(location)) {

      for (MemoryLocation memoryLocation : state.getTrackedMemoryLocations()) {
        if (!precision.isTracking(memoryLocation, state.getTypeForMemoryLocation(memoryLocation), location.getLocationNode())) {
          state.forget(memoryLocation);
        }
      }

      abstractions.inc();
    }
  }

  /**
   * This method determines whether or not to abstract at each location.
   *
   * @return true, if an abstraction should be computed at each location, else false
   */
  private boolean abstractAtEachLocation() {
    return !alwaysAtBranch && !alwaysAtJoin && !alwaysAtFunction && !alwaysAtLoop;
  }

  private boolean abstractAtBranch(LocationState location) {
    return alwaysAtBranch && location.getLocationNode().getNumLeavingEdges() > 1;
  }

  private boolean abstractAtJoin(LocationState location) {
    return alwaysAtJoin && location.getLocationNode().getNumEnteringEdges() > 1;
  }

  private boolean abstractAtFunction(LocationState location) {
    return alwaysAtFunction && (location.getLocationNode() instanceof FunctionEntryNode
        || location.getLocationNode().getEnteringSummaryEdge() != null);
  }

  private boolean abstractAtLoop(LocationState location) {
    checkState(!alwaysAtLoop || loopHeads != null);
    return alwaysAtLoop && loopHeads.contains(location.getLocationNode());
  }

  /**
   * This method abstracts variables that exceed the threshold of assignments along the current path.
   *
   * @param state the state to abstract
   * @param precision the current precision
   * @param assignments the assignment information
   */
  private void enforcePathThreshold(ValueAnalysisState state,
      VariableTrackingPrecision precision,
      UniqueAssignmentsInPathConditionState assignments) {

    // forget the value for all variables that exceed their threshold
    for (MemoryLocation memoryLocation: state.getTrackedMemoryLocations()) {
      assignments.updateAssignmentInformation(memoryLocation, state.getValueFor(memoryLocation));

      if (assignments.exceedsThreshold(memoryLocation)) {
        state.forget(memoryLocation);
      }
    }
  }
}
