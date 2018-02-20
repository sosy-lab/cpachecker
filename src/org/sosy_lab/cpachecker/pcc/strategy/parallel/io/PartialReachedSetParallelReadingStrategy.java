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
package org.sosy_lab.cpachecker.pcc.strategy.parallel.io;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
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
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.PartitioningIOHelper;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.PartitioningUtils;

@Options(prefix = "pcc.parallel.io")
public class PartialReachedSetParallelReadingStrategy extends AbstractStrategy {

  private final PartitioningIOHelper ioHelper;
  private final PropertyCheckerCPA cpa;
  private final ShutdownNotifier shutdownNotifier;
  private final Lock lock = new ReentrantLock();

  @Option(secure=true, description = "enables parallel checking of partial certificate")
  private boolean enableParallelCheck = false;
  private int nextPartition;

  public PartialReachedSetParallelReadingStrategy(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final Path pProofFile,
      final @Nullable PropertyCheckerCPA pCpa)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pProofFile);
    pConfig.inject(this);
    ioHelper = new PartitioningIOHelper(pConfig, pLogger, pShutdownNotifier);
    shutdownNotifier = pShutdownNotifier;
    cpa = pCpa;
    addPCCStatistic(ioHelper.getPartitioningStatistc());
  }

  @Override
  public void constructInternalProofRepresentation(final UnmodifiableReachedSet pReached)
      throws InvalidConfigurationException, InterruptedException {
    ioHelper.constructInternalProofRepresentation(pReached);
  }

  @Override
  public boolean checkCertificate(final ReachedSet pReachedSet) throws CPAException, InterruptedException {
    AtomicBoolean checkResult = new AtomicBoolean(true);
    AtomicInteger availablePartitions = new AtomicInteger(0);
    AtomicInteger id = new AtomicInteger(0);
    Semaphore partitionChecked = new Semaphore(0);
    Semaphore readPartitions = new Semaphore(ioHelper.getNumPartitions());
    Collection<AbstractState> certificate = Sets.newHashSetWithExpectedSize(ioHelper.getNumPartitions());
    Multimap<CFANode, AbstractState> partitionNodes = HashMultimap.create();
    Collection<AbstractState> inOtherPartition = new ArrayList<>();
    AbstractState initialState = pReachedSet.popFromWaitlist();
    Precision initPrec = pReachedSet.getPrecision(initialState);

    logger.log(Level.INFO, "Create and start threads");
    int threads = enableParallelCheck ? numThreads : 1;
    ExecutorService executor = Executors.newFixedThreadPool(threads);
    try {
      for (int i = 0; i < threads; i++) {
        executor.execute(new ParallelPartitionChecker(availablePartitions, id, checkResult, readPartitions,
            partitionChecked, lock, ioHelper, partitionNodes, certificate, inOtherPartition, initPrec, cpa
                .getStopOperator(), cpa.getTransferRelation(), shutdownNotifier, logger));
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
      executor.shutdown();
    }
  }

  @Override
  protected void writeProofToStream(final ObjectOutputStream pOut, final UnmodifiableReachedSet pReached)
      throws IOException, InvalidConfigurationException, InterruptedException {
    ioHelper.constructInternalProofRepresentation(pReached);

    // write metadata
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
  protected void readProofFromStream(final ObjectInputStream pIn) throws ClassNotFoundException,
      InvalidConfigurationException, IOException {
    // read metadata
    ioHelper.readMetadata(pIn, true);
    // read partitions in parallel
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    try {
      AtomicBoolean success = new AtomicBoolean(true);
      AtomicInteger nextId = new AtomicInteger(0);
      Semaphore waitRead = new Semaphore(0);
      int numPartition = ioHelper.getNumPartitions();

      for (int i = 0; i < numThreads; i++) {
        executor.execute(new ParallelPartitionReader(success,waitRead, nextId, this, ioHelper, stats, logger));
      }

      try {
        waitRead.acquire(numPartition);
      } catch (InterruptedException e) {
        throw new IOException("Proof reading failed.");
      }

      if (!success.get()) {
        logger.log(Level.SEVERE, "Reading partition from proof failed.");
        throw new IOException("Reading one of the partitions failed");
      }
    } finally {
      executor.shutdown();
    }
  }

  @Override
  public Collection<Statistics> getAdditionalProofGenerationStatistics() {
    Collection<Statistics> result = new ArrayList<>(super.getAdditionalProofGenerationStatistics());
    result.add(ioHelper.getGraphStatistic());
    return result;
  }

}
