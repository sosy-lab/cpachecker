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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.executors.MultithreadingDssExecutor;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.executors.SequentialDssExecutor;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.executors.SingleWorkerDssExecutor;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssObserverWorker;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssObserverWorker.StatusAndResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssWorkerBuilder;
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

/**
 * Main class for Distributed Summary Synthesis (DSS).
 *
 * <p>DSS partitions the program into blocks and distributes verification across multiple workers
 * that communicate via message passing. The analysis follows these phases:
 *
 * <h2>1. Decomposition</h2>
 *
 * <p>The CFA is partitioned into blocks using some {@link DssBlockDecomposition}. The resulting
 * {@link BlockGraph} and the underlying CFA are then instrumented with {@link
 * BlockGraphModification} to ensure clean block boundaries.
 *
 * <h2>2. Worker Creation</h2>
 *
 * Workers are spawned for all blocks resulting from the decomposition. Special workers like the
 * {@link org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssVisualizationWorker
 * visualization worker} are created in debug mode to get an overview of all messages. The {@link
 * org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssObserverWorker observer
 * worker} is used to collect statistics.
 *
 * <h2>3. Execution and Coordination</h2>
 *
 * <p>After worker creation, DistributedSummarySynthesis coordinates the analysis execution based on
 * the configured executor. However, they all have common steps:
 *
 * <ul>
 *   <li>Execute one of three {@link DssExecutor executors} to either run DSS, a single block
 *       analysis, or DSS but every worker is scheduled after the other in deterministic order
 *   <li>Delegating the interpretation of messages to conclude a final verdict to observers.
 *   <li>Processes the final result by updating the {@link ReachedSet}:
 *       <ul>
 *         <li>For UNSAFE results: Adds a {@link DummyTargetState} to indicate property violation
 *         <li>For SAFE results: Clears the reached set to indicate successful verification
 *       </ul>
 * </ul>
 *
 * There are two execution strategies implemented in DSS:
 *
 * <ul>
 *   <li><strong>Block Graph Export Only:</strong> When {@link
 *       DssDecompositionOptions#generateBlockGraphOnly()} is enabled, the analysis stops after
 *       decomposition and exports the block graph to JSON format
 *   <li><strong>Incremental Analysis:</strong> You can provide one of three executors to run
 *       different strategies.
 * </ul>
 */
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
    SINGLE_WORKER,
    SEQUENTIAL
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
      case DSS -> new MultithreadingDssExecutor(configuration, specification);
      case SINGLE_WORKER -> new SingleWorkerDssExecutor(configuration, specification);
      case SEQUENTIAL -> new SequentialDssExecutor(configuration, specification);
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
      if (blockGraph
          .getRoot()
          .getViolationConditionLocation()
          .equals(blockGraph.getRoot().getFinalLocation())) {
        throw new AssertionError(
            "Direct successors of the root node are required to have an abstraction"
                + " location.");
      }
    }
    if (!abstractionDeadEnds.isEmpty()) {
      logger.logf(Level.INFO, "Abstraction is not possible at: %s", abstractionDeadEnds);
    }
    for (BlockNode node : modification.blockGraph().getNodes()) {
      dssStats.getAverageNumberOfEdges().setNextValue(node.getEdges().size());
    }
    dssStats.getNumberWorkers().setNextValue(blockGraph.getNodes().size());
    return modification;
  }

  private AlgorithmStatus interpretResult(StatusAndResult statusAndResult, ReachedSet reachedSet) {
    Result result = statusAndResult.result();
    if (result == Result.FALSE) {
      CompositeState cState;
      Precision initialPrecision;
      if (reachedSet.getFirstState() instanceof CompositeState cS) {
        cState = cS;
        initialPrecision = reachedSet.getPrecision(cS);
      } else {
        ARGState state = (ARGState) reachedSet.getFirstState();
        assert state != null;
        cState = (CompositeState) state.getWrappedState();
        initialPrecision = reachedSet.getPrecision(state);
      }
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

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(dssStats);
    executor.collectStatistics(statsCollection);
  }
}
