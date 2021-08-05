// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.Task;

public final class JobExecutor implements Runnable {
  private final ExecutorService executor;

  // private final Map<?, BooleanFormula<?>> summaries;

  public final LinkedBlockingQueue<Task> requestedJobs = new LinkedBlockingQueue<>();

  private Thread executorThread = null;

  private volatile boolean jobsPending = true;

  private final LogManager logManager;

  private class CompletionWatchdog implements Runnable {
    private final LinkedBlockingQueue<Future<?>> pending = new LinkedBlockingQueue<>();

    @Override
    public void run() {
      while(jobsPending) {
        while (!pending.isEmpty()) {
          try {
            Future<?> future = pending.take();
            future.get();
          } catch (InterruptedException ignored) {
            // todo: handle
          } catch(ExecutionException error) {
            logManager.log(Level.WARNING, "Task failed with exception:", error.getCause());
          }
        }

        jobsPending = false;

        try {
          synchronized(this) {
            executorThread.interrupt();
            wait();
          }
        } catch (InterruptedException ignored) {
          // Todo: Handle/Document
        }
      }
    }

    public synchronized void resume() {
      notify();
    }
  }

  private final CompletionWatchdog watchdog = new JobExecutor.CompletionWatchdog();

  private JobExecutor(final int pThreads, final Collection<Task> initialJobs, final LogManager pLogManager) {
    executor = Executors.newFixedThreadPool(pThreads);
    // summaries = Collections.unmodifiableMap(new ConcurrentHashMap<>());
    logManager = pLogManager;

    for(final Task job : initialJobs) {
      watchdog.pending.add(executor.submit(job));
    }
  }

  public static JobExecutor startJobExecutor(int pThreads, Collection<Task> initialJobs, LogManager logManager) {
    return new JobExecutor(pThreads, initialJobs, logManager);
  }

  public void start() {
    executorThread = new Thread(this, "Job Executor");
    executorThread.start();
    new Thread(watchdog,"Completion Watchdog").start();
  }

  @Override
  public void run() {
    /*
     * Continue to wait for new jobs in 'requestedJobs' as long as either
     * a) there are still jobs scheduled or running ('jobsPending == true'), which might add
     *    elements to the queue, or
     * b) some newly requested jobs already exist in the queue.
     */
    while(jobsPending || !requestedJobs.isEmpty()) {
      try {
        Task job = requestedJobs.take();
        watchdog.pending.add(executor.submit(job));

        if(!jobsPending) {
          /*
           * The watchdog had recognized before that all pending jobs completed.
           * After doing so, it set 'jobsPending' to 'false'.
           * However, we just added a new one, so 'jobsPending' becomes 'true' again, and the
           * watchdog has to resume.
           */
          jobsPending = true;
          watchdog.resume();
        }
      } catch(InterruptedException ignored) {
        // Todo: Handle
      }
    }

    /*
     * Resume watchdog such that it can shutdown itself.
     * The watchdog will do so because 'jobsPending' remains 'false' from this point onward.
     */
    watchdog.resume();

    List<Runnable> aborted = executor.shutdownNow();
    assert aborted.isEmpty();
  }

  public void requestJob(final Task task) {
    try {
      requestedJobs.put(task);
    } catch(InterruptedException ignored) {
      // TODO: Handle
    }

  }
}