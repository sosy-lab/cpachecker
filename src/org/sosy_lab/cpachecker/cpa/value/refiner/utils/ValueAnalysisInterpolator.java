
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

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.skip;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.cpa.value.Value;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisPrecision;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.MemoryLocation;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisInterpolationBasedRefiner.ValueAnalysisInterpolant;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.VariableClassification;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

@Options(prefix="cpa.value.interpolation")
public class ValueAnalysisInterpolator {
  @Option(name="weakenInterpolant", description="whether or not to heuristically weaken the candidate interpolant "
      + "prior to the interpolation process")
  private boolean weakenInterpolant = false;

  /**
   * the shutdownNotifier in use
   */
  private final ShutdownNotifier shutdownNotifier;

  /**
   * the transfer relation in use
   */
  private final ValueAnalysisTransferRelation transfer;

  /**
   * the precision in use
   */
  private final ValueAnalysisPrecision precision;

  /**
   * the collector to get the use-definition information from an error trace
   */
  private final AssumptionUseDefinitionCollector assumeCollector;

  /**
   * the set of relevant variables found by the collector
   */
  private Set<String> relevantVariables = new HashSet<>();

  /**
   * the boolean flag that indicates, if assignments through assumptions are needed for excluding the target state
   */
  private Boolean assumptionsAreRelevant = null;

  /**
   * the number of interpolations
   */
  private int numberOfInterpolationQueries = 0;

  /**
   * This method acts as the constructor of the class.
   */
  public ValueAnalysisInterpolator(final Configuration pConfig,final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier, final CFA pCfa)
          throws InvalidConfigurationException {

    pConfig.inject(this);

    try {
      shutdownNotifier  = pShutdownNotifier;
      assumeCollector   = new AssumptionUseDefinitionCollector();
      transfer          = new ValueAnalysisTransferRelation(Configuration.builder().build(), pLogger, pCfa);
      precision         = new ValueAnalysisPrecision("", Configuration.builder().build(),
          Optional.<VariableClassification>absent());
    }
    catch (InvalidConfigurationException e) {
      throw new InvalidConfigurationException("Invalid configuration for checking path: " + e.getMessage(), e);
    }
  }

  /**
   * This method derives an interpolant for the given error path and interpolation state.
   *
   * @param pErrorPath the path to check
   * @param pOffset offset of the state at where to start the current interpolation
   * @param pInputInterpolant the input interpolant
   * @throws CPAException
   * @throws InterruptedException
   */
  public ValueAnalysisInterpolant deriveInterpolant(
      final List<CFAEdge> pErrorPath,
      final int pOffset,
      final ValueAnalysisInterpolant pInputInterpolant) throws CPAException, InterruptedException {
    numberOfInterpolationQueries = 0;

    // on initial iteration
    if(pOffset == 0) {
      assumptionsAreRelevant  = isRemainingPathFeasible(pErrorPath, new ValueAnalysisState(), false);
      relevantVariables       = assumeCollector.obtainUseDefInformation(pErrorPath);
    }

    // create initial state, based on input interpolant, and create initial successor by consuming the next edge
    ValueAnalysisState initialState      = pInputInterpolant.createValueAnalysisState();
    ValueAnalysisState initialSuccessor  = getInitialSuccessor(initialState, pErrorPath.get(pOffset));

    if (initialSuccessor == null) {
      return ValueAnalysisInterpolant.FALSE;
    }

    // if initial state and successor are equal, return the input interpolant
    // in general, this returned interpolant might be stronger than needed, but only in very rare cases,
    // the weaker interpolant would be different from the input interpolant, so we spare the effort
    if (initialState.equals(initialSuccessor)) {
      return pInputInterpolant;
    }

    // if the current edge just changes the names of variables (e.g. function arguments, returned variables)
    // then return the input interpolant with those renamings
    if (isOnlyVariableRenamingEdge(pErrorPath.get(pOffset))) {
      return initialSuccessor.createInterpolant();
    }

    Iterable<CFAEdge> remainingErrorPath = skip(pErrorPath, pOffset + 1);

    // if the remaining path, i.e., the suffix, is contradicting by itself, then return the TRUE interpolant
    if (initialSuccessor.getSize() > 1 && isSuffixContradicting(remainingErrorPath)) {
      return ValueAnalysisInterpolant.TRUE;
    }

    if(weakenInterpolant) {
      initialSuccessor.retainAll(from(relevantVariables)
          .transform(MemoryLocation.FROM_STRING_TO_MEMORYLOCATION).toSet());
    }

    for (MemoryLocation currentMemoryLocation : optimizeForInterpolation(initialSuccessor)) {
      shutdownNotifier.shutdownIfNecessary();

      // temporarily remove the value of the current memory location from the candidate interpolant
      Value value = initialSuccessor.forget(currentMemoryLocation);

      // check if the remaining path now becomes feasible
      if (isRemainingPathFeasible(remainingErrorPath, initialSuccessor, assumptionsAreRelevant)) {
        initialSuccessor.assignConstant(currentMemoryLocation, value);
      }
    }

    return initialSuccessor.createInterpolant();
  }

