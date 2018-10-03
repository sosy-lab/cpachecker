/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.refiner.thresholdRefiner;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathPosition;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.UnmodifiableSMGState;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGStrongestPostOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;

public abstract class SMGThresholdFeasibilityChecker {

  protected final LogManager logger;
  protected final SMGStrongestPostOperator strongestPostOp;
  protected final UnmodifiableSMGState initialState;
  protected final SMGThresholdPrecision precision;
  protected final CFANode mainFunction;
  protected final Set<ControlAutomatonCPA> automatonCPA;
  protected ARGState cutState;

  public SMGThresholdFeasibilityChecker(
      SMGStrongestPostOperator pStrongestPostOp,
      LogManager pLogger,
      CFA pCfa,
      UnmodifiableSMGState pInitialState,
      Set<ControlAutomatonCPA> pAutomatonCPA,
      BlockOperator pBlockOperator) {
    strongestPostOp = pStrongestPostOp;
    initialState = pInitialState;
    logger = pLogger;
    precision = SMGThresholdPrecision.createStaticPrecision(false, pBlockOperator);
    mainFunction = pCfa.getMainFunction();
    automatonCPA = pAutomatonCPA;
    cutState = null;
  }

  protected abstract ReachabilityResult isReachable(
      ARGPath pPath, UnmodifiableSMGState pStartingPoint, SMGThresholdPrecision pPrecision)
      throws CPAException;

  private boolean isTarget(
      Collection<SMGState> pLastStates, CFAEdge pLastEdge, ARGState pLastState, boolean allTargets)
      throws CPATransferException, InterruptedException {

    Set<ControlAutomatonCPA> automatonCPAsToCheck = getAutomata(pLastState, allTargets);

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

  private Set<ControlAutomatonCPA> getAutomata(ARGState pLastState, boolean allTargets) {

    if (allTargets) {
      return automatonCPA;
    }

    Set<String> automatonNames =
        AbstractStates.asIterable(pLastState)
            .filter(AutomatonState.class)
            .filter(AutomatonState::isTarget)
            .transform(AutomatonState::getOwningAutomatonName)
            .toSet();

    return FluentIterable.from(automatonCPA)
        .filter(cpa -> automatonNames.contains(cpa.getTopState().getOwningAutomatonName()))
        .toSet();
  }

  private boolean isTarget(
      UnmodifiableSMGState pLastState, CFAEdge pLastEdge, ControlAutomatonCPA pAutomaton)
      throws CPATransferException, InterruptedException {

    if (pAutomaton == null) {
      return true;
    }

    StateSpacePartition defaultPartition = StateSpacePartition.getDefaultPartition();

    AbstractState initialAutomatonState =
        pAutomaton.getInitialState(mainFunction, defaultPartition);
    TransferRelation transferRelation = pAutomaton.getTransferRelation();
    Precision automatonPrecision = pAutomaton.getInitialPrecision(mainFunction, defaultPartition);

    Collection<? extends AbstractState> successors =
        transferRelation.getAbstractSuccessorsForEdge(
            initialAutomatonState, automatonPrecision, pLastEdge);
    Collection<AbstractState> strengthenResult = new HashSet<>();
    List<AbstractState> lastStateSingelton = Collections.singletonList(pLastState);

    for (AbstractState successor : successors) {
      strengthenResult.addAll(
          transferRelation.strengthen(
              successor, lastStateSingelton, pLastEdge, automatonPrecision));
    }

    return Iterables.any(strengthenResult, AbstractStates.IS_TARGET_STATE);
  }

  public boolean isFeasible(ARGPath pPath) throws CPAException, InterruptedException {
    return isFeasible(pPath, initialState);
  }

  private boolean isFeasible(ARGPath pPath, UnmodifiableSMGState pStartingPoint)
      throws CPAException, InterruptedException {
    return isFeasible(pPath, pStartingPoint, precision, false);
  }

  private boolean isFeasible(
      ARGPath pPath,
      UnmodifiableSMGState pStartingPoint,
      SMGThresholdPrecision pPrecision,
      boolean pAllTargets)
      throws CPAException, InterruptedException {

    Preconditions.checkArgument(pPath.getInnerEdges().size() > 0);
    ReachabilityResult result = isReachable(pPath, pStartingPoint, pPrecision);

    if (result.isReachable()) {
      return isTarget(
          result.getLastState(), result.getLastEdge(), pPath.getLastState(), pAllTargets);
    } else {
      return false;
    }
  }

  protected static class ReachabilityResult {

    private final boolean isReachable;
    private final Collection<SMGState> lastStates;
    private final CFAEdge lastEdge;
    private final PathPosition lastPosition;

    private ReachabilityResult(
        Collection<SMGState> pLastStates, CFAEdge pLastEdge, PathPosition pLastPosition) {

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
      assert isReachable
          : "Getting the last state of the path is only supported if the last state is reachable.";
      return lastStates;
    }

    public CFAEdge getLastEdge() {
      assert isReachable
          : "Getting the last edge of the path is only supported if the last state is reachable.";
      return lastEdge;
    }

    public PathPosition getLastPosition() {
      return lastPosition;
    }

    public static ReachabilityResult isReachable(
        Collection<SMGState> lastStates, CFAEdge lastEdge, PathPosition pLastPosition) {
      return new ReachabilityResult(lastStates, lastEdge, pLastPosition);
    }

    public static ReachabilityResult isNotReachable(PathPosition pLastPosition) {
      return new ReachabilityResult(pLastPosition);
    }
  }

  public boolean isFeasible(ARGPath pErrorPath, Collection<SMGState> pStartingPoints)
      throws CPAException, InterruptedException {
    for (UnmodifiableSMGState start : pStartingPoints) {
      if (!isFeasible(pErrorPath, start)) {
        return false;
      }
    }
    return true;
  }

  public boolean isReachable(ARGPath pErrorPath, UnmodifiableSMGState pInitialState)
      throws CPAException {

    if (pErrorPath.size() == 1) {
      return true;
    }

    return isReachable(pErrorPath, pInitialState, precision).isReachable();
  }

  public boolean isRemainingPathFeasible(
      ARGPath pRemainingErrorPath,
      UnmodifiableSMGState pState,
      CFAEdge pCurrentEdge,
      boolean pAllTargets)
      throws CPAException, InterruptedException {

    if (pRemainingErrorPath.size() > 1) {
      return isFeasible(pRemainingErrorPath, pState);
    }

    /*Prevent causing side effects when pruning.*/
    SMGState state = pState.copyOf();

    return isTarget(
        ImmutableSet.of(state), pCurrentEdge, pRemainingErrorPath.getLastState(), pAllTargets);
  }

  public boolean isReachable(ARGPath pErrorPathPrefix) throws CPAException {
    return isReachable(pErrorPathPrefix, initialState);
  }

  public PathPosition getLastReachablePosition(
      ARGPath pErrorPath, List<UnmodifiableSMGState> pStartingPoints) throws CPAException {

    PathPosition result = pErrorPath.fullPathIterator().getPosition();

    for (UnmodifiableSMGState startPoint : pStartingPoints) {
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

  protected ARGState getCutState() {
    return cutState;
  }
}