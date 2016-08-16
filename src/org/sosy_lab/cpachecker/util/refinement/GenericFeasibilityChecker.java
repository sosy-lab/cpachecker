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

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.logging.Level;

/**
 * Generic feasibility checker
 */
public class GenericFeasibilityChecker<S extends ForgetfulState<?>>
    implements FeasibilityChecker<S> {

  private final LogManager logger;

  private final StrongestPostOperator<S> strongestPostOp;
  private final S initialState;
  private final VariableTrackingPrecision precision;


  public GenericFeasibilityChecker(
      final StrongestPostOperator<S> pStrongestPostOp,
      final S pInitialState,
      final Class<? extends ConfigurableProgramAnalysis> pCpaToRefine,
      final LogManager pLogger,
      final Configuration pConfig,
      final CFA pCfa
  ) throws InvalidConfigurationException {

    strongestPostOp = pStrongestPostOp;
    initialState = pInitialState;
    logger = pLogger;
    precision = VariableTrackingPrecision.createStaticPrecision(
        pConfig, pCfa.getVarClassification(), pCpaToRefine);
  }

  @Override
  public boolean isFeasible(ARGPath path) throws CPAException, InterruptedException {
    return isFeasible(path, initialState);
  }

  @Override
  public boolean isFeasible(
      final ARGPath pPath,
      final S pStartingPoint
  ) throws CPAException, InterruptedException {
    return isFeasible(pPath, pStartingPoint, new ArrayDeque<S>());
  }

  @Override
  public final boolean isFeasible(
      final ARGPath pPath,
      final S pStartingPoint,
      final Deque<S> pCallstack
  ) throws CPAException, InterruptedException {

    try {
      S next = pStartingPoint;

      PathIterator iterator = pPath.fullPathIterator();
      while (iterator.hasNext()) {
        final CFAEdge edge = iterator.getOutgoingEdge();

        if (edge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
          next = strongestPostOp.handleFunctionCall(next, edge, pCallstack);
        }

        // we leave a function, so rebuild return-state before assigning the return-value.
        if (!pCallstack.isEmpty() && edge.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
          next = strongestPostOp.handleFunctionReturn(next, edge, pCallstack);
        }

        Optional<S> successors =
            strongestPostOp.getStrongestPost(next, precision, edge);

        // no successors => path is infeasible
        if (!successors.isPresent()) {
          logger.log(Level.FINE, "found path to be infeasible: ", iterator.getOutgoingEdge(),
              " did not yield a successor");

          return false;
        }

        // extract singleton successor state
        next = successors.get();

        // some variables might be blacklisted or tracked by BDDs
        // so perform abstraction computation here
          next = strongestPostOp.performAbstraction(next, iterator.getOutgoingEdge().getSuccessor(), pPath, precision);

        iterator.advance();
      }

      return true;
    } catch (CPATransferException e) {
      throw new CPAException("Computation of successor failed for checking path: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean isFeasible(ARGPath pPath, Set<ControlAutomatonCPA> pAutomatons) throws CPAException, InterruptedException {
    //TODO Implementation
    throw new UnsupportedOperationException("method not yet implemented");
  }

  @Override
  public boolean isFeasible(ARGPath pPath, S pStartingPoint, Set<ControlAutomatonCPA> pAutomatons)
      throws CPAException, InterruptedException {
    //TODO Implementation
    throw new UnsupportedOperationException("method not yet implemented");
  }
}