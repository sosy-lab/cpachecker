// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries;

import static com.google.common.collect.FluentIterable.from;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockOperatorDecomposer;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockTree;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.CFADecomposer;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.GivenSizeDecomposer;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.SingleBlockDecomposer;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Connection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.UpdatedTypeMap;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.observer.ErrorMessageObserver;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.observer.FaultLocalizationMessageObserver;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.observer.MessageObserverSupport;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.observer.ResultMessageObserver;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.observer.StatusObserver;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.AnalysisOptions;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryActor;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DistributedComponentsBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DistributedComponentsBuilder.Components;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@Options(prefix = "distributedSummaries")
public class DistributedSummaryAnalysis implements Algorithm, StatisticsProvider, Statistics {

  private final Configuration configuration;
  private final LogManager logger;
  private final CFA cfa;
  private final ShutdownManager shutdownManager;
  private final Specification specification;
  private final AnalysisOptions options;

  private Collection<Statistics> statsCollection;

  private final StatInt numberWorkers = new StatInt(StatKind.MAX, "number of workers");

  @Option(
      description =
          "Allows to set the algorithm for decomposing the CFA. BLOCK_OPERATOR creates blocks from"
              + " each merge/branching point to the next merge/branching point. GIVEN_SIZE merges"
              + " blocks obtained by BLOCK_OPERATOR until"
              + " distributedSummaries.desiredNumberOfBlocks blocks are present. SINGLE_BLOCK"
              + " creates one block around the complete CFA.")
  private DecompositionType decompositionType = DecompositionType.BLOCK_OPERATOR;

  @Option(
      description =
          "Choose the workers that are spawned for each block. Contrary to DEFAULT workers, SMART"
              + " workers consume multiple messages at once.")
  private WorkerType workerType = WorkerType.DEFAULT;

  @Option(description = "desired number of BlockNodes")
  private int desiredNumberOfBlocks = 5;

  @Option(
      description =
          "Whether to spawn util workers. "
              + "Util workers listen to every message and create visual output for debugging. "
              + "Workers consume resources and should not be used for benchmarks.")
  private boolean spawnUtilWorkers = true;

  private enum DecompositionType {
    BLOCK_OPERATOR,
    GIVEN_SIZE,
    SINGLE_BLOCK
  }

  private enum WorkerType {
    DEFAULT,
    SMART,
    FAULT_LOCALIZATION
  }

  public DistributedSummaryAnalysis(
      Configuration pConfig,
      LogManager pLogger,
      CFA pCfa,
      ShutdownManager pShutdownManager,
      Specification pSpecification)
      throws InvalidConfigurationException {
    configuration = pConfig;
    configuration.inject(this);
    logger = pLogger;
    cfa = pCfa;
    shutdownManager = pShutdownManager;
    specification = pSpecification;
    options = new AnalysisOptions(configuration);
    checkConfig();
  }

  /**
   * Currently, fault localization worker require linear blocks
   *
   * @throws InvalidConfigurationException if configuration for block analysis is invalid
   */
  private void checkConfig() throws InvalidConfigurationException {
    if (workerType == WorkerType.FAULT_LOCALIZATION) {
      if (decompositionType != DecompositionType.BLOCK_OPERATOR) {
        throw new InvalidConfigurationException(
            "FaultLocalizationWorker needs decomposition with type "
                + DecompositionType.BLOCK_OPERATOR
                + " but got "
                + decompositionType);
      }
    } else {
      if (options.isFlPreconditionAlwaysTrue()) {
        throw new InvalidConfigurationException(
            "Unused option: faultLocalizationPreconditionAlwaysTrue. Fault localization is"
                + " deactivated");
      }
    }
  }

  private CFADecomposer getDecomposer() throws InvalidConfigurationException {
    switch (decompositionType) {
      case BLOCK_OPERATOR:
        return new BlockOperatorDecomposer(configuration);
      case GIVEN_SIZE:
        return new GivenSizeDecomposer(
            new BlockOperatorDecomposer(configuration), desiredNumberOfBlocks);
      case SINGLE_BLOCK:
        return new SingleBlockDecomposer();
      default:
        throw new AssertionError("Unknown DecompositionType: " + decompositionType);
    }
  }

