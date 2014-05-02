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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipInputStream;

import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.pcc.strategy.AbstractStrategy;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.PartitioningIOHelper;

@Options(prefix = "pcc")
public class PartialReachedSetParallelReadingStrategy extends AbstractStrategy {

  private final PartitioningIOHelper ioHelper;
  private final PropertyCheckerCPA cpa;
  private final ShutdownNotifier shutdownNotifier;
  private final Lock lock = new ReentrantLock();

  @Option(description = "enables parallel checking of partial certificate")
  private boolean enableParallelCheck = false;
  private int nextPartition;

  public PartialReachedSetParallelReadingStrategy(Configuration pConfig, LogManager pLogger,
      ShutdownNotifier pShutdownNotifier, PropertyCheckerCPA pCpa)
      throws InvalidConfigurationException {
    super(pConfig, pLogger);
    pConfig.inject(this);
    ioHelper = new PartitioningIOHelper(pConfig, pLogger, pShutdownNotifier, pCpa);
    shutdownNotifier = pShutdownNotifier;
    cpa = pCpa;
    // TODO Auto-generated constructor stub, all elements needed?
  }

  @Override
  public void constructInternalProofRepresentation(final UnmodifiableReachedSet pReached)
      throws InvalidConfigurationException {
    ioHelper.constructInternalProofRepresentation(pReached);
  }

  @Override
  public boolean checkCertificate(final ReachedSet pReachedSet) throws CPAException, InterruptedException {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  protected void writeProofToStream(final ObjectOutputStream pOut, final UnmodifiableReachedSet pReached)
      throws IOException,
      InvalidConfigurationException {
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
    AtomicBoolean success = new AtomicBoolean(true);
    Semaphore waitRead = new Semaphore(0);
    int numPartition = ioHelper.getNumPartitions();
    for (int i = 0; i < numPartition; i++) {
      executor.execute(new ParallelPartitionReader(i, success, waitRead, numPartition));
    }

    try {
      waitRead.acquire(numPartition);
    } catch (InterruptedException e) {
      throw new IOException("Proof reading failed.");
    }

    if (!success.get()) { throw new IOException("Reading one of the partitions failed"); }

    executor.shutdown();
  }

  // TODO put in own file as public class
  private class ParallelPartitionReader implements Runnable {

    private final int partitionIndex;
    private final AtomicBoolean success;
    private final Semaphore waitRead;
    private final int numPartitions;

    public ParallelPartitionReader(final int index, AtomicBoolean pSuccess, Semaphore pWaitRead, int pNumPartition) {
      partitionIndex = index;
      success = pSuccess;
      waitRead = pWaitRead;
      numPartitions = pNumPartition;
    }

    private void prepareAbortion() {
      // TODO logging?
      success.set(false);
      waitRead.release(numPartitions);
    }

    @Override
    public void run() {
      Triple<InputStream, ZipInputStream, ObjectInputStream> streams = null;
      try {
        streams = openAdditionalProofStream(partitionIndex);
        ioHelper.readPartition(streams.getThird(), lock);
      } catch (IOException | ClassNotFoundException e) {
        prepareAbortion();
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

  }

}
