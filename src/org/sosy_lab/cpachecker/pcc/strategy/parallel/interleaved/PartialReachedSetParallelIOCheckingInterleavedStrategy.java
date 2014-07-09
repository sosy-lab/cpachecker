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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.pcc.strategy.AbstractStrategy;
import org.sosy_lab.cpachecker.pcc.strategy.parallel.io.ParallelPartitionReader;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.PartitionChecker;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.PartitioningIOHelper;
import org.sosy_lab.cpachecker.util.AbstractStates;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

@Options(prefix = "pcc.interleaved")
public class PartialReachedSetParallelIOCheckingInterleavedStrategy extends AbstractStrategy {

  @Option(
      name = "useReadCores",
      description = "The number of cores used exclusively for proof reading. Must be less than pcc.useCores and may not be negative. Value 0 means that the cores used for reading and checking are shared")
  private int numReadThreads = 0;

  private int nextPartition;
  private final PartitioningIOHelper ioHelper;
  private final ShutdownNotifier shutdown;
  private final PropertyCheckerCPA cpa;

  public PartialReachedSetParallelIOCheckingInterleavedStrategy(final Configuration pConfig, final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier, final PropertyCheckerCPA pCpa)
      throws InvalidConfigurationException {
    super(pConfig, pLogger);
    pConfig.inject(this);

    shutdown = pShutdownNotifier;
    cpa = pCpa;

    ioHelper = new PartitioningIOHelper(pConfig, pLogger, pShutdownNotifier, pCpa);
    numReadThreads = Math.min(numReadThreads, numThreads - 1);
    numReadThreads = Math.max(0, numReadThreads);
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
    Collection<AbstractState> certificate = new HashSet<>(ioHelper.getNumPartitions());
    Multimap<CFANode, AbstractState> partitionNodes = HashMultimap.create();
    Collection<AbstractState> inOtherPartition = new ArrayList<>();
    AbstractState initialState = pReachedSet.popFromWaitlist();
    Precision initPrec = pReachedSet.getPrecision(initialState);
    Lock lock = new ReentrantLock();
    Condition partitionReady = lock.newCondition();

    ExecutorService executor = null, readExecutor = null, checkExecutor = null;
    logger.log(Level.INFO, "Create and start threads");
    try {
      if (numReadThreads == 0) {
        executor = Executors.newFixedThreadPool(numThreads);
        startReadingThreads(executor, checkResult, partitionChecked, lock, partitionReady);
        startCheckingThreads(executor, checkResult, partitionChecked, certificate, partitionNodes, inOtherPartition,
            initPrec, lock, partitionReady);
      } else {
        readExecutor = Executors.newFixedThreadPool(numReadThreads);
        startReadingThreads(readExecutor, checkResult, partitionChecked, lock, partitionReady);
        checkExecutor = Executors.newFixedThreadPool(numThreads - numReadThreads);
        startCheckingThreads(checkExecutor, checkResult, partitionChecked, certificate, partitionNodes, inOtherPartition,
            initPrec, lock, partitionReady);
      }

      partitionChecked.acquire(ioHelper.getNumPartitions());

      if (!checkResult.get()) { return false; }

      logger.log(Level.INFO, "Check if all are checked");
      for (AbstractState outState : inOtherPartition) {
        if (!cpa.getStopOperator().stop(outState, partitionNodes.get(AbstractStates.extractLocation(outState)), initPrec)) {
          logger
              .log(Level.SEVERE,
                  "Not all outer partition nodes are in other partitions. Following state not contained: ",
                  outState);
          return false;
        }
      }

      logger.log(Level.INFO, "Check if initial state is covered.");
      if (!cpa.getStopOperator().stop(initialState, partitionNodes.get(AbstractStates.extractLocation(initialState)),
          initPrec)) {
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
      if (executor != null) {
        executor.shutdown();
      }
      if (readExecutor != null) {
        readExecutor.shutdown();
      }
      if (checkExecutor != null) {
        checkExecutor.shutdown();
      }
    }
  }

  private void startReadingThreads(final ExecutorService pReadingExecutor, final AtomicBoolean pCheckResult,
      final Semaphore pPartitionChecked, final Lock pLock, final Condition pPartitionReady) {
    for (int i = 0; i < ioHelper.getNumPartitions(); i++) {
      pReadingExecutor.execute(new ParallelPartitionReader(i, pCheckResult, pPartitionChecked, this, ioHelper,
          pPartitionReady, pLock, false, stats));
    }
  }

  private void startCheckingThreads(final ExecutorService pCheckingExecutor, final AtomicBoolean pCheckResult,
      final Semaphore pPartitionChecked, final Collection<AbstractState> pCertificate,
      final Multimap<CFANode, AbstractState> pInPartition, final Collection<AbstractState> pInOtherPartition,
      final Precision pInitialPrecision, final Lock pLock, final Condition pPartitionReady) {
    for (int i = 0; i < ioHelper.getNumPartitions(); i++) {
      pCheckingExecutor.execute(new PartitionChecker(i, pCheckResult, pPartitionChecked, pCertificate,
          pInOtherPartition, pInPartition, pInitialPrecision, cpa, pLock, pPartitionReady, ioHelper, shutdown, logger));
    }
  }

  @Override
  protected void writeProofToStream(final ObjectOutputStream pOut, final UnmodifiableReachedSet pReached)
      throws IOException, InvalidConfigurationException, InterruptedException {
    ioHelper.constructInternalProofRepresentation(pReached);

    // write meta data
    ioHelper.writeMetadata(pOut, pReached.size(), ioHelper.getNumPartitions());
    nextPartition = 0;
  }

  @Override
  protected boolean writeAdditionalProofStream(final ObjectOutputStream pOut) throws IOException {
    // write next partition
    ioHelper.writePartition(pOut, ioHelper.getPartition(nextPartition));
    nextPartition++;
    return nextPartition < ioHelper.getNumPartitions();
  }

  @Override
  protected void readProofFromStream(ObjectInputStream pIn) throws ClassNotFoundException,
      InvalidConfigurationException, IOException {
    // read metadata
    ioHelper.readMetadata(pIn, true);
  }

}
