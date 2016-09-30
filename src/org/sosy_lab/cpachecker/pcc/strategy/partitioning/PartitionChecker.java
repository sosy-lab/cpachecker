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
package org.sosy_lab.cpachecker.pcc.strategy.partitioning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PartitioningCheckingHelper;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;


public class PartitionChecker {

  private final Multimap<CFANode, AbstractState> partitionParts = HashMultimap.create();
  private final List<AbstractState> certificatePart = new ArrayList<>();
  private final Collection<AbstractState> mustBeInCertificate = new HashSet<>();

  private final PartitioningCheckingHelper partitionHelper;
  private final PartitioningIOHelper ioHelper;
  private final Precision initPrec;
  private final StopOperator stop;
  private final TransferRelation transfer;

  private final ShutdownNotifier shutdownNotifier;
  private final LogManager logger;

  private final Timer transferTime;
  private final Timer stopTime;

  private  ExecutorService executors = null;
  private Semaphore sem;
  private Lock lock;
  private int numThreads;

  public PartitionChecker(final Precision pInitPrecision, final StopOperator pStop, final TransferRelation pTransfer,
      final PartitioningIOHelper pIOHelper, final PartitioningCheckingHelper pHelperInfo,
      final ShutdownNotifier pShutdownNotifier, final LogManager pLogger) {
    initPrec = pInitPrecision;
    stop = pStop;
    transfer = pTransfer;

    ioHelper = pIOHelper;
    partitionHelper = pHelperInfo;

    shutdownNotifier = pShutdownNotifier;
    logger = pLogger;

    transferTime = new Timer();
    stopTime = new Timer();
  }

  public PartitionChecker(final Precision pInitPrec, final StopOperator pStopOperator,
      final TransferRelation pTransferRelation, final PartitioningIOHelper pIoHelper,
      final PartitioningCheckingHelper pCheckInfo, final ShutdownNotifier pShutdownNotifier,
      final LogManager pLogger, final Timer pStopTimer, final Timer pTransferTimer) {
    initPrec = pInitPrec;
    stop = pStopOperator;
    transfer = pTransferRelation;

    ioHelper = pIoHelper;
    partitionHelper = pCheckInfo;

    shutdownNotifier = pShutdownNotifier;
    logger = pLogger;

    transferTime = pTransferTimer;
    stopTime = pStopTimer;
  }


  public PartitionChecker(final Precision pInitPrec, final StopOperator pStopOperator,
      final TransferRelation pTransferRelation, final PartitioningIOHelper pIoHelper,
      final PartitioningCheckingHelper pCheckInfo, final ShutdownNotifier pShutdownNotifier,
      final LogManager pLogger, final ExecutorService pExecutorService, final int pNumThreads) {
    initPrec = pInitPrec;
    stop = pStopOperator;
    transfer = pTransferRelation;

    ioHelper = pIoHelper;
    partitionHelper = pCheckInfo;

    shutdownNotifier = pShutdownNotifier;
    logger = pLogger;

    transferTime = new Timer();
    stopTime = new Timer();

    numThreads = pNumThreads;

    executors = pExecutorService;
    sem = new Semaphore(0);
    lock = new ReentrantLock();
  }

  public void checkPartition(int pIndex){
    Multimap<CFANode, AbstractState> statesPerLocation = HashMultimap.create();
    Pair<AbstractState[], AbstractState[]> partition = ioHelper.getPartition(pIndex);
    Preconditions.checkNotNull(partition);

    // add nodes of partition
    for (AbstractState internalNode : partition.getFirst()) {
      addElement(internalNode, true, statesPerLocation);
    }

    // add adjacent nodes of other partition
    for (AbstractState adjacentNode : partition.getSecond()) {
      addElement(adjacentNode, false, statesPerLocation);
    }

    if (executors == null) {
      inspectPartitionElementSequential(statesPerLocation);
    } else {
      inspectPartitionElementParallel(statesPerLocation);
    }
  }

  private void inspectPartitionElementSequential(Multimap<CFANode, AbstractState> statesPerLocation) {
    boolean isCovered;
    AbstractState checkedState;
    CFANode loc;
    Collection<? extends AbstractState> successors;
    int nextPos = 0;

    while (nextPos<certificatePart.size()) {
      if (shutdownNotifier.shouldShutdown()) {
        partitionHelper.abortCheckingPreparation();
        return;
      }

      if (certificatePart.size() + partitionHelper.getCurrentCertificateSize() > ioHelper
          .getSavedReachedSetSize()) {
        logger.log(Level.SEVERE, "Checking failed, recomputed certificate bigger than original reached set.");
        partitionHelper.abortCheckingPreparation();
        return;
      }

      checkedState = certificatePart.get(nextPos++);

      // compute successors
      try {
        transferTime.start();
        successors = transfer.getAbstractSuccessors(checkedState, initPrec);
        transferTime.stop();


        for (AbstractState successor : successors) {
          // check if covered
          loc = AbstractStates.extractLocation(successor);

          stopTime.start();
          isCovered = stop.stop(successor, statesPerLocation.get(loc), initPrec);
          stopTime.stop();
          if (!isCovered) {
            certificatePart.add(successor);
          }
        }
      } catch (CPATransferException | InterruptedException e) {
        stopTime.stopIfRunning();
        transferTime.stopIfRunning();
        logger.log(Level.SEVERE, "Checking failed, successor computation failed");
        partitionHelper.abortCheckingPreparation();
        return;
      } catch (CPAException e) {
        stopTime.stopIfRunning();
        transferTime.stopIfRunning();
        logger.log(Level.SEVERE, "Checking failed, checking successor coverage failed");
        partitionHelper.abortCheckingPreparation();
        return;
      }
    }
  }

