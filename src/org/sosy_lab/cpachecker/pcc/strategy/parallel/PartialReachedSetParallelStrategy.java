// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy.parallel;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
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
      secure = true,
      description =
          "If enabled, distributes checking of partial elements depending on actual checking costs,"
              + " else uses the number of elements")
  private boolean enableLoadDistribution = false;

  public PartialReachedSetParallelStrategy(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      final Path pProofFile,
      @Nullable PropertyCheckerCPA pCpa)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pShutdownNotifier, pProofFile, pCpa);
    pConfig.inject(this);
  }

  @Override
  public boolean checkCertificate(final ReachedSet pReachedSet)
      throws CPAException, InterruptedException {

    List<AbstractState> certificate = new ArrayList<>(savedReachedSetSize);
    Precision initialPrec = pReachedSet.getPrecision(pReachedSet.getFirstState());

    AtomicBoolean result = new AtomicBoolean(true);
    AtomicInteger nextElement = new AtomicInteger(0);
    Lock lock = new ReentrantLock();
    Semaphore waitForThreads = new Semaphore(0);

    PartialChecker[] transitiveClosureThreads = new PartialChecker[numThreads];
    for (int i = 0; i < transitiveClosureThreads.length; i++) {
      transitiveClosureThreads[i] =
          enableLoadDistribution
              ? new PartialChecker(
                  nextElement, certificate, initialPrec, result, lock, waitForThreads)
              : new PartialChecker(i, certificate, initialPrec, result, lock, waitForThreads);
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
        if (!cpa.getStopOperator()
            .stop(
                initialState,
                statesPerLocation.get(AbstractStates.extractLocation(initialState)),
                initialPrec)) {
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

    public PartialChecker(
        final int pStartIndex,
        final List<AbstractState> pCertificate,
        final Precision pInitPrec,
        final AtomicBoolean pResult,
        final Lock pMutex,
        final Semaphore pCoordinate) {
      startIndex = pStartIndex;
      certificate = pCertificate;
      initPrec = pInitPrec;
      result = pResult;
      mutex = pMutex;
      coordination = pCoordinate;
      assert !enableLoadDistribution;
      indexProvider = null;
    }

    public PartialChecker(
        final AtomicInteger pIndexProvider,
        final List<AbstractState> pCertificate,
        final Precision pInitPrec,
        final AtomicBoolean pResult,
        final Lock pMutex,
        final Semaphore pCoordinate) {
      assert enableLoadDistribution;
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

        for (int i = enableLoadDistribution ? indexProvider.getAndIncrement() : startIndex;
            i < reachedSet.length && result.get();
            i = enableLoadDistribution ? indexProvider.getAndIncrement() : i + numThreads) {
          shutdownNotifier.shutdownIfNecessary();
          currentStates.add(reachedSet[i]);

          while (index < currentStates.size() && result.get()) {
            shutdownNotifier.shutdownIfNecessary();

            for (AbstractState succ :
                cpa.getTransferRelation()
                    .getAbstractSuccessors(currentStates.get(index++), initPrec)) {
              if (!cpa.getStopOperator()
                  .stop(
                      succ,
                      statesPerLocation.get(AbstractStates.extractLocation(succ)),
                      initPrec)) {
                if (stopAddingAtReachedSetSize
                    && savedReachedSetSize <= certificate.size() + currentStates.size()) {
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
        logger.logUserException(Level.FINE, e, "Computation of successors failed.");
        abort();
      } catch (CPAException e) {
        logger.logUserException(Level.FINE, e, "Stop check failed for successor.");
        abort();
      } catch (Exception e) {
        logger.logException(Level.WARNING, e, "Unknown problem");
        abort();
      }
    }

    private void abort() {
      result.set(false);
      coordination.release(numThreads);
    }
  }
}
