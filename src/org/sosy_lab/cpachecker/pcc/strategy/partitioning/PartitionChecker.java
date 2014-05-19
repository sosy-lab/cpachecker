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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;


public class PartitionChecker implements Runnable {

  private final int partitionNumber;

  private final AtomicBoolean checkResult;
  private final Semaphore mainSemaphore;
  private final Lock lock;
  private Condition partitionReady;

  private final Collection<AbstractState> certificate;
  private final Multimap<CFANode, AbstractState> isInPartition;
  private final Collection<AbstractState> mustBeContainedInCertificate;
  private final List<AbstractState> addToCertificate = new ArrayList<>();
  private final List<AbstractState> addToContainedInCertificate = new ArrayList<>();

  private final PartitioningIOHelper ioHelper;
  private final Precision initPrec;
  private final StopOperator stop;
  private final TransferRelation transfer;

  private final Deque<AbstractState> waitlist = new ArrayDeque<>();
  private final Multimap<CFANode, AbstractState> statesPerLocation = HashMultimap.create();

  private final ShutdownNotifier shutdownNotifier;
  private final LogManager logger;


  public PartitionChecker(final int pNumber, final AtomicBoolean pCheckResult, final Semaphore pPartitionChecked,
      final Collection<AbstractState> pCertificate, final Collection<AbstractState> pInOtherPartition,
      final Multimap<CFANode, AbstractState> pInPartition, final Precision pInitPrec,
      final ConfigurableProgramAnalysis pCpa, final Lock pLock, final PartitioningIOHelper pHelper,
      final ShutdownNotifier pShutdown, final LogManager pLogger) {
    partitionNumber = pNumber;
    checkResult = pCheckResult;
    mainSemaphore = pPartitionChecked;
    certificate = pCertificate;
    mustBeContainedInCertificate = pInOtherPartition;
    isInPartition = pInPartition;
    initPrec = pInitPrec;
    if (pCpa instanceof ARGCPA) {
      stop = ((ARGCPA) pCpa).getWrappedCPAs().get(0).getStopOperator();
      transfer = ((ARGCPA) pCpa).getWrappedCPAs().get(0).getTransferRelation();
    } else {
      stop = pCpa.getStopOperator();
      transfer = pCpa.getTransferRelation();
    }
    lock = pLock;
    ioHelper = pHelper;
    shutdownNotifier = pShutdown;
    logger = pLogger;
    partitionReady = null;
  }

  public PartitionChecker(final int pNumber, final AtomicBoolean pCheckResult, final Semaphore pPartitionChecked,
      final Collection<AbstractState> pCertificate, final Collection<AbstractState> pInOtherPartition,
      final Multimap<CFANode, AbstractState> pInPartition, final Precision pInitPrec,
      final ConfigurableProgramAnalysis pCpa, final Lock pLock, final Condition pPartitionReady,
      final PartitioningIOHelper pHelper, final ShutdownNotifier pShutdown, final LogManager pLogger) {
    this(pNumber, pCheckResult, pPartitionChecked, pCertificate, pInOtherPartition, pInPartition, pInitPrec, pCpa,
        pLock, pHelper, pShutdown, pLogger);
    partitionReady = pPartitionReady;
  }

  @Override
  public void run() {
    try {
      Pair<AbstractState[], AbstractState[]> partition = null;
      lock.lock();
      try {
        while ((partition = ioHelper.getPartition(partitionNumber)) == null) {
          if (!checkResult.get()) { return; }
          if (partitionReady == null) {
            logger.log(Level.SEVERE, "Not configured for interleaved proof reading and checking");
            abortPreparation();
            return;
          }
          partitionReady.await();
        }
      } catch (InterruptedException e) {
        abortPreparation();
        return;
      } finally {
        lock.unlock();
      }


      // add nodes of partition
      for (AbstractState internalNode : partition.getFirst()) {
        addElement(internalNode, true);
      }

      // add adjacent nodes of other partition
      for (AbstractState adjacentNode : partition.getSecond()) {
        addElement(adjacentNode, false);
      }

      AbstractState checkedState;
      Collection<? extends AbstractState> successors;


      while (!waitlist.isEmpty()) {
        if (shutdownNotifier.shouldShutdown()) {
          abortPreparation();
          return;
        }

        if (addToCertificate.size() + certificate.size() > ioHelper
            .getSavedReachedSetSize()) {
          logger.log(Level.SEVERE, "Checking failed, recomputed certificate bigger than original reached set.");
          abortPreparation();
          return;
        }

        checkedState = waitlist.pop();

        // compute successors
        try {
          successors = transfer.getAbstractSuccessors(checkedState, initPrec, null);


          for (AbstractState successor : successors) {
            // check if covered
            if (!stop.stop(successor, statesPerLocation.get(AbstractStates.extractLocation(successor)), initPrec)) {
              addElement(successor, true);
            }
          }
        } catch (CPATransferException | InterruptedException e) {
          logger.log(Level.SEVERE, "Checking failed, successor computation failed");
          abortPreparation();
          return;
        } catch (CPAException e) {
          logger.log(Level.SEVERE, "Checking failed, checking successor coverage failed");
          abortPreparation();
          return;
        }
      }

      lock.lock();
      try {
        certificate.addAll(addToCertificate);
        mustBeContainedInCertificate.addAll(addToContainedInCertificate);
        for (AbstractState internalNode : partition.getFirst()) {
          isInPartition.put(AbstractStates.extractLocation(internalNode), internalNode);
        }
      } finally {
        lock.unlock();
      }

      mainSemaphore.release();
    } catch (Exception e2) {
      logger.log(Level.SEVERE, "Unexpected failure during proof reading");
      e2.printStackTrace();
      abortPreparation();
    }
  }

  private void addElement(final AbstractState element, final boolean inCertificate) {
    CFANode node = AbstractStates.extractLocation(element);
    statesPerLocation.put(node, element);
    if (inCertificate) {
      addToCertificate.add(element);
      waitlist.push(element);
    } else {
      addToContainedInCertificate.add(element);
    }
  }

  private void abortPreparation() {
    checkResult.set(false);
    if (partitionReady != null) {
      lock.lock();
      try {
        partitionReady.signalAll();
      } finally {
        lock.unlock();
      }
    }
    mainSemaphore.release(ioHelper.getNumPartitions());
  }

}
