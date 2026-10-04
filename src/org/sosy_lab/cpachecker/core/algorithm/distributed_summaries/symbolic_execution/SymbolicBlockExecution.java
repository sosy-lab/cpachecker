// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.symbolic_execution;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.DssDecompositionOptions;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.DssDecompositionOptions.DecompositionType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraphModification;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraphModification.Modification;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
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
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProviderImpl;

/**
 * Symbolic execution on the block graph of distributed summary synthesis (DSS).
 *
 * <p>The program is decomposed into blocks exactly like DSS does it. Each block is then summarized
 * once by an exact path formula, and a {@link SymbolicBlockWorker} per block exchanges exact
 * conditions with its neighbors: strongest postconditions forward from the program entry and
 * preimages of the target locations backward. Blocks talk through the message infrastructure of
 * DSS, but there is no block analysis, abstraction or refinement: if a precondition meets a
 * violation condition, the program is unsafe; if no block has work left, it is safe.
 *
 * <p>Only blocks from which a target location is reachable in the block graph take part, because no
 * other block can contribute to a violation.
 *
 * <p>The inlining decomposition is required: a block's conditions do not say in which calling
 * context the block runs, which is only precise if every block has exactly one.
 */
public class SymbolicBlockExecution implements Algorithm, StatisticsProvider {

  private final Configuration configuration;
  private final LogManager logger;
  private final CFA initialCfa;
  private final ShutdownNotifier shutdownNotifier;
  private final Specification specification;
  private final SymbolicBlockExecutionOptions options;
  private final DssDecompositionOptions decompositionOptions;
  private final DssMessageFactory messageFactory;
  private final SymbolicBlockExecutionStatistics statistics =
      new SymbolicBlockExecutionStatistics();

  public SymbolicBlockExecution(
      Configuration pConfiguration,
      LogManager pLogger,
      CFA pCfa,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification)
      throws InvalidConfigurationException {
    configuration = pConfiguration;
    logger = pLogger;
    initialCfa = pCfa;
    shutdownNotifier = pShutdownNotifier;
    specification = pSpecification;
    options = new SymbolicBlockExecutionOptions(pConfiguration);
    decompositionOptions = new DssDecompositionOptions(pConfiguration, pCfa);
    if (decompositionOptions.getDecompositionType() != DecompositionType.INLINING_DECOMPOSITION) {
      throw new InvalidConfigurationException(
          "Symbolic block execution requires"
              + " distributedSummaries.decomposition.decompositionType=INLINING_DECOMPOSITION,"
              + " because its conditions do not carry a calling context");
    }
    messageFactory = new DssMessageFactory(new DssAnalysisOptions(pConfiguration));
    // The threads create their solvers and encoders only when the analysis runs. Creating one now
    // reports an invalid configuration before that, and marks the options as used.
    new SymbolicBlockAnalysis(
            pConfiguration, pLogger, pShutdownNotifier, pCfa, options, statistics, new AtomicLong())
        .close();
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    Modification modification = decompose();
    CFA cfa = modification.cfa();
    BlockGraph blockGraph = modification.blockGraph();

    statistics.targetLocationTime.start();
    ImmutableSet<CFANode> targets;
    try {
      targets =
          new TargetLocationProviderImpl(shutdownNotifier, logger, cfa)
              .tryGetAutomatonTargetLocations(cfa.getMainFunction(), specification);
    } finally {
      statistics.targetLocationTime.stop();
    }
    if (targets.size() == cfa.nodes().size()) {
      // the provider answers with all locations if it fails
      throw new CPAException("Symbolic block execution could not determine the target locations");
    }

    ImmutableSet<BlockNode> relevant = blocksThatReachTargets(blockGraph, targets);
    statistics.setBlockCounts(blockGraph.getNodes().size(), relevant.size(), targets.size());
    logger.logf(
        Level.INFO,
        "Decomposed CFA in %d blocks, %d of which can reach one of %d target locations.",
        blockGraph.getNodes().size(),
        relevant.size(),
        targets.size());

    Result result = relevant.isEmpty() ? Result.TRUE : execute(cfa, relevant, targets);
    logger.log(Level.INFO, "Symbolic block execution finished with result", result);
    if (result == Result.FALSE) {
      addDummyTarget(pReachedSet);
    } else {
      pReachedSet.clear();
    }
    return AlgorithmStatus.SOUND_AND_PRECISE;
  }

