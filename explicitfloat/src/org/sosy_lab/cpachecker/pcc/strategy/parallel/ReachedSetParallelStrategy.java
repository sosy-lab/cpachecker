/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.concurrency.Threads;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.pcc.strategy.ReachedSetStrategy;
import org.sosy_lab.cpachecker.util.AbstractStates;

@Options
public class ReachedSetParallelStrategy extends ReachedSetStrategy{

  public ReachedSetParallelStrategy(Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier,
      PropertyCheckerCPA pCpa) throws InvalidConfigurationException {
    super(pConfig, pLogger, pShutdownNotifier, pCpa);
  }

  @Override
  public boolean checkCertificate(final ReachedSet pReachedSet) throws CPAException, InterruptedException {
    /*also restrict stop to elements of same location as analysis does*/
    StopOperator stop = cpa.getStopOperator();
    Precision initialPrec = pReachedSet.getPrecision(pReachedSet.getFirstState());

    // check if initial element covered
    AbstractState initialState = pReachedSet.popFromWaitlist();
    assert (initialState == pReachedSet.getFirstState() && pReachedSet.size() == 1);

    try {
      stats.getStopTimer().start();
      if (!stop.stop(initialState, statesPerLocation.get(AbstractStates.extractLocation(initialState)), initialPrec)) {
        logger.log(Level.FINE, "Cannot check that initial element is covered by result.");
        return false;
      }
    } catch (CPAException e) {
      logger.logException(Level.FINE, e, "Stop check failed for initial element.");
      return false;
    } finally {
      stats.getStopTimer().stop();
    }

    // instantiate parallel threads and check if elements form transitive closure
    //TODO possibly instantiate (and start) threads earlier in constructor
    CheckingHelper[] helper = new CheckingHelper[numThreads-1];
    Thread[] helperThreads = new Thread[numThreads-1];
    int length = reachedSet.length/numThreads;

    for (int i = 0; i < helper.length; i++) {
      shutdownNotifier.shutdownIfNecessary();
      helper[i] = new CheckingHelper(i * length, length, initialPrec);
      helperThreads[i] = Threads.newThread(helper[i]);
      helperThreads[i].start();
    }

    Collection<? extends AbstractState> successors;
    for (int i = length * helper.length; i < reachedSet.length; i++) {

      shutdownNotifier.shutdownIfNecessary();
      stats.increaseIteration();

      try {
        stats.getTransferTimer().start();
        successors = cpa.getTransferRelation().getAbstractSuccessors(reachedSet[i], initialPrec, null);
        stats.getTransferTimer().stop();

        for (AbstractState succ : successors) {
          try {
            stats.getStopTimer().start();
            if (!stop.stop(succ, statesPerLocation.get(AbstractStates.extractLocation(succ)), initialPrec)) {
              logger.log(Level.FINE, "Cannot check that result is transitive closure.", "Successor ", succ,
                  "of element ", reachedSet[i], "not covered by result.");
              return false;
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

    for (int i = 0; i < helper.length; i++) {
      helperThreads[i].join();
      if (!helper[i].result) { return false; }
    }

    stats.getPropertyCheckingTimer().start();
    try {
      return cpa.getPropChecker().satisfiesProperty(Arrays.asList(reachedSet));
    } finally {
      stats.getPropertyCheckingTimer().stop();
    }
  }

  protected class CheckingHelper implements Runnable {

    private int start;
    private int numElem;
    private Precision initialPrec;
    private boolean result = true;


    protected CheckingHelper(int startIndex, int pNumElem, Precision pInitialPrec) {
      if (startIndex < 0 || pNumElem < 1) {
        throw new NumberFormatException(
            "Start index must be postive or 0 and number of elements to check must be positive.");
      }
      start = startIndex;
      numElem = pNumElem;
      initialPrec = pInitialPrec;
    }

    @Override
    public void run() {// TODO how to integrate shutdown notifier in here, unprotected access to stats
      // ExecutorService one possibility
      StopOperator stop = cpa.getStopOperator();
      Collection<? extends AbstractState> successors;
      for (int i=start;i<start+numElem;i++) {

        stats.increaseIteration();

        try {
          stats.getTransferTimer().start();
          successors = cpa.getTransferRelation().getAbstractSuccessors(reachedSet[i], initialPrec, null);
          stats.getTransferTimer().stop();

          for (AbstractState succ : successors) {
            try {
              stats.getStopTimer().start();
              if (!stop.stop(succ, statesPerLocation.get(AbstractStates.extractLocation(succ)), initialPrec)) {
                logger.log(Level.FINE, "Cannot check that result is transitive closure.", "Successor ", succ,
                    "of element ", reachedSet[i], "not covered by result.");
                result = false;
              }
            } finally {
              stats.getStopTimer().stop();
            }
          }
        } catch (CPATransferException | InterruptedException e) {// TODO how to deal with interrupted exception
          logger.logException(Level.FINE, e, "Computation of successors failed.");
          result = false;
        } catch (CPAException e) {
          logger.logException(Level.FINE, e, "Stop check failed for successor.");
          result = false;
        }
      }
    }
  }

}
