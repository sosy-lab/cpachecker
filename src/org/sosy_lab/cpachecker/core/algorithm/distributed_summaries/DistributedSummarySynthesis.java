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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BridgeDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.DssBlockDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.ImportDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.MergeBlockNodesDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.SingleBlockDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.VerticalMergeDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraphModification;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraphModification.Modification;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNodeWithoutGraphInformation;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.linear_decomposition.LinearBlockNodeDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DssConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DssDefaultQueue;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DssPrioritizeViolationConditionQueue;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssMessage.MessageConverter;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssMessage.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssStatisticsMessage.DssStatisticType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.memory.InMemoryDssConnectionProvider;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssActor;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisWorker;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssObserverWorker;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssObserverWorker.StatusAndResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssWorkerBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssWorkerBuilder.Components;
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
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "distributedSummaries")
public class DistributedSummarySynthesis implements Algorithm, StatisticsProvider, Statistics {

  private record OldAndNewMessages(List<DssMessage> oldMessages, List<DssMessage> newMessages) {}

  private final Configuration configuration;
  private final DssMessageFactory messageFactory;
  private final LogManager logger;
  private final CFA initialCFA;
  private final ShutdownManager shutdownManager;
  private final Specification specification;
  private final DssAnalysisOptions options;

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
          "Allows to set the algorithm for decomposing the CFA. LINEAR_DECOMPOSITION creates blocks"
              + " from each merge/branching point to the next merge/branching point."
              + " MERGE_DECOMPOSITION merges blocks obtained by LINEAR_DECOMPOSITION. The final"
              + " number of blocks should converge to the number of functions in the program."
              + " NO_DECOMPOSITION creates one block around the CFA.",
      secure = true)
  private DecompositionType decompositionType = DecompositionType.MERGE_DECOMPOSITION;

  @Option(
      description =
          "Change the queue type. VIOLATION_CONDITION prioritizes the processing"
              + " of ViolationConditionMessages. DEFAULT does not differ between PostCondition and"
              + " ViolationCondition messages.",
      secure = true)
  private QueueType queue = QueueType.DEFAULT;

  @Option(
      description =
          "The number of blocks is dependent by the number of functions in the program."
              + "A tolerance of 1 means, that we subtract 1 of the total number of functions.",
      secure = true)
  private boolean allowSingleBlockDecompositionWhenMerging = false;

  @Option(
      description =
          "Abstraction nodes are added to each block after they are created. "
              + "They are needed to strengthen the preconditions of blocks. "
              + "Missing blocks make the analysis slower but not impossible.",
      secure = true)
  private boolean allowMissingAbstractionNodes = true;

  @FileOption(Type.OUTPUT_FILE)
  @Option(description = "Where to store the block graph in JSON format", secure = true)
  private Path blockCFAFile = Path.of("block_analysis/blocks.json");

  @Option(description = "Whether to stop after exporting the blockgraph", secure = true)
  private boolean generateBlockGraphOnly = false;

  @Option(description = "Import an existing decomposition from a file", secure = true)
  @FileOption(Type.OPTIONAL_INPUT_FILE)
  private Path importDecomposition = null;

  @Option(description = "Whether to spawn a worker for only one block id", secure = true)
  private String spawnWorkerForId = "";

  @FileOption(Type.OPTIONAL_INPUT_FILE)
  @Option(
      description =
          "List of input files that contain preconditions and verification conditions that should"
              + " be assumed as 'known' by block-summary analysis."
              + " Each file must contain a single, valid JSON DssMessage."
              + " If at least one file is provided, the block-summary analysis assumes"
              + " these pre- and verification-conditions."
              + " If no file is provided, the block-summary analysis assumes"
              + " the precondition 'true' and the verification condition 'false'.",
      secure = true)
  private List<Path> knownConditions = ImmutableList.of();

  @FileOption(Type.OPTIONAL_INPUT_FILE)
  @Option(
      description =
          "List of input files that contain preconditions and verification conditions that should"
              + " be assumed as 'new' by block-summary analysis."
              + " For each message in this list, block-summary analysis will perform a new analysis"
              + " run in the order of occurrence."
              + " Each file must contain a single, valid JSON DssMessage."
              + " If at least one file is provided, the block-summary analysis assumes"
              + " these pre- and verification-conditions."
              + " If no file is provided, the block-summary analysis assumes"
              + " the precondition 'true' and the verification condition 'false'.",
      secure = true)
  private List<Path> newConditions = ImmutableList.of();

  @FileOption(Type.OUTPUT_DIRECTORY)
  @Option(description = "Where to write responses", secure = true)
  private Path outputMessages = Path.of("messages/");

  private enum DecompositionType {
    LINEAR_DECOMPOSITION,
    MERGE_DECOMPOSITION,
    BRIDGE_DECOMPOSITION,
    NO_DECOMPOSITION
  }

  private enum QueueType {
    VIOLATION_CONDITION,
    DEFAULT
  }

  public DistributedSummarySynthesis(
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
    options = new DssAnalysisOptions(configuration);
    // We inject the DssMessageFactory into all other block-summary components from here
    // because this is the outermost class for their setup.
    messageFactory = new DssMessageFactory(options);
    stats = new HashMap<>();

    if (Stream.concat(knownConditions.stream(), newConditions.stream())
        .anyMatch(f -> !Files.isRegularFile(f))) {
      throw new InvalidConfigurationException(
          "All input messages must be files that exist: " + knownConditions + ", " + newConditions);
    }
  }

  private Supplier<BlockingQueue<DssMessage>> getQueueSupplier() {
    return switch (queue) {
      case VIOLATION_CONDITION -> () -> new DssPrioritizeViolationConditionQueue();
      case DEFAULT -> () -> new DssDefaultQueue();
    };
  }

  private DssBlockDecomposition getDecomposer()
      throws InvalidConfigurationException, IOException, CPAException {
    if (importDecomposition != null) {
      return new ImportDecomposition(importDecomposition);
    }
    BlockOperator blockOperator = new BlockOperator();
    configuration.inject(blockOperator);
    blockOperator.setCFA(initialCFA);
    Predicate<CFANode> isBlockEnd = n -> blockOperator.isBlockEnd(n, -1);
    return switch (decompositionType) {
      case LINEAR_DECOMPOSITION -> new LinearBlockNodeDecomposition(isBlockEnd);
      case MERGE_DECOMPOSITION ->
          new MergeBlockNodesDecomposition(
              new LinearBlockNodeDecomposition(isBlockEnd),
              2,
              Comparator.comparing(BlockNodeWithoutGraphInformation::getId),
              allowSingleBlockDecompositionWhenMerging);
      case BRIDGE_DECOMPOSITION ->
          new VerticalMergeDecomposition(
              new BridgeDecomposition(), 1, Comparator.comparingInt(b -> b.getEdges().size()));
      case NO_DECOMPOSITION -> new SingleBlockDecomposition();
    };
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    logger.log(Level.INFO, "Starting block analysis...");
    try {
      // create blockGraph and reduce to relevant parts
      DssBlockDecomposition decomposer = getDecomposer();
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
      ImmutableSet<CFANode> abstractionDeadEnds = modification.metadata().unableToAbstract();
      numberWorkersWithoutAbstraction.setNextValue(abstractionDeadEnds.size());
      if (!abstractionDeadEnds.isEmpty() && !allowMissingAbstractionNodes) {
        for (String successorId : blockGraph.getRoot().getSuccessorIds()) {
          for (BlockNode node : blockGraph.getNodes()) {
            if (node.getId().equals(successorId)) {
              if (node.getViolationConditionLocation().equals(node.getFinalLocation())) {
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
        BlockNode blockNode =
            blocks.stream().filter(b -> b.getId().equals(spawnWorkerForId)).findAny().orElseThrow();
        Components build =
            new DssWorkerBuilder(
                    cfa,
                    new InMemoryDssConnectionProvider(getQueueSupplier()),
                    specification,
                    messageFactory)
                .addAnalysisWorker(blockNode, options)
                .build();

        DssAnalysisWorker actor = (DssAnalysisWorker) Iterables.getOnlyElement(build.actors());
        // use list instead of set. Each message has a unique timestamp,
        // so there will be no duplicates that a set can remove.
        // But the equality checks are unnecessarily expensive
        List<DssMessage> response = new ArrayList<>();
        if (knownConditions.isEmpty() && newConditions.isEmpty()) {
          response.addAll(actor.runInitialAnalysis());
        } else {
          OldAndNewMessages preparedBatches =
              prepareOldAndNewMessages(knownConditions, newConditions);
          for (DssMessage message : preparedBatches.oldMessages()) {
            actor.storeMessage(message);
          }
          for (DssMessage message : preparedBatches.newMessages()) {
            response.addAll(actor.processMessage(message));
          }
        }
        writeAllMessages(response);
        return AlgorithmStatus.NO_PROPERTY_CHECKED;
      }

      Components components = createComponentsDss(cfa, blockGraph);
      if (components.connections().size() != 1) {
        throw new CPAException("Components need to provide exactly one additional connection");
      }

      numberWorkers.setNextValue(components.actors().size());

      // listen to messages
      try (DssConnection mainThreadConnection = components.connections().get(0)) {
        DssFixpointNotifier.init(
            messageFactory,
            mainThreadConnection,
            components.connections().size() + components.actors().size());
        // run workers
        for (DssActor worker : components.actors()) {
          Thread thread = new Thread(worker, worker.getId());
          thread.setDaemon(true);
          thread.start();
        }
        String observerId = "observer";
        LogManager observerLogger = getLogger(options, observerId);
        DssObserverWorker observer =
            new DssObserverWorker(
                observerId, mainThreadConnection, blocks.size(), messageFactory, observerLogger);
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
    } catch (InvalidConfigurationException | IOException | SolverException e) {
      logger.logException(Level.SEVERE, e, "Block analysis stopped unexpectedly.");
      throw new CPAException("Component Analysis run into an error.", e);
    } finally {
      logger.log(Level.INFO, "Block analysis finished.");
    }
  }

  private void writeAllMessages(List<DssMessage> response) throws IOException {
    MessageConverter converter = new MessageConverter();
    int messageCount = 0;
    for (DssMessage dssMessage : response) {
      Files.createDirectories(outputMessages);
      final String outputFileNamePrefix = dssMessage.getType().name();
      final String outputFileName = outputFileNamePrefix + messageCount + ".json";
      Path outputPath = outputMessages.resolve(outputFileName);
      Files.writeString(outputPath, converter.messageToJson(dssMessage));
      messageCount++;
    }
  }

  private OldAndNewMessages prepareOldAndNewMessages(
      List<Path> pKnownConditions, List<Path> pNewConditions) throws IOException {
    MessageConverter converter = new MessageConverter();
    List<DssMessage> toBeConsideredOld = new ArrayList<>();
    List<DssMessage> toBeConsideredNew = new ArrayList<>();
    // known conditions always stay 'old' and never become 'true'
    for (Path knownMessageFile : pKnownConditions) {
      DssMessage message = converter.jsonToMessage(Files.readString(knownMessageFile));
      toBeConsideredOld.add(message);
    }

    // new conditions can be considered 'new' (the default), but under certain conditions
    // we can avoid unnecessary analysis when we know that considering them 'old' is semantically
    // equivalent.
    // effect of a new postcondition: starts for each verification condition a new analysis run that
    // considers all known + the new postcondition
    // Multiple new postconditions have the same effect as only taking one of them as 'new' and the
    // others as 'old'.
    boolean isFirstPostcondition = true;
    for (Path newMessageFile : pNewConditions) {
      DssMessage message = converter.jsonToMessage(Files.readString(newMessageFile));
      if (message.getType() == MessageType.BLOCK_POSTCONDITION) {
        if (isFirstPostcondition) {
          // Do postconditions first, so that information is known before error conditions are
          // checked
          toBeConsideredNew.add(0, message);
          isFirstPostcondition = false;
        } else {
          toBeConsideredOld.add(message);
        }
      } else {
        toBeConsideredNew.add(message);
      }
    }
    return new OldAndNewMessages(toBeConsideredOld, toBeConsideredNew);
  }

  private LogManager getLogger(DssAnalysisOptions pOptions, String workerId) {
    try {
      return pOptions.getLogDirectory() != null
          ? BasicLogManager.createWithHandler(
              new FileHandler(pOptions.getLogDirectory().toString() + "/" + workerId + ".log"))
          : LogManager.createNullLogManager();
    } catch (IOException e) {
      return LogManager.createNullLogManager();
    }
  }

  private Components createComponentsDss(CFA cfa, BlockGraph blockGraph)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    ImmutableSet<BlockNode> blocks = blockGraph.getNodes();
    DssWorkerBuilder builder =
        new DssWorkerBuilder(
                cfa,
                new InMemoryDssConnectionProvider(getQueueSupplier()),
                specification,
                messageFactory)
            .createAdditionalConnections(1)
            .addRootWorker(blockGraph.getRoot(), options);
    for (BlockNode distinctNode : blocks) {
      averageNumberOfEdges.setNextValue(distinctNode.getEdges().size());
      builder = builder.addAnalysisWorker(distinctNode, options);
    }
    if (options.isDebugModeEnabled()) {
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
              (Map<?, ?>) map.remove(DssStatisticType.FORWARD_ANALYSIS_STATS.name()));
      for (Entry<?, ?> entry : map.entrySet()) {
        writer =
            writer.put(
                DssStatisticType.valueOf(entry.getKey().toString()).getName(),
                convert(entry.getKey().toString(), entry.getValue().toString()));
        mergeInto(overall, entry.getKey().toString(), entry.getValue());
      }
      for (Entry<?, ?> entry : forwardMap.entrySet()) {
        writer =
            writer.put(
                DssStatisticType.valueOf(entry.getKey().toString()).getName() + " (forward)",
                convert(entry.getKey().toString(), entry.getValue().toString()));
        mergeInto(overall, entry.getKey().toString(), entry.getValue());
      }
      writer = writer.endLevel();
    }
    writer = writer.put("Overall", "Sum of all blocks").beginLevel();
    for (Entry<String, Object> stringObjectEntry : overall.entrySet()) {
      writer =
          writer.put(
              DssStatisticType.valueOf(stringObjectEntry.getKey()).getName() + " (overall)",
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
    if (DssStatisticType.valueOf(pKey).isFormatAsTime()) {
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
