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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.pcc.strategy.AbstractStrategy;
import org.sosy_lab.cpachecker.pcc.strategy.parallel.ParallelPartitionChecker;
import org.sosy_lab.cpachecker.pcc.strategy.parallel.io.ParallelPartitionReader;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.PartitioningIOHelper;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.PartitioningUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import javax.annotation.Nullable;

@Options(prefix = "pcc.interleaved")
public class PartialReachedSetParallelIOCheckingInterleavedStrategy extends AbstractStrategy {

  @Option(secure=true,
      name = "useReadCores",
      description = "The number of cores used exclusively for proof reading. Must be less than pcc.useCores and may not be negative. Value 0 means that the cores used for reading and checking are shared")
  private int numReadThreads = 0;

  private int nextPartition;
  private final PartitioningIOHelper ioHelper;
  private final ShutdownNotifier shutdown;
  private final PropertyCheckerCPA cpa;

  public PartialReachedSetParallelIOCheckingInterleavedStrategy(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final @Nullable PropertyCheckerCPA pCpa)
      throws InvalidConfigurationException {
    super(pConfig, pLogger);
    pConfig.inject(this);

    shutdown = pShutdownNotifier;
    cpa = pCpa;

    ioHelper = new PartitioningIOHelper(pConfig, pLogger, pShutdownNotifier);
    numReadThreads = Math.min(numReadThreads, numThreads - 1);
    numReadThreads = Math.max(0, numReadThreads);
    addPCCStatistic(ioHelper.getPartitioningStatistc());
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
    Semaphore partitionsRead = new Semaphore(0);
    Collection<AbstractState> certificate = Sets.newHashSetWithExpectedSize(ioHelper.getNumPartitions());
    Multimap<CFANode, AbstractState> partitionNodes = HashMultimap.create();
    Collection<AbstractState> inOtherPartition = new ArrayList<>();
    AbstractState initialState = pReachedSet.popFromWaitlist();
    Precision initPrec = pReachedSet.getPrecision(initialState);
    Lock lock = new ReentrantLock();

    ExecutorService executor = null, readExecutor = null, checkExecutor = null;
    logger.log(Level.INFO, "Create and start threads");
    try {
      if (numReadThreads == 0) {
        executor = Executors.newFixedThreadPool(numThreads);
        startReadingThreads(numThreads, executor, checkResult, partitionsRead);
        startCheckingThreads(numThreads, executor, checkResult, partitionsRead, partitionChecked, certificate,
            partitionNodes, inOtherPartition,
            initPrec, lock);
      } else {
        readExecutor = Executors.newFixedThreadPool(numReadThreads);
        startReadingThreads(numReadThreads, readExecutor, checkResult, partitionsRead);
        checkExecutor = Executors.newFixedThreadPool(numThreads - numReadThreads);
        startCheckingThreads(numThreads - numReadThreads, checkExecutor, checkResult, partitionsRead, partitionChecked,
            certificate, partitionNodes, inOtherPartition,
            initPrec, lock);
      }

      partitionChecked.acquire(ioHelper.getNumPartitions());

      if (!checkResult.get()) { return false; }

      logger.log(Level.INFO, "Add initial state to elements for which it will be checked if they are covered by partition nodes of certificate.");
      inOtherPartition.add(initialState);

      logger.log(Level.INFO,
              "Check if initial state and all nodes which should be contained in different partition are covered by certificate (partition node).");
      if (!PartitioningUtils.areElementsCoveredByPartitionElement(inOtherPartition, partitionNodes, cpa.getStopOperator(),
          initPrec)) {
        logger.log(Level.SEVERE,
            "Initial state or a state which should be in other partition is not covered by certificate.");
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

  private void startReadingThreads(final int threads, final ExecutorService pReadingExecutor, final AtomicBoolean pCheckResult,
      final Semaphore partitionsRead) {
    AtomicInteger nextPartitionId = new AtomicInteger(0);
    for (int i = 0; i < threads; i++) {
      pReadingExecutor.execute(new ParallelPartitionReader(pCheckResult, partitionsRead, nextPartitionId, this,
          ioHelper, stats, logger));
    }
  }

  private void startCheckingThreads(final int threads, final ExecutorService pCheckingExecutor, final AtomicBoolean pCheckResult,
      final Semaphore pPartitionsRead, final Semaphore pPartitionChecked, final Collection<AbstractState> pCertificate,
      final Multimap<CFANode, AbstractState> pInPartition, final Collection<AbstractState> pInOtherPartition,
      final Precision pInitialPrecision, final Lock pLock) {
    AtomicInteger availablePartitions = new AtomicInteger(0);
    AtomicInteger nextId = new AtomicInteger(0);
    for (int i = 0; i < threads; i++) {
      pCheckingExecutor.execute(new ParallelPartitionChecker(availablePartitions, nextId, pCheckResult, pPartitionsRead,
          pPartitionChecked, pLock, ioHelper, pInPartition, pCertificate, pInOtherPartition, pInitialPrecision, cpa
              .getStopOperator(), cpa.getTransferRelation(), shutdown, logger));
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

  @Override
  public Collection<Statistics> getAdditionalProofGenerationStatistics() {
    Collection<Statistics> result = new ArrayList<>(super.getAdditionalProofGenerationStatistics());
    result.add(ioHelper.getGraphStatistic());
    return result;
  }

}
