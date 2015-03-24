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

import java.util.ArrayDeque;
import java.util.ArrayList;
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
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.MutableARGPath;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.PrefixProvider;

import com.google.common.base.Optional;

/**
 * Generic feasibility checker
 */
public abstract class GenericFeasibilityChecker<S extends AbstractState>
    implements FeasibilityChecker<S>, PrefixProvider {

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
  public boolean isFeasible(ARGPath path) throws CPAException {
    return isFeasible(path, initialState);
  }

  @Override
  public boolean isFeasible(ARGPath path, S startingPoint) throws CPAException {
    final List<ARGPath> infeasiblePrefixes =
        getInfeasiblePrefixes(path, startingPoint, new ArrayDeque<S>());

    return path.size() == infeasiblePrefixes.get(0).size();
  }

  @Override
  public List<ARGPath> getInfeasilbePrefixes(ARGPath path)
      throws CPAException, InterruptedException {
    return getInfeasiblePrefixes(path, initialState, new ArrayDeque<S>());
  }


  @Override
  public List<ARGPath> getInfeasiblePrefixes(
      final ARGPath path,
      final S pInitial,
      final Deque<S> callstack
  ) throws CPAException {

    List<ARGPath> prefixes = new ArrayList<>();
    boolean performAbstraction = precision.allowsAbstraction();

    try {
      MutableARGPath currentPrefix = new MutableARGPath();
      S next = pInitial;

      ARGPath.PathIterator iterator = path.pathIterator();
      while (iterator.hasNext()) {
        final CFAEdge edge = iterator.getOutgoingEdge();

        Optional<S> successor = getSuccessor(next, edge, callstack);

        currentPrefix.addLast(Pair.of(iterator.getAbstractState(), iterator.getOutgoingEdge()));

        // no successors => path is infeasible
        if (!successor.isPresent()) {
          logger.log(Level.FINE, "found infeasible prefix: ",
              iterator.getOutgoingEdge(), " did not yield a successor");

          prefixes.add(currentPrefix.immutableCopy());

          currentPrefix = new MutableARGPath();
          successor     = Optional.of(next);
        }

        // extract singleton successor state
        next = successor.get();

        if (performAbstraction) {
          next = performAbstractions(next, edge.getSuccessor(), path);
        }

        iterator.advance();
      }

      // prefixes is empty => path is feasible, so add complete path
      if (prefixes.isEmpty()) {
        logger.log(Level.FINE, "no infeasible prefixes found - path is feasible");
        prefixes.add(path);
      }

      return prefixes;
    } catch (CPAException e) {
      throw new CPAException("Computation of successor failed for checking path: " + e.getMessage(), e);
    }
  }

  private Optional<S> getSuccessor(final S pNext,
      final CFAEdge pEdge,
      final Deque<S> pCallstack)
      throws CPAException {

    S next = pNext;

    if (pEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
      next = strongestPostOp.handleFunctionCall(next, pEdge, pCallstack);
    }

    if (!pCallstack.isEmpty() && pEdge.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
      next = strongestPostOp.handleFunctionReturn(next, pEdge, pCallstack);
    }

    return strongestPostOp.getStrongestPost(next, precision, pEdge);
  }

  protected abstract S performAbstractions(S pNext, CFANode pLocation, ARGPath pPath);
}
