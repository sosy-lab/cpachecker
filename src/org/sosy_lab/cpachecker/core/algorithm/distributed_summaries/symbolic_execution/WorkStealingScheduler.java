// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.symbolic_execution;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.jspecify.annotations.Nullable;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Runs the blocks on a fixed number of threads, each with one solver context, instead of giving
 * every block a thread of its own.
 *
 * <p>A coordinator keeps the messages of every block and one queue of blocks with pending messages
 * per thread. A thread takes the next block from its own queue and processes all messages that wait
 * for it at once. A block is queued for the thread that analyzed it last, because another thread
 * would have to parse the formulas of the block into its own solver context first. A thread whose
 * queue is empty steals the block that waits longest in the queue of another thread. No block is
 * analyzed by two threads at a time.
 *
 * <p>The program is safe once no block is queued or analyzed, because then no message is pending.
 */
final class WorkStealingScheduler implements SymbolicBlockScheduler {

  private final int threads;
  private final AnalysisFactory analysisFactory;
  private final ShutdownNotifier shutdownNotifier;
  private final SymbolicBlockExecutionStatistics statistics;
  private final LogManager logger;

  WorkStealingScheduler(
      int pThreads,
      AnalysisFactory pAnalysisFactory,
      ShutdownNotifier pShutdownNotifier,
      SymbolicBlockExecutionStatistics pStatistics,
      LogManager pLogger) {
    checkArgument(pThreads > 0, "At least one thread is required, but got %s", pThreads);
    threads = pThreads;
    analysisFactory = pAnalysisFactory;
    shutdownNotifier = pShutdownNotifier;
    statistics = pStatistics;
    logger = pLogger;
  }

  @Override
  public Result run(ImmutableList<SymbolicBlockWorker> pWorkers)
      throws CPAException, InterruptedException {
    Coordinator coordinator = new Coordinator(pWorkers);
    ShutdownManager workerShutdown = ShutdownManager.createWithParent(shutdownNotifier);
    ShutdownRequestListener wakeUp = reason -> coordinator.fail(new InterruptedException(reason));
    shutdownNotifier.registerAndCheckImmediately(wakeUp);
    try (ExecutorService executor =
        Executors.newThreadPerTaskExecutor(
            Thread.ofPlatform().name("symbolic-block-thread-", 0).daemon().factory())) {
      try {
        for (int i = 0; i < threads; i++) {
          final int thread = i;
          executor.execute(() -> work(thread, coordinator, workerShutdown.getNotifier()));
        }
        return coordinator.awaitVerdict();
      } finally {
        // Stops the threads that are still analyzing a block once the verdict is known.
        workerShutdown.requestShutdown("Symbolic block execution finished");
      }
    } finally {
      shutdownNotifier.unregister(wakeUp);
    }
  }

  private void work(int pThread, Coordinator pCoordinator, ShutdownNotifier pNotifier) {
    try (SymbolicBlockAnalysis analysis = analysisFactory.create(pNotifier)) {
      for (Task task = pCoordinator.next(pThread);
          task != null;
          task = pCoordinator.next(pThread)) {
        pNotifier.shutdownIfNecessary();
        SymbolicBlockWorker worker = task.slot().worker;
        List<DssMessage> produced = new ArrayList<>();
        if (task.initialize()) {
          produced.addAll(worker.initialize(analysis));
        }
        // one message at a time, until the block finds a violation
        for (DssMessage message : task.messages()) {
          if (produced.stream().anyMatch(m -> m.getType() == DssMessage.DssMessageType.RESULT)) {
            break;
          }
          produced.addAll(worker.processMessage(analysis, message));
        }
        pCoordinator.complete(task, ImmutableList.copyOf(produced));
      }
    } catch (InterruptedException e) {
      if (!pNotifier.shouldShutdown()) {
        pCoordinator.fail(e);
      }
    } catch (CPAException | SolverException | InvalidConfigurationException | RuntimeException e) {
      if (!pNotifier.shouldShutdown()) {
        logger.logDebugException(e, "Analysis of a block failed");
        pCoordinator.fail(e);
      }
    }
  }

  /** The bookkeeping of one block. All fields are guarded by the lock of the coordinator. */
  private static final class Slot {
    private final SymbolicBlockWorker worker;
    private final List<DssMessage> mailbox = new ArrayList<>();
    private boolean initialized;
    private boolean queued;
    private boolean running;
    private int owner;

    private Slot(SymbolicBlockWorker pWorker, int pOwner) {
      worker = pWorker;
      owner = pOwner;
    }
  }

  /** One round of a block: its initialization if still due, then all messages that waited. */
  private record Task(Slot slot, boolean initialize, ImmutableList<DssMessage> messages) {}

