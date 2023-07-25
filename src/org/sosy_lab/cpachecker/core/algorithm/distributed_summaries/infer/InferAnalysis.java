// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.infer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.BlockSummaryAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.parallel_decomposition.ParallelBlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.parallel_decomposition.ParallelBlockNodeDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.memory.InMemoryBlockSummaryConnectionProvider;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryActor;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryAnalysisOptions;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryObserverWorker.StatusAndResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryWorkerBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryWorkerBuilder.Components;
import org.sosy_lab.cpachecker.core.defaults.DummyTargetState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;

@Options(prefix = "infer")
public class InferAnalysis extends BlockSummaryAnalysis
    implements Algorithm, StatisticsProvider, Statistics {

  private final CFA initialCFA;
  private final LogManager logger;
  private final Specification specification;
  private final ShutdownManager shutdownManager;
  private final InferOptions options;
  private final BlockSummaryAnalysisOptions blockSummaryOptions;

  private final Map<String, Object> stats;

  private final StatInt numberWorkers = new StatInt(StatKind.MAX, "Number of worker");
  private final StatInt averageNumberOfEdges =
      new StatInt(StatKind.AVG, "Average number of edges in block");

  @Option(
      description =
          "Whether to spawn util workers. "
              + "Util workers listen to every message and create visual output for debugging. "
              + "Workers consume resources and should not be used for benchmarks.")
  private boolean spawnUtilWorkers = true;

  public InferAnalysis(
      Configuration pConfig,
      LogManager pLogger,
      CFA pInitialCFA,
      ShutdownManager pShutdownManager,
      Specification pSpecification)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pInitialCFA, pShutdownManager, pSpecification);
    initialCFA = pInitialCFA;
    logger = pLogger;
    specification = pSpecification;
    shutdownManager = pShutdownManager;
    options = new InferOptions(pConfig);
    blockSummaryOptions = new BlockSummaryAnalysisOptions(pConfig);
    stats = new HashMap<>();
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    logger.log(Level.INFO, "Starting infer analysis...");
    try {
      // create blockGraph and reduce to relevant parts
      ParallelBlockNodeDecomposition decomposer = new ParallelBlockNodeDecomposition();
      ParallelBlockGraph blockGraph = decomposer.decompose(initialCFA);
      blockGraph.checkConsistency(shutdownManager.getNotifier());
      logger.logf(
          Level.INFO,
          "Decomposed CFA in %d blocks using parallel decomposition.",
          blockGraph.getNodes().size());

      // create workers
      Collection<BlockNode> blocks = blockGraph.getNodes();
      int expectedRootStrengthens = blockGraph.getUniquePaths();
      String entryFunctionName = blockGraph.getEntryBlock().getFirst().getFunctionName();
      BlockSummaryWorkerBuilder builder =
          new BlockSummaryWorkerBuilder(
                  initialCFA,
                  new InMemoryBlockSummaryConnectionProvider(getQueueSupplier()),
                  specification)
              .createAdditionalConnections(1)
              .addInferRootWorker(
                  blockGraph.getRoot(),
                  options,
                  blocks.size(),
                  expectedRootStrengthens,
                  entryFunctionName);
      for (BlockNode distinctNode : blocks) {
        averageNumberOfEdges.setNextValue(distinctNode.getEdges().size());
        builder = builder.addInferWorker(distinctNode, options);
      }

      if (spawnUtilWorkers) {
        builder = builder.addVisualizationWorker(blockGraph, blockSummaryOptions);
      }

      Components components = builder.build();

      numberWorkers.setNextValue(components.actors().size());

      // run workers
      for (BlockSummaryActor worker : components.actors()) {
        Thread thread = new Thread(worker, worker.getId());
        thread.setDaemon(true);
        thread.start();
      }

      // listen to messages
      try (BlockSummaryConnection mainThreadConnection = components.connections().get(0)) {
        InferObserverWorker observer =
            new InferObserverWorker(
                "observer", mainThreadConnection, blockSummaryOptions, blocks.size());
        // blocks the thread until result message is received
        StatusAndResult resultPair = observer.observe();
        Result result = resultPair.result();
        stats.putAll(observer.getStats());
        if (result == Result.FALSE) {
          ARGState state = (ARGState) reachedSet.getFirstState();
          assert state != null;
          CompositeState cState = (CompositeState) state.getWrappedState();
          Precision initialPrecision = reachedSet.getPrecision(state);
          assert cState != null;
          List<AbstractState> states = new ArrayList<>(cState.getWrappedStates());
          states.add(DummyTargetState.withoutTargetInformation());
          reachedSet.add(new ARGState(new CompositeState(states), null), initialPrecision);
        } else if (result == Result.TRUE) {
          reachedSet.clear();
        }
        return resultPair.status();
      }
    } catch (InvalidConfigurationException | IOException e) {
      logger.logException(Level.SEVERE, e, "Infer analysis stopped unexpectedly.");
      throw new CPAException("Component Analysis ran into an error.", e);
    } finally {
      logger.log(Level.INFO, "Infer analysis finished.");
    }
  }
}
