// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

@Options
public class ReachedSetStrategy extends SequentialReadStrategy {

  protected AbstractState[] reachedSet;
  protected Multimap<CFANode, AbstractState> statesPerLocation;
  protected PropertyCheckerCPA cpa;
  protected final ShutdownNotifier shutdownNotifier;

  public ReachedSetStrategy(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Path pProofFile,
      @Nullable PropertyCheckerCPA pCpa)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pProofFile);
    cpa = pCpa;
    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  public void constructInternalProofRepresentation(
      UnmodifiableReachedSet pReached, ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    reachedSet = new AbstractState[pReached.size()];
    pReached.asCollection().toArray(reachedSet);
    if (reachedSet.length > 0 && reachedSet[0] instanceof ARGState) {
      for (int i = 0; i < reachedSet.length; i++) {
        reachedSet[i] = ((ARGState) reachedSet[i]).getWrappedState();
      }
    }
    proofInfo.addInfoForStates(reachedSet);
    orderReachedSetByLocation(reachedSet);
  }

  @Override
  public boolean checkCertificate(final ReachedSet pReachedSet)
      throws CPAException, InterruptedException {

    /*also restrict stop to elements of same location as analysis does*/
    StopOperator stop = cpa.getStopOperator();
    Precision initialPrec = pReachedSet.getPrecision(pReachedSet.getFirstState());

    // check if initial element covered
    AbstractState initialState = pReachedSet.popFromWaitlist();
    assert (initialState == pReachedSet.getFirstState() && pReachedSet.size() == 1);

    try {
      stats.stopTimer.start();
      if (!stop.stop(
          initialState,
          statesPerLocation.get(AbstractStates.extractLocation(initialState)),
          initialPrec)) {
        logger.log(Level.FINE, "Cannot check that initial element is covered by result.");
        return false;
      }
    } catch (CPAException e) {
      logger.logException(Level.FINE, e, "Stop check failed for initial element.");
      return false;
    } finally {
      stats.stopTimer.stop();
    }

    // check if elements form transitive closure
    Collection<? extends AbstractState> successors;
    for (AbstractState state : reachedSet) {

      shutdownNotifier.shutdownIfNecessary();
      stats.countIterations++;

      try {
        stats.transferTimer.start();
        successors = cpa.getTransferRelation().getAbstractSuccessors(state, initialPrec);
        stats.transferTimer.stop();

        for (AbstractState succ : successors) {
          try {
            stats.stopTimer.start();
            if (!stop.stop(
                succ, statesPerLocation.get(AbstractStates.extractLocation(succ)), initialPrec)) {
              logger.log(
                  Level.FINE,
                  "Cannot check that result is transitive closure.",
                  "Successor ",
                  succ,
                  "of element ",
                  state,
                  "not covered by result.");
              return false;
            }
          } finally {
            stats.stopTimer.stop();
          }
        }
      } catch (CPATransferException e) {
        logger.logException(Level.FINE, e, "Computation of successors failed.");
        return false;
      } catch (CPAException e) {
        logger.logException(Level.FINE, e, "Stop check failed for successor.");
        return false;
      }
    }
    stats.propertyCheckingTimer.start();
    try {
      return cpa.getPropChecker().satisfiesProperty(Arrays.asList(reachedSet));
    } finally {
      stats.propertyCheckingTimer.stop();
    }
  }

  @Override
  protected Object getProofToWrite(
      UnmodifiableReachedSet pReached, ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    constructInternalProofRepresentation(pReached, pCpa);
    return reachedSet;
  }

  @Override
  protected void prepareForChecking(Object pReadProof) throws InvalidConfigurationException {
    try {
      stats.preparationTimer.start();
      if (!(pReadProof instanceof AbstractState[])) {
        throw new InvalidConfigurationException(
            "Proof Type requires reached set as set of abstract states.");
      }
      reachedSet = (AbstractState[]) pReadProof;
      stats.increaseProofSize(reachedSet.length);
      orderReachedSetByLocation(reachedSet);
    } finally {
      stats.preparationTimer.stop();
    }
  }

  protected void orderReachedSetByLocation(AbstractState[] pReached) {
    statesPerLocation = HashMultimap.create();
    for (AbstractState state : pReached) {
      statesPerLocation.put(AbstractStates.extractLocation(state), state);
    }
  }
}
