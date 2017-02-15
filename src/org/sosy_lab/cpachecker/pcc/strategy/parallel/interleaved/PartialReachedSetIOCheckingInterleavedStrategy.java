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
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.zip.ZipInputStream;
import javax.annotation.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
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
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.PartialReachedSetDirectedGraph;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.PartitioningIOHelper;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.PartitioningUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Triple;

public class PartialReachedSetIOCheckingInterleavedStrategy extends AbstractStrategy {

  private final PartitioningIOHelper ioHelper;
  private final PropertyCheckerCPA cpa;
  private final ShutdownNotifier shutdownNotifier;
  private final Lock lock = new ReentrantLock();

  public PartialReachedSetIOCheckingInterleavedStrategy(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final Path pProofFile,
      final @Nullable PropertyCheckerCPA pCpa)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pProofFile);
    ioHelper = new PartitioningIOHelper(pConfig, pLogger, pShutdownNotifier);
    cpa = pCpa;
    shutdownNotifier = pShutdownNotifier;
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
    AtomicInteger nextId = new AtomicInteger(0);
    AtomicInteger availableForChecking =new AtomicInteger(0);
    Semaphore partitionsRead = new Semaphore(0);
    Semaphore partitionChecked = new Semaphore(0);
    Collection<AbstractState> certificate = Sets.newHashSetWithExpectedSize(ioHelper.getSavedReachedSetSize());
    Multimap<CFANode, AbstractState> partitionNodes = HashMultimap.create();
    Collection<AbstractState> inOtherPartition = new ArrayList<>();
    AbstractState initialState = pReachedSet.popFromWaitlist();
    Precision initPrec = pReachedSet.getPrecision(initialState);

   logger.log(Level.INFO, "Create and start threads");
    ExecutorService executor = Executors.newFixedThreadPool(numThreads-1);
    try {
      for (int i = 0; i < numThreads-1; i++) {
        executor.execute(new ParallelPartitionChecker(availableForChecking, nextId, checkResult, partitionsRead,
            partitionChecked, lock, ioHelper, partitionNodes, certificate, inOtherPartition, initPrec, cpa
                .getStopOperator(), cpa.getTransferRelation(), shutdownNotifier, logger));
      }

      // read partitions
      new PartitionReader(checkResult, partitionsRead, partitionChecked).run();

      if (!checkResult.get()) { return false; }

      // help checking remaining partitions
      new ParallelPartitionChecker(availableForChecking, nextId, checkResult, partitionsRead,
          partitionChecked, lock, ioHelper, partitionNodes, certificate, inOtherPartition, initPrec, cpa
              .getStopOperator(), cpa.getTransferRelation(), shutdownNotifier, logger).run();

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
    Pair<PartialReachedSetDirectedGraph, List<Set<Integer>>> partitioning =
        ioHelper.computePartialReachedSetAndPartition(pReached);

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

  @Override
  public Collection<Statistics> getAdditionalProofGenerationStatistics() {
    Collection<Statistics> result = new ArrayList<>(super.getAdditionalProofGenerationStatistics());
    result.add(ioHelper.getGraphStatistic());
    return result;
  }

  private class PartitionReader implements Runnable {

    private final AtomicBoolean checkResult;
    private final Semaphore partitionsRead;
    private final Semaphore checkedPartitions;

    public PartitionReader(final AtomicBoolean pCheckResult, final Semaphore pPartitionsRead,
        final Semaphore pCheckedPartitions) {
      checkResult = pCheckResult;
      partitionsRead = pPartitionsRead;
      checkedPartitions = pCheckedPartitions;
    }

    @Override
    public void run() {
      Triple<InputStream, ZipInputStream, ObjectInputStream> streams = null;
      try {
        streams = openProofStream();
        ObjectInputStream o = streams.getThird();
        ioHelper.readMetadata(o, false);
        for (int i = 0; i < ioHelper.getNumPartitions() && checkResult.get(); i++) {
          ioHelper.readPartition(o, stats);
          if (shutdownNotifier.shouldShutdown()) {
            abort();
            break;
          }
          partitionsRead.release();
        }
      } catch (IOException | ClassNotFoundException e) {
        logger.logUserException(Level.SEVERE, e, "Partition reading failed. Stop checking");
        abort();
      } catch (Exception e2) {
        logger.logException(Level.SEVERE, e2, "Unexpected failure during proof reading");
        abort();
      } finally {
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

    private void abort() {
      checkResult.set(false);
      partitionsRead.release(ioHelper.getNumPartitions());
      checkedPartitions.release(ioHelper.getNumPartitions());
    }

  }

}
