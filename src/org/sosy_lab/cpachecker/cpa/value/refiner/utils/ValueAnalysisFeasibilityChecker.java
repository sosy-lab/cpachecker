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
import java.util.Deque;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.MutableARGPath;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

public class ValueAnalysisFeasibilityChecker {

  private final LogManager logger;
  private final ValueAnalysisTransferRelation transfer;
  private final VariableTrackingPrecision precision;

  /**
   * This method acts as the constructor of the class.
   *
   * @param pLogger the logger to use
   * @param pCfa the cfa in use
   * @param pInitial the initial state for starting the exploration
   * @throws InvalidConfigurationException
   */
  public ValueAnalysisFeasibilityChecker(LogManager pLogger, CFA pCfa, Configuration config) throws InvalidConfigurationException {
    logger    = pLogger;

    transfer  = new ValueAnalysisTransferRelation(Configuration.builder().build(), pLogger, pCfa);
    precision = VariableTrackingPrecision.createStaticPrecision(config, pCfa.getVarClassification());
  }

  /**
   * This method checks if the given path is feasible, when not tracking the given set of variables.
   *
   * @param path the path to check
   * @return true, if the path is feasible, else false
   * @throws CPAException
   * @throws InterruptedException
   */
  public boolean isFeasible(final MutableARGPath path) throws CPAException, InterruptedException {
    return isFeasible(path, new ValueAnalysisState(), new ArrayDeque<ValueAnalysisState>());
  }

  /**
   * This method checks if the given path is feasible, starting with the given initial state.
   *
   * @param path the path to check
   * @param pInitial the initial state
   * @param pCallstack the initial callstack
   * @return true, if the path is feasible, else false
   * @throws CPAException
   * @throws InterruptedException
   */
  public boolean isFeasible(final MutableARGPath path, final ValueAnalysisState pInitial)
          throws CPAException, InterruptedException {
    return isFeasible(path, pInitial, new ArrayDeque<ValueAnalysisState>());
  }

  /**
   * This method checks if the given path is feasible, starting with the given initial state.
   *
   * @param path the path to check
   * @param pInitial the initial state
   * @param pCallstack the initial callstack
   * @return true, if the path is feasible, else false
   * @throws CPAException
   * @throws InterruptedException
   */
  public boolean isFeasible(final MutableARGPath path, final ValueAnalysisState pInitial, final Deque<ValueAnalysisState> pCallstack)
      throws CPAException, InterruptedException {

    return path.size() == getInfeasilbePrefix(path, pInitial, pCallstack).size();
  }

  /**
   * This method obtains the shortest prefix of the path, that is infeasible by itself.
   * If the path is feasible, the whole path is returned.
   * We assume that the path starts at the CFA-root-node and does not skip any edge on its way.
   *
   * @param path the path to check
   * @param pInitial the initial state
   * @param pCallstack the initial callstack
   * @return the shortest prefix of the path that is feasible by itself
   * @throws CPAException
   * @throws InterruptedException
   */
  public MutableARGPath getInfeasilbePrefix(final MutableARGPath path, final ValueAnalysisState pInitial,
                                     final Deque<ValueAnalysisState> pCallstack)
          throws CPAException, InterruptedException {
    return getInfeasilbePrefixes(path, pInitial,pCallstack).get(0);
  }

  /**
   * This method obtains a list of prefixes of the path, that are infeasible by themselves. If the path is feasible, the whole path
   * is returned as the only element of the list.
   *
   * @param path the path to check
   * @param pInitial the initial state
   * @return the list of prefix of the path that are feasible by themselves
   * @throws CPAException
   * @throws InterruptedException
   */
  public List<MutableARGPath> getInfeasilbePrefixes(final MutableARGPath path,
                                             final ValueAnalysisState pInitial,
                                             final Deque<ValueAnalysisState> callstack)
      throws CPAException, InterruptedException {

    List<MutableARGPath> prefixes = new ArrayList<>();

    try {
      MutableARGPath currentPrefix   = new MutableARGPath();
      ValueAnalysisState next = pInitial;

      // we need a callstack to handle recursive functioncalls,
      // because they override variables of current scope and we have to rebuild them.
      // TODO optimisation: use only for recursion, currently every functioncall is rebuild

      for (Pair<ARGState, CFAEdge> pathElement : path) {
        final CFAEdge edge = pathElement.getSecond();

        // we enter a function, so lets add the previous state to the stack
        if (edge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
          callstack.addLast(next);
        }

        // we leave a function, so rebuild return-state before assigning the return-value.
        if (!callstack.isEmpty() && edge.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
          // rebuild states with info from previous state
          final ValueAnalysisState callState = callstack.removeLast();
          next = next.rebuildStateAfterFunctionCall(callState);
        }

        Collection<ValueAnalysisState> successors = transfer.getAbstractSuccessorsForEdge(
            next,
            precision,
            edge);

        currentPrefix.addLast(pathElement);

        // no successors => path is infeasible
        if (successors.isEmpty()) {
          logger.log(Level.FINE, "found infeasible prefix: ", pathElement.getSecond(), " did not yield a successor");
          prefixes.add(currentPrefix);

          currentPrefix = new MutableARGPath();
          successors    = Sets.newHashSet(next);
        }

        // extract singleton successor state
        next = Iterables.getOnlyElement(successors);
      }

      // prefixes is empty => path is feasible, so add complete path
      if (prefixes.isEmpty()) {
        logger.log(Level.FINE, "no infeasible prefixes found - path is feasible");
        prefixes.add(path);
      }

      return prefixes;
    } catch (CPATransferException e) {
      throw new CPAException("Computation of successor failed for checking path: " + e.getMessage(), e);
    }
  }

  public List<Pair<ValueAnalysisState, CFAEdge>> evaluate(final MutableARGPath path)
      throws CPAException, InterruptedException {

    assert(isFeasible(path)) : "Cannot reevalute an infeasible path!";

    try {
      List<Pair<ValueAnalysisState, CFAEdge>> reevaluatedPath = new ArrayList<>();
      ValueAnalysisState next = new ValueAnalysisState();

      for (Pair<ARGState, CFAEdge> pathElement : path) {
        Collection<ValueAnalysisState> successors = transfer.getAbstractSuccessorsForEdge(
            next,
            precision,
            pathElement.getSecond());

        // extract singleton successor state
        next = Iterables.getOnlyElement(successors);

        reevaluatedPath.add(Pair.of(next, pathElement.getSecond()));
      }

      return reevaluatedPath;
    } catch (CPATransferException e) {
      throw new CPAException("Computation of successor failed for checking path: " + e.getMessage(), e);
    }
  }
}
