
/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.refinement;

import java.util.Optional;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathPosition;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.Deque;
import java.util.Set;

/**
 * Generic {@link EdgeInterpolator} that creates interpolants based on
 * {@link MemoryLocation MemoryLocations}. A {@link VariableTrackingPrecision} is used as precision.
 *
 * @param <S> the abstract state to create interpolants based on
 * @param <T> the type the abstract state uses for returning forgotten information
 * @param <I> the type of interpolants that are used and will be created
 */
@Options(prefix="cpa.value.interpolation")
public class GenericEdgeInterpolator<S extends ForgetfulState<T>, T, I extends Interpolant<S>>
    implements EdgeInterpolator<S, I> {

  @Option(secure=true, description="apply optimizations based on equality of input interpolant and candidate interpolant")
  private boolean applyItpEqualityOptimization = true;

  @Option(secure=true, description="apply optimizations based on CFA edges with only variable-renaming semantics")
  private boolean applyRenamingOptimization = true;

  @Option(secure=true, description="apply optimizations based on infeasibility of suffix")
  private boolean applyUnsatSuffixOptimization = true;

  @Option(secure=true, description="whether or not to manage the callstack, which is needed for BAM")
  private boolean manageCallstack = true;

  /**
   * the shutdownNotifier in use
   */
  private final ShutdownNotifier shutdownNotifier;

  /**
   * the postOperator relation in use
   */
  private final StrongestPostOperator<S> postOperator;

  private final InterpolantManager<S, I> interpolantManager;

  private final S initialState;

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
  private final FeasibilityChecker<S> checker;

  /**
   * This method acts as the constructor of the class.
   */
  public GenericEdgeInterpolator(
      final StrongestPostOperator<S> pStrongestPostOperator,
      final FeasibilityChecker<S> pFeasibilityChecker,
      final InterpolantManager<S, I> pInterpolantManager,
      final S pInitialState,
      final Class<? extends ConfigurableProgramAnalysis> pCpaToRefine,
      final Configuration pConfig,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa
  ) throws InvalidConfigurationException {

    pConfig.inject(this, GenericEdgeInterpolator.class);

    try {
      checker            = pFeasibilityChecker;
      postOperator       = pStrongestPostOperator;
      interpolantManager = pInterpolantManager;
      initialState       = pInitialState;

      precision          = VariableTrackingPrecision.createStaticPrecision(
          pConfig, pCfa.getVarClassification(), pCpaToRefine);

      shutdownNotifier   = pShutdownNotifier;
    }
    catch (InvalidConfigurationException e) {
      throw new InvalidConfigurationException("Invalid configuration for checking path: "
          + e.getMessage(), e);
    }
  }

  /**
   * This method derives an interpolant for the given error path and interpolation state.
   *
   * @param pErrorPath the path to check
   * @param pCurrentEdge the current edge to interpolate
   * @param pCallstack the current call stack
   * @param pOffset offset of the state at where to start the current interpolation
   * @param pInputInterpolant the input interpolant
   */
  @Override
  public I deriveInterpolant(
      final ARGPath pErrorPath,
      final CFAEdge pCurrentEdge,
      final Deque<S> pCallstack,
      final PathPosition pOffset,
      final I pInputInterpolant
  ) throws CPAException, InterruptedException {

    numberOfInterpolationQueries = 0;

    // create initial state, based on input interpolant, and create initial successor by consuming
    // the next edge
    S initialState = pInputInterpolant.reconstructState();

    // TODO callstack-management depends on a forward-iteration on a single path.
    // TODO Thus interpolants have to be computed from front to end. Can we assure this?
    final Optional<S> maybeSuccessor;
    if (pCurrentEdge == null) {
      PathIterator it = pOffset.fullPathIterator();
      Optional<S> intermediate = Optional.of(initialState);
      do {
        if (!intermediate.isPresent()) {
          break;
        }

        intermediate = getInitialSuccessor(intermediate.get(), it.getOutgoingEdge(), pCallstack);
        it.advance();
      } while (!it.isPositionWithState());
      maybeSuccessor = intermediate;
    } else {
      maybeSuccessor = getInitialSuccessor(initialState, pCurrentEdge, pCallstack);
    }

    if (!maybeSuccessor.isPresent()) {
      return interpolantManager.getFalseInterpolant();
    }

    S initialSuccessor = maybeSuccessor.get();

    // if initial state and successor are equal, return the input interpolant
    // in general, this returned interpolant might be stronger than needed, but only in very rare
    // cases, the weaker interpolant would be different from the input interpolant, so we spare the
    // effort
    if (applyItpEqualityOptimization && initialState.equals(initialSuccessor)) {
      return pInputInterpolant;
    }

    // if the current edge just changes the names of variables
    // (e.g. function arguments, returned variables)
    // then return the input interpolant with those renamings
    if (applyRenamingOptimization && isOnlyVariableRenamingEdge(pCurrentEdge)) {
      return interpolantManager.createInterpolant(initialSuccessor);
    }

    ARGPath remainingErrorPath = pOffset.iterator().getSuffixExclusive();

    // if the remaining path, i.e., the suffix, is contradicting by itself, then return the TRUE
    // interpolant
    if (applyUnsatSuffixOptimization
        && pInputInterpolant.isTrue()
        && initialSuccessor.getSize() > 1
        && isSuffixContradicting(remainingErrorPath)) {
      return interpolantManager.getTrueInterpolant();
    }

    for (MemoryLocation currentMemoryLocation : determineMemoryLocationsToInterpolateOn(initialSuccessor)) {
      shutdownNotifier.shutdownIfNecessary();

      // temporarily remove the value of the current memory location from the candidate
      // interpolant
      T forgottenInformation = initialSuccessor.forget(currentMemoryLocation);

      // check if the remaining path now becomes feasible
      if (isRemainingPathFeasible(remainingErrorPath, initialSuccessor)) {
        initialSuccessor.remember(currentMemoryLocation, forgottenInformation);
      }
    }

    return interpolantManager.createInterpolant(initialSuccessor);
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
  private Set<MemoryLocation> determineMemoryLocationsToInterpolateOn(final S candidateInterpolant) {
    return candidateInterpolant.getTrackedMemoryLocations();
  }

  /**
   * This method checks, if the given error path is contradicting in itself.
   *
   * @param errorPath the error path to check.
   * @return true, if the given error path is contradicting in itself, else false
   */
  private boolean isSuffixContradicting(ARGPath errorPath)
      throws CPAException, InterruptedException {
    return !isRemainingPathFeasible(errorPath, initialState);
  }

  /**
   * This method returns the number of performed interpolations.
   *
   * @return the number of performed interpolations
   */
  @Override
  public int getNumberOfInterpolationQueries() {
    return numberOfInterpolationQueries;
  }

  /**
   * This method gets the initial successor, i.e. the state following the initial state.
   *
   * @param pInitialState the initial state, i.e. the state represented by the input interpolant.
   * @param pInitialEdge the initial edge of the error path
   * @return the initial successor
   */
  private Optional<S> getInitialSuccessor(
      final S pInitialState,
      final CFAEdge pInitialEdge,
      final Deque<S> pCallstack
  ) throws CPAException, InterruptedException {

    S oldState = pInitialState;

    // we enter a function, so lets add the previous state to the stack
    if (manageCallstack && pInitialEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
      oldState = postOperator.handleFunctionCall(oldState, pInitialEdge, pCallstack);
    }

    // we leave a function, so rebuild return-state before assigning the return-value.
    if (manageCallstack && !pCallstack.isEmpty() && pInitialEdge.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
      oldState = postOperator.handleFunctionReturn(oldState, pInitialEdge, pCallstack);
    }

    return postOperator.getStrongestPost(oldState, precision, pInitialEdge);
  }

  /**
   * This method checks, whether or not the (remaining) error path is feasible when starting with
   * the given (pseudo) initial state.
   *
   * @param remainingErrorPath the error path to check feasibility on
   * @param state the (pseudo) initial state
   * @return true, it the path is feasible, else false
   */
  public boolean isRemainingPathFeasible(ARGPath remainingErrorPath, S state)
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
    // if the edge is null this is a dynamic multi edge
    cfaEdge != null

        // renames from calledFn::___cpa_temp_result_var_ to callerFN::assignedVar
        // if the former is relevant, so is the latter
        && cfaEdge.getEdgeType() == CFAEdgeType.FunctionReturnEdge

    // for the next two edge types this would also work, but variables
    // from the calling/returning function would be added to interpolant
    // as they are not "cleaned up" by the transfer relation
    // so these two stay out for now

    //|| cfaEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge
    //|| cfaEdge.getEdgeType() == CFAEdgeType.ReturnStatementEdge
    ;
  }
}
