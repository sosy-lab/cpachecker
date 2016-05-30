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

import com.google.common.collect.ImmutableList;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class SMGFeasibilityChecker {

  private final LogManager logger;
  private final SMGStrongestPostOperator strongestPostOp;
  private final SMGState initialState;
  private final SMGPrecision precision;
  private final CFANode mainFunction;
  private final Set<ControlAutomatonCPA> automatonCPA;

  public SMGFeasibilityChecker(SMGStrongestPostOperator pStrongestPostOp, LogManager pLogger,
      CFA pCfa, SMGState pInitialState, Set<ControlAutomatonCPA> pAutomatonCPA) {
    strongestPostOp = pStrongestPostOp;
    initialState = pInitialState;
    logger = pLogger;
    precision = SMGPrecision.createStaticPrecision(false, pLogger);
    mainFunction = pCfa.getMainFunction();
    automatonCPA = pAutomatonCPA;
  }

  private ReachabilityResult isReachable(
       ARGPath pPath,
       SMGState pStartingPoint,
       SMGPrecision precision) throws CPAException, InterruptedException {

 // We don't want sideffects of smg transfer relation for smg state propagating.
    SMGState start = new SMGState(pStartingPoint);
    Collection<SMGState> next = new ArrayList<>();
    next.add(start);

    try {
      CFAEdge edge = null;

      PathIterator iterator = pPath.fullPathIterator();

      if (!iterator.hasNext()) {
        Collection<SMGState> lastStates = ImmutableList.of(pStartingPoint);
        return ReachabilityResult.isReachable(lastStates, null);
      }

      while (iterator.hasNext()) {
        edge = iterator.getOutgoingEdge();

        Collection<SMGState> successors =
            strongestPostOp.getStrongestPost(next, precision, edge);

        // no successors => path is infeasible
        if (!successors.isEmpty()) {
          logger.log(Level.FINE, "found path to be infeasible: ", iterator.getOutgoingEdge(),
              " did not yield a successor");

          return ReachabilityResult.isNotReachable();
        }

        iterator.advance();
      }

      return ReachabilityResult.isReachable(next, edge);
    } catch (CPATransferException e) {
      throw new CPAException("Computation of successor failed for checking path: " + e.getMessage(), e);
    }
  }

  private boolean isTarget(Collection<SMGState> pLastStates, CFAEdge pLastEdge)
          throws CPATransferException, InterruptedException {

    for (SMGState lastState : pLastStates) {
      // prune unreachable to detect memory leak that was detected by abstraction
      lastState.pruneUnreachable();

      for (ControlAutomatonCPA automaton : automatonCPA) {
        if (isTarget(lastState, pLastEdge, automaton)) {
          return true;
        }
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

  public boolean isFeasible(ARGPath pPath) throws CPAException, InterruptedException {
    return isFeasible(pPath, initialState);
  }

  public boolean isFeasible(ARGPath pPath, SMGState pStartingPoint)
          throws CPAException, InterruptedException {
    return isFeasible(pPath, pStartingPoint, precision);
  }

  public boolean isFeasible(ARGPath pPath, SMGState pStartingPoint,
      SMGPrecision pPrecision)
          throws CPAException, InterruptedException {

    ReachabilityResult result = isReachable(pPath, pStartingPoint, pPrecision);

    if (result.isReachable()) {

      return isTarget(result.getLastState(), result.getLastEdge());
    } else {
      return false;
    }
  }

  private static class ReachabilityResult {

    private static final ReachabilityResult NOT_REACHABLE = new ReachabilityResult(false, null, null);

    private final boolean isReachable;
    private final Collection<SMGState> lastStates;
    private final CFAEdge lastEdge;

    private ReachabilityResult(boolean pIsReachable, Collection<SMGState> pLastStates, CFAEdge pLastEdge) {
      isReachable = pIsReachable;
      lastStates = pLastStates;
      lastEdge = pLastEdge;
    }

    public boolean isReachable() {
      return isReachable;
    }

    public Collection<SMGState> getLastState() {
      assert isReachable == true : "Getting the last state of the path is only supported if the last state is reachable.";
      return lastStates;
    }

    public CFAEdge getLastEdge() {
      assert isReachable == true : "Getting the last edge of the path is only supported if the last state is reachable.";
      return lastEdge;
    }

    public static ReachabilityResult isReachable(Collection<SMGState> lastStates, CFAEdge lastEdge) {
      return new ReachabilityResult(true, lastStates, lastEdge);
    }

    public static ReachabilityResult isNotReachable() {
      return NOT_REACHABLE;
    }
  }

  public boolean isFeasible(ARGPath pErrorPath, List<SMGState> pStartingPoints)
      throws CPAException, InterruptedException {

    boolean result = true;

    for (SMGState start : pStartingPoints) {
      result = result && isFeasible(pErrorPath, start);

      if (!result) {
        break;
      }
    }

    return result;
  }
}