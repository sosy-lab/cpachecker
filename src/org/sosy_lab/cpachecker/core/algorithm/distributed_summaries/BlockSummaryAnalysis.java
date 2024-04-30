// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterables;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockSummaryCFADecomposer;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BridgeDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.ImportDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.MergeBlockNodesDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.SingleBlockDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraphModification;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraphModification.Modification;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNodeWithoutGraphInformation;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.linear_decomposition.LinearBlockNodeDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryDefaultQueue;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryPrioritizeErrorConditionQueue;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage.MessageConverter;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryStatisticsMessage.BlockSummaryStatisticType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.memory.InMemoryBlockSummaryConnectionProvider;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryActor;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryAnalysisOptions;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryAnalysisWorker;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryObserverWorker;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryObserverWorker.StatusAndResult;
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
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@Options(prefix = "distributedSummaries")
public class BlockSummaryAnalysis implements Algorithm, StatisticsProvider, Statistics {

  private final Configuration configuration;
  private final LogManager logger;
  private final CFA initialCFA;
  private final ShutdownManager shutdownManager;
  private final Specification specification;
  private final BlockSummaryAnalysisOptions options;

  private final Map<String, Object> stats;

  private final StatInt numberWorkers = new StatInt(StatKind.MAX, "Number of worker");
  private final StatInt averageNumberOfEdges =
      new StatInt(StatKind.AVG, "Average number of edges in block");
  private final StatInt numberWorkersWithoutAbstraction =
      new StatInt(StatKind.MAX, "Worker without abstraction");

  private final StatTimer decompositionTimer = new StatTimer("Decomposition time");
  private final StatTimer instrumentationTimer = new StatTimer("Instrumentation time");

  @Option(
      description =
          "How workers should behave. Unlike DSS, INVARIANTS works with guessed summaries.")
  private Strategy strategy = Strategy.DSS;

  @Option(
      description =
          "Allows to set the algorithm for decomposing the CFA. LINEAR_DECOMPOSITION creates blocks"
              + " from each merge/branching point to the next merge/branching point."
              + " MERGE_DECOMPOSITION merges blocks obtained by LINEAR_DECOMPOSITION. The final"
              + " number of blocks should converge to the number of functions in the program."
              + " NO_DECOMPOSITION creates one block around the CFA.")
  private DecompositionType decompositionType = DecompositionType.MERGE_DECOMPOSITION;

  @Option(
      description =
          "Whether to spawn util workers. "
              + "Util workers listen to every message and create visual output for debugging. "
              + "Workers consume resources and should not be used for benchmarks.")
  private boolean spawnUtilWorkers = false;

  @Option(
      description =
          "Change the queue type. ERRROR_CONDITION prioritizes the processing"
              + " ofErrorConditionMessages. DEFAULT does not differ between PostCondition and"
              + " ErrorCondition messages.")
  private QueueType queue = QueueType.DEFAULT;

  @Option(
      description =
          "The number of blocks is dependent by the number of functions in the program."
              + "A tolerance of 1 means, that we subtract 1 of the total number of functions.")
  private boolean allowSingleBlockDecompositionWhenMerging = false;

  @Option(
      description =
          "Abstraction nodes are added to each block after they are created. "
              + "They are needed to strengthen the preconditions of blocks. "
              + "Missing blocks make the analysis slower but not impossible.")
  private boolean allowMissingAbstractionNodes = true;

  @FileOption(Type.OUTPUT_FILE)
  @Option(description = "Where to store the block graph in JSON format")
  private Path blockCFAFile = Path.of("block_analysis/blocks.json");

  @Option(description = "Whether to stop after exporting the blockgraph")
  private boolean generateBlockGraphOnly = false;

  @Option(description = "Import an existing decomposition from a file")
  @FileOption(Type.OPTIONAL_INPUT_FILE)
  private Path importDecomposition = null;

  @Option(description = "Whether to spawn a worker for only one block id")
  private String spawnWorkerForId = "";

  @FileOption(Type.OUTPUT_DIRECTORY)
  @Option(description = "Where to find messages in a file format")
  private Path inputMessages = Path.of("input/");

  @FileOption(Type.OUTPUT_DIRECTORY)
  @Option(description = "Where to write responses")
  private Path outputMessages = Path.of("messages/");

  private enum DecompositionType {
    LINEAR_DECOMPOSITION,
    MERGE_DECOMPOSITION,
    BRIDGE_DECOMPOSITION,
    NO_DECOMPOSITION
  }

  private enum QueueType {
    ERROR_CONDITION,
    DEFAULT
  }

  private enum Strategy {
    DSS,
    INVARIANTS
  }

