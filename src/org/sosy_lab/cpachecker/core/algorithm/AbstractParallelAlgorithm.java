// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.or;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition.getDefaultPartition;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.conditions.ReachedSetAdjustingCPA;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets.AggregatedReachedSetManager;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;
import org.sosy_lab.cpachecker.util.resources.ThreadCpuTimeLimit;
import org.sosy_lab.cpachecker.util.statistics.StatisticsUtils;

public abstract class AbstractParallelAlgorithm implements Algorithm, StatisticsProvider {

  protected final LogManager logger;
  protected final ShutdownManager shutdownManager;
  protected final AbstractParallelAlgorithmStatistics stats;
  private final CFA cfa;
  private final AggregatedReachedSetManager aggregatedReachedSetManager;

  private final List<ConditionAdjustmentEventSubscriber> conditionAdjustmentEventSubscribers =
      new CopyOnWriteArrayList<>();
  private CFANode mainEntryNode = null;
  private ImmutableList<Callable<ParallelAnalysisResult>> analyses;

  public AbstractParallelAlgorithm(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      AggregatedReachedSets pAggregatedReachedSets,
      AbstractParallelAlgorithmStatistics pStats) {
    logger = checkNotNull(pLogger);
    shutdownManager = ShutdownManager.createWithParent(checkNotNull(pShutdownNotifier));
    cfa = checkNotNull(pCfa);

    aggregatedReachedSetManager = new AggregatedReachedSetManager();
    aggregatedReachedSetManager.addAggregated(pAggregatedReachedSets);

    stats = pStats;
  }

