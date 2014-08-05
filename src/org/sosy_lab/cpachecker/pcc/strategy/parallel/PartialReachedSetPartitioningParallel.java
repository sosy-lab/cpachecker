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
package org.sosy_lab.cpachecker.pcc.strategy.parallel;

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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
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
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.PartitionChecker;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.PartitioningIOHelper;
import org.sosy_lab.cpachecker.util.AbstractStates;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;


public class PartialReachedSetPartitioningParallel extends AbstractStrategy{

  private final PartitioningIOHelper ioHelper;
  private final PropertyCheckerCPA cpa;
  private final ShutdownNotifier shutdownNotifier;

  public PartialReachedSetPartitioningParallel(final Configuration pConfig, final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier, final PropertyCheckerCPA pCpa)
      throws InvalidConfigurationException {
    super(pConfig, pLogger);
    shutdownNotifier = pShutdownNotifier;
    cpa = pCpa;
    ioHelper = new PartitioningIOHelper(pConfig, pLogger, pShutdownNotifier, pCpa);
    addPCCStatistic(ioHelper.getPartitioningStatistc());
  }

  @Override
  public void constructInternalProofRepresentation(UnmodifiableReachedSet pReached)
      throws InvalidConfigurationException, InterruptedException {
    ioHelper.constructInternalProofRepresentation(pReached);
  }

  @Override
  public boolean checkCertificate(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    AtomicBoolean checkResult = new AtomicBoolean(true);
    Semaphore partitionChecked = new Semaphore(0);
    Lock lock = new ReentrantLock();
    Collection<AbstractState> certificate = new HashSet<>(ioHelper.getNumPartitions());
    Multimap<CFANode, AbstractState> partitionNodes = HashMultimap.create();
    Collection<AbstractState> inOtherPartition = new ArrayList<>();
    AbstractState initialState = pReachedSet.popFromWaitlist();
    Precision initPrec = pReachedSet.getPrecision(initialState);

    logger.log(Level.INFO, "Create and start threads");
    ExecutorService executor = Executors.newFixedThreadPool(numThreads );
    try {
      for (int i = 0; i < ioHelper.getNumPartitions(); i++) {
        executor.execute(new PartitionChecker(i, checkResult, partitionChecked, certificate, inOtherPartition,
            partitionNodes, initPrec, cpa, lock, ioHelper, shutdownNotifier, logger));
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
      executor.shutdown();
    }
  }

  @Override
  protected void writeProofToStream(ObjectOutputStream pOut, UnmodifiableReachedSet pReached) throws IOException,
      InvalidConfigurationException, InterruptedException {
    ioHelper.writeProof(pOut,pReached);
  }

  @Override
  protected void readProofFromStream(ObjectInputStream pIn) throws ClassNotFoundException,
      InvalidConfigurationException, IOException {
    ioHelper.readMetadata(pIn, true);
    for (int i = 0; i < ioHelper.getNumPartitions(); i++) {
      ioHelper.readPartition(pIn, stats);
    }
  }

}
