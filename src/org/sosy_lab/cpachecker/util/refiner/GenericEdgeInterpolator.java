
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
package org.sosy_lab.cpachecker.util.refiner;

import java.util.Deque;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import com.google.common.base.Optional;

public class GenericEdgeInterpolator<S extends ForgetfulState<T>, T, I extends Interpolant<S>>
    implements EdgeInterpolator<S, T, I> {

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
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa
  ) throws InvalidConfigurationException {

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
   * @param pOffset offset of the state at where to start the current interpolation
   * @param pInputInterpolant the input interpolant
   * @throws org.sosy_lab.cpachecker.exceptions.CPAException
   * @throws InterruptedException
   */
  @Override
  public I deriveInterpolant(
      final ARGPath pErrorPath,
      final CFAEdge pCurrentEdge,
      final Deque<S> callstack,
      final int pOffset,
      final I pInputInterpolant
  ) throws CPAException {

    numberOfInterpolationQueries = 0;

    // create initial state, based on input interpolant, and create initial successor by consuming
    // the next edge
    S initialState = pInputInterpolant.reconstructState();

    // TODO callstack-management depends on a forward-iteration on a single path.
    // TODO Thus interpolants have to be computed from front to end. Can we assure this?
    final Optional<S> maybeSuccessor = getInitialSuccessor(initialState, pCurrentEdge, callstack);

    if (!maybeSuccessor.isPresent()) {
      return interpolantManager.getFalseInterpolant();
    }

    S initialSuccessor = maybeSuccessor.get();

    // if initial state and successor are equal, return the input interpolant
    // in general, this returned interpolant might be stronger than needed, but only in very rare
    // cases, the weaker interpolant would be different from the input interpolant, so we spare the
    // effort
    if (initialState.equals(initialSuccessor)) {
      return pInputInterpolant;
    }

    // if the current edge just changes the names of variables
    // (e.g. function arguments, returned variables)
    // then return the input interpolant with those renamings
    if (isOnlyVariableRenamingEdge(pCurrentEdge)) {
      return interpolantManager.createInterpolant(initialSuccessor);
    }

    try {
      ARGPath remainingErrorPath = pErrorPath.obtainSuffix(pOffset + 1);

      // if the remaining path, i.e., the suffix, is contradicting by itself, then return the TRUE
      // interpolant
      if (initialSuccessor.getSize() > 1 && isSuffixContradicting(remainingErrorPath)) {
        return interpolantManager.getTrueInterpolant();
      }

      for (MemoryLocation currentMemoryLocation : initialSuccessor.getTrackedMemoryLocations()) {
        shutdownNotifier.shutdownIfNecessary();

        // temporarily remove the value of the current memory location from the candidate
        // interpolant
        T forgottenInformation = initialSuccessor.forget(currentMemoryLocation);

        // check if the remaining path now becomes feasible
        if (isRemainingPathFeasible(remainingErrorPath, initialSuccessor)) {
          initialSuccessor.remember(currentMemoryLocation, forgottenInformation);
        }
      }
    } catch (InterruptedException e) {
      throw new CPAException("Interrupted while computing interpolant", e);
    }

    return interpolantManager.createInterpolant(initialSuccessor);
  }

  /**
   * This method checks, if the given error path is contradicting in itself.
   *
   * @param errorPath the error path to check.
   * @return true, if the given error path is contradicting in itself, else false
   * @throws InterruptedException
   * @throws CPAException
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
   * @param initialState the initial state, i.e. the state represented by the input interpolant.
   * @param pInitialEdge the initial edge of the error path
   * @return the initial successor
   * @throws CPAException
   */
  private Optional<S> getInitialSuccessor(
      final S pInitialState,
      final CFAEdge pInitialEdge,
      final Deque<S> pCallstack
  ) throws CPAException {

    S oldState = pInitialState;

    // we enter a function, so lets add the previous state to the stack
    if (pInitialEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
      oldState = postOperator.handleFunctionCall(oldState, pInitialEdge, pCallstack);
    }

    // we leave a function, so rebuild return-state before assigning the return-value.
    if (!pCallstack.isEmpty() && pInitialEdge.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
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
   * @throws CPAException
   */
  private boolean isRemainingPathFeasible(ARGPath remainingErrorPath, S state)
      throws CPAException {
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
