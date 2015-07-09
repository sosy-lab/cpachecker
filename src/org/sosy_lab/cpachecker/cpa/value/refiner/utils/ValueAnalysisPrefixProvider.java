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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.MutableARGPath;
import org.sosy_lab.cpachecker.cpa.conditions.path.AssignmentsInPathCondition.UniqueAssignmentsInPathConditionState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisInterpolant;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.refinement.InfeasiblePrefix;
import org.sosy_lab.cpachecker.util.refinement.PrefixProvider;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

public class ValueAnalysisPrefixProvider implements PrefixProvider {

  private final LogManager logger;
  private final ValueAnalysisTransferRelation transfer;
  private final VariableTrackingPrecision precision;
  private MutableARGPath feasiblePrefix;
  private final CFA cfa;

  /**
   * This method acts as the constructor of the class.
   *
   * @param pLogger the logger to use
   * @param pCfa the cfa in use
   * @param pInitial the initial state for starting the exploration
   * @throws InvalidConfigurationException
   */
  public ValueAnalysisPrefixProvider(LogManager pLogger, CFA pCfa, Configuration config) throws InvalidConfigurationException {
    logger = pLogger;
    cfa    = pCfa;

    transfer = new ValueAnalysisTransferRelation(Configuration.builder().build(), pLogger, cfa);
    precision = VariableTrackingPrecision.createStaticPrecision(config, cfa.getVarClassification(), ValueAnalysisCPA.class);
  }

  /**
   * This method obtains a list of prefixes of the path, that are infeasible by themselves. If the path is feasible, the whole path
   * is returned as the only element of the list.
   *
   * @param path the path to check
   * @return the list of prefix of the path that are feasible by themselves
   * @throws CPAException
   */
  @Override
  public List<InfeasiblePrefix> extractInfeasiblePrefixes(final ARGPath path)
      throws CPAException {
    return extractInfeasilbePrefixes(path, new ValueAnalysisState());
  }

  /**
   * This method obtains a list of prefixes of the path, that are infeasible by themselves. If the path is feasible, the whole path
   * is returned as the only element of the list.
   *
   * @param path the path to check
   * @param pInitial the initial state
   * @return the list of prefix of the path that are feasible by themselves
   * @throws CPAException
   */
  public List<InfeasiblePrefix> extractInfeasilbePrefixes(final ARGPath path, final ValueAnalysisState pInitial)
      throws CPAException {

    List<InfeasiblePrefix> prefixes = new ArrayList<>();
    boolean performAbstraction = precision.allowsAbstraction();
    Deque<ValueAnalysisState> callstack = new ArrayDeque<>();

    Set<MemoryLocation> exceedingMemoryLocations = obtainExceedingMemoryLocations(path);

    try {
      feasiblePrefix = new MutableARGPath();
      ValueAnalysisState next = ValueAnalysisState.copyOf(pInitial);

      PathIterator iterator = path.pathIterator();
      while (iterator.hasNext()) {
        final CFAEdge edge = iterator.getOutgoingEdge();

        // we enter a function, so lets add the previous state to the stack
        if (edge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
          callstack.addLast(next);
        }

        // we leave a function, so rebuild return-state before assigning the return-value.
        if (!callstack.isEmpty() && edge.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
          // rebuild states with info from previous state
          final ValueAnalysisState callState = callstack.removeLast();
          next = next.rebuildStateAfterFunctionCall(callState, (FunctionExitNode)edge.getPredecessor());
        }

        Collection<ValueAnalysisState> successors = transfer.getAbstractSuccessorsForEdge(
            next,
            precision,
            edge);

        feasiblePrefix.addLast(Pair.of(iterator.getAbstractState(), iterator.getOutgoingEdge()));

        // no successors => path is infeasible
        if (successors.isEmpty()) {
          logger.log(Level.FINE, "found infeasible prefix: ", iterator.getOutgoingEdge(), " did not yield a successor");

          // add infeasible prefix
          prefixes.add(buildInfeasiblePrefix(path, feasiblePrefix));

          // continue with feasible prefix
          Pair<ARGState, CFAEdge> assumeState = feasiblePrefix.removeLast();

          feasiblePrefix.add(Pair.<ARGState, CFAEdge>of(assumeState.getFirst(),
              BlankEdge.buildNoopEdge(
                  assumeState.getSecond().getPredecessor(),
                  assumeState.getSecond().getSuccessor())));

          successors = Sets.newHashSet(next);
        }

        // extract singleton successor state
        next = Iterables.getOnlyElement(successors);

        // some variables might be blacklisted or tracked by BDDs
        // so perform abstraction computation here
        if(performAbstraction) {
          for (MemoryLocation memoryLocation : next.getTrackedMemoryLocations()) {
            if (!precision.isTracking(memoryLocation,
                next.getTypeForMemoryLocation(memoryLocation),
                iterator.getOutgoingEdge().getSuccessor())) {
              next.forget(memoryLocation);
            }
          }
        }

        for(MemoryLocation exceedingMemoryLocation : exceedingMemoryLocations) {
          next.forget(exceedingMemoryLocation);
        }

        iterator.advance();
      }

      return prefixes;
    } catch (CPATransferException e) {
      throw new CPAException("Computation of infeasible prefixes failed: " + e.getMessage(), e);
    }
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

  private InfeasiblePrefix buildInfeasiblePrefix(final ARGPath path, MutableARGPath currentPrefix) {
    MutableARGPath infeasiblePrefix = new MutableARGPath();
    infeasiblePrefix.addAll(currentPrefix);

    // for interpolation, one transition after the infeasible transition is needed,
    // so we add exactly that extra transition to the successor state
    infeasiblePrefix.add(obtainSuccessorTransition(path, currentPrefix.size()));

    UseDefRelation useDefRelation = new UseDefRelation(infeasiblePrefix.immutableCopy(),
        cfa.getVarClassification().isPresent()
          ? cfa.getVarClassification().get().getIntBoolVars()
          : Collections.<String>emptySet());

    List<Pair<ARGState, ValueAnalysisInterpolant>> interpolants = new UseDefBasedInterpolator(
        logger,
        infeasiblePrefix.immutableCopy(),
        useDefRelation,
        cfa.getMachineModel()).obtainInterpolants();

    return InfeasiblePrefix.buildForValueDomain(infeasiblePrefix.immutableCopy(),
        FluentIterable.from(interpolants).transform(Pair.<ValueAnalysisInterpolant>getProjectionToSecond()).toList());
  }

  /**
   * This method returns the pair of state and edge at the given offset.
   */
  private Pair<ARGState, CFAEdge> obtainSuccessorTransition(final ARGPath path, final int offset) {
    Pair<ARGState, CFAEdge> transition = path.obtainTransitionAt(offset);
    return Pair.<ARGState, CFAEdge>of(transition.getFirst(),
        BlankEdge.buildNoopEdge(transition.getSecond().getPredecessor(), transition.getSecond().getSuccessor()));
  }

  public ARGPath extractFeasilbePath(final ARGPath path)
      throws CPAException {
    extractInfeasiblePrefixes(path);
    return feasiblePrefix.immutableCopy();
  }
}