  private final class Coordinator {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition changed = lock.newCondition();
    private final ImmutableMap<String, Slot> slots;
    private final List<ArrayDeque<Slot>> queues;

    /** The number of blocks that are queued or analyzed, i.e., the work that is left. */
    private int active;

    private @Nullable Result verdict;
    private @Nullable Exception failure;

    private Coordinator(ImmutableList<SymbolicBlockWorker> pWorkers) {
      queues = new ArrayList<>(threads);
      for (int i = 0; i < threads; i++) {
        queues.add(new ArrayDeque<>());
      }
      ImmutableMap.Builder<String, Slot> slotBuilder = ImmutableMap.builder();
      int next = 0;
      for (SymbolicBlockWorker worker : pWorkers) {
        // every block has to be initialized, so all of them start queued, spread evenly
        Slot slot = new Slot(worker, next);
        next = (next + 1) % threads;
        slot.queued = true;
        queues.get(slot.owner).addLast(slot);
        slotBuilder.put(worker.getBlockId(), slot);
      }
      slots = slotBuilder.buildOrThrow();
      active = slots.size();
      if (active == 0) {
        verdict = Result.TRUE;
      }
    }

    private boolean isOver() {
      return verdict != null || failure != null;
    }

    /** Waits for a block to analyze, or returns null once the analysis is over. */
    private @Nullable Task next(int pThread) throws InterruptedException {
      lock.lock();
      try {
        while (!isOver()) {
          Slot slot = queues.get(pThread).pollFirst();
          if (slot == null) {
            slot = steal(pThread);
          }
          if (slot != null) {
            slot.queued = false;
            slot.running = true;
            slot.owner = pThread;
            boolean initialize = !slot.initialized;
            slot.initialized = true;
            ImmutableList<DssMessage> messages = ImmutableList.copyOf(slot.mailbox);
            slot.mailbox.clear();
            return new Task(slot, initialize, messages);
          }
          changed.await();
        }
        return null;
      } finally {
        lock.unlock();
      }
    }

    private @Nullable Slot steal(int pThread) {
      for (int offset = 1; offset < threads; offset++) {
        Slot slot = queues.get((pThread + offset) % threads).pollLast();
        if (slot != null) {
          statistics.stolenBlocks.increment();
          return slot;
        }
      }
      return null;
    }

    /** Delivers the messages a block produced, and queues the block again if messages wait. */
    private void complete(Task pTask, ImmutableList<DssMessage> pProduced) {
      lock.lock();
      try {
        Slot slot = pTask.slot();
        // Still running while delivering, so that a message to itself only fills its mailbox.
        for (DssMessage message : pProduced) {
          switch (message.getType()) {
            case POST_CONDITION -> deliver(message, slot.worker.postconditionReceivers());
            case VIOLATION_CONDITION -> deliver(message, slot.worker.violationConditionReceivers());
            case RESULT -> {
              if (!isOver()) {
                verdict = message.getResult();
              }
            }
            case EXCEPTION, WITNESS -> {}
          }
        }
        slot.running = false;
        if (slot.mailbox.isEmpty()) {
          active--;
        } else {
          enqueue(slot);
        }
        if (active == 0 && !isOver()) {
          verdict = Result.TRUE;
        }
        changed.signalAll();
      } finally {
        lock.unlock();
      }
    }

    private void deliver(DssMessage pMessage, Iterable<String> pReceivers) {
      for (String receiver : pReceivers) {
        Slot slot = slots.get(receiver);
        slot.mailbox.add(pMessage);
        if (!slot.queued && !slot.running) {
          active++;
          enqueue(slot);
        }
      }
    }

    private void enqueue(Slot pSlot) {
      pSlot.queued = true;
      queues.get(pSlot.owner).addLast(pSlot);
    }

    private void fail(Exception pFailure) {
      lock.lock();
      try {
        if (!isOver()) {
          failure = pFailure;
        }
        changed.signalAll();
      } finally {
        lock.unlock();
      }
    }

    private Result awaitVerdict() throws CPAException, InterruptedException {
      Exception cause;
      lock.lock();
      try {
        while (!isOver()) {
          changed.await();
        }
        if (verdict != null) {
          return verdict;
        }
        cause = checkNotNull(failure);
      } finally {
        lock.unlock();
      }
      shutdownNotifier.shutdownIfNecessary();
      switch (cause) {
        case InterruptedException interrupted -> throw interrupted;
        case CPAException cpaException -> throw cpaException;
        default ->
            throw new CPAException("Symbolic block execution failed: " + cause.getMessage(), cause);
      }
    }
  }
}
