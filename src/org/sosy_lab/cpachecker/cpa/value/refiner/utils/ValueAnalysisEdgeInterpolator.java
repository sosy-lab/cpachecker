
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

import java.util.Collection;
import java.util.Deque;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisInterpolant;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import com.google.common.collect.Iterables;

@Options(prefix="cpa.value.interpolation")
public class ValueAnalysisEdgeInterpolator {

  @Option(secure=true, description="apply optimizations based on equality of input interpolant and candidate interpolant")
  private boolean applyItpEqualityOptimization = true;

  @Option(secure=true, description="apply optimizations based on CFA edges with only variable-renaming semantics")
  private boolean applyRenamingOptimization = true;

  @Option(secure=true, description="apply optimizations based on infeasibility of suffix")
  private boolean applyUnsatSuffixOptimization = true;

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
  private final VariableTrackingPrecision precision;

  /**
   * the number of interpolations
   */
  private int numberOfInterpolationQueries = 0;

  /**
   * the error path checker to be used for feasibility checks
   */
  private final ValueAnalysisFeasibilityChecker checker;

  /**
   * constant to denote that a transition did not yield a successor
   */
  private static final ValueAnalysisState NO_SUCCESSOR = null;

  /**
   * This method acts as the constructor of the class.
   */
  public ValueAnalysisEdgeInterpolator(final Configuration pConfig,final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier, final CFA pCfa)
          throws InvalidConfigurationException {

    pConfig.inject(this);

    try {
      shutdownNotifier  = pShutdownNotifier;
      checker           = new ValueAnalysisFeasibilityChecker(pLogger, pCfa, pConfig);
      transfer          = new ValueAnalysisTransferRelation(Configuration.builder().build(), pLogger, pCfa);
      precision         = VariableTrackingPrecision.createStaticPrecision(pConfig, pCfa.getVarClassification(), ValueAnalysisCPA.class);
    }
    catch (InvalidConfigurationException e) {
      throw new InvalidConfigurationException("Invalid configuration for checking path: " + e.getMessage(), e);
    }
  }

  /**
   * This method derives an interpolant for the given error path and interpolation state.
   *
   * @param pErrorPath the path to interpolate
   * @param pCurrentEdge the current edge to interpolate
   * @param pCallstack the current call stack
   * @param pOffset offset of the state at where to start the current interpolation
   * @param pInputInterpolant the input interpolant
   * @throws CPAException
   * @throws InterruptedException
   */
  public ValueAnalysisInterpolant deriveInterpolant(
      final ARGPath pErrorPath,
      final CFAEdge pCurrentEdge,
      final Deque<ValueAnalysisState> pCallstack,
      final int pOffset,
      final ValueAnalysisInterpolant pInputInterpolant) throws CPAException, InterruptedException {
    numberOfInterpolationQueries = 0;

    // create initial state, based on input interpolant, and create initial successor by consuming the next edge
    ValueAnalysisState initialState = pInputInterpolant.createValueAnalysisState();

    // TODO callstack-management depends on a forward-iteration on a single path.
    // TODO Thus interpolants have to be computed from front to end. Can we assure this?
    ValueAnalysisState initialSuccessor = getInitialSuccessor(initialState, pCurrentEdge, pCallstack);

    if (initialSuccessor == NO_SUCCESSOR) {
      return ValueAnalysisInterpolant.FALSE;
    }

    // if initial state and successor are equal, return the input interpolant
    // in general, this returned interpolant might be stronger than needed, but only in very rare cases,
    // the weaker interpolant would be different from the input interpolant, so we spare the effort
    if (applyItpEqualityOptimization && initialState.equals(initialSuccessor)) {
      return pInputInterpolant;
    }

    // if the current edge just changes the names of variables (e.g. function arguments, returned variables)
    // then return the input interpolant with those renamings
    if (applyRenamingOptimization && isOnlyVariableRenamingEdge(pCurrentEdge)) {
      return initialSuccessor.createInterpolant();
    }

    ARGPath remainingErrorPath = pErrorPath.obtainSuffix(pOffset + 1);

    // return interpolant TRUE if input interpolant is still TRUE and suffix is contradicting by itself
    if (applyUnsatSuffixOptimization
        && pInputInterpolant.isTrue()
        && initialSuccessor.getSize() > 1
        && isSuffixContradicting(remainingErrorPath)) {
      return ValueAnalysisInterpolant.TRUE;
    }

    for (MemoryLocation currentMemoryLocation : determineMemoryLocationsToInterpolateOn(pCurrentEdge, initialSuccessor)) {
      shutdownNotifier.shutdownIfNecessary();

      // temporarily remove the value of the current memory location from the candidate interpolant
      Pair<Value, Type> value = initialSuccessor.forget(currentMemoryLocation);

      // check if the remaining path now becomes feasible
      if (isRemainingPathFeasible(remainingErrorPath, initialSuccessor)) {
        initialSuccessor.assignConstant(currentMemoryLocation, value.getFirst(), value.getSecond());
      }
    }

    return initialSuccessor.createInterpolant();
  }

