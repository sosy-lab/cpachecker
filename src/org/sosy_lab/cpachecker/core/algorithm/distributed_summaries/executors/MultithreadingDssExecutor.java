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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssAllWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.DssDefaultQueue;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.DssWorkCounter;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.DssMessageBroadcaster;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.witness.DssWitnessArgStateCollector;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssActor;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssActors;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisWorker;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssObserverWorker;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssWorkerBuilder;
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
 * <p>Every worker runs in a thread of its own, because a block analysis has to stay on the thread
 * that created it. Proofs are found if all workers are waiting for new messages and no message is
 * left in any queue, which the shared {@link DssWorkCounter} detects. The verdict TRUE is then
 * broadcast to all workers.
 *
 * <p>The analysis is started by calling {@link DssAnalysisWorker#runInitialAnalysis()} on all
 * workers. The monitoring of the messages is done by {@link DssObserverWorker}, which blocks until
 * it can determine a final verification verdict (SAFE, UNSAFE, or timeout)
 */
public class MultithreadingDssExecutor implements DssExecutor {

  private static final String OBSERVER_WORKER_ID = "__observer__";
  private static final String PROOF_SENDER_ID = "dss-fixpoint";

  private final DssMessageFactory messageFactory;
  private final DssAnalysisOptions options;
  private final Specification specification;
  private final ShutdownManager shutdownManager;

  public MultithreadingDssExecutor(
      Configuration pConfiguration, Specification pSpecification, ShutdownManager pShutdownManager)
      throws InvalidConfigurationException {
    specification = pSpecification;
    options = new DssAnalysisOptions(pConfiguration);
    messageFactory = new DssMessageFactory(options);
    shutdownManager = pShutdownManager;
  }

  private DssActors createDssActors(
      CFA cfa,
      BlockGraph blockGraph,
      DssWitnessArgStateCollector stateCollector,
      DssAllWorkerStatistics allWorkerStatistics,
      DssWorkCounter workCounter,
      ShutdownManager workerShutdownManager)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    ImmutableSet<BlockNode> blocks = blockGraph.getNodes();
    DssWorkerBuilder builder =
        new DssWorkerBuilder(
            cfa,
            specification,
            () -> new DssDefaultQueue(workCounter),
            messageFactory,
            allWorkerStatistics,
            workerShutdownManager);
    for (BlockNode distinctNode : blocks) {
      builder = builder.addAnalysisWorker(distinctNode, options);
    }
    if (options.isDebugModeEnabled()) {
      builder = builder.addVisualizationWorker(blockGraph, options);
    }
    builder.addObserverWorker(OBSERVER_WORKER_ID, blockGraph, options, stateCollector);
    return builder.build();
  }

  @Override
  public StatusAndResult execute(
      CFA cfa,
      BlockGraph blockGraph,
      DssWitnessArgStateCollector stateCollector,
      DssAllWorkerStatistics allWorkerStatistics)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    DssWorkCounter workCounter = new DssWorkCounter();
    // Stops the analyses that are still running once DSS finished.
    ShutdownManager workerShutdownManager =
        ShutdownManager.createWithParent(shutdownManager.getNotifier());
    // Closing the executor waits for all workers, and happens before the actors are closed.
    try (DssActors actors =
            createDssActors(
                cfa,
                blockGraph,
                stateCollector,
                allWorkerStatistics,
                workCounter,
                workerShutdownManager);
        ExecutorService executor =
            Executors.newThreadPerTaskExecutor(
                Thread.ofPlatform().name("dss-worker-", 0).daemon().factory())) {
      DssObserverWorker observer = Iterables.getOnlyElement(actors.getObservers());
      Preconditions.checkState(
          observer.getId().equals(OBSERVER_WORKER_ID),
          "Observer worker must have id %s but has id %s",
          OBSERVER_WORKER_ID,
          observer.getId());
      for (DssAnalysisWorker worker : actors.getAnalysisWorkers()) {
        // The block analysis can only be closed by the thread that created it.
        executor.execute(
            () -> {
              try {
                worker.run();
              } finally {
                worker.close();
              }
            });
      }
      for (DssActor actor : actors.getRemainingActors()) {
        executor.execute(actor);
      }
      DssMessageBroadcaster broadcaster = observer.getConnection().getBroadcaster();
      executor.execute(() -> broadcastProofOnceNoWorkIsLeft(workCounter, broadcaster));

      try {
        // Blocks until all WITNESS(es) or EXCEPTION arrives
        return observer.observe();
      } finally {
        // No worker is needed anymore, but two kinds of workers do not stop on their own:
        // - A worker that is still analyzing, e.g., because another block found a violation,
        //   checks the shutdown request regularly, but an interrupt would mostly go unnoticed.
        // - A worker that waits in take() for a message that never arrives does not notice the
        //   shutdown request, but take() throws once shutdownNow() interrupts its thread.
        workerShutdownManager.requestShutdown("DSS finished");
        executor.shutdownNow();
      }
    }
  }

  private void broadcastProofOnceNoWorkIsLeft(
      DssWorkCounter workCounter, DssMessageBroadcaster broadcaster) {
    try {
      workCounter.awaitNoWorkLeft();
    } catch (InterruptedException e) {
      // The analysis ended with another verdict, so there is nothing to prove anymore.
      return;
    }
    broadcaster.broadcastToAll(messageFactory.createDssResultMessage(PROOF_SENDER_ID, Result.TRUE));
  }
}
