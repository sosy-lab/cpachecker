// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.executors;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.DssDefaultQueue;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssActor;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssActors;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssObserverWorker;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssObserverWorker.StatusAndResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssThreadMonitor;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssWorkerBuilder;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * The native DSS spawns multiple workers through {@link DssWorkerBuilder}:
 *
 * <p>For each block, an {@link DssWorkerBuilder#addAnalysisWorker(BlockNode, DssAnalysisOptions)
 * analysis worker} is created. The worker operating on a block without predecessor is responsible
 * to claim a specification violation if a violation condition is about to be propagated. If {@link
 * DssAnalysisOptions#isDebugModeEnabled() debug mode} is enabled, a {@link
 * DssWorkerBuilder#addVisualizationWorker(BlockGraph, DssAnalysisOptions) visualization worker} is
 * used to provide a visualization of the message exchange between analysis workers.
 *
 * <p>Proofs are found if all workers are waiting for new messages. The {@link DssThreadMonitor
 * thread monitor} broadcasts the verdict TRUE if all workers are done.
 *
 * <p>The analysis is started by calling {@link
 * org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisWorker#runInitialAnalysis()}
 * on all workers. The monitoring of the messages is done by {@link DssObserverWorker}, which blocks
 * until it can determine a final verification verdict (SAFE, UNSAFE, or timeout)
 */
public class MultithreadingDssExecutor implements DssExecutor {

  private static final String OBSERVER_WORKER_ID = "__observer__";

  private final DssMessageFactory messageFactory;
  private final DssAnalysisOptions options;
  private final Specification specification;
  private final List<Statistics> stats;

  public MultithreadingDssExecutor(Configuration pConfiguration, Specification pSpecification)
      throws InvalidConfigurationException {
    specification = pSpecification;
    options = new DssAnalysisOptions(pConfiguration);
    messageFactory = new DssMessageFactory(options);
    stats = new ArrayList<>();
  }

  private DssActors createDssActors(CFA cfa, BlockGraph blockGraph)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    ImmutableSet<BlockNode> blocks = blockGraph.getNodes();
    DssWorkerBuilder builder =
        new DssWorkerBuilder(cfa, specification, () -> new DssDefaultQueue(), messageFactory);
    for (BlockNode distinctNode : blocks) {
      builder = builder.addAnalysisWorker(distinctNode, options);
    }
    if (options.isDebugModeEnabled()) {
      builder = builder.addVisualizationWorker(blockGraph, options);
    }
    builder.addObserverWorker(OBSERVER_WORKER_ID, blockGraph.getNodes().size(), options);
    return builder.build();
  }

  @Override
  public StatusAndResult execute(CFA cfa, BlockGraph blockGraph)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    DssActors actors = createDssActors(cfa, blockGraph);
    stats.addAll(actors.getWorkersWithStats());
    DssObserverWorker observer = Iterables.getOnlyElement(actors.getObservers());
    Preconditions.checkState(
        observer.getId().equals(OBSERVER_WORKER_ID),
        "Observer worker must have id %s but has id %s",
        OBSERVER_WORKER_ID,
        observer.getId());
    // run workers
    List<Thread> threads = new ArrayList<>(actors.getActors().size());
    for (DssActor worker :
        Iterables.concat(actors.getAnalysisWorkers(), actors.getRemainingActors())) {
      Thread thread = new Thread(worker, worker.getId());
      threads.add(thread);
      thread.setDaemon(true);
      thread.start();
    }

    Preconditions.checkNotNull(observer, "Observer worker must be present in actors.");
    // sends a result message iff all workers are waiting
    DssThreadMonitor monitor =
        new DssThreadMonitor(threads, messageFactory, observer.getConnection());
    monitor.setDaemon(true);
    monitor.start();
    // blocks the thread until the result message is received
    return observer.observe();
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.addAll(stats);
  }
}