  private Modification decompose() throws CPAException, InterruptedException {
    statistics.decompositionTime.start();
    try {
      BlockGraph blockGraph =
          decompositionOptions.getConfiguredDecomposition().decompose(initialCfa);
      blockGraph.checkConsistency(shutdownNotifier);
      return BlockGraphModification.instrumentCFA(initialCfa, blockGraph, configuration, logger);
    } catch (IOException e) {
      throw new CPAException("Could not read the decomposition: " + e.getMessage(), e);
    } finally {
      statistics.decompositionTime.stop();
    }
  }

  private Result execute(
      CFA pCfa, ImmutableSet<BlockNode> pRelevant, ImmutableSet<CFANode> pTargets)
      throws CPAException, InterruptedException {
    Set<String> relevantIds = new LinkedHashSet<>();
    for (BlockNode block : pRelevant) {
      relevantIds.add(block.getId());
    }
    ImmutableList.Builder<SymbolicBlockWorker> workers = ImmutableList.builder();
    for (BlockNode block : pRelevant) {
      workers.add(
          new SymbolicBlockWorker(
              block,
              pTargets,
              FluentIterable.from(block.getSuccessorIds()).filter(relevantIds::contains).toSet(),
              messageFactory,
              statistics,
              logger));
    }
    AtomicLong privateNames = new AtomicLong();
    SymbolicBlockScheduler.AnalysisFactory analysisFactory =
        notifier ->
            new SymbolicBlockAnalysis(
                configuration, logger, notifier, pCfa, options, statistics, privateNames);
    SymbolicBlockScheduler scheduler =
        options.threads() == 0
            ? new ThreadPerBlockScheduler(analysisFactory, messageFactory, shutdownNotifier, logger)
            : new WorkStealingScheduler(
                options.threads(), analysisFactory, shutdownNotifier, statistics, logger);
    return scheduler.run(workers.build());
  }

  /**
   * The blocks that contain a target location or have a path to such a block in the block graph. A
   * target location belongs to the block that reaches it with one of its edges.
   */
  private static ImmutableSet<BlockNode> blocksThatReachTargets(
      BlockGraph pBlockGraph, ImmutableSet<CFANode> pTargets) {
    ImmutableMap<String, BlockNode> blocksById =
        FluentIterable.from(pBlockGraph.getNodes()).uniqueIndex(BlockNode::getId);
    Set<BlockNode> relevant = new LinkedHashSet<>();
    Deque<BlockNode> waitlist = new ArrayDeque<>();
    for (BlockNode block : pBlockGraph.getNodes()) {
      if (FluentIterable.from(block.getEdges())
          .transform(CFAEdge::getSuccessor)
          .anyMatch(pTargets::contains)) {
        relevant.add(block);
        waitlist.add(block);
      }
    }
    while (!waitlist.isEmpty()) {
      for (String predecessor : waitlist.pop().getPredecessorIds()) {
        BlockNode block = blocksById.get(predecessor);
        if (relevant.add(block)) {
          waitlist.add(block);
        }
      }
    }
    // keep the order of the block graph, so that the threads start in a reproducible order
    return FluentIterable.from(pBlockGraph.getNodes()).filter(relevant::contains).toSet();
  }

  /**
   * Marks the reached set as unsafe the same way DSS does: with a copy of the initial state that
   * contains a target state.
   */
  private static void addDummyTarget(ReachedSet pReachedSet) {
    AbstractState first = pReachedSet.getFirstState();
    Precision precision = pReachedSet.getPrecision(first);
    List<AbstractState> components = new ArrayList<>();
    AbstractState wrapped = first instanceof ARGState argState ? argState.getWrappedState() : first;
    if (wrapped instanceof CompositeState composite) {
      components.addAll(composite.getWrappedStates());
    } else {
      components.add(wrapped);
    }
    components.add(DummyTargetState.withoutTargetInformation());
    pReachedSet.add(new ARGState(new CompositeState(components), null), precision);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(statistics);
  }
}
