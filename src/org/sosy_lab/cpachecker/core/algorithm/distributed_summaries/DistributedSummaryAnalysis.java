// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockOperatorDecomposer;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.CFADecomposer;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.GivenSizeDecomposer;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.SingleBlockDecomposer;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummarySortedMessageQueue;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.memory.InMemoryBlockSummaryConnectionProvider;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryActor;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryAnalysisOptions;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryObserverWorker;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryWorkerBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryWorkerBuilder.Components;
import org.sosy_lab.cpachecker.core.defaults.DummyTargetState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;

@Options(prefix = "distributedSummaries")
public class DistributedSummaryAnalysis implements Algorithm {

  private final Configuration configuration;
  private final LogManager logger;
  private final CFA cfa;
  private final ShutdownManager shutdownManager;
  private final Specification specification;
  private final BlockSummaryAnalysisOptions options;

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
    SMART
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
    options = new BlockSummaryAnalysisOptions(configuration);
  }

  private CFADecomposer getDecomposer() throws InvalidConfigurationException {
    switch (decompositionType) {
      case BLOCK_OPERATOR:
        return new BlockOperatorDecomposer(configuration, shutdownManager.getNotifier());
      case GIVEN_SIZE:
        return new GivenSizeDecomposer(
            new BlockOperatorDecomposer(configuration, shutdownManager.getNotifier()),
            desiredNumberOfBlocks);
      case SINGLE_BLOCK:
        return new SingleBlockDecomposer(shutdownManager.getNotifier());
      default:
        throw new AssertionError("Unknown DecompositionType: " + decompositionType);
    }
  }

  private BlockSummaryWorkerBuilder analysisWorker(
      BlockSummaryWorkerBuilder pBuilder, BlockNode pNode) {
    switch (workerType) {
      case DEFAULT:
        return pBuilder.addAnalysisWorker(pNode, options);
      case SMART:
        return pBuilder.addSmartAnalysisWorker(pNode, options);
      default:
        throw new AssertionError("Unknown WorkerType: " + workerType);
    }
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    logger.log(Level.INFO, "Starting block analysis...");
    try {
      // create blockGraph and reduce to relevant parts
      CFADecomposer decomposer = getDecomposer();
      BlockGraph blockGraph = decomposer.cut(cfa);
      logger.logf(
          Level.INFO,
          "Decomposed CFA in %d blocks using the %s.",
          blockGraph.getDistinctNodes().size(),
          decomposer.getClass().getCanonicalName());

      // create workers
      Collection<BlockNode> blocks = blockGraph.getDistinctNodes();
      BlockSummaryWorkerBuilder builder =
          new BlockSummaryWorkerBuilder(
              cfa,
              new InMemoryBlockSummaryConnectionProvider(
                  () -> new BlockSummarySortedMessageQueue()),
              specification,
              configuration,
              shutdownManager);
      builder = builder.createAdditionalConnections(1);
      for (BlockNode distinctNode : blocks) {
        if (distinctNode.isRoot()) {
          builder = builder.addRootWorker(distinctNode, options);
        } else {
          builder = analysisWorker(builder, distinctNode);
        }
      }
      builder = builder.addResultCollectorWorker(blocks, options);

      if (spawnUtilWorkers) {
        builder = builder.addVisualizationWorker(blockGraph, options);
      }

      Components components = builder.build();

      numberWorkers.setNextValue(components.getWorkers().size());

      // run workers
      for (BlockSummaryActor worker : components.getWorkers()) {
        Thread thread = new Thread(worker, worker.getId());
        thread.setDaemon(true);
        thread.start();
      }

      // listen to messages
      try (BlockSummaryConnection mainThreadConnection =
          components.getAdditionalConnections().get(0)) {
        BlockSummaryObserverWorker observer =
            new BlockSummaryObserverWorker("observer", mainThreadConnection, options);
        Pair<AlgorithmStatus, Result> resultPair = observer.observe();
        Result result = resultPair.getSecond();
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
        return resultPair.getFirst();
      }
    } catch (InvalidConfigurationException | IOException pE) {
      logger.logException(Level.SEVERE, pE, "Block analysis stopped unexpectedly.");
      throw new CPAException("Component Analysis run into an error.", pE);
    } finally {
      logger.log(Level.INFO, "Block analysis finished.");
    }
  }
}
