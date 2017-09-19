/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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

import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.zip.ZipInputStream;
import javax.annotation.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.HistoryForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.pcc.strategy.AbstractStrategy;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.CMCPartitionChecker;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.CMCPartitioningIOHelper;
import org.sosy_lab.cpachecker.pcc.strategy.util.cmc.AssumptionAutomatonGenerator;
import org.sosy_lab.cpachecker.pcc.strategy.util.cmc.PartialCPABuilder;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

// FIXME unsound strategy
public class PartialReachedSetIOCheckingOnlyInterleavedCMCStrategy extends AbstractStrategy {

  private final Configuration config;
  private final ShutdownNotifier shutdown;
  private final PartialCPABuilder cpaBuilder;
  private final AssumptionAutomatonGenerator automatonWriter;
  private int numProofs;

  public PartialReachedSetIOCheckingOnlyInterleavedCMCStrategy(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final Path pProofFile,
      final @Nullable CFA pCFA,
      final @Nullable Specification pSpecification)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pProofFile);
    cpaBuilder = new PartialCPABuilder(pConfig, pLogger, pShutdownNotifier, pCFA, pSpecification);
    automatonWriter = new AssumptionAutomatonGenerator(pConfig, pLogger);
    config = pConfig;
    logger = pLogger;
    shutdown = pShutdownNotifier;
  }

  @Override
  public void constructInternalProofRepresentation(UnmodifiableReachedSet pReached)
      throws InvalidConfigurationException, InterruptedException {
    throw new InvalidConfigurationException(
        "Interleaved proof reading and checking strategies do not  support internal PCC with result check algorithm");
  }

  @Override
  public boolean checkCertificate(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    pReachedSet.popFromWaitlist();
    if (numProofs <= 0) {
      logger.log(Level.SEVERE, "No proofs provided.");
      return false;
    }

    try {

      PropertyCheckerCPA[] cpas = new PropertyCheckerCPA[numProofs];
      CMCPartitioningIOHelper[] ioHelpers = new CMCPartitioningIOHelper[numProofs];
      AbstractState[] roots = new AbstractState[numProofs];
      AtomicBoolean checkResult = new AtomicBoolean(true);
      // we may use one semaphore only because we check and read proof parts sequentially
      Semaphore partitionsAvailable = new Semaphore(0);
      Semaphore automatonAvailable = new Semaphore(1);

      Thread readingThread =
          new Thread(
              new ProofPartReader(automatonAvailable, partitionsAvailable, checkResult, ioHelpers, cpas, roots,
                  new ReachedSetFactory(config)));
      try {
        readingThread.start();

        Pair<ARGState, List<ARGState>> checkingResult;

        CMCPartitionChecker checker;

        for (int i = 0; checkResult.get() && i < numProofs;) {
          shutdown.shutdownIfNecessary();

          partitionsAvailable.acquire();

          if(!checkResult.get()) {
            return false;
          }

          // partitioning checker
          checker = new CMCPartitionChecker(cpas[i], checkResult, shutdown, logger, roots[i]);

          // initial state coverage checked by checker
          // assume that root is first state in first partition
          for (int j = 0; j < ioHelpers[i].getNumPartitions() && checkResult.get(); j++) {
            shutdown.shutdownIfNecessary();
            partitionsAvailable.acquire();

            if(!checkResult.get()) {
              return false;
            }

            checker.checkPartition(ioHelpers[i].getPartition(j).getFirst(), ioHelpers[i].getPartition(j).getSecond(),
                ioHelpers[i].getEdgesForPartition(j), ioHelpers[i].getSavedReachedSetSize());
          }

          // check if all external nodes are checked in different partition
          if (!checker.checkCoverageOfExternalsAndInitialState()) {
            logger.log(Level.SEVERE,
                "Elements which should be checked in different partition are not checked or initial state not covered");
            return false;
          }

          // inspect safety
          if(!cpas[i].getPropChecker().satisfiesProperty(checker.getInspectedStates())){
            logger.log(Level.SEVERE, "Property violation in certificate found.");
            return false;
          }

          checkingResult = checker.getAutomatonReconstructionInfo();

          i++;
          if(i<numProofs) {
            // write assumption automaton for next round
            automatonWriter.writeAutomaton(checkingResult.getFirst(), checkingResult.getSecond());
            automatonAvailable.release();
          } else {
            if(!checkingResult.getSecond().isEmpty()) {
              return false;
            }
          }
        }
        return checkResult.get();
      } finally {
        checkResult.set(false);
        readingThread.interrupt();
      }
    } catch (InvalidConfigurationException e) {
      logger.log(Level.SEVERE, "Cannot set up infrastructure for proof checking.");
      return false;
    }
  }

  @Override
  protected void writeProofToStream(ObjectOutputStream pOut, UnmodifiableReachedSet pReached) throws IOException,
      InvalidConfigurationException, InterruptedException {
    if (!(pReached instanceof HistoryForwardingReachedSet)) { throw new InvalidConfigurationException(
        "Reached sets used by restart algorithm are not memorized. Please enable option analysis.memorizeReachedAfterRestart"); }

    List<ReachedSet> partialReachedSets =
        ((HistoryForwardingReachedSet) pReached).getAllReachedSetsUsedAsDelegates();

    if (partialReachedSets == null || partialReachedSets.isEmpty()) {
      logger.log(Level.SEVERE, "No proof parts available. Proof cannot be generated.");
      return;
    }

    List<ConfigurableProgramAnalysis> cpas = ((HistoryForwardingReachedSet) pReached).getCPAs();

    if (partialReachedSets.size() != cpas.size()) {
      logger.log(Level.SEVERE, "Analysis inconsistent. Proof cannot be generated.");
      return;
    }

    logger.log(Level.FINEST, "Write number of proof parts to proof");
    pOut.writeInt(partialReachedSets.size());

    CMCPartitioningIOHelper ioHelper;
    Set<ARGState> unexplored;
    try {
      ReachedSet reached;
      for (int i = 0; i < partialReachedSets.size(); i++) {
        GlobalInfo.getInstance().setUpInfoFromCPA(cpas.get(i));
        reached = partialReachedSets.get(i);

        unexplored = Sets.newHashSetWithExpectedSize(reached.getWaitlist().size());
        for (AbstractState toExplore : reached.getWaitlist()) {
          unexplored.add((ARGState) toExplore);
        }

        ioHelper = new CMCPartitioningIOHelper(config, logger, shutdown,
            automatonWriter.getAllAncestorsFor(unexplored), unexplored, (ARGState) reached.getFirstState());
        ioHelper.writeProof(pOut, reached);
     }
    } catch (ClassCastException e) {
      logger.log(Level.SEVERE, "Stop writing proof. Not all analysis use ARG CPA as top level CPA");
    }
  }

  @Override
  protected void readProofFromStream(ObjectInputStream pIn) throws ClassNotFoundException,
      InvalidConfigurationException, IOException {
    numProofs = pIn.readInt();
    // remaining proof parts are read interleaved, no further reading required
  }

  private class ProofPartReader implements Runnable {

    private final AtomicBoolean checkResult;
    private final Semaphore mainSemaphore, startReading;
    private final CMCPartitioningIOHelper[] ioHelperPerProofPart;
    private final PropertyCheckerCPA[] cpas;
    private final AbstractState[] roots;
    private final ReachedSetFactory factory;

    public ProofPartReader(final Semaphore pReadNext, Semaphore pPartitionsAvailable, final AtomicBoolean pCheckResult,
        final CMCPartitioningIOHelper[] pIoHelpers, final PropertyCheckerCPA[] pCpas, final AbstractState[] pRoots,
        final ReachedSetFactory pFactory) {
      startReading = pReadNext;
      checkResult = pCheckResult;
      mainSemaphore = pPartitionsAvailable;
      ioHelperPerProofPart = pIoHelpers;
      cpas = pCpas;
      roots = pRoots;
      factory = pFactory;
    }

    @Override
    public void run() {
      Triple<InputStream, ZipInputStream, ObjectInputStream> streams = null;
      try {
        streams = openProofStream();
        ObjectInputStream o = streams.getThird();
        o.readInt();

        CMCPartitioningIOHelper ioHelper;
        ConfigurableProgramAnalysis cpa;

        boolean mustReadAndCheckSequentially;

        for (int i = 0; i < numProofs && checkResult.get(); i++) {
          startReading.acquire();
          ioHelper = new CMCPartitioningIOHelper(config, logger, shutdown);
          // save helper for proof checking
          ioHelperPerProofPart[i] = ioHelper;
          // create config for reading and save for later checking
          cpa = cpaBuilder.buildPartialCPA(i, factory);
          if (!(cpa instanceof PropertyCheckerCPA)) {
            logger.log(Level.SEVERE,
                    "Conflicting configuration: Partial proofs must be checked with CPA based strategy but toplevel CPA is not a PropertyCheckerCPA as needed");
            abortPreparation();
            break;
          }
          cpas[i] = (PropertyCheckerCPA) cpa;
          GlobalInfo.getInstance().setUpInfoFromCPA(cpas[i]);

          mustReadAndCheckSequentially = CPAs.retrieveCPA(cpa, PredicateCPA.class) != null;

          ioHelper.readMetadata(o, true);
          roots[i] = ioHelper.getRoot();

          if(roots[i] == null) {
            logger.log(Level.SEVERE, "Root node not well specified in proof.");
            abortPreparation();
            break;
          }

          // release for set up checking of particular, partial proof
          if(!mustReadAndCheckSequentially) {
            mainSemaphore.release();
          }

          for (int j = 0; j < ioHelper.getNumPartitions() && checkResult.get(); j++) {
            ioHelper.readPartition(o, stats);

            if (shutdown.shouldShutdown()) {
              abortPreparation();
              break;
            }

            // release to check partition
            if(!mustReadAndCheckSequentially) {
              mainSemaphore.release();
            }
          }
          if(mustReadAndCheckSequentially) {
            mainSemaphore.release(ioHelper.getNumPartitions()+1);
          }
        }
      } catch (IOException | ClassNotFoundException e) {
        logger.logUserException(Level.SEVERE, e, "Partition reading failed. Stop checking");
        abortPreparation();
      } catch(InterruptedException exp) {
        if(checkResult.get()) {
          logger.log(Level.SEVERE, "Unexpected interrupt. Stop checking.");
          abortPreparation();
        }
      } catch (Exception e2) {
        logger.logException(Level.SEVERE, e2, "Unexpected failure during proof reading");
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
      mainSemaphore.release(2);
    }

  }

}
