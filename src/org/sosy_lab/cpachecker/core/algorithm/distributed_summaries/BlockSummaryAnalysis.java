// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraphBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockOperatorPredicate;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.CFADecomposer;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.KnownBlockEndsDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.MergeDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.SingleBlockDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummarySortedMessageQueue;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryStatisticsMessage.BlockSummaryStatisticType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.memory.InMemoryBlockSummaryConnectionProvider;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryActor;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryAnalysisOptions;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryObserverWorker;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryWorkerBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryWorkerBuilder.Components;
import org.sosy_lab.cpachecker.core.defaults.DummyTargetState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@Options(prefix = "distributedSummaries")
public class BlockSummaryAnalysis implements Algorithm, StatisticsProvider, Statistics {

  private final Configuration configuration;
  private final LogManager logger;
  private final CFA cfa;
  private final ShutdownManager shutdownManager;
  private final Specification specification;
  private final BlockSummaryAnalysisOptions options;

  private final Map<String, Object> stats;

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
  private int desiredNumberOfBlocks = 0;

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

  public BlockSummaryAnalysis(
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
    stats = new HashMap<>();
  }

  private CFADecomposer getDecomposer() throws InvalidConfigurationException {
    switch (decompositionType) {
      case BLOCK_OPERATOR:
        return new KnownBlockEndsDecomposition(
            shutdownManager.getNotifier(), new BlockOperatorPredicate(cfa, configuration));
      case GIVEN_SIZE:
        return new MergeDecomposition(
            new KnownBlockEndsDecomposition(
                shutdownManager.getNotifier(), new BlockOperatorPredicate(cfa, configuration)),
            desiredNumberOfBlocks,
            shutdownManager.getNotifier());
      case SINGLE_BLOCK:
        return new SingleBlockDecomposition(shutdownManager.getNotifier());
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
      BlockGraphBuilder graphBuilder =
          new BlockGraphBuilder(cfa, decomposer, shutdownManager, configuration, logger);
      Pair<BlockGraph, CFA> blockGraphCFAPair = graphBuilder.build();
      BlockGraph blockGraph = blockGraphCFAPair.getFirstNotNull();
      CFA mutatedCFA = blockGraphCFAPair.getSecondNotNull();
      logger.logf(
          Level.INFO,
          "Decomposed CFA in %d blocks using the %s.",
          blockGraph.getDistinctNodes().size(),
          decomposer.getClass().getCanonicalName());

      // create workers
      Collection<BlockNode> blocks = blockGraph.getDistinctNodes();
      BlockSummaryWorkerBuilder builder =
          new BlockSummaryWorkerBuilder(
              mutatedCFA,
              new InMemoryBlockSummaryConnectionProvider(
                  () -> new BlockSummarySortedMessageQueue()),
              specification,
              configuration);
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
            new BlockSummaryObserverWorker(
                "observer", mainThreadConnection, options, blocks.size());
        Pair<AlgorithmStatus, Result> resultPair = observer.observe();
        Result result = resultPair.getSecond();
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
        return resultPair.getFirst();
      }
    } catch (InvalidConfigurationException | IOException pE) {
      logger.logException(Level.SEVERE, pE, "Block analysis stopped unexpectedly.");
      throw new CPAException(getClass().getSimpleName() + " run into an error.", pE);
    } finally {
      logger.log(Level.INFO, "Block analysis finished.");
    }
  }

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(out);
    Map<String, Object> overall = new HashMap<>();
    for (String blockID : ImmutableList.sortedCopyOf(stats.keySet())) {
      writer = writer.put("BlockID " + blockID, blockID).beginLevel();
      Object mapObject = stats.get(blockID);
      Map<?, ?> map = new LinkedHashMap<>((Map<?, ?>) mapObject);
      Map<?, ?> forwardMap =
          ImmutableSortedMap.copyOf(
              (Map<?, ?>) map.remove(BlockSummaryStatisticType.FORWARD_ANALYSIS_STATS.name()));
      Map<?, ?> backwardMap =
          ImmutableSortedMap.copyOf(
              (Map<?, ?>) map.remove(BlockSummaryStatisticType.BACKWARD_ANALYSIS_STATS.name()));
      for (Entry<?, ?> entry : map.entrySet()) {
        writer =
            writer.put(
                BlockSummaryStatisticType.valueOf(entry.getKey().toString()).getName(),
                convert(entry.getKey().toString(), entry.getValue().toString()));
        mergeInto(overall, entry.getKey().toString(), entry.getValue());
      }
      for (Entry<?, ?> entry : forwardMap.entrySet()) {
        writer =
            writer.put(
                BlockSummaryStatisticType.valueOf(entry.getKey().toString()).getName()
                    + " (forward)",
                convert(entry.getKey().toString(), entry.getValue().toString()));
        mergeInto(overall, entry.getKey().toString(), entry.getValue());
      }
      for (Entry<?, ?> entry : backwardMap.entrySet()) {
        writer =
            writer.put(
                BlockSummaryStatisticType.valueOf(entry.getKey().toString()).getName()
                    + " (backward)",
                convert(entry.getKey().toString(), entry.getValue().toString()));
        mergeInto(overall, entry.getKey().toString(), entry.getValue());
      }
      writer = writer.endLevel();
    }
    writer = writer.put("Overall", "Sum of all blocks").beginLevel();
    for (Entry<String, Object> stringObjectEntry : overall.entrySet()) {
      writer =
          writer.put(
              BlockSummaryStatisticType.valueOf(stringObjectEntry.getKey()).getName(),
              convert(stringObjectEntry.getKey(), stringObjectEntry.getValue().toString()));
    }
  }

  private String convert(String pKey, String pNumber) {
    if (BlockSummaryStatisticType.valueOf(pKey).isFormatAsTime()) {
      return TimeSpan.ofNanos(Long.parseLong(pNumber)).formatAs(TimeUnit.SECONDS);
    }
    return pNumber;
  }

  private void mergeInto(Map<String, Object> pOverall, String pKey, Object pValue) {
    pOverall.merge(
        pKey, pValue, (v1, v2) -> Long.parseLong(v1.toString()) + Long.parseLong(v2.toString()));
  }

  @Override
  public @Nullable String getName() {
    return "Distributed Summary Analysis";
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(this);
  }
}
