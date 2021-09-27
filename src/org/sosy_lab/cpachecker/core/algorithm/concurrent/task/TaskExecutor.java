// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.task;

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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.ShareableBooleanFormula;

/**
 * JobExecutor manages execution of concurrent analysis tasks from {@linkplain
 * org.sosy_lab.cpachecker.core.algorithm.concurrent.task concurrent.task}.
 *
 * <p>After creating the executor using the public constructor {@link #TaskExecutor(int,
 * LogManager)}, user can request execution of {@link Task}s with {@link #requestJob(Task)}. In
 * particular, scheduled tasks can use this method to spawn further tasks themselves. Actual
 * execution starts as soon as {@link #start()} gets called. {@link TaskExecutor} shuts down as soon
 * as all requested jobs (including ones spawned by running jobs themselves) have completed. To wait
 * for this situation, the user can call {@link #waitForCompletion()}, which blocks until all jobs
 * have completed.
 *
 * <p>{@link TaskExecutor} can modify requested jobs before actually posting them for execution. It
 * does so if data with which a new job has been created (e.g. a block summary) has become outdated
 * due to a concurrent task which completed and which calculated an updated version of such data.
 */
public final class TaskExecutor implements Runnable {
  private final LinkedBlockingQueue<TaskRequest> requestedJobs = new LinkedBlockingQueue<>();
  private final ExecutorService executor;
  private final LogManager logManager;
  private final Collection<Thread> waitingOnCompletion = new LinkedList<>();
  private final CompletionWatchdog watchdog = new TaskExecutor.CompletionWatchdog();
  private final Thread executorThread = new Thread(this, TaskExecutor.getThreadName());

  private final AtomicBoolean jobsPending = new AtomicBoolean(true);

  private final Table<Block, Block, ShareableBooleanFormula> summaries = HashBasedTable.create();
  private final Map<Block, Integer> summaryVersion = Maps.newHashMap();
  private final Set<CFANode> alreadyPropagated = new HashSet<>();

  /**
   * Prepare a new {@link TaskExecutor}. Actual execution does not start until {@link #start()} gets
   * called.
   *
   * @param pThreads    Requested number of threads for job execution
   * @param pLogManager {@link LogManager} used for log messages
   */
  public TaskExecutor(final int pThreads, final LogManager pLogManager) {
    executor = Executors.newFixedThreadPool(pThreads);
    logManager = pLogManager;
  }

  /**
   * Return the name of the thread which executes the {@link TaskExecutor}.
   *
   * @return Name of the thread which executes the {@link TaskExecutor}
   */
  public static String getThreadName() {
    return "Job Executor";
  }

  /**
   * Request the {@link TaskExecutor} to start executing requested jobs.
   */
  public void start() {
    executorThread.start();
    new Thread(watchdog, "Completion Watchdog").start();
  }

