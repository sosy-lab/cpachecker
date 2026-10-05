// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.symbolic_execution;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.DssDefaultQueue;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.DssWorkCounter;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.CommunicationId;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.DssCommunicationEntity;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.DssMessageBroadcaster;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.DssSchedulerConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssExceptionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssResultMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssViolationConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssWitnessMessage;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Runs every block in a thread of its own, exactly like the workers of distributed summary
 * synthesis: each block has its own {@link DssDefaultQueue}, the blocks send their conditions
 * through a {@link DssMessageBroadcaster}, and a shared {@link DssWorkCounter} detects the moment
 * in which no block is busy and no message is waiting, which proves the program safe.
 *
 * <p>The verdict reaches the calling thread through a queue of its own that takes no part in the
 * work counter, because it does no work.
 */
final class ThreadPerBlockScheduler implements SymbolicBlockScheduler {

  private static final String OBSERVER_ID = "__observer__";
  private static final String PROOF_SENDER_ID = "symbolic-block-fixpoint";

  private final AnalysisFactory analysisFactory;
  private final DssMessageFactory messageFactory;
  private final SymbolicBlockExecutionOptions options;
  private final SymbolicBlockExecutionStatistics statistics;
  private final ShutdownNotifier shutdownNotifier;
  private final LogManager logger;

  ThreadPerBlockScheduler(
      AnalysisFactory pAnalysisFactory,
      DssMessageFactory pMessageFactory,
      SymbolicBlockExecutionOptions pOptions,
      SymbolicBlockExecutionStatistics pStatistics,
      ShutdownNotifier pShutdownNotifier,
      LogManager pLogger) {
    analysisFactory = pAnalysisFactory;
    messageFactory = pMessageFactory;
    options = pOptions;
    statistics = pStatistics;
    shutdownNotifier = pShutdownNotifier;
    logger = pLogger;
  }

  @Override
  public Result run(ImmutableList<SymbolicBlockWorker> pWorkers)
      throws CPAException, InterruptedException {
    DssWorkCounter workCounter = new DssWorkCounter();
    DssDefaultQueue verdicts = new DssDefaultQueue();
    ImmutableMap.Builder<CommunicationId, DssDefaultQueue> queueBuilder = ImmutableMap.builder();
    queueBuilder.put(new CommunicationId(OBSERVER_ID, DssCommunicationEntity.OBSERVER), verdicts);
    for (SymbolicBlockWorker worker : pWorkers) {
      queueBuilder.put(
          new CommunicationId(worker.getBlockId(), DssCommunicationEntity.BLOCK),
          new DssDefaultQueue(workCounter));
    }
    ImmutableMap<CommunicationId, DssDefaultQueue> queues = queueBuilder.buildOrThrow();
    DssMessageBroadcaster broadcaster = new DssMessageBroadcaster(queues);
    ProofTracker proofs =
        new ProofTracker(pWorkers.size(), options.forwardProof(), options.backwardProof());

    // Stops the blocks that are still busy once the verdict is known.
    ShutdownManager workerShutdown = ShutdownManager.createWithParent(shutdownNotifier);
    // A time limit has to wake up the calling thread, which waits for the verdict.
    ShutdownRequestListener wakeUp =
        reason ->
            verdicts.offer(
                messageFactory.createDssExceptionMessage(
                    OBSERVER_ID, new InterruptedException(reason)));
    shutdownNotifier.registerAndCheckImmediately(wakeUp);
    try (ExecutorService executor =
        Executors.newThreadPerTaskExecutor(
            Thread.ofPlatform().name("symbolic-block-", 0).daemon().factory())) {
      try {
        for (SymbolicBlockWorker worker : pWorkers) {
          DssDefaultQueue queue =
              queues.get(new CommunicationId(worker.getBlockId(), DssCommunicationEntity.BLOCK));
          executor.execute(
              new SymbolicBlockActor(
                  worker,
                  proofs,
                  new DssSchedulerConnection(queue, broadcaster),
                  analysisFactory,
                  messageFactory,
                  workerShutdown.getNotifier(),
                  logger));
        }
        executor.execute(() -> broadcastProofOnceNoWorkIsLeft(workCounter, proofs, broadcaster));
        Result result = awaitVerdict(verdicts);
        if (result == Result.TRUE) {
          proofs.proof().ifPresent(statistics::setProof);
        }
        return result;
      } finally {
        workerShutdown.requestShutdown("Symbolic block execution finished");
        // Interrupts the actors that wait for a message and the proof detection.
        executor.shutdownNow();
      }
    } finally {
      shutdownNotifier.unregister(wakeUp);
    }
  }

  private Result awaitVerdict(DssDefaultQueue pVerdicts) throws CPAException, InterruptedException {
    while (true) {
      DssMessage message = pVerdicts.take();
      switch (message) {
        case DssResultMessage result -> {
          return result.getResult();
        }
        case DssExceptionMessage exception -> {
          shutdownNotifier.shutdownIfNecessary();
          // The message carries the stack trace, whose first line names the exception.
          String stackTrace = exception.getExceptionMessage();
          logger.log(Level.FINE, "Block", exception.getSenderId(), "failed:", stackTrace);
          throw new CPAException(
              "Symbolic block execution failed in block "
                  + exception.getSenderId()
                  + ": "
                  + Splitter.on('\n').split(stackTrace).iterator().next());
        }
        case DssPostConditionMessage postcondition -> {}
        case DssViolationConditionMessage violationCondition -> {}
        case DssWitnessMessage witness -> {}
      }
    }
  }

  private void broadcastProofOnceNoWorkIsLeft(
      DssWorkCounter pWorkCounter, ProofTracker pProofs, DssMessageBroadcaster pBroadcaster) {
    try {
      pWorkCounter.awaitNoWorkLeft();
    } catch (InterruptedException e) {
      // The analysis ended with another verdict.
      return;
    }
    if (pProofs.claim(ProofTracker.Proof.NO_WORK_LEFT)) {
      pBroadcaster.broadcastToAll(
          messageFactory.createDssResultMessage(PROOF_SENDER_ID, Result.TRUE));
    }
  }
}
