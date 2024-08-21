// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy.parallel;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
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
public class ReachedSetParallelStrategy extends ReachedSetStrategy {

  public ReachedSetParallelStrategy(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Path pProofFile,
      @Nullable PropertyCheckerCPA pCpa)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pShutdownNotifier, pProofFile, pCpa);
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
    }

    // instantiate parallel threads and check if elements form transitive closure
    // TODO possibly instantiate (and start) threads earlier in constructor
    CheckingHelper[] helper = new CheckingHelper[numThreads - 1];
    Thread[] helperThreads = new Thread[numThreads - 1];
    int length = reachedSet.length / numThreads;

    ThreadFactory threadFactory =
        new ThreadFactoryBuilder()
            .setNameFormat("ReachedSetParallelStrategy-checkCertificate-%d")
            .build();
    for (int i = 0; i < helper.length; i++) {
      shutdownNotifier.shutdownIfNecessary();
      helper[i] = new CheckingHelper(i * length, length, initialPrec);
      helperThreads[i] = threadFactory.newThread(helper[i]);
      helperThreads[i].start();
    }

    Collection<? extends AbstractState> successors;
    for (int i = length * helper.length; i < reachedSet.length; i++) {

      shutdownNotifier.shutdownIfNecessary();
      stats.increaseIteration();

      try {
        successors = cpa.getTransferRelation().getAbstractSuccessors(reachedSet[i], initialPrec);

        for (AbstractState succ : successors) {
          if (!stop.stop(
              succ, statesPerLocation.get(AbstractStates.extractLocation(succ)), initialPrec)) {
            logger.log(
                Level.FINE,
                "Cannot check that result is transitive closure.",
                "Successor ",
                succ,
                "of element ",
                reachedSet[i],
                "not covered by result.");
            return false;
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
      if (!helper[i].result) {
        return false;
      }
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
    public void
        run() { // TODO how to integrate shutdown notifier in here, unprotected access to stats
      // ExecutorService one possibility
      StopOperator stop = cpa.getStopOperator();
      Collection<? extends AbstractState> successors;
      for (int i = start; i < start + numElem; i++) {

        try {
          successors = cpa.getTransferRelation().getAbstractSuccessors(reachedSet[i], initialPrec);

          for (AbstractState succ : successors) {

            if (!stop.stop(
                succ, statesPerLocation.get(AbstractStates.extractLocation(succ)), initialPrec)) {
              logger.log(
                  Level.FINE,
                  "Cannot check that result is transitive closure.",
                  "Successor ",
                  succ,
                  "of element ",
                  reachedSet[i],
                  "not covered by result.");
              result = false;
            }
          }
        } catch (CPATransferException
            | InterruptedException e) { // TODO how to deal with interrupted exception
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
