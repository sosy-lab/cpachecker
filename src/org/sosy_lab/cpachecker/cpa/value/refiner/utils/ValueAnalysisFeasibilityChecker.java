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
package org.sosy_lab.cpachecker.cpa.value.refiner.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.conditions.path.AssignmentsInPathCondition.UniqueAssignmentsInPathConditionState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisStrongestPostOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.refiner.GenericFeasibilityChecker;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import com.google.common.base.Optional;

public class ValueAnalysisFeasibilityChecker extends GenericFeasibilityChecker<ValueAnalysisState> {

  private final LogManager logger;
  private final ValueAnalysisStrongestPostOperator strongestPostOp;
  private final VariableTrackingPrecision precision;

  /**
   * This method acts as the constructor of the class.
   *
   * @param pLogger the logger to use
   * @param pCfa the cfa in use
   * @param pInitial the initial state for starting the exploration
   * @throws InvalidConfigurationException
   */
  public ValueAnalysisFeasibilityChecker(
      final LogManager pLogger,
      final CFA pCfa,
      final Configuration config
  ) throws InvalidConfigurationException {

    super(new ValueAnalysisStrongestPostOperator(pLogger, config, pCfa),
          new ValueAnalysisState(),
          ValueAnalysisCPA.class,
          pLogger,
          config,
          pCfa);

    strongestPostOp = new ValueAnalysisStrongestPostOperator(pLogger, config, pCfa);
    logger    = pLogger;
    precision = VariableTrackingPrecision.createStaticPrecision(config, pCfa.getVarClassification(), ValueAnalysisCPA.class);
  }

  /**
   * This method checks if the given path is feasible, starting with the given initial state.
   *
   * @param path the path to check
   * @param pInitial the initial state
   * @param pCallstack the initial callstack
   * @return true, if the path is feasible, else false
   * @throws CPAException
   */
  public boolean isFeasible(
      final ARGPath path,
      final ValueAnalysisState pInitial,
      final Deque<ValueAnalysisState> pCallstack
  ) throws CPAException {

    return path.size() == getInfeasiblePrefixes(path, pInitial, pCallstack).get(0).size();
  }

  @Override
  protected ValueAnalysisState performAbstractions(ValueAnalysisState pNext, CFANode pLocation, ARGPath pPath) {
    final ValueAnalysisState valueState = ValueAnalysisState.copyOf(pNext);

    final Set<MemoryLocation> exceedingMemoryLocations = obtainExceedingMemoryLocations(pPath);

    // some variables might be blacklisted or tracked by BDDs
    // so perform abstraction computation here
    for (MemoryLocation memoryLocation : valueState.getTrackedMemoryLocations()) {
      if (!precision.isTracking(memoryLocation,
          valueState.getTypeForMemoryLocation(memoryLocation), pLocation)) {
        valueState.forget(memoryLocation);
      }
    }

    for(MemoryLocation exceedingMemoryLocation : exceedingMemoryLocations) {
      valueState.forget(exceedingMemoryLocation);
    }

    return valueState;
  }

  private Set<MemoryLocation> obtainExceedingMemoryLocations(ARGPath path) {
    UniqueAssignmentsInPathConditionState assignments =
        AbstractStates.extractStateByType(path.getLastState(),
        UniqueAssignmentsInPathConditionState.class);

    if(assignments == null) {
      return Collections.emptySet();
    }

    return assignments.getMemoryLocationsExceedingHardThreshold();
  }

  public List<Pair<ValueAnalysisState, CFAEdge>> evaluate(final ARGPath path)
      throws CPAException {

    try {
      List<Pair<ValueAnalysisState, CFAEdge>> reevaluatedPath = new ArrayList<>();
      ValueAnalysisState next = new ValueAnalysisState();

      PathIterator iterator = path.pathIterator();
      while (iterator.hasNext()) {
        Optional<ValueAnalysisState> successor = strongestPostOp.getStrongestPost(
            next,
            precision,
            iterator.getOutgoingEdge());

        if(!successor.isPresent()) {
          return reevaluatedPath;
        }

        // extract singleton successor state
        next = successor.get();

        reevaluatedPath.add(Pair.of(next, iterator.getOutgoingEdge()));

        iterator.advance();
      }

      return reevaluatedPath;
    } catch (CPATransferException e) {
      throw new CPAException("Computation of successor failed for checking path: " + e.getMessage(), e);
    }
  }
}
