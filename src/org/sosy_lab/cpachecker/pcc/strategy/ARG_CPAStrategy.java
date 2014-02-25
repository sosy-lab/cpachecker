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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options
public class ARG_CPAStrategy extends AbstractStrategy {

  @Option(name = "pcc.checkPropertyPerElement", description = "")
  private boolean singleCheck = false;
  private List<AbstractState> visitedStates;
  private ARGState root;
  private PropertyCheckerCPA cpa;
  private final ShutdownNotifier shutdownNotifier;

  public ARG_CPAStrategy(Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier,
      PropertyCheckerCPA pCpa) throws InvalidConfigurationException {
    super(pConfig, pLogger);
    pConfig.inject(this);
    cpa = pCpa;
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
    StopOperator stop = cpa.getWrappedCPAs().get(0).getStopOperator();
    if (!singleCheck) {
      visitedStates = new Vector<>();
    }

    logger.log(Level.INFO, "Proof check algorithm started");

    ARGState initialState = (ARGState) pReachedSet.popFromWaitlist();
    Precision initialPrecision = pReachedSet.getPrecision(initialState);

    logger.log(Level.FINE, "Checking root state");

    if (!stop
        .stop(initialState.getWrappedState(), Collections.singleton(root.getWrappedState()), initialPrecision)) {
      logger.log(Level.WARNING, "Root state of proof is invalid.");
      return false;
    }

    pReachedSet.add(root, initialPrecision);

    while (pReachedSet.hasWaitingState()) {
      shutdownNotifier.shutdownIfNecessary();

      stats.countIterations++;
      ARGState state = (ARGState) pReachedSet.popFromWaitlist();
      if (!singleCheck) {
        visitedStates.add(root);
      } else {
        cpa.getPropChecker().satisfiesProperty(state);
      }

      logger.log(Level.FINE, "Looking at state", state);


      if (state.isCovered()) {

        logger.log(Level.FINER, "State is covered by another abstract state; checking coverage");
        ARGState coveringState = state.getCoveringState();

        if (!pReachedSet.contains(coveringState)) {
          pReachedSet.add(coveringState, initialPrecision);
        }

        stats.stopTimer.start();
        if (!isCoveringCycleFree(state)) {
          stats.stopTimer.stop();
          logger.log(Level.WARNING, "Found cycle in covering relation for state", state);
          return false;
        }
        if (!stop.stop(state.getWrappedState(), Collections.singleton(coveringState.getWrappedState()),
            initialPrecision)) {
          stats.stopTimer.stop();
          logger.log(Level.WARNING, "State", state, "is not covered by", coveringState);
          return false;
        }
        stats.stopTimer.stop();
      } else {
        stats.transferTimer.start();
        ArrayList<AbstractState> successors = new ArrayList<>(state.getChildren().size());
        for (ARGState argS : state.getChildren()) {
          successors.add(argS.getWrappedState());
          pReachedSet.add(argS, initialPrecision);
        }
        Collection<? extends AbstractState> computedSuccessors =
            cpa.getWrappedCPAs().get(0).getTransferRelation()
                .getAbstractSuccessors(state.getWrappedState(), initialPrecision, null);
        logger.log(Level.FINER, "Checking abstract successors", successors);
        for (AbstractState succ : computedSuccessors) {
          if (!stop.stop(succ, successors, initialPrecision)) {
            stats.transferTimer.stop();
            logger.log(Level.WARNING, "State", state, "has other successors than", successors);
            return false;
          }
        }
        stats.transferTimer.stop();
      }
    }
    if (!singleCheck) {
      return cpa.getPropChecker().satisfiesProperty(visitedStates);
    }
    return true;
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
