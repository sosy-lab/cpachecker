// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy.parallel;

import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PartitioningCheckingHelper;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.PartitionChecker;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.PartitioningIOHelper;

public class ParallelPartitionChecker implements Runnable, PartitioningCheckingHelper {

  private final AtomicInteger numPartitionsAcquiredForChecking;
  private final AtomicInteger nextPartition;
  private final AtomicBoolean checkResult;
  private final Semaphore readAndUnprocessedPartitions;
  private final Semaphore checkedPartitions;
  private final Lock mutex;

  private final PartitioningIOHelper ioHelper;
  private final PartitionChecker checker;

  private final Collection<AbstractState> certificate;
  private final Multimap<CFANode, AbstractState> partitionElems;
  private final Collection<AbstractState> inOtherPartition;

  private final ShutdownNotifier shutdownNotifier;

  public ParallelPartitionChecker(
      final AtomicInteger pAvailablePartitions,
      final AtomicInteger pNextId,
      final AtomicBoolean pCheckResult,
      final Semaphore pReadButUnprocessed,
      final Semaphore pPartitionsChecked,
      final Lock pMutex,
      final PartitioningIOHelper pIOHelper,
      final Multimap<CFANode, AbstractState> partitionElements,
      final Collection<AbstractState> pCertificate,
      final Collection<AbstractState> pInOtherPartition,
      final Precision init,
      final StopOperator stop,
      final TransferRelation transfer,
      final ShutdownNotifier pShutdownNotifier,
      final LogManager pLogger) {
    numPartitionsAcquiredForChecking = pAvailablePartitions;
    nextPartition = pNextId;
    checkResult = pCheckResult;
    readAndUnprocessedPartitions = pReadButUnprocessed;
    checkedPartitions = pPartitionsChecked;
    mutex = pMutex;

    ioHelper = pIOHelper;
    certificate = pCertificate;
    partitionElems = partitionElements;
    inOtherPartition = pInOtherPartition;

    shutdownNotifier = pShutdownNotifier;

    checker =
        new PartitionChecker(init, stop, transfer, ioHelper, this, pShutdownNotifier, pLogger);
  }

  @Override
  public void run() {
    int nextPartitionId;
    while (numPartitionsAcquiredForChecking.incrementAndGet() <= ioHelper.getNumPartitions()) {
      if (shutdownNotifier.shouldShutdown()) {
        abortCheckingPreparation();
      }

      if (!checkResult.get()) {
        break;
      }

      try {
        readAndUnprocessedPartitions.acquire();
      } catch (InterruptedException e) {
        abortCheckingPreparation();
        return;
      }
      nextPartitionId = nextPartition.getAndIncrement();

      if (!checkResult.get()) {
        break;
      }

      if (shutdownNotifier.shouldShutdown()) {
        abortCheckingPreparation();
      }
      checker.checkPartition(nextPartitionId);

      mutex.lock();
      try {
        checker.addCertificatePartsToCertificate(certificate);
        checker.addPartitionElements(partitionElems);
        checker.addElementsCheckedInOtherPartitions(inOtherPartition);
      } finally {
        mutex.unlock();
      }

      checkedPartitions.release();

      checker.clearAllSavedPartitioningElements();
    }
  }

  @Override
  public int getCurrentCertificateSize() {
    return certificate.size();
  }

  @Override
  public void abortCheckingPreparation() {
    checkResult.set(false);
    readAndUnprocessedPartitions.release(ioHelper.getNumPartitions());
    checkedPartitions.release(ioHelper.getNumPartitions());
  }
}
