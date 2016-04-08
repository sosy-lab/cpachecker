/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.smg.refiner;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.smg.SMGCPA;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.refinement.FeasibilityChecker;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;

import com.google.common.base.Optional;

public class SMGFeasibilityChecker implements FeasibilityChecker<SMGState> {

  private final LogManager logger;

  private final StrongestPostOperator<SMGState> strongestPostOp;
  private final SMGState initialState;
  private final VariableTrackingPrecision precision;

  private final CFANode mainFunction;

  public SMGFeasibilityChecker(StrongestPostOperator<SMGState> pStrongestPostOp, LogManager pLogger, CFA pCfa,
      Configuration pConfig, SMGState pInitialState) throws InvalidConfigurationException {
    strongestPostOp = pStrongestPostOp;
    initialState = pInitialState;
    logger = pLogger;
    precision = VariableTrackingPrecision.createStaticPrecision(
        pConfig, pCfa.getVarClassification(), SMGCPA.class);

    mainFunction = pCfa.getMainFunction();
  }

  @Override
  public boolean isFeasible(ARGPath path) throws CPAException, InterruptedException {
    return isFeasible(path, initialState);
  }

  @Override
  public boolean isFeasible(
      final ARGPath pPath,
      final SMGState pStartingPoint) throws CPAException, InterruptedException {
    return isFeasible(pPath, pStartingPoint, new ArrayDeque<SMGState>());
  }

  @Override
  public boolean isFeasible(
      final ARGPath pPath,
      final SMGState pStartingPoint,
      final Deque<SMGState> pCallstack) throws CPAException, InterruptedException {
    return isReachable(pPath, pStartingPoint, pCallstack).isReachable();
  }

  private ReachabilityResult isReachable(
      final ARGPath pPath,
      final SMGState pStartingPoint,
      final Deque<SMGState> pCallstack) throws CPAException, InterruptedException {

    try {
      //TODO ugly, copying SMGState
      // We don't want sideffects of smg transfer relation propagating.
      SMGState next = new SMGState(pStartingPoint);
      CFAEdge edge = null;

      PathIterator iterator = pPath.fullPathIterator();

      if(!iterator.hasNext()) {
        return ReachabilityResult.isReachable(pStartingPoint, null);
      }

      while (iterator.hasNext()) {
        edge = iterator.getOutgoingEdge();

        if (edge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
          next = strongestPostOp.handleFunctionCall(next, edge, pCallstack);
        }

        // we leave a function, so rebuild return-state before assigning the return-value.
        if (!pCallstack.isEmpty() && edge.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
          next = strongestPostOp.handleFunctionReturn(next, edge, pCallstack);
        }

        Optional<SMGState> successors =
            strongestPostOp.getStrongestPost(next, precision, edge);

        // no successors => path is infeasible
        if (!successors.isPresent()) {
          logger.log(Level.FINE, "found path to be infeasible: ", iterator.getOutgoingEdge(),
              " did not yield a successor");

          return ReachabilityResult.isNotReachable();
        }

        // extract singleton successor state
        next = successors.get();

        // some variables might be blacklisted or tracked by BDDs
        // so perform abstraction computation here
        next = strongestPostOp.performAbstraction(next, iterator.getOutgoingEdge().getSuccessor(), pPath, precision);

        iterator.advance();
      }

      return ReachabilityResult.isReachable(next, edge);
    } catch (CPATransferException e) {
      throw new CPAException("Computation of successor failed for checking path: " + e.getMessage(), e);
    }
  }

  private boolean isTarget(SMGState pLastState, CFAEdge pLastEdge, Set<ControlAutomatonCPA> pAutomaton)
      throws CPATransferException, InterruptedException {

    // prune unreachable to detect memory leak that was detected by abstraction
    pLastState.pruneUnreachable();

    for (ControlAutomatonCPA automaton : pAutomaton) {
      if (isTarget(pLastState, pLastEdge, automaton)) {
        return true;
      }
    }

    return false;
  }



  private boolean isTarget(SMGState pLastState, CFAEdge pLastEdge, ControlAutomatonCPA pAutomaton) throws CPATransferException, InterruptedException {

    if (pAutomaton == null) {
      return true;
    }

    StateSpacePartition defaultPartition = StateSpacePartition.getDefaultPartition();

    AbstractState initialAutomatonState = pAutomaton.getInitialState(mainFunction, defaultPartition);
    TransferRelation transferRelation = pAutomaton.getTransferRelation();
    Precision automatonPrecision = pAutomaton.getInitialPrecision(mainFunction, defaultPartition);

    Collection<? extends AbstractState> successors =
        transferRelation.getAbstractSuccessorsForEdge(initialAutomatonState, automatonPrecision, pLastEdge);
    Collection<AbstractState> strengthenResult = new HashSet<>();
    List<AbstractState> lastStateSingelton = new ArrayList<>(1);
    lastStateSingelton.add(pLastState);

    for (AbstractState successor : successors) {
      Collection<? extends AbstractState> strengthenResultForSuccessor =
          transferRelation.strengthen(successor, lastStateSingelton, pLastEdge, automatonPrecision);

      if (strengthenResultForSuccessor == null) {
        strengthenResult.add(successor);
      } else {
        strengthenResult.addAll(strengthenResultForSuccessor);
      }
    }

    for (AbstractState state : strengthenResult) {
      if (state instanceof Targetable && ((Targetable) state).isTarget()) {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean isFeasible(ARGPath pPath, Set<ControlAutomatonCPA> pAutomaton) throws CPAException, InterruptedException {
    return isFeasible(pPath, initialState, pAutomaton);
  }

  @Override
  public boolean isFeasible(ARGPath pPath, SMGState pStartingPoint, Set<ControlAutomatonCPA> pAutomaton)
      throws CPAException, InterruptedException {

    ReachabilityResult result = isReachable(pPath, pStartingPoint, new ArrayDeque<SMGState>());

    if(result.isReachable()) {

      return isTarget(result.getLastState(), result.getLastEdge(), pAutomaton);
    } else {
      return false;
    }
  }

  private static class ReachabilityResult {

    private static final ReachabilityResult NOT_REACHABLE = new ReachabilityResult(false, null, null);

    private final boolean isReachable;
    private final SMGState lastState;
    private final CFAEdge lastEdge;

    private ReachabilityResult(boolean pIsReachable, SMGState pLastState, CFAEdge pLastEdge) {
      isReachable = pIsReachable;
      lastState = pLastState;
      lastEdge = pLastEdge;
    }

    public boolean isReachable() {
      return isReachable;
    }

    public SMGState getLastState() {
      assert isReachable == true : "Getting the last state of the path is only supported if the last state is reachable.";
      return lastState;
    }

    public CFAEdge getLastEdge() {
      assert isReachable == true : "Getting the last edge of the path is only supported if the last state is reachable.";
      return lastEdge;
    }

    public static ReachabilityResult isReachable(SMGState lastState, CFAEdge lastEdge) {
      return new ReachabilityResult(true, lastState, lastEdge);
    }

    public static ReachabilityResult isNotReachable() {
      return NOT_REACHABLE;
    }
  }
}