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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssObserverWorker;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssObserverWorker.StatusAndResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssThreadMonitor;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssWorkerBuilder;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class NaiveDssExecutor implements DssExecutor {

  private static final String OBSERVER_WORKER_ID = "__observer__";

  private final DssMessageFactory messageFactory;
  private final DssAnalysisOptions options;
  private final Specification specification;
  private final List<Statistics> stats;

  public NaiveDssExecutor(Configuration pConfiguration, Specification pSpecification)
      throws InvalidConfigurationException {
    specification = pSpecification;
    options = new DssAnalysisOptions(pConfiguration);
    messageFactory = new DssMessageFactory(options);
    stats = new ArrayList<>();
  }

  private List<DssActor> createDssActors(CFA cfa, BlockGraph blockGraph)
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

  public StatusAndResult execute(CFA cfa, BlockGraph blockGraph)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    List<DssActor> actors = createDssActors(cfa, blockGraph);
    // run workers
    List<Thread> threads = new ArrayList<>(actors.size());
    DssObserverWorker observer = null;
    for (DssActor worker : actors) {
      if (worker instanceof Statistics workerStatistics) {
        stats.add(workerStatistics);
      }
      if (worker.getId().equals(OBSERVER_WORKER_ID)) {
        // the observer worker is special, it does not run in a thread
        // but blocks the main thread until all workers are finished
        if (worker instanceof DssObserverWorker o) {
          observer = o;
          continue;
        }
        throw new AssertionError(
            "Observer worker must be an instance of DssObserverWorker, but is: "
                + worker.getClass().getName());
      }
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
