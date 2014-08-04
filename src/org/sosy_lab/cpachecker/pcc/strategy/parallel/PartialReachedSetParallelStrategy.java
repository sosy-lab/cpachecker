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
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.pcc.strategy.PartialReachedSetStrategy;
import org.sosy_lab.cpachecker.util.AbstractStates;

@Options(prefix = "pcc.partial")
public class PartialReachedSetParallelStrategy extends PartialReachedSetStrategy {

  @Option(
      description = "If enabled, distributes checking of partial elements depending on actual checking costs, else uses the number of elements")
  private boolean enableLoadDistribution = false;

  public PartialReachedSetParallelStrategy(Configuration pConfig, LogManager pLogger,
      ShutdownNotifier pShutdownNotifier, PropertyCheckerCPA pCpa) throws InvalidConfigurationException {
    super(pConfig, pLogger, pShutdownNotifier, pCpa);
    pConfig.inject(this);
  }

  @Override
  public boolean checkCertificate(final ReachedSet pReachedSet) throws CPAException, InterruptedException {

    List<AbstractState> certificate = new ArrayList<>(savedReachedSetSize);
    Precision initialPrec = pReachedSet.getPrecision(pReachedSet.getFirstState());

    AtomicBoolean result = new AtomicBoolean(true);
    AtomicInteger nextElement = new AtomicInteger(0);
    Lock lock = new ReentrantLock();
    Semaphore waitForThreads = new Semaphore(0);

    PartialChecker[] transitiveClosureThreads = new PartialChecker[numThreads];
    for (int i = 0; i < transitiveClosureThreads.length; i++) {
      transitiveClosureThreads[i] =
          enableLoadDistribution ? new PartialChecker(nextElement, certificate, initialPrec, result, lock,
              waitForThreads) :
              new PartialChecker(i, certificate, initialPrec, result, lock, waitForThreads);
      transitiveClosureThreads[i].start();
    }

    try {
      waitForThreads.acquire(numThreads);

      if (!result.get()) {
        logger.log(Level.FINE, "Checking failed");
        return false;
      }

      // check initial element
      AbstractState initialState = pReachedSet.popFromWaitlist();
      assert (initialState == pReachedSet.getFirstState() && pReachedSet.size() == 1);

      try {
        stats.getStopTimer().start();
        if (!cpa.getStopOperator().stop(initialState,
            statesPerLocation.get(AbstractStates.extractLocation(initialState)), initialPrec)) {
          logger.log(Level.FINE, "Initial element not in partial reached set.");
          return false;
        }
      } catch (CPAException e) {
        logger.logException(Level.FINE, e, "Stop check failed for initial element.");
        return false;
      } finally {
        stats.getStopTimer().stop();
      }


      stats.getPropertyCheckingTimer().start();
      try {
        return cpa.getPropChecker().satisfiesProperty(certificate);
      } finally {
        stats.getPropertyCheckingTimer().stop();
      }
    } finally {
      for (Thread t : transitiveClosureThreads) {
        t.interrupt();
      }
    }
  }

  private class PartialChecker extends Thread {

    private final int startIndex;
    private final List<AbstractState> certificate;
    private final Precision initPrec;

    private final AtomicInteger indexProvider;
    private final AtomicBoolean result;
    private final Lock mutex;
    private final Semaphore coordination;

    public PartialChecker(final int pStartIndex, final List<AbstractState> pCertificate, final Precision pInitPrec,
        final AtomicBoolean pResult, final Lock pMutex, final Semaphore pCoordinate) {
      startIndex = pStartIndex;
      certificate = pCertificate;
      initPrec = pInitPrec;
      result = pResult;
      mutex = pMutex;
      coordination = pCoordinate;
      assert(!enableLoadDistribution);
      indexProvider = null;
    }

    public PartialChecker(final AtomicInteger pIndexProvider, final List<AbstractState> pCertificate, final Precision pInitPrec,
        final AtomicBoolean pResult, final Lock pMutex, final Semaphore pCoordinate) {
      assert(enableLoadDistribution);
      startIndex = 0;
      certificate = pCertificate;
      initPrec = pInitPrec;
      result = pResult;
      mutex = pMutex;
      coordination = pCoordinate;
      indexProvider = pIndexProvider;
    }

    @Override
    public void run() {
      List<AbstractState> currentStates = new ArrayList<>(savedReachedSetSize / numThreads);
      try {
        int index = 0;

        for (int i = enableLoadDistribution ? indexProvider.getAndIncrement() : startIndex; i < reachedSet.length
            && result.get(); i = enableLoadDistribution ? indexProvider.getAndIncrement() : i + numThreads) {
          shutdownNotifier.shutdownIfNecessary();
          currentStates.add(reachedSet[i]);

          while (index < currentStates.size() && result.get()) {
            shutdownNotifier.shutdownIfNecessary();

            for (AbstractState succ : cpa.getTransferRelation().getAbstractSuccessors(currentStates.get(index++),
                initPrec, null)) {
              if (!cpa.getStopOperator().stop(succ, statesPerLocation.get(AbstractStates.extractLocation(succ)),
                  initPrec)) {
                if (stopAddingAtReachedSetSize && savedReachedSetSize == certificate.size() + currentStates.size()) {
                  logger.log(Level.FINE, "Too many states recomputed");
                  abort();
                  return;
                }
                currentStates.add(succ);
              }

            }
          }
        }

        mutex.lock();
        try {
          certificate.addAll(currentStates);
        } finally {
          mutex.unlock();
        }
        coordination.release();

      } catch (CPATransferException e) {
        logger.logException(Level.FINE, e, "Computation of successors failed.");
        abort();
      } catch (CPAException e) {
        logger.logException(Level.FINE, e, "Stop check failed for successor.");
        abort();
      } catch (Exception e) {
        e.printStackTrace();
        abort();
      }
    }

    private void abort() {
      result.set(false);
      coordination.release(numThreads);
    }
  }
}
