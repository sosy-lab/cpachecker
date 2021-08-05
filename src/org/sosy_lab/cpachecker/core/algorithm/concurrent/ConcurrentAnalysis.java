// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent;

import com.google.common.collect.ImmutableList;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.blockgraph.BlockGraph;
import org.sosy_lab.cpachecker.cfa.blockgraph.builder.BlockGraphBuilder;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.Task;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.TaskFactory;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;

public class ConcurrentAnalysis implements Algorithm {

  private final Algorithm algorithm;

  private final LogManager logger;

  @SuppressWarnings({"FieldCanBeLocal","UnusedVariable"})
  private final CFA cfa;

  @SuppressWarnings({"FieldCanBeLocal","UnusedVariable"})
  private final ShutdownNotifier shutdownNotifier;

  @SuppressWarnings({"FieldCanBeLocal","UnusedVariable"})
  private final Configuration config;

  private final Specification specification;

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
    shutdownNotifier = pShutdownNotifier;
    config = pConfig;
    specification = pSpecification;
  }

  public static Algorithm create(
      final Algorithm pAlgorithm,
      final CFA pCFA,
      final Configuration pConfig,
      final Specification pSpecification,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    return new ConcurrentAnalysis(pAlgorithm, pCFA, pConfig, pSpecification, pLogger, pShutdownNotifier);
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    AlgorithmStatus status = algorithm.run(reachedSet);

    logger.log(Level.INFO, "Starting concurrent analysis ...");

    try {
      BlockOperator blk = new BlockOperator();
      config.inject(blk);
      blk.setCFA(cfa);

      BlockGraph graph = BlockGraphBuilder.create(shutdownNotifier).build(cfa.getMainFunction(), blk);

      final Block entry =  graph.getEntry();

      final int processors = Runtime.getRuntime().availableProcessors();
      JobExecutor executor = JobExecutor.startJobExecutor(processors, ImmutableList.of(), logger);

      TaskFactory taskFactory = new TaskFactory().set(config, Configuration.class)
          .set(specification, Specification.class).set(logger, LogManager.class)
          .set(shutdownNotifier, ShutdownNotifier.class).set(cfa, CFA.class)
          .set(executor, JobExecutor.class);

      // Solver solver = Solver.create(config, logger, shutdownNotifier);
      // BooleanFormulaManager formulaManager = solver.getFormulaManager().getBooleanFormulaManager();

      Task task = taskFactory.createForwardAnalysis(entry);

      executor.requestJob(task);
      executor.start();
    } catch(InvalidConfigurationException ignored) {
      logger.log(Level.SEVERE, "Invalid configuration.");
    }

    // TODO: Wait for completion!
    // logger.log(Level.INFO, "Stopping concurrent analysis (finished) ...");
    return status;
  }
}