  /**
   * Interpolation on (long) error paths may be expensive, so it might pay off to limit the set of
   * memory locations on which to interpolate.
   *
   * This method determines those memory locations on which to interpolate.
   * Memory locations that are in the candidate interpolant but are not returned here will end up
   * in the final interpolant, without any effort spent on interpolation.
   * Memory locations that are returned here are subject for interpolation, and might be eliminated
   * from the final interpolant (at the cost of doing one interpolation query for each of them).
   *
   * Basically, one could return here the intersection of those memory locations that are contained
   * in the candidate interpolant and those that are referenced in the current edge.
   * Hence, all memory locations that are in the candidate interpolant but are not referenced in
   * the current edge would also end up in the final interpolant.
   * This optimization was removed again in commit r16007 because the payoff did not justify
   * maintaining the code, esp. as other optimizations work equally well with less code.
   */
  private Set<MemoryLocation> determineMemoryLocationsToInterpolateOn(final CFAEdge pCurrentEdge,
      ValueAnalysisState candidateInterpolant) {

    return candidateInterpolant.getTrackedMemoryLocations();
  }

  /**
   * This method checks, if the given error path is contradicting in itself.
   *
   * @param errorPath the error path to check.
   * @return true, if the given error path is contradicting in itself, else false
   * @throws InterruptedException
   * @throws CPAException
   */
  private boolean isSuffixContradicting(ARGPath errorPath) throws CPAException, InterruptedException {
    return !isRemainingPathFeasible(errorPath, new ValueAnalysisState());
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
  private ValueAnalysisState getInitialSuccessor(ValueAnalysisState initialState,
      final CFAEdge initialEdge, final Deque<ValueAnalysisState> callstack)
      throws CPATransferException {

    // we enter a function, so lets add the previous state to the stack
    if (initialEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
      callstack.addLast(initialState);
    }

    // we leave a function, so rebuild return-state before assigning the return-value.
    if (!callstack.isEmpty() && initialEdge.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
      // rebuild states with info from previous state
      final ValueAnalysisState callState = callstack.removeLast();
      initialState = initialState.rebuildStateAfterFunctionCall(callState, (FunctionExitNode)initialEdge.getPredecessor());
    }

    Collection<ValueAnalysisState> successors = transfer.getAbstractSuccessorsForEdge(
        initialState,
        precision,
        initialEdge);

    return Iterables.getOnlyElement(successors, NO_SUCCESSOR);
  }

  /**
   * This method checks, whether or not the (remaining) error path is feasible when starting with the given (pseudo) initial state.
   *
   * @param remainingErrorPath the error path to check feasibility on
   * @param state the (pseudo) initial state
   * @return true, it the path is feasible, else false
   * @throws InterruptedException
   * @throws CPAException
   */
  private boolean isRemainingPathFeasible(ARGPath remainingErrorPath, ValueAnalysisState state)
      throws CPAException, InterruptedException {
    numberOfInterpolationQueries++;
    return checker.isFeasible(remainingErrorPath, state);
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
