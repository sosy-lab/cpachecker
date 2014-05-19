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
package org.sosy_lab.cpachecker.pcc.strategy.parallel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.pcc.strategy.PartialReachedSetStrategy;
import org.sosy_lab.cpachecker.util.AbstractStates;

@Options
public class PartialReachedSetParallelStrategy extends PartialReachedSetStrategy {

  public PartialReachedSetParallelStrategy(Configuration pConfig, LogManager pLogger,
      ShutdownNotifier pShutdownNotifier, PropertyCheckerCPA pCpa) throws InvalidConfigurationException {
    super(pConfig, pLogger, pShutdownNotifier, pCpa);
  }

  @Override
  public boolean checkCertificate(final ReachedSet pReachedSet) throws CPAException, InterruptedException {
    /* TODO parallelize, currently non-parallelized version,
     *  consider how to integrate shutdown notifier in here and
     *  avoid unprotected access to stats*/
 // TODO for parallelization use Runtime.getRuntime().availableProcessors(); to identify maximal number of parallel threads
    int certificateSize = 0;

    List<AbstractState> certificate = new ArrayList<>(savedReachedSetSize);
    for (AbstractState elem : reachedSet) {
      certificate.add(elem);
    }

    /*also restrict stop to elements of same location as analysis does*/
    StopOperator stop = cpa.getStopOperator();
    Precision initialPrec = pReachedSet.getPrecision(pReachedSet.getFirstState());

    // check initial element
    AbstractState initialState = pReachedSet.popFromWaitlist();
    assert (initialState == pReachedSet.getFirstState() && pReachedSet.size() == 1);

    try {
      stats.getStopTimer().start();
      if (!stop.stop(initialState, statesPerLocation.get(AbstractStates.extractLocation(initialState)), initialPrec)) {
        logger.log(Level.FINE, "Initial element not in partial reached set.", "Add to elements whose successors ",
            "must be computed.");
        certificate.add(initialState);
      }
    } catch (CPAException e) {
      logger.logException(Level.FINE, e, "Stop check failed for initial element.");
      return false;
    } finally {
      stats.getStopTimer().stop();
    }

    // check if elements form transitive closure
    Collection<? extends AbstractState> successors;
    while (certificateSize<certificate.size()) {

      shutdownNotifier.shutdownIfNecessary();
      stats.increaseIteration();

      try {
        stats.getTransferTimer().start();
        successors =
            cpa.getTransferRelation().getAbstractSuccessors(certificate.get(certificateSize++), initialPrec,
                null);
        stats.getTransferTimer().stop();

        for (AbstractState succ : successors) {
          try {
            stats.getStopTimer().start();
            if (!stop.stop(succ, statesPerLocation.get(AbstractStates.extractLocation(succ)), initialPrec)) {
              logger.log(Level.FINE, "Successor ", succ, " not in partial reached set.",
                  "Add to elements whose successors ", "must be computed.");
              if (stopAddingAtReachedSetSize && savedReachedSetSize == certificate.size()) { return false; }
              certificate.add(succ);
            }
          } finally {
            stats.getStopTimer().stop();
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
    stats.getPropertyCheckingTimer().start();
    try {
      return cpa.getPropChecker().satisfiesProperty(certificate);
    } finally {
      stats.getPropertyCheckingTimer().stop();
    }
  }
}