  private DistributedComponentsBuilder analysisWorker(
      DistributedComponentsBuilder pBuilder, BlockNode pNode, UpdatedTypeMap pMap) {
    switch (workerType) {
      case DEFAULT:
        return pBuilder.addAnalysisWorker(pNode, options, pMap);
      case SMART:
        return pBuilder.addSmartAnalysisWorker(pNode, options, pMap);
      case FAULT_LOCALIZATION:
        throw new AssertionError("Fault localization is not supported");
        // return pBuilder.addFaultLocalizationWorker(pNode, pMap, options);
      default:
        throw new AssertionError("Unknown WorkerType: " + workerType);
    }
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    logger.log(Level.INFO, "Starting block analysis...");
    try {
      // create block tree and reduce to relevant parts
      CFADecomposer decomposer = getDecomposer();
      BlockTree tree = decomposer.cut(cfa);
      logger.logf(
          Level.INFO,
          "Decomposed CFA in %d blocks using the %s.",
          tree.getDistinctNodes().size(),
          decomposer.getClass().getCanonicalName());

      // create type map (maps variables to their type)
      SSAMap ssaMap = getTypeMap(tree);
      UpdatedTypeMap map = new UpdatedTypeMap(ssaMap);

      // create workers
      Collection<BlockNode> blocks = tree.getDistinctNodes();
      DistributedComponentsBuilder builder =
          new DistributedComponentsBuilder(cfa, specification, configuration, shutdownManager);
      builder = builder.createAdditionalConnections(1);
      for (BlockNode distinctNode : blocks) {
        if (distinctNode.isRoot()) {
          builder = builder.addRootWorker(distinctNode, options, map);
        } else {
          builder = analysisWorker(builder, distinctNode, map);
        }
      }
      builder = builder.addResultCollectorWorker(blocks, options);

      if (spawnUtilWorkers) {
        builder = builder.addVisualizationWorker(tree, options, configuration);
      }

      Components components = builder.build();

      numberWorkers.setNextValue(components.getWorkers().size());

      // run workers
      for (BlockSummaryActor worker : components.getWorkers()) {
        Thread thread = new Thread(worker, worker.getId());
        thread.setDaemon(true);
        thread.start();
      }

      // create message listener
      MessageObserverSupport listener = new MessageObserverSupport();
      listener.register(new ResultMessageObserver(reachedSet));
      listener.register(new ErrorMessageObserver());
      listener.register(new StatusObserver());

      // listen to messages
      try (Connection mainThreadConnection = components.getAdditionalConnections().get(0)) {
        mainThreadConnection.collectStatistics(statsCollection);
        if (workerType == WorkerType.FAULT_LOCALIZATION) {
          listener.register(
              new FaultLocalizationMessageObserver(logger, mainThreadConnection, configuration));
        }

        // wait for result
        while (true) {
          // breaks if one observer wants to finish.
          if (listener.process(mainThreadConnection.read())) {
            break;
          }
        }

        // finish and shutdown
        listener.finish();

        // shutting down workers is not necessary because they will react to result/error messages
        // correctly themselves. (components.getWorkers().forEach(Worker::shutdown);)
      }

      return listener.getObserver(StatusObserver.class).getStatus();
    } catch (InvalidConfigurationException | IOException pE) {
      logger.logException(Level.SEVERE, pE, "Block analysis stopped unexpectedly.");
      throw new CPAException("Component Analysis run into an error.", pE);
    } finally {
      logger.log(Level.INFO, "Block analysis finished.");
    }
  }

  private SSAMap getTypeMap(BlockTree pTree)
      throws InvalidConfigurationException, CPATransferException, InterruptedException {
    Solver solver = Solver.create(configuration, logger, shutdownManager.getNotifier());
    PathFormulaManagerImpl manager =
        new PathFormulaManagerImpl(
            solver.getFormulaManager(),
            configuration,
            logger,
            shutdownManager.getNotifier(),
            cfa,
            AnalysisDirection.FORWARD);
    return manager
        .makeFormulaForPath(
            from(pTree.getDistinctNodes()).transformAndConcat(b -> b.getEdgesInBlock()).toList())
        .getSsa();
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatisticsCollection) {
    statsCollection = pStatisticsCollection;
    pStatisticsCollection.add(this);
  }

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    StatisticsWriter.writingStatisticsTo(out).put(numberWorkers);
  }

  @Override
  public @Nullable String getName() {
    return "DistributedSummaryAnalysis";
  }
}
