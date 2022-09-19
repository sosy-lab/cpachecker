// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy.parallel.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.zip.ZipInputStream;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.pcc.strategy.AbstractStrategy;
import org.sosy_lab.cpachecker.pcc.strategy.AbstractStrategy.PCStrategyStatistics;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.PartitioningIOHelper;
import org.sosy_lab.cpachecker.util.Triple;

public class ParallelPartitionReader implements Runnable {

  private final AtomicBoolean success;
  private final Semaphore waitRead;
  private final Semaphore partitionChecked;

  private final AtomicInteger nextPartition;

  private final AbstractStrategy strategy;
  private final PartitioningIOHelper ioHelper;

  private final PCStrategyStatistics stats;
  private final LogManager logger;

  private static final Lock lock = new ReentrantLock();

  public ParallelPartitionReader(
      final AtomicBoolean isSuccess,
      final Semaphore partitionsRead,
      final Semaphore pPartitionChecked,
      final AtomicInteger nextPartitionId,
      final AbstractStrategy proofReader,
      final PartitioningIOHelper pIOHelper,
      final PCStrategyStatistics pStats,
      final LogManager pLogger) {
    success = isSuccess;
    waitRead = partitionsRead;
    partitionChecked = pPartitionChecked;
    nextPartition = nextPartitionId;
    strategy = proofReader;
    ioHelper = pIOHelper;
    stats = pStats;
    logger = pLogger;
  }

  public ParallelPartitionReader(
      final AtomicBoolean isSuccess,
      final Semaphore partitionsRead,
      final AtomicInteger nextPartitionId,
      final AbstractStrategy proofReader,
      final PartitioningIOHelper pIOHelper,
      final PCStrategyStatistics pStats,
      final LogManager pLogger) {
    this(isSuccess, partitionsRead, null, nextPartitionId, proofReader, pIOHelper, pStats, pLogger);
  }

  private void prepareAbortion() {
    success.set(false);
    waitRead.release(ioHelper.getNumPartitions());
    if (partitionChecked != null) {
      partitionChecked.release(ioHelper.getNumPartitions());
    }
  }

  @Override
  @SuppressWarnings("Finally") // not really better doable without switching to Closer
  public void run() {
    Triple<InputStream, ZipInputStream, ObjectInputStream> streams = null;
    int nextId;
    while ((nextId = nextPartition.getAndIncrement()) < ioHelper.getNumPartitions()) {
      try {
        streams = strategy.openAdditionalProofStream(nextId);
        ioHelper.readPartition(streams.getThird(), stats, lock);
        waitRead.release();
      } catch (IOException | ClassNotFoundException e) {
        logger.logUserException(Level.SEVERE, e, "Partition reading failed. Stop checking");
        prepareAbortion();
      } catch (Exception e2) {
        logger.logException(Level.SEVERE, e2, "Unexpected failure during proof reading");
        prepareAbortion();
      } finally {
        if (streams != null) {
          try {
            streams.getThird().close();
            streams.getSecond().close();
            streams.getFirst().close();
          } catch (IOException e) {
            throw new AssertionError(e);
          }
        }
      }
    }
  }
}