  /**
   * Suspend the thread which calls this method until {@link TaskExecutor} has completed all
   * requested jobs.
   */
  public void waitForCompletion() {
    Thread currentThread = Thread.currentThread();
    waitingOnCompletion.add(currentThread);

    while (!executor.isShutdown()) {
      try {
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (currentThread) {
          currentThread.wait();
        }
      } catch (InterruptedException ignored) {
        /*
         * If a thread waiting on completion of JobExecutor gets interrupted, it checks whether
         * ExecutorService is already in shutdown state. If this is not the case, it enters waiting
         * state again.
         */
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
    while (jobsPending.get() || !requestedJobs.isEmpty()) {
      try {
        TaskRequest job = requestedJobs.take();
        Task task = job.finalize(summaries, summaryVersion, alreadyPropagated);
        Future<AlgorithmStatus> future = executor.submit(task);
        watchdog.await(future);

        if (!jobsPending.get()) {
          /*
           * The watchdog had recognized before that all pending jobs completed. After doing so, it
           * set 'jobsPending' to 'false'. However, we just added a new one, so 'jobsPending'
           * becomes 'true' again, and the watchdog has to resume.
           */
          jobsPending.set(true);
          watchdog.resume();
        }
      } catch (final InterruptedException ignored) {
        /*
         * If JobExecutor gets interrupted while waiting for new tasks in requestedJobs, the while
         * condition before checks again whether jobs are still pending or additional ones have been
         * requested. If either is the case, it again calls requestedJobs.take().
         */
      } catch (final TaskInvalidatedException exception) {
        continue;
      }
    }

    /*
     * Resume watchdog such that it can shut down itself.
     * The watchdog will do so because 'jobsPending' remains 'false' from this point onward.
     */
    watchdog.resume();
    shutdown();
  }

  /**
   * Complete the shutdown of the JobExecutor after all schedules jobs have completed.
   *
   * <p><em>Precondition:</em> All scheduled jobs must have completed and no new ones requested.
   */
  private void shutdown() {
    assert requestedJobs.isEmpty();
    assert !jobsPending.get();

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

    for (final Thread waitingThread : waitingOnCompletion) {
      //noinspection SynchronizationOnLocalVariableOrMethodParameter
      synchronized (waitingThread) {
        waitingThread.notify();
      }
    }
  }

  /**
   * Request a new {@link Task} for execution.
   *
   * @param pTask The new job to execute.
   */
  void requestJob(final TaskRequest pRequest) {
    checkNotNull(pRequest);

    boolean success = false;

    while (!success) {
      try {
        requestedJobs.put(pRequest);
        success = true;
      } catch (InterruptedException ignored) {
        success = requestedJobs.contains(pRequest);
      }
    }
  }

  /**
   * {@link CompletionWatchdog} runs in a separate thread and monitors the status of its
   * accompanying {@link TaskExecutor}. For each {@link Task} requested for execution in the {@link
   * TaskExecutor}, {@link CompletionWatchdog} waits on the corresponding {@link Future}. As soon as
   * the {@link Future}s of all jobs have completed and no new ones have been requested, it
   * interrupts {@link TaskExecutor} (which is waiting for new tasks in its {@link
   * LinkedBlockingQueue}) such that it can shut down itself.
   */
  private class CompletionWatchdog implements Runnable {
    private final LinkedBlockingQueue<Future<?>> pending = new LinkedBlockingQueue<>();

    @Override
    public void run() {
      while (jobsPending.get()) {
        while (!pending.isEmpty()) {
          try {
            Future<?> future = pending.take();
            blockUntilFinished(future);
          } catch (InterruptedException ignored) {
            /*
             * If CompletionWatchdog gets interrupted while waiting for new Futures, it checks again
             * whether 'pending' is empty. If this is the case, it is time to request shutdown of
             * the JobExecutor.
             */
          }
        }

        jobsPending.set(false);

        synchronized (this) {
          executorThread.interrupt();
          do {
            try {
              wait();
            } catch (InterruptedException ignored) {
              /*
               * If CompletionWatchdog gets interrupted while waiting for JobExecutor to either
               *  a) add a new job for execution and set jobsPending to 'true' or
               *  b) shut down itself because no new jobs are available,
               * it just enters waiting state again.
               */
            }
          } while (Thread.interrupted());
        }
      }
    }

    /**
     * Let {@linkplain CompletionWatchdog CompletionWatchdog} wait until a {@link Future} completes.
     *
     * @param pFuture The {@link Future} on which to wait.
     */
    private void blockUntilFinished(final Future<?> pFuture) {
      while (!pFuture.isDone()) {
        try {
          pFuture.get();
        } catch (InterruptedException ignored) {
          /*
           * If CompletionWatchdog gets interrupted while waiting for the Future, it enters waiting
           * state again due to the enclosing loop.
           */
        } catch (ExecutionException error) {
          logManager.log(Level.WARNING, "Task failed with exception:", error.getCause());
        }
      }
    }

    /**
     * Resume {@linkplain CompletionWatchdog CompletionWatchdog} operations.
     *
     * <p>After detecting a situation in which {@link TaskExecutor} appears to have completed all
     * work, {@linkplain CompletionWatchdog CompletionWatchdog} requests shutdown of the {@link
     * TaskExecutor} and puts itself into waiting state. {@link TaskExecutor} then checks whether
     * the conditions for shutdown are really fulfilled and takes appropriate action. Afterwards,
     * {@link TaskExecutor} calls {@link #resume()} such that the {@linkplain CompletionWatchdog
     * CompletionWatchdog} can inspect the result: There either has been more work it was not aware
     * of (in this case {@linkplain CompletionWatchdog CompletionWatchdog} resumes normal
     * operation), or the shutdown request was justified and {@link TaskExecutor} shut down (in this
     * case {@linkplain CompletionWatchdog CompletionWatchdog} also terminates).
     */
    private synchronized void resume() {
      notify();
    }

    /**
     * Inform {@link CompletionWatchdog} about a new {@link Future} for whose completion it must
     * wait before asking {@link TaskExecutor} to shut down.
     *
     * @param pFuture The {@link Future} on which to wait.
     */
    private void await(Future<?> pFuture) {
      boolean success = false;
      while (!success) {
        try {
          pending.put(pFuture);
          success = true;
        } catch (InterruptedException exception) {
          /*
           * If the method gets interrupted while waiting to add the new pFuture to the list of
           * pending futures, 'success' remains 'false' and the while loop results in a new
           * attempt. This continues until the operation succeeds.
           */
        }
      }
    }
  }
}
