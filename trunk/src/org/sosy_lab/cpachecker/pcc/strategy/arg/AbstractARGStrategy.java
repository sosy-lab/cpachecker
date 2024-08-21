// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy.arg;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.base.Preconditions;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PropertyChecker;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.pcc.strategy.SequentialReadStrategy;

public abstract class AbstractARGStrategy extends SequentialReadStrategy {

  private ARGState root;
  protected final PropertyChecker propChecker;
  private final ShutdownNotifier shutdownNotifier;

  protected AbstractARGStrategy(
      Configuration pConfig,
      LogManager pLogger,
      PropertyChecker pPropertyChecker,
      ShutdownNotifier pShutdownNotifier,
      Path pProofFile)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pProofFile);
    propChecker = pPropertyChecker;
    shutdownNotifier = pShutdownNotifier;
  }

  public ARGState getARG() {
    Preconditions.checkNotNull(root);
    return root;
  }

  @Override
  public void constructInternalProofRepresentation(
      UnmodifiableReachedSet pReached, ConfigurableProgramAnalysis pCpa) {
    if (correctReachedSetFormatForProof(pReached)) {
      root = (ARGState) pReached.getFirstState();
      stats.increaseProofSize(1);
    }
  }

  private boolean correctReachedSetFormatForProof(UnmodifiableReachedSet pReached) {
    if (!(pReached.getFirstState() instanceof ARGState)
        || (extractLocation(pReached.getFirstState()) == null)) {
      logger.log(
          Level.SEVERE, "Proof cannot be generated because checked property not known to be true.");
      return false;
    }
    return true;
  }

  @Override
  public boolean checkCertificate(ReachedSet pReachedSet)
      throws CPAException, InterruptedException {
    return checkCertificate(pReachedSet, root, null);
  }

  protected boolean checkCertificate(
      ReachedSet pReachedSet, ARGState pRoot, @Nullable List<ARGState> incompleteStates)
      throws CPAException, InterruptedException {
    // TODO does not account for strengthen yet (proof check will fail if strengthen is needed to
    // explain successor states)
    initChecking(pRoot);

    logger.log(Level.INFO, "Proof check algorithm started");

    ARGState initialState = (ARGState) pReachedSet.popFromWaitlist();
    Precision initialPrecision = pReachedSet.getPrecision(initialState);

    logger.log(Level.FINE, "Checking root state");

    if (!checkCovering(initialState, pRoot, initialPrecision)) {
      return false;
    }

    pReachedSet.add(pRoot, initialPrecision);

    do {

      if (!prepareNextWaitlistIteration(pReachedSet)) {
        return false;
      }

      while (pReachedSet.hasWaitingState()) {
        shutdownNotifier.shutdownIfNecessary();

        stats.increaseIteration();
        ARGState state = (ARGState) pReachedSet.popFromWaitlist();

        logger.log(Level.FINE, "Looking at state", state);

        if (!checkForStatePropertyAndOtherStateActions(state)) {
          if (incompleteStates != null) {
            incompleteStates.add(state);
            continue;
          }
          logger.log(Level.INFO, "Property violation at state", state);
          return false;
        }

        if (state.isCovered()) {
          if (!checkCoveredStates(state, pReachedSet, initialPrecision)) {
            return false;
          }
        } else {
          if (!checkAndAddSuccessors(state, pReachedSet, initialPrecision, incompleteStates)) {
            return false;
          }
        }
      }
    } while (!isCheckComplete());

    stats.increaseProofSize(pReachedSet.size() - 1);
    return isCheckSuccessful();
  }

  private boolean checkCoveredStates(
      final ARGState pCovered, final ReachedSet pReachedSet, final Precision pPrecision)
      throws CPAException, InterruptedException {
    logger.log(Level.FINER, "State is covered by another abstract state; checking coverage");
    ARGState coveringState = pCovered.getCoveringState();

    if (!pReachedSet.contains(coveringState)) {
      if (treatStateIfCoveredByUnkownState(pCovered, coveringState, pReachedSet, pPrecision)) {
        return true;
      }
    }

    stats.getStopTimer().start();
    if (!isCoveringCycleFree(pCovered)) {
      stats.getStopTimer().stop();
      logger.log(Level.WARNING, "Found cycle in covering relation for state", pCovered);
      return false;
    }
    if (!checkCovering(pCovered, coveringState, pPrecision)) {
      stats.getStopTimer().stop();
      logger.log(Level.WARNING, "State", pCovered, "is not covered by", coveringState);
      return false;
    }
    stats.getStopTimer().stop();
    return true;
  }

  private boolean checkAndAddSuccessors(
      final ARGState pPredecessor,
      final ReachedSet pReachedSet,
      final Precision pPrecision,
      @Nullable List<ARGState> pIncompleteStates)
      throws InterruptedException, CPAException {
    stats.getTransferTimer().start();
    Collection<ARGState> successors = pPredecessor.getChildren();
    logger.log(Level.FINER, "Checking abstract successors", successors);
    if (!checkSuccessors(pPredecessor, successors, pPrecision)) {
      stats.getTransferTimer().stop();
      if (pIncompleteStates != null) {
        pIncompleteStates.add(pPredecessor);
        logger.log(
            Level.FINER,
            "State",
            pPredecessor,
            "is explored incompletely, will be recorded in the assumption automaton.");
        return true;
      }
      logger.log(Level.WARNING, "State", pPredecessor, "has other successors than", successors);
      return false;
    }
    stats.getTransferTimer().stop();

    if (!addSuccessors(successors, pReachedSet, pPrecision)) {
      return false;
    }
    return true;
  }

  @Override
  protected Object getProofToWrite(
      UnmodifiableReachedSet pReached, ConfigurableProgramAnalysis pCpa) {
    constructInternalProofRepresentation(pReached, pCpa);
    return root;
  }

  @Override
  protected void prepareForChecking(Object pReadProof) throws InvalidConfigurationException {
    try {
      stats.getPreparationTimer().start();
      if (!(pReadProof instanceof ARGState)) {
        throw new InvalidConfigurationException("Proof Strategy requires ARG.");
      }
      root = (ARGState) pReadProof;
    } finally {
      stats.getPreparationTimer().stop();
    }
  }

  private boolean isCoveringCycleFree(ARGState pState) {
    Set<ARGState> seen = new HashSet<>();
    seen.add(pState);
    while (pState.isCovered()) {
      pState = pState.getCoveringState();
      boolean isNew = seen.add(pState);
      if (!isNew) {
        return false;
      }
    }
    return true;
  }

  protected boolean checkForStatePropertyAndOtherStateActions(ARGState pState) {
    try {
      stats.getPropertyCheckingTimer().start();
      return propChecker.satisfiesProperty(pState);
    } finally {
      stats.getPropertyCheckingTimer().stop();
    }
  }

  protected abstract void initChecking(ARGState pRoot);

  protected abstract boolean prepareNextWaitlistIteration(ReachedSet pReachedSet);

  protected abstract boolean treatStateIfCoveredByUnkownState(
      ARGState pCovered, ARGState pCoveringState, ReachedSet pReachedSet, Precision pPrecision);

  protected abstract boolean checkCovering(
      ARGState pCovered, ARGState pCovering, Precision pPrecision)
      throws CPAException, InterruptedException;

  protected abstract boolean checkSuccessors(
      ARGState pPredecessor, Collection<ARGState> pSuccessors, Precision pPrecision)
      throws InterruptedException, CPAException;

  protected abstract boolean addSuccessors(
      Collection<ARGState> pSuccessors, ReachedSet pReachedSet, Precision pPrecision);

  protected abstract boolean isCheckSuccessful();

  protected abstract boolean isCheckComplete();
}
