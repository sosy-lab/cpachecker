// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.Message;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.completion.TaskCompletionMessage;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.request.RequestInvalidatedException;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.request.TaskRequest;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.Task;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.util.ShareableBooleanFormula;

/**
 * JobExecutor manages execution of concurrent analysis tasks from {@linkplain
 * org.sosy_lab.cpachecker.core.algorithm.concurrent.task concurrent.task}.
 *
 * <p>After creating the executor using the public constructor {@link #Scheduler(int,
 * LogManager)}, user can request execution of {@link Task}s with {@link #requestJob(Task)}. In
 * particular, scheduled tasks can use this method to spawn further tasks themselves. Actual
 * execution starts as soon as {@link #start()} gets called. {@link Scheduler} shuts down as soon
 * as all requested jobs (including ones spawned by running jobs themselves) have completed. To wait
 * for this situation, the user can call {@link #waitForCompletion()}, which blocks until all jobs
 * have completed.
 *
 * <p>{@link Scheduler} can modify requested jobs before actually posting them for execution. It
 * does so if data with which a new job has been created (e.g. a block summary) has become outdated
 * due to a concurrent task which completed and which calculated an updated version of such data.
 */
public final class Scheduler implements Runnable {
  private final LinkedBlockingQueue<Message> messages = new LinkedBlockingQueue<>();
  private final ExecutorService executor;
  private final ShutdownNotifier shutdownNotifier;
  private final LogManager logManager;
  private final Collection<Thread> waitingForCompletion = new LinkedList<>();
  private final Thread schedulerThread = new Thread(this, Scheduler.getThreadName());
  private final Table<Block, Block, ShareableBooleanFormula> summaries = HashBasedTable.create();
  private final Map<Block, Integer> summaryVersion = Maps.newHashMap();
  private final Set<CFANode> alreadyPropagated = new HashSet<>();
  private int jobCount = 0;
  private volatile boolean complete = false;
  
  /**
   * Prepare a new {@link Scheduler}. Actual execution does not start until {@link #start()} gets
   * called.
   *
   * @param pThreads    Requested number of threads for job execution
   * @param pLogManager {@link LogManager} used for log messages
   */
  public Scheduler(
      final int pThreads,
      final LogManager pLogManager,
      final ShutdownNotifier pShutdownNotifier) {
    executor = Executors.newFixedThreadPool(pThreads);
    logManager = pLogManager;
    shutdownNotifier = pShutdownNotifier;
  }

  /**
   * Return the name of the thread which executes the {@link Scheduler}.
   *
   * @return Name of the thread which executes the {@link Scheduler}
   */
  public static String getThreadName() {
    return "Job Executor";
  }

  /**
   * Request the {@link Scheduler} to start executing requested jobs.
   */
  public void start() {
    schedulerThread.start();
  }

  /**
   * Suspend the thread which calls this method until {@link Scheduler} has completed all
   * requested jobs.
   */
  public void waitForCompletion() {
    Thread currentThread = Thread.currentThread();
    waitingForCompletion.add(currentThread);

    while (!complete) {
      try {
        synchronized (currentThread) {
          currentThread.wait();
        }
      } catch (final InterruptedException exception) {
        if (shutdownNotifier.shouldShutdown()) {
          return;
        }
      }
    }
  }

  /**
   * Continuously schedule requested jobs for execution, until no jobs are running no more and no
   * new ones have been requested. These conditions apply as soon as the analysis is complete.
   */
  @Override
  public void run() {
    /*
     * Continue to wait for new jobs in 'requestedJobs' as long as either
     * a) there are still jobs scheduled or running ('jobsPending == true'), which might add
     *    elements to the queue, or
     * b) some new jobs have been requested for execution.
     */
    while (jobCount > 0 || !messages.isEmpty()) {
      try {
        Message message = messages.take();
        message.accept(new MessageProcessingVisitor());
      } catch (final InterruptedException ignored) {
        if (shutdownNotifier.shouldShutdown()) {
          return;
        }
      }
    }

    shutdown();
  }

  /**
   * Complete the shutdown of the JobExecutor after all schedules jobs have completed.
   *
   * <p><em>Precondition:</em> All scheduled jobs must have completed and no new ones requested.
   */
  private void shutdown() {
    assert messages.isEmpty();
    assert jobCount == 0;

    /*
     * The method shutdown() gets called after all jobs have completed.
     * The method ExecutorService::shutdownNow() must therefore return an empty collection of jobs
     * which still awaiting execution.
     * The shutdown state of the ExecutorService in general serves as indication whether all jobs
     * have completed, because shutdownNow() gets called only at this location here, were this
     * condition is met.
     */
    List<Runnable> aborted = executor.shutdownNow();
    assert aborted.isEmpty();
    
    complete = true;
    
    for (final Thread waitingThread : waitingForCompletion) {
      synchronized (waitingThread) {
        waitingThread.notify();
      }
    }
  }

  /**
   * Send a message to the scheduler.
   *
   * @param pMessage The new job to execute.
   */
  public void sendMessage(final Message pMessage) {
    checkNotNull(pMessage);

    boolean success = false;

    while (!success) {
      try {
        messages.put(pMessage);
        success = true;
      } catch (InterruptedException ignored) {
        if (shutdownNotifier.shouldShutdown()) {
          return;
        }
      }
    }
  }

  public class MessageProcessingVisitor {
    public void visit(final TaskRequest pRequest) {
      try {
        Task newTask = pRequest.process(summaries, summaryVersion, alreadyPropagated);
        executor.submit(newTask);
        
        ++jobCount;
      } catch (final RequestInvalidatedException ignored) {
      } catch (final Exception exception) {
        logManager.log(Level.SEVERE, "Exception occurred!", exception);
      } catch (final AssertionError assertion) {
        logManager.log(Level.SEVERE, "Assertion violated!", assertion);
      }
    }

    public void visit(final TaskCompletionMessage pCompletionMessage) {
      --jobCount;

      if (jobCount == 0 && messages.isEmpty()) {
        logManager.log(Level.INFO, "All tasks completed.");
        schedulerThread.interrupt();
        shutdown();
      }
    }
  }
}