  /**
   * This method checks, if the given error path is contradicting in itself.
   *
   * @param errorPath the error path to check.
   * @return true, if the given error path is contradicting in itself, else false
   * @throws CPATransferException
   */
  private boolean isSuffixContradicting(Iterable<CFAEdge> errorPath) throws CPATransferException {
    return !isRemainingPathFeasible(errorPath, new ValueAnalysisState(), assumptionsAreRelevant);
  }

  /**
   * This method returns a (possibly) reordered collection of memory locations to interpolate, which favors non-loop variables
   * to be part of the interpolant.
   *
   * @param valueAnalysisState the collection of interpolation candidates, encoded in an value-analysis state
   * @return a (possibly) reordered and reduced collection of memory locations to interpolate
   */
  private Collection<MemoryLocation> optimizeForInterpolation(ValueAnalysisState valueAnalysisState) {

    return Sets.newHashSet(valueAnalysisState.getTrackedMemoryLocations());/*

    ArrayDeque<MemoryLocation> reOrderedMemoryLocations = new ArrayDeque<>();

    // move loop-variables to the front - being checked for relevance earlier minimizes their impact on feasibility
    for(MemoryLocation currentMemoryLocation : trackedMemoryLocations) {
      reOrderedMemoryLocations.addLast(currentMemoryLocation);
    }

    return reOrderedMemoryLocations;*/
  }

  /**
   * This method returns the number of performed interpolations.
   *
   * @return the number of performed interpolations
   */
  public int getNumberOfInterpolationQueries() {
    return numberOfInterpolationQueries;
  }

  /**
   * This method gets the initial successor, i.e. the state following the initial state.
   *
   * @param initialState the initial state, i.e. the state represented by the input interpolant.
   * @param initialEdge the initial edge of the error path
   * @return the initial successor
   * @throws CPATransferException
   */
  private ValueAnalysisState getInitialSuccessor(ValueAnalysisState initialState, CFAEdge initialEdge)
      throws CPATransferException {

    Collection<ValueAnalysisState> successors = transfer.getAbstractSuccessors(
        initialState,
        precision,
        initialEdge);

    return extractSingletonSuccessor(initialState, initialEdge, assumptionsAreRelevant, successors);
  }

  /**
   * This method checks, whether or not the (remaining) error path is feasible when starting with the given (pseudo) initial state.
   *
   * @param remainingErrorPath the error path to check feasibility on
   * @param state the (pseudo) initial state
   * @param ignoreAssumptions whether or not to ignore the assigning semantics of assume edges
   * @return true, it the path is feasible, else false
   * @throws CPATransferException
   */
  private boolean isRemainingPathFeasible(Iterable<CFAEdge> remainingErrorPath, ValueAnalysisState state, boolean ignoreAssumptions)
      throws CPATransferException {
    numberOfInterpolationQueries++;

    for (CFAEdge currentEdge : remainingErrorPath) {
      Collection<ValueAnalysisState> successors = transfer.getAbstractSuccessors(
        state,
        precision,
        currentEdge);

      if (successors.isEmpty()) {
        return false;
      }

      state = extractSingletonSuccessor(state, currentEdge, ignoreAssumptions, successors);
    }
    return true;
  }

  /**
   * This method extracts the single successor from the (empty or singleton) set of successors.
   *
   * @param predecessor the predecessor state
   * @param currentEdge the current edge
   * @param successors the (empty or singleton) set of successors
   * @return the only successor or null, if no set of successors is empty
   */
  private ValueAnalysisState extractSingletonSuccessor(ValueAnalysisState predecessor,
      CFAEdge currentEdge, boolean ignoreAssumptions, Collection<ValueAnalysisState> successors) {
    if(successors.isEmpty()) {
      return null;
    }

    else if(!ignoreAssumptions && currentEdge.getEdgeType() == CFAEdgeType.AssumeEdge) {
      return predecessor.clone();
    }

    else {
      return Iterables.getOnlyElement(successors);
    }
  }

  /**
   * This method checks, if the given edge is only renaming variables.
   *
   * @param cfaEdge the CFA edge to check
   * @return true, if the given edge is only renaming variables
   */
  private boolean isOnlyVariableRenamingEdge(CFAEdge cfaEdge) {
    return
        // renames from calledFn::___cpa_temp_result_var_ to callerFN::assignedVar
        // if the former is relevant, so is the latter
        cfaEdge.getEdgeType() == CFAEdgeType.FunctionReturnEdge

        // for the next two edge types this would also work, but variables
        // from the calling/returning function would be added to interpolant
        // as they are not "cleaned up" by the transfer relation
        // so these two stay out for now

        //|| cfaEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge
        //|| cfaEdge.getEdgeType() == CFAEdgeType.ReturnStatementEdge
        ;
  }
}
