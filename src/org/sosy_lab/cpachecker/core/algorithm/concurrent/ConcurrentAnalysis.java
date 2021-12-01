// This file is part of qAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import static org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus.NO_PROPERTY_CHECKED;

import java.util.Collection;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.blockgraph.BlockGraph;
import org.sosy_lab.cpachecker.cfa.blockgraph.builder.BlockGraphBuilder;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.MessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.util.ErrorOrigin;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;

public class ConcurrentAnalysis implements Algorithm, StatisticsProvider {

  private final Algorithm algorithm;

  private final LogManager logger;

  @SuppressWarnings({"FieldCanBeLocal", "UnusedVariable"})
  private final CFA cfa;

  @SuppressWarnings({"FieldCanBeLocal", "UnusedVariable"})
  private final Configuration config;

  private final Specification specification;

  private final ShutdownManager shutdownManager;

  private final Scheduler scheduler;
  
  private ConcurrentAnalysis(
      final Algorithm pAlgorithm,
      final CFA pCFA,
      final Configuration pConfig,
      final Specification pSpecification,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier) {
    algorithm = pAlgorithm;
    cfa = pCFA;
    logger = pLogger;
    shutdownManager = ShutdownManager.createWithParent(pShutdownNotifier);
    config = pConfig;
    specification = pSpecification;

    final int processors = Runtime.getRuntime().availableProcessors();
    scheduler = new Scheduler(processors, logger, shutdownManager);
  }

  public static Algorithm create(
      final Algorithm pAlgorithm,
      final CFA pCFA,
      final Configuration pConfig,
      final Specification pSpecification,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier) {
    return new ConcurrentAnalysis(
        pAlgorithm, pCFA, pConfig, pSpecification, pLogger, pShutdownNotifier);
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    // reachedSet.clear();
    AlgorithmStatus status = NO_PROPERTY_CHECKED; // algorithm.run(reachedSet);
    
    logger.log(Level.INFO, "Starting concurrent analysis ...");

    try {
      BlockOperator blk = new BlockOperator();
      config.inject(blk);
      blk.setCFA(cfa);

      BlockGraph graph =
          BlockGraphBuilder.create(shutdownManager.getNotifier()).build(cfa.getMainFunction(), blk);

      MessageFactory messageFactory =
          MessageFactory.factory()
              .set(config, Configuration.class)
              .set(specification, Specification.class)
              .set(logger, LogManager.class)
              .set(shutdownManager.getNotifier(), ShutdownNotifier.class)
              .set(cfa, CFA.class)
              .set(scheduler, Scheduler.class)
              .createInstance();

      for (final Block block : graph.getBlocks()) {
        messageFactory.sendForwardAnalysisRequest(block);
      }

      Optional<ErrorOrigin> error = Optional.empty();
      scheduler.start();
      try {
        error = scheduler.waitForCompletion();  
      } catch(final Throwable throwable) {
        logger.log(WARNING, "Unexpected exception while waiting for analysis completion.");
      }
              
      if (error.isPresent()) {
        reachedSet.addNoWaitlist(error.orElseThrow().getState(), error.orElseThrow().getPrecision());
      }
      
      status = scheduler.getStatus();
    } catch (InvalidConfigurationException exception) {
      logger.log(SEVERE, "Invalid configuration:", exception.getMessage());
      status = NO_PROPERTY_CHECKED;
    }

    logger.log(Level.INFO, "Stopping concurrent analysis ...");
    return status;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    if(algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider) algorithm).collectStatistics(statsCollection);
    }
    
    scheduler.collectStatistics(statsCollection);
  }
}
