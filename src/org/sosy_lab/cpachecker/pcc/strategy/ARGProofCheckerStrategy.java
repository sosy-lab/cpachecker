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
package org.sosy_lab.cpachecker.pcc.strategy;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PropertyChecker;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.pcc.propertychecker.NoTargetStateChecker;

@Options
public class ARGProofCheckerStrategy extends AbstractStrategy {

  private ARGState root;
  private ProofChecker checker;
  private PropertyChecker propChecker;
  private final ShutdownNotifier shutdownNotifier;

  public ARGProofCheckerStrategy(Configuration pConfig, LogManager pLogger,
      ShutdownNotifier pShutdownNotifier, ProofChecker pChecker)
      throws InvalidConfigurationException {
    super(pConfig, pLogger);
    checker = pChecker;
    propChecker = new NoTargetStateChecker();
    if (pChecker instanceof PropertyCheckerCPA) {
      propChecker = ((PropertyCheckerCPA) pChecker).getPropChecker();
    }
    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  public void constructInternalProofRepresentation(UnmodifiableReachedSet pReached) {
    if (correctReachedSetFormatForProof(pReached)) {
      root = (ARGState) pReached.getFirstState();
    }
  }

  @Override
  public boolean checkCertificate(final ReachedSet pReachedSet) throws CPAException, InterruptedException {
    //TODO does not account for strengthen yet (proof check will fail if strengthen is needed to explain successor states)

    logger.log(Level.INFO, "Proof check algorithm started");

    AbstractState initialState = pReachedSet.popFromWaitlist();
    Precision initialPrecision = pReachedSet.getPrecision(initialState);

    logger.log(Level.FINE, "Checking root state");

    if (!(checker.isCoveredBy(initialState, root) && checker.isCoveredBy(root, initialState))) {
      logger.log(Level.WARNING, "Root state of proof is invalid.");
      return false;
    }

    pReachedSet.add(root, initialPrecision);

    Set<ARGState> postponedStates = new HashSet<>();

    Set<ARGState> waitingForUnexploredParents = new HashSet<>();
    Set<ARGState> inWaitlist = new HashSet<>();
    inWaitlist.add(root);

    boolean unexploredParent;

    do {
      for (ARGState e : postponedStates) {
        if (!pReachedSet.contains(e.getCoveringState())) {
          logger.log(Level.WARNING, "Covering state", e.getCoveringState(), "was not found in reached set");
          return false;
        }
        pReachedSet.reAddToWaitlist(e);
      }
      postponedStates.clear();

      while (pReachedSet.hasWaitingState()) {
        shutdownNotifier.shutdownIfNecessary();

        stats.countIterations++;
        ARGState state = (ARGState) pReachedSet.popFromWaitlist();
        inWaitlist.remove(state);

        logger.log(Level.FINE, "Looking at state", state);

        stats.propertyCheckingTimer.start();
        if (!propChecker.satisfiesProperty(state)) {
          stats.propertyCheckingTimer.stop();
          return false;
        }
        stats.propertyCheckingTimer.stop();

        if (state.isCovered()) {

          logger.log(Level.FINER, "State is covered by another abstract state; checking coverage");
          ARGState coveringState = state.getCoveringState();

          if (!pReachedSet.contains(coveringState)) {
            postponedStates.add(state);
            continue;
          }

          stats.stopTimer.start();
          if (!isCoveringCycleFree(state)) {
            stats.stopTimer.stop();
            logger.log(Level.WARNING, "Found cycle in covering relation for state", state);
            return false;
          }
          if (!checker.isCoveredBy(state, coveringState)) {
            stats.stopTimer.stop();
            logger.log(Level.WARNING, "State", state, "is not covered by", coveringState);
            return false;
          }
          stats.stopTimer.stop();
        } else {
          stats.transferTimer.start();
          Collection<ARGState> successors = state.getChildren();
          logger.log(Level.FINER, "Checking abstract successors", successors);
          if (!checker.areAbstractSuccessors(state, null, successors)) {
            stats.transferTimer.stop();
            logger.log(Level.WARNING, "State", state, "has other successors than", successors);
            return false;
          }
          stats.transferTimer.stop();
          for (ARGState e : successors) {
            unexploredParent = false;
            for (ARGState p : e.getParents()) {
              if (!pReachedSet.contains(p) || inWaitlist.contains(p)) {
                waitingForUnexploredParents.add(e);
                unexploredParent = true;
                break;
              }
            }
            if (unexploredParent) {
              continue;
            }
            if (pReachedSet.contains(e)) {
              // state unknown parent of e
              logger.log(Level.WARNING, "State", e, "has other parents than", e.getParents());
              return false;
            } else {
              waitingForUnexploredParents.remove(e);
              pReachedSet.add(e, initialPrecision);
              inWaitlist.add(e);
            }
          }
        }
      }
    } while (!postponedStates.isEmpty());

    return waitingForUnexploredParents.isEmpty();
  }

  private boolean isCoveringCycleFree(ARGState pState) {
    HashSet<ARGState> seen = new HashSet<>();
    seen.add(pState);
    while (pState.isCovered()) {
      pState = pState.getCoveringState();
      boolean isNew = seen.add(pState);
      if (!isNew) { return false; }
    }
    return true;
  }

  private boolean correctReachedSetFormatForProof(UnmodifiableReachedSet pReached) {
    if (pReached.getFirstState() == null
        || !(pReached.getFirstState() instanceof ARGState)
        || (extractLocation(pReached.getFirstState()) == null)) {
      logger.log(Level.SEVERE, "Proof cannot be generated because checked property not known to be true.");
      return false;
    }
    return true;
  }

  @Override
  protected Object getProofToWrite(UnmodifiableReachedSet pReached) {
    constructInternalProofRepresentation(pReached);
    return root;
  }

  @Override
  protected void prepareForChecking(Object pReadProof) throws InvalidConfigurationException {
    try {
      stats.preparationTimer.start();
    if (!(pReadProof instanceof ARGState)) {
      throw new InvalidConfigurationException("Proof Strategy requires ARG.");
    }
    root = (ARGState) pReadProof;
    } finally {
      stats.preparationTimer.stop();
    }
  }

}
