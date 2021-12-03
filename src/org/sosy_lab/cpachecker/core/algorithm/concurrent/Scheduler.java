// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;
import static org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus.NO_PROPERTY_CHECKED;
import static org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus.SOUND_AND_PRECISE;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.ConcurrentStatisticsCollector.TaskStatistics;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.Message;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.completion.ErrorReachedProgramEntryMessage;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.completion.TaskAbortedMessage;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.completion.TaskCompletedMessage;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.request.RequestInvalidatedException;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.request.TaskRequest;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.Task;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.backward.BackwardAnalysisCore;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.forward.ForwardAnalysisCore;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.util.ErrorOrigin;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.util.ReusableCoreComponents;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.util.ShareableBooleanFormula;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;

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
  private final String benignShutdownReason = "Error reached program entry.";
  private final LogManager logManager;
  private final Collection<Thread> waitingForCompletion = new ArrayList<>();
  private final Thread schedulerThread = new Thread(this, Scheduler.getThreadName());

  private final Table<Block, Block, ShareableBooleanFormula> summaries = HashBasedTable.create();
  private final Map<Block, Integer> summaryVersion = new HashMap<>();
  private final Set<CFANode> alreadyPropagated = new HashSet<>();
  private final ConcurrentStatisticsCollector statisticsCollector;
  private int jobCount = 0;
  private volatile boolean complete = false;
  private volatile Optional<ErrorOrigin> target = Optional.empty();
  private volatile AlgorithmStatus status = SOUND_AND_PRECISE;

  private final BlockingQueue<ReusableCoreComponents> idleForwardAnalysisComponents 
      = new LinkedBlockingQueue<>();
  private final BlockingQueue<ReusableCoreComponents> idleBackwardAnalysisComponents 
      = new LinkedBlockingQueue<>();
  
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

    statisticsCollector = new ConcurrentStatisticsCollector();
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
          if (!complete) {
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
    if (complete) {
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
    shutdownManager.requestShutdown(benignShutdownReason);
    executor.shutdownNow();
    messages.clear();
    jobCount = 0;
    complete = true;

    shutdown();
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statisticsCollector.collectStatistics(statsCollection);
  }

  public AlgorithmStatus getStatus() {
    if (!complete) {
      logManager.log(WARNING, "Status requested from Scheduler before analysis completed.\n"
          + "Reporting NO_PROPERTY_CHECKED.");
      return NO_PROPERTY_CHECKED;
    }

    return status;
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

      retrieveReusableComponents(pMessage.getTask());
      
      status = status.update(pMessage.getStatus());
      final TaskStatistics statistics = pMessage.getStatistics();
      statistics.accept(statisticsCollector);

      shutdownIfComplete();
    }

    public void visit(@SuppressWarnings("unused") final TaskAbortedMessage pMessage) {
      /*
       * If tasks abort because a shutdown has been requested, there can be two different reasons: 
       * (a) The analysis is complete because an error condition reached program entry. 
       *     In this case, the scheduler must continue to track completed and running tasks and will
       *     eventually reach a state where all tasks have stopped and where it can report the 
       *     analysis as complete.
       * (b) Shutdown has been requested for a reason external to the analysis, i.e. the analysis
       *     gets cancelled. In this case, tasks might abort in an unsound state and the analysis 
       *     must be marked as incomplete. To do so, the scheduler stops to track the number of 
       *     running tasks by not decrementing it within this method. As a result, if getStatus() 
       *     gets called later on, the scheduler will report the analysis as incomplete.  
       * 
       * If no shutdown has been requested, the task aborted because it has been invalidated or 
       * due to other expected situations within the algorithm. In this case, the scheduler 
       * continues to track the number of running jobs. 
       */
      ShutdownNotifier notifier = shutdownManager.getNotifier();
      if(notifier.shouldShutdown()) {
        if(!notifier.getReason().equals(benignShutdownReason)) {
          /*
           * Shutdown requested for a reason external to the analysis. 
           * Give up on tracking the number of running tasks, which will lead to the analysis 
           * getting reported as incomplete later-on.  
           */
          return;
        }
      }
      
      --jobCount;
      shutdownIfComplete();
    }

    public void visit(final ErrorReachedProgramEntryMessage pMessage) {
      logManager.log(FINE, "Error reached program entry.");
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

    @SuppressFBWarnings(
        value="RV_RETURN_VALUE_IGNORED", 
        justification="Reusable components are stored and retrieved on a best-effort basis. " 
            + "If storing components with offer() is not possible immediately, the components are " 
            + "just discarded (which is okay). Therefore, there is no need to check the return " 
            + "value of offer()."
    )
    
    private void retrieveReusableComponents(final Task pTask) {
      /*
       * If the completed task is a BackwardAnalysisCore which has created a 
       * BackwardAnalysisContinuationRequest, no component reuse is possible (yet), because the 
       * continued backward analysis already re-uses the components.
       * 
       * Todo: Remove ContinuationRequests as seperate messages/job requests altogether.
       *       Implement a different way to ensure that non-terminating BackwardAnalysis jobs don't
       *       starve the analysis by occupying all available threads.
       */
      if(pTask instanceof BackwardAnalysisCore) {
        BackwardAnalysisCore analysis = (BackwardAnalysisCore) pTask;
        if(analysis.hasCreatedContinuationRequest()) {
          return;
        }
      }

      /*if(pTask instanceof ForwardAnalysisCore) {
        BackwardAnalysisCore analysis = (BackwardAnalysisCore) pTask;
        if(analysis.hasCreatedContinuationRequest()) {
          return;
        }
      }*/
      
      /*ARGCPA argcpa = pTask.getCPA();
      CompositeCPA compositeCPA = ((WrapperCPA)argcpa).retrieveWrappedCpa(CompositeCPA.class);
      
      ReusableCoreComponents reusableCoreComponents
          = new ReusableCoreComponents(compositeCPA, pTask.getAlgorithm(), pTask.getReachedSet());
      */
      /*
       * Best-effort attempt to store reusable components.
       * If not possible immediately, they get discarded.
       */
      /*if(pTask instanceof ForwardAnalysisCore) {
        idleForwardAnalysisComponents.offer(reusableCoreComponents);
      } else if(pTask instanceof BackwardAnalysisCore) {
        idleBackwardAnalysisComponents.offer(reusableCoreComponents);
      }*/
    }
  }
  
  public Optional<ReusableCoreComponents> requestIdleForwardAnalysisComponents() {
    return Optional.ofNullable(idleForwardAnalysisComponents.poll()); 
  }

  public Optional<ReusableCoreComponents> requestIdleBackwardAnalysisComponents() {
    return Optional.ofNullable(idleBackwardAnalysisComponents.poll());
  }
}