  public BlockSummaryAnalysis(
      Configuration pConfig,
      LogManager pLogger,
      CFA pInitialCFA,
      ShutdownManager pShutdownManager,
      Specification pSpecification)
      throws InvalidConfigurationException {
    configuration = pConfig;
    configuration.inject(this);
    logger = pLogger;
    initialCFA = pInitialCFA;
    shutdownManager = pShutdownManager;
    specification = pSpecification;
    options = new BlockSummaryAnalysisOptions(configuration);
    stats = new HashMap<>();
  }

  private Supplier<BlockingQueue<BlockSummaryMessage>> getQueueSupplier() {
    return switch (queue) {
      case ERROR_CONDITION -> () -> new BlockSummaryPrioritizeErrorConditionQueue();
      case DEFAULT -> () -> new BlockSummaryDefaultQueue();
    };
  }

  private BlockSummaryCFADecomposer getDecomposer()
      throws InvalidConfigurationException, IOException {
    if (importDecomposition != null) {
      return new ImportDecomposition(importDecomposition);
    }
    BlockOperator blockOperator = new BlockOperator();
    configuration.inject(blockOperator);
    blockOperator.setCFA(initialCFA);
    Predicate<CFANode> isBlockEnd = n -> blockOperator.isBlockEnd(n, -1);
    return switch (decompositionType) {
      case LINEAR_DECOMPOSITION -> new LinearBlockNodeDecomposition(isBlockEnd);
      case MERGE_DECOMPOSITION -> new MergeBlockNodesDecomposition(
          new LinearBlockNodeDecomposition(isBlockEnd),
          0,
          Comparator.comparing(BlockNodeWithoutGraphInformation::getId),
          allowSingleBlockDecompositionWhenMerging);
      case BRIDGE_DECOMPOSITION -> new MergeBlockNodesDecomposition(
          new BridgeDecomposition(),
          0,
          Comparator.comparingInt(b -> b.getEdges().size()),
          allowSingleBlockDecompositionWhenMerging);
      case NO_DECOMPOSITION -> new SingleBlockDecomposition();
    };
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    logger.log(Level.INFO, "Starting block analysis...");
    try {
      // create blockGraph and reduce to relevant parts
      BlockSummaryCFADecomposer decomposer = getDecomposer();
      decompositionTimer.start();
      BlockGraph blockGraph = decomposer.decompose(initialCFA);
      decompositionTimer.stop();
      blockGraph.checkConsistency(shutdownManager.getNotifier());
      if (generateBlockGraphOnly) {
        blockGraph.export(blockCFAFile);
        logger.logf(Level.INFO, "Block graph exported to %s.", blockCFAFile);
        return AlgorithmStatus.NO_PROPERTY_CHECKED;
      }
      instrumentationTimer.start();
      Modification modification =
          BlockGraphModification.instrumentCFA(initialCFA, blockGraph, configuration, logger);
      instrumentationTimer.stop();
      ImmutableSet<CFANode> abstractionDeadEnds = modification.unableToAbstract();
      numberWorkersWithoutAbstraction.setNextValue(abstractionDeadEnds.size());
      if (!abstractionDeadEnds.isEmpty() && !allowMissingAbstractionNodes) {
        for (String successorId : blockGraph.getRoot().getSuccessorIds()) {
          for (BlockNode node : blockGraph.getNodes()) {
            if (node.getId().equals(successorId)) {
              if (node.getAbstractionLocation().equals(node.getLast())) {
                throw new AssertionError(
                    "Direct successors of the root node are required to have an abstraction"
                        + " location.");
              }
            }
          }
        }
      }
      if (!abstractionDeadEnds.isEmpty()) {
        logger.logf(Level.INFO, "Abstraction is not possible at: %s", abstractionDeadEnds);
      }
      CFA cfa = modification.cfa();
      blockGraph = modification.blockGraph();
      logger.logf(
          Level.INFO,
          "Decomposed CFA in %d blocks using the %s.",
          blockGraph.getNodes().size(),
          decompositionType);

      // create workers
      Collection<BlockNode> blocks = blockGraph.getNodes();
      if (!spawnWorkerForId.isBlank()) {
        FileUtils.deleteDirectory(outputMessages.toFile());
        int counter = 0;
        MessageConverter converter = new MessageConverter();
        BlockNode blockNode =
            blocks.stream().filter(b -> b.getId().equals(spawnWorkerForId)).findAny().orElseThrow();
        Components build =
            new BlockSummaryWorkerBuilder(
                    cfa,
                    new InMemoryBlockSummaryConnectionProvider(getQueueSupplier()),
                    specification)
                .addAnalysisWorker(blockNode, options)
                .build();
        BlockSummaryAnalysisWorker actor =
            (BlockSummaryAnalysisWorker) Iterables.getOnlyElement(build.actors());
        for (File file : Objects.requireNonNull(inputMessages.toFile().listFiles(File::isFile))) {
          for (BlockSummaryMessage blockSummaryMessage :
              actor.processMessage(converter.jsonToMessage(Files.readAllBytes(file.toPath())))) {
            Path message = outputMessages.resolve("M" + counter++ + ".txt");
            Files.write(message, converter.messageToJson(blockSummaryMessage));
          }
        }
        return AlgorithmStatus.NO_PROPERTY_CHECKED;
      }

      Components components = createComponents(cfa, blockGraph);
      if (components.connections().size() != 1) {
        throw new CPAException("Components need to provide exactly one additional connection");
      }

      numberWorkers.setNextValue(components.actors().size());

      // listen to messages
      try (BlockSummaryConnection mainThreadConnection = components.connections().get(0)) {
        FixpointNotifier.init(
            mainThreadConnection, components.connections().size() + components.actors().size());
        // run workers
        for (BlockSummaryActor worker : components.actors()) {
          Thread thread = new Thread(worker, worker.getId());
          thread.setDaemon(true);
          thread.start();
        }
        BlockSummaryObserverWorker observer =
            new BlockSummaryObserverWorker(
                "observer", mainThreadConnection, options, blocks.size());
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
      logger.logException(Level.SEVERE, e, "Block analysis stopped unexpectedly.");
      throw new CPAException("Component Analysis run into an error.", e);
    } finally {
      logger.log(Level.INFO, "Block analysis finished.");
    }
  }

  private Components createComponents(CFA pCfa, BlockGraph pBlockGraph)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    return switch (strategy) {
      case DSS -> createComponentsDSS(pCfa, pBlockGraph);
      case INVARIANTS -> createComponentsInvariants(pCfa, pBlockGraph);
    };
  }

