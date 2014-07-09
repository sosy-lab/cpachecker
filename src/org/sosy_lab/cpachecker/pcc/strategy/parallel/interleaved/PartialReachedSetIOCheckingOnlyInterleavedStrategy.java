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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.pcc.strategy.AbstractStrategy;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.PartialReachedSetDirectedGraph;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.PartitioningIOHelper;
import org.sosy_lab.cpachecker.util.AbstractStates;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;


public class PartialReachedSetIOCheckingOnlyInterleavedStrategy extends AbstractStrategy {

  private final PartitioningIOHelper ioHelper;
  private final PropertyCheckerCPA cpa;
  private final ShutdownNotifier shutdownNotifier;

  public PartialReachedSetIOCheckingOnlyInterleavedStrategy(final Configuration pConfig, final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier, final PropertyCheckerCPA pCpa)
      throws InvalidConfigurationException {
    super(pConfig, pLogger);
    ioHelper = new PartitioningIOHelper(pConfig, pLogger, pShutdownNotifier, pCpa);
    cpa = pCpa;
    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  public void constructInternalProofRepresentation(UnmodifiableReachedSet pReached)
      throws InvalidConfigurationException, InterruptedException {
    throw new InvalidConfigurationException(
        "Interleaved proof reading and checking strategies do not  support internal PCC with result check algorithm");
  }

  @Override
  public boolean checkCertificate(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    AtomicBoolean checkResult = new AtomicBoolean(true);
    Semaphore partitionsAvailable = new Semaphore(0);

    List<AbstractState> certificate = new ArrayList<>(ioHelper.getSavedReachedSetSize());
    Multimap<CFANode, AbstractState> inPartition = HashMultimap.create();
    Collection<AbstractState> inOtherPartition = new ArrayList<>();

    AbstractState initialState = pReachedSet.popFromWaitlist();
    Precision initPrec = pReachedSet.getPrecision(initialState);

    logger.log(Level.INFO, "Create reading thread");
    Thread readingThread = new Thread(new PartitionReader(checkResult, partitionsAvailable));
    try {
      readingThread.start();

      int index;
      AbstractState checkedState;
      Collection<? extends AbstractState> successors;

      Multimap<CFANode, AbstractState> coveringInCurrentPartition = HashMultimap.create();

      for (int i = 0; i < ioHelper.getNumPartitions() && checkResult.get(); i++) {
        partitionsAvailable.acquire();

        index = certificate.size();
        coveringInCurrentPartition.clear();

        // add nodes of partition
        addToCurrentCoveringNodes(coveringInCurrentPartition, ioHelper.getPartition(i).getFirst());
        inPartition.putAll(coveringInCurrentPartition);
        for (AbstractState checkState : ioHelper.getPartition(i).getFirst()) {
          certificate.add(checkState);
        }

        // add adjacent nodes of other partition
        addToCurrentCoveringNodes(coveringInCurrentPartition, ioHelper.getPartition(i).getSecond());

        while (index < certificate.size() && checkResult.get()) {
          shutdownNotifier.shutdownIfNecessary();

          checkedState = certificate.get(index++);

          // compute successors
          try {
            successors = cpa.getTransferRelation().getAbstractSuccessors(checkedState, initPrec, null);


            for (AbstractState successor : successors) {
              // check if covered
              if (!cpa.getStopOperator().stop(successor,
                  coveringInCurrentPartition.get(AbstractStates.extractLocation(successor)), initPrec)) {
                certificate.add(successor);
                if (certificate.size() > ioHelper.getSavedReachedSetSize()) {
                  logger.log(Level.SEVERE, "Checking failed, recomputed certificate bigger than original reached set.");
                  return false;
                }
              }
            }
          } catch (CPATransferException | InterruptedException e) {
            logger.log(Level.SEVERE, "Checking failed, successor computation failed");
            return false;
          } catch (CPAException e) {
            logger.log(Level.SEVERE, "Checking failed, checking successor coverage failed");
            return false;
          }

        }

      }

      if (!checkResult.get()) { return false; }

      logger.log(Level.INFO, "Check if all are checked");
      for (AbstractState outState : inOtherPartition) {
        if (!cpa.getStopOperator().stop(outState, inPartition.get(AbstractStates.extractLocation(outState)), initPrec)) {
          logger
              .log(Level.SEVERE,
                  "Not all outer partition nodes are in other partitions. Following state not contained: ",
                  outState);
          return false;
        }
      }

      logger.log(Level.INFO, "Check if initial state is covered.");
      if (!cpa.getStopOperator().stop(initialState, inPartition.get(AbstractStates.extractLocation(initialState)),
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
      checkResult.set(false);
      readingThread.interrupt();
    }
  }

  private void addToCurrentCoveringNodes(Multimap<CFANode, AbstractState> coveringInCurrentPartition,
      AbstractState[] nodes) {
    CFANode node;
    for (AbstractState internalNode : nodes) {
      node = AbstractStates.extractLocation(internalNode);
      coveringInCurrentPartition.put(node, internalNode);
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

  private class PartitionReader implements Runnable {

    private final AtomicBoolean checkResult;
    private final Semaphore mainSemaphore;

    public PartitionReader(final AtomicBoolean pCheckResult, final Semaphore pPartitionChecked) {
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

        for (int i = 0; i < ioHelper.getNumPartitions() && checkResult.get(); i++) {
          ioHelper.readPartition(o, stats);

          if (shutdownNotifier.shouldShutdown()) {
            abortPreparation();
            break;
          }
          mainSemaphore.release();
        }
      } catch (IOException | ClassNotFoundException e) {
        logger.log(Level.SEVERE, "Partition reading failed. Stop checking");
        abortPreparation();
      } catch (Exception e2) {
        logger.log(Level.SEVERE, "Unexpected failure during proof reading");
        e2.printStackTrace();
        abortPreparation();
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

    private void abortPreparation() {
      checkResult.set(false);
      mainSemaphore.release();
    }

  }

}
