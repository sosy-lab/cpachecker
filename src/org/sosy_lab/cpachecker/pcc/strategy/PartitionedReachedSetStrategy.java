// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PartitioningCheckingHelper;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.PartitionChecker;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.PartitioningIOHelper;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.PartitioningUtils;

public class PartitionedReachedSetStrategy extends AbstractStrategy {

  private final PartitioningIOHelper ioHelper;
  private final PropertyCheckerCPA cpa;
  private final ShutdownNotifier shutdownNotifier;

  public PartitionedReachedSetStrategy(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final Path pProofFile,
      final @Nullable PropertyCheckerCPA pCpa)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pProofFile);

    ioHelper = new PartitioningIOHelper(pConfig, pLogger, pShutdownNotifier);
    cpa = pCpa;
    shutdownNotifier = pShutdownNotifier;
    addPCCStatistic(ioHelper.getPartitioningStatistc());
  }

  @Override
  public boolean checkCertificate(ReachedSet pReachedSet)
      throws CPAException, InterruptedException {
    final AtomicBoolean checkResult = new AtomicBoolean(true);

    Multimap<CFANode, AbstractState> partitionNodes = HashMultimap.create();
    Collection<AbstractState> inOtherPartition = new HashSet<>();
    Collection<AbstractState> certificate =
        Sets.newHashSetWithExpectedSize(ioHelper.getSavedReachedSetSize());

    AbstractState initialState = pReachedSet.popFromWaitlist();
    Precision initPrec = pReachedSet.getPrecision(initialState);

    PartitioningCheckingHelper checkInfo =
        new PartitioningCheckingHelper() {

          @Override
          public int getCurrentCertificateSize() {
            return 0;
          }

          @Override
          public void abortCheckingPreparation() {
            checkResult.set(false);
          }
        };
    PartitionChecker checker =
        new PartitionChecker(
            initPrec,
            cpa.getStopOperator(),
            cpa.getTransferRelation(),
            ioHelper,
            checkInfo,
            shutdownNotifier,
            logger);

    for (int i = 0; i < ioHelper.getNumPartitions() && checkResult.get(); i++) {
      checker.checkPartition(i);
      checker.addCertificatePartsToCertificate(certificate);
      checker.clearPartitionElementsSavedForInspection();
    }

    if (!checkResult.get()) {
      return false;
    }

    checker.addPartitionElements(partitionNodes);
    checker.addElementsCheckedInOtherPartitions(inOtherPartition);

    logger.log(
        Level.INFO,
        "Add initial state to elements for which it will be checked if they are covered by"
            + " partition nodes of certificate.");
    inOtherPartition.add(initialState);

    logger.log(
        Level.INFO,
        "Check if initial state and all nodes which should be contained in different partition are"
            + " covered by certificate (partition node).");
    if (!PartitioningUtils.areElementsCoveredByPartitionElement(
        inOtherPartition, partitionNodes, cpa.getStopOperator(), initPrec)) {
      logger.log(
          Level.SEVERE,
          "Initial state or a state which should be in other partition is not covered by"
              + " certificate.");
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
  public void constructInternalProofRepresentation(
      UnmodifiableReachedSet pReached, ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException, InterruptedException {
    ioHelper.constructInternalProofRepresentation(pReached, pCpa);
  }

  @Override
  protected void writeProofToStream(
      ObjectOutputStream pOut, UnmodifiableReachedSet pReached, ConfigurableProgramAnalysis pCpa)
      throws IOException, InvalidConfigurationException, InterruptedException {
    ioHelper.writeProof(pOut, pReached, pCpa);
  }

  @Override
  protected void readProofFromStream(ObjectInputStream pIn)
      throws ClassNotFoundException, InvalidConfigurationException, IOException {
    ioHelper.readProof(pIn, stats);
  }

  @Override
  public Collection<Statistics> getAdditionalProofGenerationStatistics() {
    Collection<Statistics> result = new ArrayList<>(super.getAdditionalProofGenerationStatistics());
    result.add(ioHelper.getGraphStatistic());
    return result;
  }
}
