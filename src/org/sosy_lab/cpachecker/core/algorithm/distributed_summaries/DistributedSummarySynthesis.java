// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
<<<<<<< HEAD
=======
import java.util.Map;
import java.util.Map.Entry;
import java.util.SequencedMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.FileHandler;
>>>>>>> main
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.DssBlockDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.DssDecompositionOptions;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraphModification;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraphModification.Modification;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.executors.DssExecutor;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.executors.NaiveDssExecutor;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.executors.SingleWorkerDssExecutor;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssObserverWorker.StatusAndResult;
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
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "distributedSummaries")
public class DistributedSummarySynthesis implements Algorithm, StatisticsProvider {

  private final Configuration configuration;
  private final LogManager logger;
  private final CFA initialCFA;
  private final ShutdownManager shutdownManager;
  private final DistributedSummarySynthesisStatistics dssStats;
  private final DssExecutor executor;
  private final DssDecompositionOptions decompositionOptions;

  @Option(description = "Decomposition type to use for the block analysis.", secure = true)
  private ExecutorType executorType = ExecutorType.DSS;

  private enum ExecutorType {
    DSS,
    SINGLE_WORKER
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

    decompositionOptions = new DssDecompositionOptions(configuration, pInitialCFA);
    dssStats = new DistributedSummarySynthesisStatistics();

    logger = pLogger;
    initialCFA = pInitialCFA;
    shutdownManager = pShutdownManager;
    executor = getExecutor(pSpecification);
  }

  private DssExecutor getExecutor(Specification specification)
      throws InvalidConfigurationException {
    return switch (executorType) {
      case DSS -> new NaiveDssExecutor(configuration, specification);
      case SINGLE_WORKER -> new SingleWorkerDssExecutor(configuration, specification);
    };
  }

  private BlockGraph decompose(DssBlockDecomposition decomposer) throws InterruptedException {
    dssStats.getDecompositionTimer().start();
    BlockGraph blockGraph = decomposer.decompose(initialCFA);
    dssStats.getDecompositionTimer().stop();
    blockGraph.checkConsistency(shutdownManager.getNotifier());
    return blockGraph;
  }

  private Modification modifyBlockGraph(BlockGraph blockGraph) {
    dssStats.getInstrumentationTimer().start();
    Modification modification =
        BlockGraphModification.instrumentCFA(initialCFA, blockGraph, configuration, logger);
    dssStats.getInstrumentationTimer().stop();
    ImmutableSet<CFANode> abstractionDeadEnds = modification.metadata().unableToAbstract();
    dssStats.getNumberWorkersWithoutAbstraction().setNextValue(abstractionDeadEnds.size());
    if (!abstractionDeadEnds.isEmpty() && !decompositionOptions.allowMissingAbstractionNodes()) {
      for (BlockNode node : blockGraph.getRoots()) {
        if (node.getViolationConditionLocation().equals(node.getFinalLocation())) {
          throw new AssertionError(
              "Direct successors of the root node are required to have an abstraction"
                  + " location.");
        }
      }
    }
    if (!abstractionDeadEnds.isEmpty()) {
      logger.logf(Level.INFO, "Abstraction is not possible at: %s", abstractionDeadEnds);
    }
    for (BlockNode node : modification.blockGraph().getNodes()) {
      dssStats.getAverageNumberOfEdges().setNextValue(node.getEdges().size());
    }
    return modification;
  }

  private AlgorithmStatus interpretResult(StatusAndResult statusAndResult, ReachedSet reachedSet) {
    Result result = statusAndResult.result();
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
    return statusAndResult.status();
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    logger.log(Level.INFO, "Starting block analysis...");
    try {
      // create blockGraph and reduce to relevant parts
      BlockGraph blockGraph = decompose(decompositionOptions.getConfiguredDecomposition());
      if (decompositionOptions.generateBlockGraphOnly()) {
        blockGraph.export(decompositionOptions.getBlockCFAFile());
        logger.logf(
            Level.INFO, "Block graph exported to %s.", decompositionOptions.getBlockCFAFile());
        return AlgorithmStatus.NO_PROPERTY_CHECKED;
      }

      Modification modification = modifyBlockGraph(blockGraph);
      CFA cfa = modification.cfa();
      blockGraph = modification.blockGraph();
      logger.logf(
          Level.INFO,
          "Decomposed CFA in %d blocks using the %s.",
          blockGraph.getNodes().size(),
          decompositionOptions.getDecompositionType());

      return interpretResult(executor.execute(cfa, blockGraph), reachedSet);
    } catch (InvalidConfigurationException | IOException | SolverException e) {
      logger.logException(Level.SEVERE, e, "Block analysis stopped unexpectedly.");
      throw new CPAException("Component Analysis run into an error.", e);
    } finally {
      logger.log(Level.INFO, "Block analysis finished.");
    }
  }

<<<<<<< HEAD
=======
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
          toBeConsideredNew.addFirst(message);
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
      SequencedMap<?, ?> map = new LinkedHashMap<>((Map<?, ?>) mapObject);
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

>>>>>>> main
  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(dssStats);
    executor.collectStatistics(statsCollection);
  }
}
