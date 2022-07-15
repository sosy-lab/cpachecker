// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy.arg;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.zip.ZipInputStream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.HistoryForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.pcc.strategy.AbstractStrategy;
import org.sosy_lab.cpachecker.pcc.strategy.util.cmc.AssumptionAutomatonGenerator;
import org.sosy_lab.cpachecker.pcc.strategy.util.cmc.PartialCPABuilder;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

public class ARG_CMCStrategy extends AbstractStrategy {

  private final Configuration globalConfig;
  private final ShutdownNotifier shutdown;
  private final PartialCPABuilder cpaBuilder;
  private final AssumptionAutomatonGenerator automatonWriter;

  private ARGState[] roots;
  private boolean proofKnown = false;

  public ARG_CMCStrategy(
      Configuration pConfig,
      LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final Path pProofFile,
      final @Nullable CFA pCfa,
      final @Nullable Specification pSpecification)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pProofFile);
    // pConfig.inject(this);
    globalConfig = pConfig;
    shutdown = pShutdownNotifier;
    cpaBuilder = new PartialCPABuilder(pConfig, pLogger, pShutdownNotifier, pCfa, pSpecification);
    automatonWriter = new AssumptionAutomatonGenerator(pConfig, pLogger);
  }

  @Override
  public void constructInternalProofRepresentation(
      UnmodifiableReachedSet pReached, ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException, InterruptedException {
    if (!(pReached instanceof HistoryForwardingReachedSet)) {
      throw new InvalidConfigurationException(
          "Reached sets used by restart algorithm are not memorized. Please enable option"
              + " analysis.memorizeReachedAfterRestart");
    }

    Collection<ReachedSet> partialReachedSets =
        ((HistoryForwardingReachedSet) pReached).getAllReachedSetsUsedAsDelegates();
    roots = new ARGState[partialReachedSets.size()];

    if (roots.length <= 0) {
      logger.log(Level.SEVERE, "No proof parts available. Proof cannot be generated.");
      return;
    }

    int index = 0;
    for (ReachedSet partialReached : partialReachedSets) {
      if (!(partialReached.getFirstState() instanceof ARGState)
          || (extractLocation(partialReached.getFirstState()) == null)) {
        logger.log(
            Level.SEVERE,
            "Proof cannot be generated because checked property not known to be true.");
        roots = null;
        proofKnown = false;
        return;
      } else {
        stats.increaseProofSize(1);
        roots[index++] = (ARGState) partialReached.getFirstState();
      }
    }

    proofKnown = true;
  }

  @Override
  protected void writeProofToStream(
      ObjectOutputStream pOut, UnmodifiableReachedSet pReached, ConfigurableProgramAnalysis pCpa)
      throws IOException, InvalidConfigurationException, InterruptedException {
    constructInternalProofRepresentation(pReached, pCpa);
    if (proofKnown) {
      HistoryForwardingReachedSet historyReached = (HistoryForwardingReachedSet) pReached;
      if (historyReached.getAllReachedSetsUsedAsDelegates().size()
          != historyReached.getCPAs().size()) {
        logger.log(
            Level.SEVERE,
            "Proof cannot be generated, inconsistency in number of analyses, contradicting number"
                + " of CPAs and reached sets.");
      }
      // proof construction succeeded
      pOut.writeInt(roots.length);

      for (int i = 0; i < historyReached.getCPAs().size(); i++) {
        GlobalInfo.getInstance().setUpInfoFromCPA(historyReached.getCPAs().get(i));
        pOut.writeObject(roots[i]);
      }
    }
  }

  @Override
  protected void readProofFromStream(ObjectInputStream pIn)
      throws ClassNotFoundException, InvalidConfigurationException, IOException {
    roots = new ARGState[Math.max(pIn.readInt(), 0)];
  }

  @Override
  public boolean checkCertificate(ReachedSet pReachedSet)
      throws CPAException, InterruptedException {
    logger.log(Level.INFO, "Start checking partial ARGs");
    pReachedSet.popFromWaitlist();

    return checkAndReadSequentially();
  }

  @SuppressWarnings("Finally") // not really better doable without switching to Closer
  private boolean checkAndReadSequentially() throws InterruptedException {
    try {
      final ReachedSetFactory factory = new ReachedSetFactory(globalConfig, logger);
      List<ARGState> incompleteStates = new ArrayList<>();
      ConfigurableProgramAnalysis cpa;

      Triple<InputStream, ZipInputStream, ObjectInputStream> streams = null;
      try {
        streams = openProofStream();
        ObjectInputStream o = streams.getThird();
        o.readInt();

        Object readARG;
        for (int i = 0; i < roots.length; i++) {
          logger.log(Level.FINEST, "Build CPA for reading and checking partial ARG", i);
          cpa = cpaBuilder.buildPartialCPA(i, factory);
          GlobalInfo.getInstance().setUpInfoFromCPA(cpa);
          readARG = o.readObject();
          if (!(readARG instanceof ARGState)) {
            return false;
          }

          roots[i] = (ARGState) readARG;

          incompleteStates.clear();
          shutdown.shutdownIfNecessary();

          // check current partial ARG
          logger.log(Level.INFO, "Start checking partial ARG ", i);
          if (roots[i] == null
              || !checkPartialARG(factory.create(cpa), roots[i], incompleteStates, i, cpa)) {
            logger.log(Level.FINE, "Checking of partial ARG ", i, " failed.");
            return false;
          }
          shutdown.shutdownIfNecessary();

          if (i + 1 != roots.length) {
            // write automaton for next partial ARG
            logger.log(
                Level.FINE,
                "Write down report of non-checked states which is provided to next partial ARG"
                    + " check. Report is given by assumption automaton.");
            automatonWriter.writeAutomaton(roots[i], incompleteStates);
            shutdown.shutdownIfNecessary();
          }
          logger.log(Level.INFO, "Checking of partial ARG ", i, " finished");
        }
      } catch (IOException | ClassNotFoundException e) {
        logger.logUserException(Level.SEVERE, e, "Partition reading failed. Stop checking");
        return false;
      } catch (InvalidConfigurationException e) {
        logger.log(Level.SEVERE, "Could not set up a configuration for partial ARG checking");
        return false;
      } catch (CPAException e2) {
        logger.logException(Level.SEVERE, e2, "Failure during proof reading or checking");
        return false;
      } finally {
        logger.log(Level.INFO, "Stop checking partial ARGs");
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

      return incompleteStates.isEmpty() && roots.length > 0;

    } catch (InvalidConfigurationException e1) {
      logger.log(Level.SEVERE, "Cannot create reached sets for partial ARG checking", e1);
      return false;
    }
  }

  @SuppressWarnings({"unused", "Finally"}) // not really better doable without switching to Closer
  private boolean checkAndReadInterleaved() throws InterruptedException, CPAException {
    final ConfigurableProgramAnalysis[] cpas = new ConfigurableProgramAnalysis[roots.length];
    try {
      final ReachedSetFactory factory = new ReachedSetFactory(globalConfig, logger);
      final AtomicBoolean checkResult = new AtomicBoolean(true);
      final Semaphore partitionsAvailable = new Semaphore(0);

      Thread readerThread =
          new Thread(
              new Runnable() {

                @Override
                public void run() {
                  Triple<InputStream, ZipInputStream, ObjectInputStream> streams = null;
                  try {
                    streams = openProofStream();
                    ObjectInputStream o = streams.getThird();
                    o.readInt();

                    Object readARG;
                    for (int i = 0; i < roots.length && checkResult.get(); i++) {
                      logger.log(Level.FINEST, "Build CPA for correctly reading ", i);
                      cpas[i] = cpaBuilder.buildPartialCPA(i, factory);
                      GlobalInfo.getInstance().setUpInfoFromCPA(cpas[i]);
                      readARG = o.readObject();
                      if (!(readARG instanceof ARGState)) {
                        abortPreparation();
                      }

                      roots[i] = (ARGState) readARG;

                      if (shutdown.shouldShutdown()) {
                        abortPreparation();
                        break;
                      }
                      partitionsAvailable.release();
                    }
                  } catch (IOException | ClassNotFoundException e) {
                    logger.logUserException(
                        Level.SEVERE, e, "Partition reading failed. Stop checking");
                    abortPreparation();
                  } catch (Exception e2) {
                    logger.logException(
                        Level.SEVERE, e2, "Unexpected failure during proof reading");
                    abortPreparation();
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

                private void abortPreparation() {
                  checkResult.set(false);
                  partitionsAvailable.release();
                }
              });

      try {
        if (proofKnown) {
          partitionsAvailable.release(roots.length);
        } else {
          readerThread.start();
        }

        List<ARGState> incompleteStates = new ArrayList<>();

        // check partial ARGs
        for (int i = 0; i < roots.length && checkResult.get(); i++) {
          // wait until next partial ARG is read
          partitionsAvailable.acquire();
          incompleteStates.clear();
          shutdown.shutdownIfNecessary();

          // check current partial ARG
          logger.log(Level.INFO, "Start checking partial ARG ", i);
          if (!checkResult.get()
              || roots[i] == null
              || !checkPartialARG(
                  factory.create(cpas[i]), roots[i], incompleteStates, i, cpas[i])) {
            logger.log(Level.FINE, "Checking of partial ARG ", i, " failed.");
            return false;
          }
          shutdown.shutdownIfNecessary();

          if (i + 1 != roots.length) {
            // write automaton for next partial ARG
            logger.log(
                Level.FINE,
                "Write down report of non-checked states which is provided to next partial ARG"
                    + " check. Report is given by assumption automaton.");
            automatonWriter.writeAutomaton(roots[i], incompleteStates);
            shutdown.shutdownIfNecessary();
          }
          logger.log(Level.INFO, "Checking of partial ARG ", i, " finished");
        }

        return checkResult.get() && incompleteStates.isEmpty() && roots.length > 0;

      } catch (InvalidConfigurationException e) {
        logger.log(Level.SEVERE, "Could not set up a configuration for partial ARG checking");
      } finally {
        logger.log(Level.INFO, "Stop checking partial ARGs");
        checkResult.set(false);
        readerThread.interrupt();
      }
    } catch (InvalidConfigurationException e1) {
      logger.log(Level.SEVERE, "Cannot create reached sets for partial ARG checking", e1);
      return false;
    }

    return false;
  }

  private boolean checkPartialARG(
      ReachedSet pReachedSet,
      ARGState pRoot,
      List<ARGState> pIncompleteStates,
      int iterationNumber,
      ConfigurableProgramAnalysis cpa)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    logger.log(Level.FINER, "Set up proof checking for partial ARG ", iterationNumber);
    // set up proof checker
    logger.log(Level.FINEST, "Initialize reached set");
    CFANode mainFun = AbstractStates.extractLocation(pRoot);
    if (mainFun == null) {
      throw new InvalidConfigurationException(
          "Require that ARG states contain location information.");
    }
    pReachedSet.add(
        cpa.getInitialState(mainFun, StateSpacePartition.getDefaultPartition()),
        cpa.getInitialPrecision(mainFun, StateSpacePartition.getDefaultPartition()));

    AbstractARGStrategy partialProofChecker;
    logger.log(Level.FINEST, "Build checking instance");
    // require ARG_CPA strategy because the other ARG based strategy does not take strengthening
    // into account
    // strengthening is required for assumption guiding CPA
    Preconditions.checkState(
        cpa instanceof PropertyCheckerCPA,
        "Conflicting configuration: Partial ARGs must be checked with CPA based strategy but"
            + " toplevel CPA is not a PropertyCheckerCPA as needed");
    partialProofChecker =
        new ARG_CPAStrategy(globalConfig, logger, shutdown, proofFile, (PropertyCheckerCPA) cpa);

    logger.log(Level.FINER, "Start checking algorithm for partial ARG ", iterationNumber);
    return partialProofChecker.checkCertificate(pReachedSet, pRoot, pIncompleteStates);
  }
}