  private Components createComponentsDSS(CFA cfa, BlockGraph blockGraph)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    ImmutableSet<BlockNode> blocks = blockGraph.getNodes();
    BlockSummaryWorkerBuilder builder =
        new BlockSummaryWorkerBuilder(
                cfa, new InMemoryBlockSummaryConnectionProvider(getQueueSupplier()), specification)
            .createAdditionalConnections(1)
            .addRootWorker(blockGraph.getRoot(), options);
    for (BlockNode distinctNode : blocks) {
      averageNumberOfEdges.setNextValue(distinctNode.getEdges().size());
      builder = builder.addAnalysisWorker(distinctNode, options);
    }
    if (spawnUtilWorkers) {
      builder = builder.addVisualizationWorker(blockGraph, options);
    }
    return builder.build();
  }

  private Components createComponentsInvariants(CFA cfa, BlockGraph blockGraph)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    ImmutableSet<BlockNode> blocks = blockGraph.getNodes();
    BlockSummaryWorkerBuilder builder =
        new BlockSummaryWorkerBuilder(
                cfa, new InMemoryBlockSummaryConnectionProvider(getQueueSupplier()), specification)
            .createAdditionalConnections(1)
            .addRootWorker(blockGraph.getRoot(), options);
    int sum =
        Integer.max(
            1,
            blocks.stream()
                .mapToInt(b -> b.getSuccessorIds().size() * b.getPredecessorIds().size())
                .sum());
    int cores = Runtime.getRuntime().availableProcessors();
    for (BlockNode distinctNode : blocks) {
      averageNumberOfEdges.setNextValue(distinctNode.getEdges().size());
      int share = distinctNode.getSuccessorIds().size() * distinctNode.getPredecessorIds().size();
      double percentage = ((double) share) / ((double) sum);
      int coresForBlock = Integer.max(1, (int) Math.round(percentage * cores));
      builder = builder.addHubWorker(distinctNode, options, shutdownManager, coresForBlock);
    }
    if (spawnUtilWorkers) {
      builder = builder.addVisualizationWorker(blockGraph, options);
    }
    return builder.build();
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
      writer = writer.endLevel();
    }
    writer = writer.put("Overall", "Sum of all blocks").beginLevel();
    for (Entry<String, Object> stringObjectEntry : overall.entrySet()) {
      writer =
          writer.put(
              BlockSummaryStatisticType.valueOf(stringObjectEntry.getKey()).getName()
                  + " (overall)",
              convert(stringObjectEntry.getKey(), stringObjectEntry.getValue().toString()));
    }
    writer
        .put(numberWorkers)
        .put(numberWorkersWithoutAbstraction)
        .put(averageNumberOfEdges)
        .put(instrumentationTimer)
        .put(decompositionTimer);
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
