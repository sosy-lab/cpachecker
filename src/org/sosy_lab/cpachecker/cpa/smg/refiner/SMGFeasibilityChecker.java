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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;

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
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathPosition;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;

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
      CFA pCfa, SMGState pInitialState, Set<ControlAutomatonCPA> pAutomatonCPA,
      BlockOperator pBlockOperator) {
    strongestPostOp = pStrongestPostOp;
    initialState = pInitialState;
    logger = pLogger;
    precision = SMGPrecision.createStaticPrecision(false, pLogger, pBlockOperator);
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

      PathIterator iterator = pPath.pathIterator();

      while (iterator.hasNext()) {
        edge = iterator.getOutgoingEdge();

//        int c = 0;

//        for (SMGState state : next) {
//          SMGDebugTest.dumpPlot("beforeStrongest" + c, state);
//          c++;
//        }

        Collection<SMGState> successors =
            strongestPostOp.getStrongestPost(next, precision, edge);

//        c = 0;
//
//        for (SMGState state : successors) {
//          SMGDebugTest.dumpPlot("afterStrongest" + c, state);
//          c++;
//        }

        // no successors => path is infeasible
        if (successors.isEmpty()) {
          logger.log(Level.FINE, "found path to be infeasible: ", iterator.getOutgoingEdge(),
              " did not yield a successor");

          return ReachabilityResult.isNotReachable(iterator.getPosition());
        }

        iterator.advance();
        next.clear();
        next.addAll(successors);
      }

      return ReachabilityResult.isReachable(next, edge, iterator.getPosition());
    } catch (CPATransferException e) {
      throw new CPAException("Computation of successor failed for checking path: " + e.getMessage(), e);
    }
  }

  private boolean isTarget(Collection<SMGState> pLastStates, CFAEdge pLastEdge, ARGState pLastState,
      boolean allTargets) throws CPATransferException, InterruptedException {

    Set<ControlAutomatonCPA> automatonCPAsToCheck =
        getAutomatons(pLastState, allTargets);

    for (SMGState lastState : pLastStates) {
      // prune unreachable to detect memory leak that was detected by abstraction
      lastState.pruneUnreachable();

      for (ControlAutomatonCPA automaton : automatonCPAsToCheck) {
        boolean isTarget = isTarget(lastState, pLastEdge, automaton);
        if (allTargets && isTarget) {
          return true;
        } else if (!allTargets && !isTarget) {
          return false;
        }
      }
    }

    return !allTargets;
  }

  private Set<ControlAutomatonCPA> getAutomatons(ARGState pLastState,
      boolean allTargets) {

    if (allTargets) {
      return automatonCPA;
    }

    Predicate<? super AutomatonState> automatonStateIsTarget = (AutomatonState state) -> {
      return state.isTarget() ? true : false;
    };

    Function<AutomatonState, String> toNameFunction = (AutomatonState state) -> {return state.getOwningAutomatonName();};

    Set<String> automatonNames = AbstractStates.asIterable(pLastState).filter(AutomatonState.class)
        .filter(automatonStateIsTarget).transform(toNameFunction).toSet();

    Predicate<? super ControlAutomatonCPA> automatonNameFilter =
        ((ControlAutomatonCPA automaton) -> {
          return automatonNames.contains(automaton.getTopState().getOwningAutomatonName());
        });

    Set<ControlAutomatonCPA> automatonCPAsToCheck =
        FluentIterable.from(automatonCPA).filter(automatonNameFilter).toSet();

    return automatonCPAsToCheck;
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

      strengthenResult.addAll(strengthenResultForSuccessor);
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
    return isFeasible(pPath, pStartingPoint, pPrecision, false);
  }

  public boolean isFeasible(ARGPath pPath, SMGState pStartingPoint,
      SMGPrecision pPrecision, boolean pAllTargets)
          throws CPAException, InterruptedException {

    Preconditions.checkArgument(pPath.getInnerEdges().size() > 0);

    ReachabilityResult result = isReachable(pPath, pStartingPoint, pPrecision);

    if (result.isReachable()) {

      return isTarget(result.getLastState(), result.getLastEdge(), pPath.getLastState(), pAllTargets);
    } else {
      return false;
    }
  }

  private static class ReachabilityResult {

    private final boolean isReachable;
    private final Collection<SMGState> lastStates;
    private final CFAEdge lastEdge;
    private final PathPosition lastPosition;

    private ReachabilityResult(Collection<SMGState> pLastStates,
        CFAEdge pLastEdge, PathPosition pLastPosition) {

      Preconditions.checkNotNull(pLastEdge);
      Preconditions.checkNotNull(pLastStates);

      isReachable = true;
      lastStates = pLastStates;
      lastEdge = pLastEdge;
      lastPosition = pLastPosition;
    }

    public ReachabilityResult(PathPosition pLastPosition) {
      isReachable = false;
      lastStates = null;
      lastEdge = null;
      lastPosition = pLastPosition;
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

    public PathPosition getLastPosition() {
      return lastPosition;
    }

    public static ReachabilityResult isReachable(Collection<SMGState> lastStates, CFAEdge lastEdge,
        PathPosition pLastPosition) {
      return new ReachabilityResult(lastStates, lastEdge, pLastPosition);
    }

    public static ReachabilityResult isNotReachable(PathPosition pLastPosition) {
      return new ReachabilityResult(pLastPosition);
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

  public boolean isReachable(ARGPath pErrorPath, SMGState pInitialState)
      throws CPAException, InterruptedException {

    if (pErrorPath.size() == 1) {
      return true;
    }

    return isReachable(pErrorPath, pInitialState, precision).isReachable();
  }

  public boolean isRemainingPathFeasible(ARGPath pRemainingErrorPath, SMGState pState,
      CFAEdge pCurrentEdge, boolean pAllTargets) throws CPAException, InterruptedException {

    if (pRemainingErrorPath.size() > 1) {
      return isFeasible(pRemainingErrorPath, pState);
    }

    /*Prevent causing side effects when pruning.*/
    SMGState state = new SMGState(pState);

    return isTarget(ImmutableSet.of(state), pCurrentEdge, pRemainingErrorPath.getLastState(),
        pAllTargets);
  }

  public boolean isReachable(ARGPath pErrorPathPrefix) throws CPAException, InterruptedException {
    return isReachable(pErrorPathPrefix, initialState);
  }

  public PathPosition getLastReachablePosition(ARGPath pErrorPath, List<SMGState> pStartingPoints)
      throws CPAException, InterruptedException {

    PathPosition result = pErrorPath.fullPathIterator().getPosition();

    for (SMGState startPoint : pStartingPoints) {
      ReachabilityResult reachabilityResult = isReachable(pErrorPath, startPoint, precision);
      PathPosition reachabilityResultPosition = reachabilityResult.getLastPosition();

      if (result.iterator().getIndex() < reachabilityResultPosition.iterator().getIndex()) {
        result = reachabilityResultPosition;
      }
    }

    return result;
  }

  public boolean isFeasible(ARGPath pErrorPath, boolean pAllTargets)
      throws CPAException, InterruptedException {
    return isFeasible(pErrorPath, initialState, precision, pAllTargets);
  }
}