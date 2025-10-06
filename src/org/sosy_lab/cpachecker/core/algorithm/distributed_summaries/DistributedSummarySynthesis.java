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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
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

//  private record ModifiedBlockGraphKey(CFA cfa, BlockGraph blockGraph) {
//      ModifiedBlockGraphKey {
//          Objects.requireNonNull(cfa);
//          Objects.requireNonNull(blockGraph);
//      }
//  }

  private static final Map<BlockGraph, Modification> modifiedBlockGraphCache = new HashMap<>();

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
//    ModifiedBlockGraphKey key = new ModifiedBlockGraphKey(initialCFA, blockGraph);
    synchronized (modifiedBlockGraphCache) {
      if (modifiedBlockGraphCache.containsKey(blockGraph)) {
        logger.logf(Level.INFO, "Using cached modified block graph.");
        dssStats.getInstrumentationTimer().stop();
        return modifiedBlockGraphCache.get(blockGraph);
      }
      Modification modification =
          BlockGraphModification.instrumentCFA(initialCFA, blockGraph, configuration, logger);
      modifiedBlockGraphCache.put(blockGraph, modification);


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
//        TODO: Export block graph with CFA nodes starting 0
        blockGraph.export(decompositionOptions.getBlockCFAFile(), initialCFA);
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
