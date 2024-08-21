// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.refiner;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
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
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.path.PathPosition;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.UnmodifiableSMGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.refinement.FeasibilityChecker;

public class SMGFeasibilityChecker implements FeasibilityChecker<UnmodifiableSMGState> {

  private final LogManager logger;
  private final SMGStrongestPostOperator strongestPostOp;
  private final UnmodifiableSMGState initialState;
  private final SMGPrecision precision;
  private final CFANode mainFunction;
  private final Set<ControlAutomatonCPA> automatonCPA;

  public SMGFeasibilityChecker(
      SMGStrongestPostOperator pStrongestPostOp,
      LogManager pLogger,
      CFA pCfa,
      UnmodifiableSMGState pInitialState,
      Set<ControlAutomatonCPA> pAutomatonCPA) {
    strongestPostOp = pStrongestPostOp;
    initialState = pInitialState;
    logger = pLogger;
    precision = SMGPrecision.createStaticPrecision(false);
    mainFunction = pCfa.getMainFunction();
    automatonCPA = pAutomatonCPA;
  }

  private ReachabilityResult isReachable(
      ARGPath pPath, UnmodifiableSMGState pStartingPoint, SMGPrecision pPrecision)
      throws CPAException, InterruptedException {

    // We don't want sideffects of smg transfer relation for smg state propagating.
    SMGState start = pStartingPoint.copyOf();
    Collection<SMGState> next = new ArrayList<>();
    next.add(start);

    try {
      CFAEdge edge = null;

      PathIterator iterator = pPath.pathIterator();

      while (iterator.hasNext()) {
        edge = iterator.getOutgoingEdge();

        Collection<SMGState> successors = strongestPostOp.getStrongestPost(next, pPrecision, edge);

        // no successors => path is infeasible
        if (successors.isEmpty()) {
          logger.log(
              Level.FINE,
              "found path to be infeasible: ",
              iterator.getOutgoingEdge(),
              " did not yield a successor");

          return ReachabilityResult.isNotReachable(iterator.getPosition());
        }

        iterator.advance();
        next.clear();
        next.addAll(successors);
      }

      return ReachabilityResult.isReachable(next, edge, iterator.getPosition());
    } catch (CPATransferException e) {
      throw new CPAException(
          "Computation of successor failed for checking path: " + e.getMessage(), e);
    }
  }

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

    return Iterables.any(strengthenResult, AbstractStates::isTargetState);
  }

  @Override
  public boolean isFeasible(ARGPath pPath) throws CPAException, InterruptedException {
    return isFeasible(pPath, initialState);
  }

  @Override
  public boolean isFeasible(ARGPath pPath, UnmodifiableSMGState pStartingPoint)
      throws CPAException, InterruptedException {
    return isFeasible(pPath, pStartingPoint, precision, false);
  }

  boolean isFeasible(ARGPath pErrorPath, boolean pAllTargets)
      throws CPAException, InterruptedException {
    return isFeasible(pErrorPath, initialState, precision, pAllTargets);
  }

  private boolean isFeasible(
      ARGPath pPath,
      UnmodifiableSMGState pStartingPoint,
      SMGPrecision pPrecision,
      boolean pAllTargets)
      throws CPAException, InterruptedException {

    Preconditions.checkArgument(!pPath.getInnerEdges().isEmpty());
    ReachabilityResult result = isReachable(pPath, pStartingPoint, pPrecision);

    if (result.isReachable()) {
      return isTarget(
          result.getLastState(), result.getLastEdge(), pPath.getLastState(), pAllTargets);
    } else {
      return false;
    }
  }

  private static class ReachabilityResult {

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

    @SuppressWarnings("unused")
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

  @Override
  public boolean isFeasible(
      ARGPath pPath, UnmodifiableSMGState pStartingPoint, Deque<UnmodifiableSMGState> pCallstack)
      throws CPAException, InterruptedException {
    // TODO Implementation
    throw new UnsupportedOperationException("method not yet implemented");
  }
}
