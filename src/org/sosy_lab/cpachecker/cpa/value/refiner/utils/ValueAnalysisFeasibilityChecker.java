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
import java.util.Collection;
import java.util.Deque;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisPrecision;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.VariableClassification;

import com.google.common.base.Optional;

public class ValueAnalysisFeasibilityChecker {

  private final CFA cfa;
  private final LogManager logger;
  private final ValueAnalysisTransferRelation transfer;
  private final Configuration config;

  /**
   * This method acts as the constructor of the class.
   *
   * @param pLogger the logger to use
   * @param pCfa the cfa in use
   * @param pInitial the initial state for starting the exploration
   * @throws InvalidConfigurationException
   */
  public ValueAnalysisFeasibilityChecker(LogManager pLogger, CFA pCfa) throws InvalidConfigurationException {
    this.cfa    = pCfa;
    this.logger = pLogger;

    config    = Configuration.builder().build();
    transfer  = new ValueAnalysisTransferRelation(config, logger, cfa);
  }

  /**
   * This method checks if the given path is feasible, when not tracking the given set of variables.
   *
   * @param path the path to check
   * @return true, if the path is feasible, else false
   * @throws CPAException
   * @throws InterruptedException
   */
  public boolean isFeasible(final ARGPath path) throws CPAException, InterruptedException {
    try {
      return isFeasible(path,
          new ValueAnalysisPrecision("", config, Optional.<VariableClassification>absent()),
          new ValueAnalysisState());
    }
    catch (InvalidConfigurationException e) {
      throw new CPAException("Configuring ValueAnalysisFeasibilityChecker failed: " + e.getMessage(), e);
    }
  }

  /**
   * This method checks if the given path is feasible, when not tracking the given set of variables, starting with the
   * given initial state.
   *
   * @param path the path to check
   * @param pPrecision the precision to use
   * @param pInitial the initial state
   * @return true, if the path is feasible, else false
   * @throws CPAException
   * @throws InterruptedException
   */
  public boolean isFeasible(final ARGPath path, final ValueAnalysisPrecision pPrecision, final ValueAnalysisState pInitial)
      throws CPAException, InterruptedException {

    return path.size() == getInfeasilbePrefix(path, pPrecision, pInitial).size();
  }

  /**
   * This method obtains the prefix of the path, that is infeasible by itself. If the path is feasible, the whole path
   * is returned
   *
   * @param path the path to check
   * @param pPrecision the precision to use
   * @param pInitial the initial state
   * @return the prefix of the path that is feasible by itself
   * @throws CPAException
   * @throws InterruptedException
   */
  public ARGPath getInfeasilbePrefix(final ARGPath path, final ValueAnalysisPrecision pPrecision, final ValueAnalysisState pInitial)
      throws CPAException, InterruptedException {
    try {
      ValueAnalysisState next = pInitial;

      ARGPath prefix = new ARGPath();

      // we need a callstack to handle recursive functioncalls,
      // because they override variables of current scope and we have to rebuild them.
      // TODO optimisation: use only for recursion, currently every functioncall is rebuild
      final Deque<ValueAnalysisState> callstack = new ArrayDeque<>();

      for (Pair<ARGState, CFAEdge> pathElement : path) {
        final CFAEdge edge = pathElement.getSecond();

        if (edge instanceof FunctionCallEdge) {
          callstack.addLast(next);
        }

        Collection<ValueAnalysisState> successors = transfer.getAbstractSuccessors(
            next,
            pPrecision,
            edge);

        if (edge instanceof FunctionReturnEdge) {
          // rebuild states with info from previous state
          final ValueAnalysisState callState = callstack.removeLast();
          for (final ValueAnalysisState returnState : successors) {
            rebuildStateAfterFunctionCall(callState, returnState);
          }
        }

        prefix.addLast(pathElement);

        // no successors => path is infeasible
        if(successors.isEmpty()) {
          break;
        }

        // get successor state and apply precision
        next = pPrecision.computeAbstraction(successors.iterator().next(),
            AbstractStates.extractLocation(pathElement.getFirst()));
      }
      return prefix;
    } catch (CPATransferException e) {
      throw new CPAException("Computation of successor failed for checking path: " + e.getMessage(), e);
    }
  }

  /** If there was a recursive function, we have wrong values for scoped variables in the returnState.
   * This function rebuilds the correct values with information from the previous callState.
   * We override the wrong values (or insert new values, if the variable is not tracked). */
  private ValueAnalysisState rebuildStateAfterFunctionCall(ValueAnalysisState callState, ValueAnalysisState returnState) {

    // we build the returnState with all local variables from callState
    for (ValueAnalysisState.MemoryLocation trackedVar : callState.getTrackedMemoryLocations()) {
      if (trackedVar.isOnFunctionStack()) {
        // not global, but scoped
        // -> copy/assign value, because there could have been an recursive assignment.
        // this will assign too much values, but should not be unsound
        returnState.assignConstant(trackedVar, callState.getValueFor(trackedVar));
      }
    }

    return returnState;
  }
}
