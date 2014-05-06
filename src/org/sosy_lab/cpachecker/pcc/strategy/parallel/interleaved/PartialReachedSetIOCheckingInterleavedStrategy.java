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
package org.sosy_lab.cpachecker.pcc.strategy.parallel.interleaved;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.zip.ZipInputStream;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.pcc.strategy.AbstractStrategy;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.PartialReachedSetDirectedGraph;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.PartitionChecker;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.PartitioningIOHelper;


public class PartialReachedSetIOCheckingInterleavedStrategy extends AbstractStrategy {

  private final PartitioningIOHelper ioHelper;
  private final PropertyCheckerCPA cpa;
  private final ShutdownNotifier shutdownNotifier;
  private final Lock lock = new ReentrantLock();
  private final Condition partitionReady = lock.newCondition();

  public PartialReachedSetIOCheckingInterleavedStrategy(final Configuration pConfig, final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier, final PropertyCheckerCPA pCpa)
      throws InvalidConfigurationException {
    super(pConfig, pLogger);
    ioHelper = new PartitioningIOHelper(pConfig, pLogger, pShutdownNotifier, pCpa);
    cpa = pCpa;
    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  public void constructInternalProofRepresentation(final UnmodifiableReachedSet pReached)
      throws InvalidConfigurationException {
    throw new InvalidConfigurationException(
        "Interleaved proof reading and checking strategies do not  support internal PCC with result check algorithm");
  }

  @Override
  public boolean checkCertificate(final ReachedSet pReachedSet) throws CPAException, InterruptedException {
    AtomicBoolean checkResult = new AtomicBoolean(true);
    Semaphore partitionChecked = new Semaphore(0);
    Collection<AbstractState> certificate = new HashSet<>(ioHelper.getSavedReachedSetSize());
    Collection<AbstractState> inOtherPartition = new ArrayList<>();
    Precision initPrec = pReachedSet.getPrecision(pReachedSet.getFirstState());

    logger.log(Level.INFO, "Create and start threads");
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    try {
      executor.execute(new PartitionReader(checkResult, partitionChecked));
      for (int i = 0; i < ioHelper.getNumPartitions(); i++) {
        executor.execute(new PartitionChecker(i, checkResult, partitionChecked, certificate, inOtherPartition,
            initPrec,
            cpa, lock, partitionReady, ioHelper, shutdownNotifier, logger));
      }

      partitionChecked.acquire(ioHelper.getNumPartitions());

      if (!checkResult.get()) { return false; }

      logger.log(Level.INFO, "Check if all are checked");
      if (!certificate.containsAll(inOtherPartition)) {
        logger.log(Level.SEVERE, "Initial state not covered.");
        return false;
      }

      logger.log(Level.INFO, "Check if initial state is covered.");
      // TODO probably more efficient do not use certificate?
      if (!cpa.getStopOperator().stop(pReachedSet.getFirstState(), certificate, initPrec)) {
        logger.log(Level.SEVERE, "Initial state not covered.");
        return false;
      }

      logger.log(Level.INFO, "Check property.");
      stats.getPropertyCheckingTimer().start();
      try {
        if (!cpa.getPropChecker().satisfiesProperty(certificate)) {
          logger.log(Level.SEVERE, "Property violated");
          return false;
        }
      } finally {
        stats.getPropertyCheckingTimer().stop();
      }

      return true;
    } finally {
      executor.shutdown();
    }
  }

  @Override
  protected void writeProofToStream(final ObjectOutputStream pOut, final UnmodifiableReachedSet pReached)
      throws IOException,
      InvalidConfigurationException {
    Pair<PartialReachedSetDirectedGraph, List<Set<Integer>>> partitioning;
    try {
      partitioning = ioHelper.computePartialReachedSetAndPartition(pReached);
    } catch (InterruptedException e) {
      throw new IOException("Write preparation took too long", e);
    }
    ioHelper.writeMetadata(pOut, pReached.size(), partitioning.getSecond().size());
    for (Set<Integer> partition : partitioning.getSecond()) {
      ioHelper.writePartition(pOut, partition, partitioning.getFirst());
    }
  }

  @Override
  protected void readProofFromStream(final ObjectInputStream pIn) throws ClassNotFoundException,
      InvalidConfigurationException, IOException {
    ioHelper.readMetadata(pIn, true);
  }

  private void giveSignal() {
    lock.lock();
    try {
      partitionReady.signalAll();
    } finally {
      lock.unlock();
    }
  }

  private void giveSignalAndPrepareAbortion(final AtomicBoolean pValue, final Semaphore pForRelease) {
    pValue.set(false);
    giveSignal();
    pForRelease.release(ioHelper.getNumPartitions());
  }

  private class PartitionReader implements Runnable {

    private final AtomicBoolean checkResult;
    private final Semaphore mainSemaphore;

    public PartitionReader(final AtomicBoolean pCheckResult, final Semaphore pPartitionChecked) {
      checkResult = pCheckResult;
      mainSemaphore = pPartitionChecked;
    }

    @Override
    public void run() {
      Triple<InputStream, ZipInputStream, ObjectInputStream> streams = null;
      try {
        streams = openProofStream();
        ObjectInputStream o = streams.getThird();
        ioHelper.readMetadata(o, false);
        for (int i = 0; i < ioHelper.getNumPartitions() && !checkResult.get(); i++) {
          ioHelper.readPartition(o);
          if (shutdownNotifier.shouldShutdown()) {
            abortPreparation();
            break;
          }
          giveSignal();
        }
      } catch (IOException | ClassNotFoundException e) {
        logger.log(Level.SEVERE, "Partition reading failed. Stop checking");
        abortPreparation();
      }finally{
        if (streams != null) {
          try {
            streams.getThird().close();
            streams.getSecond().close();
            streams.getFirst().close();
          } catch (IOException e) {
          }
        }
      }
    }

    private void abortPreparation(){
      giveSignalAndPrepareAbortion(checkResult, mainSemaphore);
    }

  }

}
