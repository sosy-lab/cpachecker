// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus.SOUND_AND_PRECISE;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.Message;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.completion.ErrorReachedProgramEntryMessage;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.completion.TaskAbortedMessage;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.completion.TaskCompletedMessage;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.request.RequestInvalidatedException;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.request.TaskRequest;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.Task;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.util.ErrorOrigin;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.util.ShareableBooleanFormula;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;

/**
 * JobExecutor manages execution of concurrent analysis tasks from {@linkplain
 * org.sosy_lab.cpachecker.core.algorithm.concurrent.task concurrent.task}.
 *
 * <p>After creating the executor using the public constructor {@link #Scheduler(int,
 * LogManager, ShutdownManager)}, user can request execution of {@link Task}s with 
 * {@link #sendMessage(Message)}. In particular, scheduled tasks can use this method to spawn 
 * further tasks themselves. Actual execution starts as soon as {@link #start()} gets called. 
 * {@link Scheduler} shuts down as soon as all requested jobs (including ones spawned by running 
 * jobs themselves) have completed. To wait for this situation, the user can call 
 * {@link #waitForCompletion()}, which blocks until all jobs have completed.
 *
 * <p>{@link Scheduler} can modify requested jobs before actually posting them for execution. It
 * does so if data with which a new job has been created (e.g. a block summary) has become outdated
 * due to a concurrent task which completed and which calculated an updated version of such data.
 */
public final class Scheduler implements Runnable, StatisticsProvider {
  private final LinkedBlockingQueue<Message> messages = new LinkedBlockingQueue<>();
  private final ExecutorService executor;
  private final ShutdownManager shutdownManager;
  private final LogManager logManager;
  private final Collection<Thread> waitingForCompletion = new ArrayList<>();
  private final Thread schedulerThread = new Thread(this, Scheduler.getThreadName());
  
  private final Table<Block, Block, ShareableBooleanFormula> summaries = HashBasedTable.create();
  private final Map<Block, Integer> summaryVersion = new HashMap<>();
  private final Set<CFANode> alreadyPropagated = new HashSet<>();
  
  private int jobCount = 0;
  private volatile boolean complete = false;
  private volatile Optional<ErrorOrigin> target = Optional.empty();
  private volatile AlgorithmStatus status = SOUND_AND_PRECISE;
  
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
      final ShutdownManager pShutdownManager) {
    executor = Executors.newFixedThreadPool(pThreads);
    logManager = pLogManager;
    shutdownManager = pShutdownManager;
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
  public Optional<ErrorOrigin> waitForCompletion() {
    Thread currentThread = Thread.currentThread();
    waitingForCompletion.add(currentThread);

    while (!complete) {
      try {
        synchronized (currentThread) {
          if(!complete) {
            currentThread.wait(); 
          }
        }
      } catch (final InterruptedException exception) {
        if (shutdownManager.getNotifier().shouldShutdown()) {
          return target;
        }
      }
    }
    
    return target;
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
    while (!complete && (jobCount > 0 || !messages.isEmpty())) {
      try {
        Message message = messages.take();
        message.accept(new MessageProcessingVisitor());
      } catch (final InterruptedException ignored) {
        if (shutdownManager.getNotifier().shouldShutdown()) {
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
    
    /*
     * During regular shutdown, 'jobCount' must equal 0 (because this triggers the shutdown).
     * If shutdown has been requested because an error reached the program entry, then 'jobCount' 
     * might be different from 0. In this case however, 'errorReachedProgramEntry()' already set 
     * 'complete' to 'false'.
     */
    assert jobCount == 0 || complete;

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
    
    for (final Thread waitingThread : waitingForCompletion) {
      synchronized (waitingThread) {
        complete = true;
        
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
    
    /* Best-effort early-return. */
    if(complete) {
      return;
    }
    
    boolean success = false;

    while (!success) {
      try {
        messages.put(pMessage);
        success = true;
      } catch (InterruptedException ignored) {
        if (shutdownManager.getNotifier().shouldShutdown()) {
          return;
        }
      }
    }
  }
  
  private void errorReachedProgramEntry() {
    shutdownManager.requestShutdown("Error reached program entry");
    
    executor.shutdownNow();
    messages.clear();
    jobCount = 0;
    complete = true;
    
    shutdown();
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    
  }

  public class MessageProcessingVisitor {
    public void visit(final TaskRequest pMessage) {
      try {
        Task newTask = pMessage.process(summaries, summaryVersion, alreadyPropagated);
        executor.execute(newTask);
        
        ++jobCount;
      } catch (final RequestInvalidatedException ignored) {
        logManager.log(Level.INFO, "Request has been invalidated!", pMessage);
      } catch (final Exception exception) {
        logManager.log(Level.SEVERE, "Exception occurred!", exception);
      } catch (final AssertionError assertion) {
        logManager.log(Level.SEVERE, "Assertion violated!", assertion);
      }
    }

    public void visit(final TaskCompletedMessage pMessage) {
      --jobCount;
      status = status.update(pMessage.getStatus());
      
      shutdownIfComplete();
    }

    public void visit(final TaskAbortedMessage pMessage) {
      --jobCount;
      shutdownIfComplete();
    }
    
    public void visit(final ErrorReachedProgramEntryMessage pMessage) {
      target = Optional.of(pMessage.getOrigin());
      status = status.update(pMessage.getStatus());
      errorReachedProgramEntry();
    }
    
    private void shutdownIfComplete() {
      if (jobCount == 0 && messages.isEmpty()) {
        logManager.log(Level.INFO, "All tasks completed.");
        schedulerThread.interrupt();
        shutdown();
      }
    }
  }
  
  public AlgorithmStatus getStatus() {
    assert complete : "Scheduler only reports status after analysis completion";
    return status;
  }
}