  private void inspectPartitionElementParallel(Multimap<CFANode, AbstractState> statesPerLocation) {
    AtomicInteger counter = new AtomicInteger(0);
    for (int i = 0; i < numThreads; i++) {
      executors.execute(new PartitionElementParallelChecker(counter, statesPerLocation));
    }

    try {
      sem.acquire(numThreads);
    } catch (InterruptedException e) {
      partitionHelper.abortCheckingPreparation();
    }
  }

  public void addCertificatePartsToCertificate(final Collection<AbstractState> pCertificate) {
    pCertificate.addAll(certificatePart);
  }

  public void addElementsCheckedInOtherPartitions(final Collection<AbstractState> pStatesMustBeInCertificate) {
    pStatesMustBeInCertificate.addAll(mustBeInCertificate);
  }

  public void addPartitionElements(final Multimap<CFANode, AbstractState> pPartitionElements){
    pPartitionElements.putAll(partitionParts);
  }

  public void clearPartitionElementsSavedForInspection() {
    certificatePart.clear();
  }

  public void clearAllSavedPartitioningElements() {
    certificatePart.clear();
    partitionParts.clear();
    mustBeInCertificate.clear();
  }

  private void addElement(final AbstractState element, final boolean inCertificate,
      final Multimap<CFANode, AbstractState> pStatesPerLocation) {
    CFANode node = AbstractStates.extractLocation(element);
    pStatesPerLocation.put(node, element);
    if (inCertificate) {
      partitionParts.put(node, element);
      certificatePart.add(element);
    } else {
      mustBeInCertificate.add(element);
    }
  }

  private class PartitionElementParallelChecker implements Runnable {

    private AtomicInteger counter;
    private Multimap<CFANode, AbstractState> statesPerLocation;

    public PartitionElementParallelChecker(final AtomicInteger pCount,
        final Multimap<CFANode, AbstractState> pStatesPerLocation) {
      counter = pCount;
      statesPerLocation = pStatesPerLocation;
    }

    @Override
    public void run() {
      Collection<AbstractState> recomputedElements = new ArrayList<>();
      try {
        boolean isCovered;
        AbstractState checkedState;
        CFANode loc;
        Collection<? extends AbstractState> successors;
        int next;
        while ((next = counter.getAndIncrement()) < certificatePart.size()) {
          if (shutdownNotifier.shouldShutdown()) {
            partitionHelper.abortCheckingPreparation();
            return;
          }

          if (certificatePart.size() + partitionHelper.getCurrentCertificateSize() + recomputedElements.size() > ioHelper
              .getSavedReachedSetSize()) {
            logger.log(Level.SEVERE, "Checking failed, recomputed certificate bigger than original reached set.");
            partitionHelper.abortCheckingPreparation();
            return;
          }

          checkedState = certificatePart.get(next);

          // compute successors
          try {
            transferTime.start();
            successors = transfer.getAbstractSuccessors(checkedState, initPrec);
            transferTime.stop();


            for (AbstractState successor : successors) {
              // check if covered
              loc = AbstractStates.extractLocation(successor);

              stopTime.start();
              isCovered = stop.stop(successor, statesPerLocation.get(loc), initPrec);
              stopTime.stop();
              if (!isCovered) {
                recomputedElements.add(successor);
              }
            }
          } catch (CPATransferException | InterruptedException e) {
            stopTime.stopIfRunning();
            transferTime.stopIfRunning();
            logger.log(Level.SEVERE, "Checking failed, successor computation failed");
            partitionHelper.abortCheckingPreparation();
            return;
          } catch (CPAException e) {
            stopTime.stopIfRunning();
            transferTime.stopIfRunning();
            logger.log(Level.SEVERE, "Checking failed, checking successor coverage failed");
            partitionHelper.abortCheckingPreparation();
            return;
          }
        }
      } finally {
        lock.lock();
        certificatePart.addAll(recomputedElements);
        lock.unlock();
        sem.release();
      }
    }
  }
}
