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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
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
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.pcc.strategy.AbstractStrategy;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.PartitioningIOHelper;
import org.sosy_lab.cpachecker.util.AbstractStates;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;


public class PartialReachedSetIOCheckingInterleavedStrategy extends AbstractStrategy {

  private final PartitioningIOHelper ioHelper;
  private final PropertyCheckerCPA cpa;
  private final ShutdownNotifier shutdownNotifier;
  private final Lock lock = new ReentrantLock();
  private final Condition partitionReady = lock.newCondition();

  public PartialReachedSetIOCheckingInterleavedStrategy(Configuration pConfig, LogManager pLogger,
      ShutdownNotifier pShutdownNotifier, PropertyCheckerCPA pCpa)
      throws InvalidConfigurationException {
    super(pConfig, pLogger);
    ioHelper = new PartitioningIOHelper(pConfig, pLogger, pShutdownNotifier, pCpa);
    cpa = pCpa;
    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  public void constructInternalProofRepresentation(UnmodifiableReachedSet pReached)
      throws InvalidConfigurationException {
    ioHelper.constructInternalProofRepresentation(pReached);
  }

  @Override
  public boolean checkCertificate(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    AtomicBoolean checkResult = new AtomicBoolean(true);
    Semaphore partitionChecked = new Semaphore(0);
    Collection<AbstractState> certificate = new HashSet<>(ioHelper.getSavedReachedSetSize());
    Collection<AbstractState> inOtherPartition = new ArrayList<>();
    Precision initPrec = pReachedSet.getPrecision(pReachedSet.getFirstState());

    logger.log(Level.INFO, "Create and start threads");
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    executor.execute(new PartitionReader(checkResult, partitionChecked));
    for (int i = 0; i < ioHelper.getNumPartitions(); i++) {
      executor.execute(new PartitionChecker(i, checkResult, partitionChecked, certificate, inOtherPartition, initPrec,
          cpa));
    }

    partitionChecked.acquire(ioHelper.getNumPartitions());

    if(!checkResult.get()){
      return false;
    }

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
  }

  @Override
  protected void writeProofToStream(ObjectOutputStream pOut, UnmodifiableReachedSet pReached) throws IOException,
      InvalidConfigurationException {
    ioHelper.constructInternalProofRepresentation(pReached);
  }

  @Override
  protected void readProofFromStream(ObjectInputStream pIn) throws ClassNotFoundException,
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

  private void giveSignalAndPrepareAbortion(AtomicBoolean pValue, Semaphore pForRelease) {
    pValue.set(false);
    giveSignal();
    pForRelease.release(ioHelper.getNumPartitions());
  }

  private class PartitionReader implements Runnable {

    private final AtomicBoolean checkResult;
    private final Semaphore mainSemaphore;

    public PartitionReader(AtomicBoolean pCheckResult, Semaphore pPartitionChecked) {
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

  // TODO put in own class?
  private class PartitionChecker implements Runnable {

    private final int partitionNumber;

    private final AtomicBoolean checkResult;
    private final Semaphore mainSemaphore;

    private final Collection<AbstractState> certificate;
    private final Collection<AbstractState> mustBeContainedInCertificate;
    private final List<AbstractState> addToCertificate = new ArrayList<>();
    private final List<AbstractState> addToContainedInCertificate = new ArrayList<>();

    private final Precision initPrec;
    private final StopOperator stop;
    private final TransferRelation transfer;

    private final Deque<AbstractState> waitlist = new ArrayDeque<>();
    private final Multimap<CFANode, AbstractState> statesPerLocation = HashMultimap.create();



    PartitionChecker(final int pNumber, final AtomicBoolean pCheckResult, final Semaphore pPartitionChecked,
        final Collection<AbstractState> pCertificate, final Collection<AbstractState> pInOtherPartition,
        final Precision pInitPrec, final ConfigurableProgramAnalysis pCpa) {
      partitionNumber = pNumber;
      checkResult = pCheckResult;
      mainSemaphore = pPartitionChecked;
      certificate = pCertificate;
      mustBeContainedInCertificate = pInOtherPartition;
      initPrec = pInitPrec;
      if (pCpa instanceof ARGCPA) {
        stop = ((ARGCPA) pCpa).getWrappedCPAs().get(0).getStopOperator();
        transfer = ((ARGCPA) pCpa).getWrappedCPAs().get(0).getTransferRelation();
      } else {
        stop = pCpa.getStopOperator();
        transfer = pCpa.getTransferRelation();
      }
    }

    @Override
    public void run() {
      Pair<AbstractState[], AbstractState[]> partition = null;
      lock.lock();
      try {
        while (partition == null) {
          if (!checkResult.get()) { return; }
          partition = ioHelper.getPartition(partitionNumber);
          partitionReady.await();
        }
      } catch (InterruptedException e) {
        abortPreparation();
        return;
      } finally {
        lock.unlock();
      }


      // add nodes of partition
      for(AbstractState internalNode:partition.getFirst()){
        addElement(internalNode, true);
      }

      // add adjacent nodes of other partition
      for(AbstractState internalNode:partition.getSecond()){
        addElement(internalNode, false);
      }

      AbstractState checkedState;
      Collection<? extends AbstractState> successors;


      while(!waitlist.isEmpty()){
        if(shutdownNotifier.shouldShutdown()){
          abortPreparation();
          return;
        }

        if (addToCertificate.size() + certificate.size() > ioHelper.getSavedReachedSetSize()) {
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
      } finally {
        lock.unlock();
      }

      mainSemaphore.release();
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

    private void abortPreparation(){
      giveSignalAndPrepareAbortion(checkResult, mainSemaphore);
    }
  }

}