  private static boolean awaitTermination(
      ListeningExecutorService exec, long timeout, TimeUnit unit) {
    long timeoutNanos = unit.toNanos(timeout);
    long endNanos = System.nanoTime() + timeoutNanos;

    boolean interrupted = Thread.interrupted();
    try {
      while (true) {
        try {
          return exec.awaitTermination(timeoutNanos, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
          interrupted = false;
          timeoutNanos = Math.max(0, endNanos - System.nanoTime());
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  protected void setAnalyses(ImmutableList<Callable<ParallelAnalysisResult>> pAnalyses) {
    if (analyses != null) {
      throw new RuntimeException("setAnalyses may only be called once!");
    }
    analyses = pAnalyses;
    stats.noOfAlgorithmsUsed = pAnalyses.size();
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    mainEntryNode = AbstractStates.extractLocation(pReachedSet.getFirstState());

    ListeningExecutorService exec = listeningDecorator(newFixedThreadPool(analyses.size()));

    List<ListenableFuture<ParallelAnalysisResult>> futures = new ArrayList<>(analyses.size());
    for (Callable<ParallelAnalysisResult> call : analyses) {
      futures.add(exec.submit(call));
    }

    // shutdown the executor service,
    exec.shutdown();

    try {
      handleFutureResults(futures);

    } finally {
      // Wait some time so that all threads are shut down and we have a happens-before relation
      // (necessary for statistics).
      if (!awaitTermination(exec, 10, TimeUnit.SECONDS)) {
        logger.log(Level.WARNING, "Not all threads are terminated although we have a result.");
      }

      exec.shutdownNow();
    }

    return determineAlgorithmStatus(pReachedSet, futures);
  }

  protected abstract AlgorithmStatus determineAlgorithmStatus(
      ReachedSet pReachedSet, List<ListenableFuture<ParallelAnalysisResult>> pFutures);

  protected void handleFutureResults(List<ListenableFuture<ParallelAnalysisResult>> futures)
      throws InterruptedException, Error {

    for (ListenableFuture<ParallelAnalysisResult> f : Futures.inCompletionOrder(futures)) {
      try {
        handleSingleFutureResult(f, futures);
      } catch (ExecutionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof CPAException) {
          if (cause.getMessage().contains("recursion")) {
            logger.logUserException(
                Level.WARNING, cause, "Analysis not completed due to recursion");
          }
          if (cause.getMessage().contains("pthread_create")) {
            logger.logUserException(
                Level.WARNING, cause, "Analysis not completed due to concurrency");
          }

        } else {
          // cancel other computations
          futures.forEach(future -> future.cancel(true));
          shutdownManager.requestShutdown("cancelling all remaining analyses");
          Throwables.throwIfUnchecked(cause);
          // probably we need to handle IOException, ParserException,
          // InvalidConfigurationException, and InterruptedException here (#326)
          throw new UnexpectedCheckedException("analysis", cause);
        }
      } catch (CancellationException e) {
        // do nothing, this is normal if we cancel other analyses
      }
    }
  }

  protected abstract void handleSingleFutureResult(
      ListenableFuture<ParallelAnalysisResult> singleFuture,
      List<ListenableFuture<ParallelAnalysisResult>> futures)
      throws InterruptedException, ExecutionException, Error;

  protected Callable<ParallelAnalysisResult> createParallelAnalysis(
      String pAnalysisName,
      int pAnanlysisNumber,
      Configuration pConfiguration,
      Specification pSpecification)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    return createParallelAnalysis(
        pAnalysisName, pAnanlysisNumber, pConfiguration, pSpecification, false, false);
  }

  protected Callable<ParallelAnalysisResult> createParallelAnalysis(
      final String pAnalysisName,
      final int pAnalysisNumber,
      final Configuration pConfiguration,
      final Specification pSpecification,
      final boolean pSupplyReached,
      final boolean pSupplyRefinableReached)
      throws InvalidConfigurationException, CPAException, InterruptedException {

    final LogManager singleLogger =
        logger.withComponentName("Parallel analysis " + pAnalysisNumber);

    final ShutdownManager singleShutdownManager =
        ShutdownManager.createWithParent(shutdownManager.getNotifier());

    final ResourceLimitChecker singleAnalysisOverallLimit =
        ResourceLimitChecker.fromConfiguration(pConfiguration, singleLogger, singleShutdownManager);

    final CoreComponentsFactory coreComponents =
        new CoreComponentsFactory(
            pConfiguration,
            singleLogger,
            singleShutdownManager.getNotifier(),
            aggregatedReachedSetManager.asView());

    final ConfigurableProgramAnalysis cpa = coreComponents.createCPA(cfa, pSpecification);
    final Algorithm algorithm = coreComponents.createAlgorithm(cpa, cfa, pSpecification);
    final ReachedSet reached = coreComponents.createReachedSet(cpa);

    AtomicBoolean terminated = new AtomicBoolean(false);
    StatisticsEntry statisticsEntry =
        stats.getNewSubStatistics(
            reached,
            pAnalysisName,
            Iterables.getOnlyElement(
                FluentIterable.from(singleAnalysisOverallLimit.getResourceLimits())
                    .filter(ThreadCpuTimeLimit.class),
                null),
            terminated);
    return () -> {
      // TODO global info will not work correctly with parallel analyses
      // as it is a mutable singleton object
      GlobalInfo.getInstance().setUpInfoFromCPA(cpa);

      if (algorithm instanceof ConditionAdjustmentEventSubscriber) {
        conditionAdjustmentEventSubscribers.add((ConditionAdjustmentEventSubscriber) algorithm);
      }

      singleAnalysisOverallLimit.start();

      if (cpa instanceof StatisticsProvider) {
        ((StatisticsProvider) cpa).collectStatistics(statisticsEntry.subStatistics);
      }

      if (algorithm instanceof StatisticsProvider) {
        ((StatisticsProvider) algorithm).collectStatistics(statisticsEntry.subStatistics);
      }

      try {
        initializeReachedSet(cpa, mainEntryNode, reached);
      } catch (InterruptedException e) {
        singleLogger.logUserException(
            Level.INFO, e, "Initializing reached set took too long, analysis cannot be started");
        terminated.set(true);
        return ParallelAnalysisResult.absent(pAnalysisName);
      }

      ParallelAnalysisResult r =
          runParallelAnalysis(
              pAnalysisName,
              algorithm,
              reached,
              singleLogger,
              cpa,
              pSupplyReached,
              pSupplyRefinableReached,
              coreComponents,
              statisticsEntry);
      terminated.set(true);
      return r;
    };
  }

  private ParallelAnalysisResult runParallelAnalysis(
      final String analysisName,
      final Algorithm algorithm,
      final ReachedSet reached,
      final LogManager singleLogger,
      final ConfigurableProgramAnalysis cpa,
      final boolean supplyReached,
      final boolean supplyRefinableReached,
      final CoreComponentsFactory coreComponents,
      final StatisticsEntry pStatisticsEntry)
      throws CPAException {
    try {
      AlgorithmStatus status = null;
      ReachedSet currentReached = reached;
      AtomicReference<ReachedSet> oldReached = new AtomicReference<>();

      if (algorithm instanceof ReachedSetUpdater reachedSetUpdater) {
        reachedSetUpdater.register(
            new ReachedSetUpdateListener() {

              @Override
              public void updated(ReachedSet pReachedSet) {
                singleLogger.log(Level.INFO, "Updating reached set provided to other analyses");
                ReachedSet newReached = coreComponents.createReachedSet(pReachedSet.getCPA());
                for (AbstractState as : pReachedSet) {
                  newReached.addNoWaitlist(as, pReachedSet.getPrecision(as));
                }

                ReachedSet oldReachedSet = oldReached.get();
                if (oldReachedSet != null) {
                  aggregatedReachedSetManager.updateReachedSet(oldReachedSet, newReached);
                } else {
                  aggregatedReachedSetManager.addReachedSet(newReached);
                }
                oldReached.set(newReached);
              }
            });
      }

      if (!supplyRefinableReached) {
        status = algorithm.run(currentReached);
      } else {
        boolean stopAnalysis = true;
        do {

          // explore statespace fully only if the analysis is sound and no reachable error is found
          while (currentReached.hasWaitingState()) {
            status = algorithm.run(currentReached);
            if (!status.isSound()) {
              break;
            }
          }

          Preconditions.checkState(status != null, "algorithm should run at least once.");

          // check if we could prove the program to be safe
          if (status.isSound()
              && !from(currentReached)
                  .anyMatch(or(AbstractStates::isTargetState, AbstractStates::hasAssumptions))) {
            ReachedSet oldReachedSet = oldReached.get();
            if (oldReachedSet != null) {
              aggregatedReachedSetManager.updateReachedSet(oldReachedSet, currentReached);
            } else {
              aggregatedReachedSetManager.addReachedSet(currentReached);
            }
            return ParallelAnalysisResult.of(currentReached, status, analysisName);
          }

          // reset the flag
          stopAnalysis = true;
          for (ReachedSetAdjustingCPA innerCpa :
              CPAs.asIterable(cpa).filter(ReachedSetAdjustingCPA.class)) {
            if (innerCpa.adjustPrecision()) {
              singleLogger.log(Level.INFO, "Adjusting precision for CPA", innerCpa);

              stopAnalysis = false;
            }
          }
          for (ConditionAdjustmentEventSubscriber conditionAdjustmentEventSubscriber :
              conditionAdjustmentEventSubscribers) {
            if (stopAnalysis) {
              conditionAdjustmentEventSubscriber.adjustmentRefused(cpa);
            } else {
              conditionAdjustmentEventSubscriber.adjustmentSuccessful(cpa);
            }
          }

          if (status.isSound()) {
            singleLogger.log(Level.INFO, "Updating reached set provided to other analyses");
            ReachedSet oldReachedSet = oldReached.get();
            if (oldReachedSet != null) {
              aggregatedReachedSetManager.updateReachedSet(oldReachedSet, currentReached);
            } else {
              aggregatedReachedSetManager.addReachedSet(currentReached);
            }
            oldReached.set(currentReached);
          }

          if (!stopAnalysis) {
            currentReached = coreComponents.createReachedSet(cpa);
            pStatisticsEntry.reachedSet.set(currentReached);
            initializeReachedSet(cpa, mainEntryNode, currentReached);
          }
        } while (!stopAnalysis);
      }

      // only add to aggregated reached set if we haven't done so, and all necessary requirements
      // are fulfilled
      if (!currentReached.hasWaitingState()
          && supplyReached
          && !supplyRefinableReached
          && status.isPrecise()
          && status.isSound()) {
        aggregatedReachedSetManager.addReachedSet(currentReached);
      }

      return ParallelAnalysisResult.of(currentReached, status, analysisName);

    } catch (InterruptedException e) {
      singleLogger.log(Level.INFO, "Analysis was terminated");
      return ParallelAnalysisResult.absent(analysisName);
    }
  }

  private void initializeReachedSet(
      ConfigurableProgramAnalysis cpa, CFANode mainFunction, ReachedSet reached)
      throws InterruptedException {
    AbstractState initialState = cpa.getInitialState(mainFunction, getDefaultPartition());
    Precision initialPrecision = cpa.getInitialPrecision(mainFunction, getDefaultPartition());
    reached.add(initialState, initialPrecision);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

  public interface ReachedSetUpdateListener {

    void updated(ReachedSet pReachedSet);
  }

  public interface ReachedSetUpdater {

    void register(ReachedSetUpdateListener pReachedSetUpdateListener);

    void unregister(ReachedSetUpdateListener pReachedSetUpdateListener);
  }

  public interface ConditionAdjustmentEventSubscriber {

    void adjustmentSuccessful(ConfigurableProgramAnalysis pCpa);

    void adjustmentRefused(ConfigurableProgramAnalysis pCpa);
  }

  protected static class ParallelAnalysisResult {

    private final @Nullable ReachedSet reached;
    private final @Nullable AlgorithmStatus status;
    private final String analysisName;

    private ParallelAnalysisResult(
        @Nullable ReachedSet pReached, @Nullable AlgorithmStatus pStatus, String pAnalysisName) {
      reached = pReached;
      status = pStatus;
      analysisName = pAnalysisName;
    }

    public static ParallelAnalysisResult of(
        ReachedSet pReached, AlgorithmStatus pStatus, String pAnalysisName) {
      return new ParallelAnalysisResult(pReached, pStatus, pAnalysisName);
    }

    public static ParallelAnalysisResult absent(String pAnalysisName) {
      return new ParallelAnalysisResult(null, null, pAnalysisName);
    }

    public boolean hasValidReachedSet() {
      if (reached == null || status == null) {
        return false;
      }

      return (status.isPrecise() && from(reached).anyMatch(AbstractStates::isTargetState))
          || ((status.isSound() || !status.wasPropertyChecked())
              && !reached.hasWaitingState()
              && !from(reached)
                  .anyMatch(or(AbstractStates::hasAssumptions, AbstractStates::isTargetState)));
    }

    public @Nullable ReachedSet getReached() {
      return reached;
    }

    public @Nullable AlgorithmStatus getStatus() {
      return status;
    }

    public String getAnalysisName() {
      return analysisName;
    }
  }

  protected abstract static class AbstractParallelAlgorithmStatistics implements Statistics {

    private final LogManager logger;
    private final List<AbstractParallelAlgorithm.StatisticsEntry> allAnalysesStats =
        new CopyOnWriteArrayList<>();
    private int noOfAlgorithmsUsed = 0;

    protected AbstractParallelAlgorithmStatistics(LogManager pLogger) {
      logger = checkNotNull(pLogger);
    }

    public synchronized AbstractParallelAlgorithm.StatisticsEntry getNewSubStatistics(
        ReachedSet pReached,
        String pName,
        @Nullable ThreadCpuTimeLimit pRLimit,
        AtomicBoolean pTerminated) {
      Collection<Statistics> subStats = new CopyOnWriteArrayList<>();
      AbstractParallelAlgorithm.StatisticsEntry entry =
          new AbstractParallelAlgorithm.StatisticsEntry(
              subStats, pReached, pName, pRLimit, pTerminated);
      allAnalysesStats.add(entry);
      return entry;
    }

    public ImmutableList<StatisticsEntry> getAllAnalysesStats() {
      return ImmutableList.copyOf(allAnalysesStats);
    }

    protected int getNoOfAlgorithmsUsed() {
      return noOfAlgorithmsUsed;
    }

    @Override
    public abstract String getName();

    @Override
    public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
      out.println("Number of algorithms used:        " + noOfAlgorithmsUsed);

      printSubStatistics(out, result);
    }

    protected void printSubStatistics(PrintStream pOut, Result pResult) {
      for (AbstractParallelAlgorithm.StatisticsEntry subStats : allAnalysesStats) {
        pOut.println();
        pOut.println();
        String title = "Statistics for: " + subStats.name;
        pOut.println(title);
        pOut.println("=".repeat(title.length()));
        if (subStats.rLimit != null) {
          pOut.println(
              "Time spent in analysis thread "
                  + subStats.name
                  + ": "
                  + subStats.rLimit.getOverallUsedTime().formatAs(TimeUnit.SECONDS));
        }
        boolean terminated = subStats.terminated.get();
        if (terminated) {
          Result result = determineAnalysisResult(pResult, subStats.name);
          for (Statistics s : subStats.subStatistics) {
            StatisticsUtils.printStatistics(s, pOut, logger, result, subStats.reachedSet.get());
          }
        } else {
          logger.log(
              Level.INFO,
              "Cannot print statistics for",
              subStats.name,
              "because it is still running.");
        }
      }
      pOut.println("\n");
      pOut.println("Other statistics");
      pOut.println("================");
    }

    @Override
    public void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {
      for (AbstractParallelAlgorithm.StatisticsEntry subStats : allAnalysesStats) {
        writeSubOutputFiles(pResult, subStats);
      }
    }

    protected void writeSubOutputFiles(
        Result pResult, AbstractParallelAlgorithm.StatisticsEntry pSubStats) {
      if (pSubStats.terminated.get()) {
        Result result = determineAnalysisResult(pResult, pSubStats.name);
        for (Statistics s : pSubStats.subStatistics) {
          StatisticsUtils.writeOutputFiles(s, logger, result, pSubStats.reachedSet.get());
        }
      }
    }

    protected abstract Result determineAnalysisResult(Result pResult, String pActualAnalysisName);
  }

  protected static class StatisticsEntry {

    public final String name;
    private final Collection<Statistics> subStatistics;
    private final AtomicReference<ReachedSet> reachedSet;
    private final @Nullable ThreadCpuTimeLimit rLimit;

    private final AtomicBoolean terminated;

    public StatisticsEntry(
        Collection<Statistics> pSubStatistics,
        ReachedSet pReachedSet,
        String pName,
        @Nullable ThreadCpuTimeLimit pRLimit,
        AtomicBoolean pTerminated) {
      subStatistics = Objects.requireNonNull(pSubStatistics);
      reachedSet = new AtomicReference<>(Objects.requireNonNull(pReachedSet));
      name = Objects.requireNonNull(pName);
      rLimit = pRLimit;
      terminated = Objects.requireNonNull(pTerminated);
    }

    public ReachedSet getReached() {
      return reachedSet.get();
    }
  }
}
