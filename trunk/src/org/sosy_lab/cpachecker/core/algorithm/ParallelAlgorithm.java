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
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
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
import org.sosy_lab.common.configuration.AnnotatedValue;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
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
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CompoundException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;
import org.sosy_lab.cpachecker.util.resources.ThreadCpuTimeLimit;
import org.sosy_lab.cpachecker.util.statistics.StatisticsUtils;

@Options(prefix = "parallelAlgorithm")
public class ParallelAlgorithm implements Algorithm, StatisticsProvider {

  @Option(
      secure = true,
      required = true,
      description =
          "List of files with configurations to use. Files can be suffixed with"
              + " ::supply-reached this signalizes that the (finished) reached set"
              + " of an analysis can be used in other analyses (e.g. for invariants"
              + " computation). If you use the suffix ::supply-reached-refinable instead"
              + " this means that the reached set supplier is additionally continously"
              + " refined (so one of the analysis has to be instanceof ReachedSetAdjustingCPA)"
              + " to make this work properly.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private List<AnnotatedValue<Path>> configFiles;

  private static final String SUCCESS_MESSAGE =
      "One of the parallel analyses has finished successfully, cancelling all other runs.";

  private final Configuration globalConfig;
  private final LogManager logger;
  private final ShutdownManager shutdownManager;
  private final CFA cfa;
  private final Specification specification;
  private final ParallelAlgorithmStatistics stats;

  private ParallelAnalysisResult finalResult = null;
  private CFANode mainEntryNode = null;
  private final AggregatedReachedSetManager aggregatedReachedSetManager;

  private final List<ConditionAdjustmentEventSubscriber> conditionAdjustmentEventSubscribers =
      new CopyOnWriteArrayList<>();

  private final ImmutableList<Callable<ParallelAnalysisResult>> analyses;

  public ParallelAlgorithm(
      Configuration config,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa,
      AggregatedReachedSets pAggregatedReachedSets)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    config.inject(this);

    stats = new ParallelAlgorithmStatistics(pLogger);
    globalConfig = config;
    logger = checkNotNull(pLogger);
    shutdownManager = ShutdownManager.createWithParent(checkNotNull(pShutdownNotifier));
    specification = checkNotNull(pSpecification);
    cfa = checkNotNull(pCfa);

    aggregatedReachedSetManager = new AggregatedReachedSetManager();
    aggregatedReachedSetManager.addAggregated(pAggregatedReachedSets);

    ImmutableList.Builder<Callable<ParallelAnalysisResult>> analysesBuilder =
        ImmutableList.builder();
    for (AnnotatedValue<Path> p : configFiles) {
      analysesBuilder.add(createParallelAnalysis(p, ++stats.noOfAlgorithmsUsed));
    }
    analyses = analysesBuilder.build();
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    mainEntryNode = AbstractStates.extractLocation(pReachedSet.getFirstState());
    ForwardingReachedSet forwardingReachedSet = (ForwardingReachedSet) pReachedSet;

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

    if (finalResult != null) {
      forwardingReachedSet.setDelegate(finalResult.getReached());
      return finalResult.getStatus();
    }

    return AlgorithmStatus.UNSOUND_AND_PRECISE;
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

  private void handleFutureResults(List<ListenableFuture<ParallelAnalysisResult>> futures)
      throws InterruptedException, Error, CPAException {

    List<CPAException> exceptions = new ArrayList<>();
    for (ListenableFuture<ParallelAnalysisResult> f : Futures.inCompletionOrder(futures)) {
      try {
        ParallelAnalysisResult result = f.get();
        if (result.hasValidReachedSet() && finalResult == null) {
          finalResult = result;
          stats.successfulAnalysisName = result.getAnalysisName();

          // cancel other computations
          futures.forEach(future -> future.cancel(true));
          logger.log(Level.INFO, result.getAnalysisName() + " finished successfully.");
          shutdownManager.requestShutdown(SUCCESS_MESSAGE);
        } else if (!result.hasValidReachedSet()) {
          logger.log(Level.INFO, result.getAnalysisName() + " finished without usable result.");
        }
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
          exceptions.add((CPAException) cause);

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

    // we do not have any result, so we propagate the found CPAExceptions upwards
    if (finalResult == null && !exceptions.isEmpty()) {
      if (exceptions.size() == 1) {
        throw Iterables.getOnlyElement(exceptions);
      } else {
        throw new CompoundException(exceptions);
      }
    }
  }

  private Callable<ParallelAnalysisResult> createParallelAnalysis(
      final AnnotatedValue<Path> pSingleConfigFileName, final int analysisNumber)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    final Path singleConfigFileName = pSingleConfigFileName.value();
    final boolean supplyReached;
    final boolean supplyRefinableReached;

    final Configuration singleConfig = createSingleConfig(singleConfigFileName, logger);
    if (singleConfig == null) {
      return () -> ParallelAnalysisResult.absent(singleConfigFileName.toString());
    }
    final ShutdownManager singleShutdownManager =
        ShutdownManager.createWithParent(shutdownManager.getNotifier());

    final LogManager singleLogger = logger.withComponentName("Parallel analysis " + analysisNumber);

    if (pSingleConfigFileName.annotation().isPresent()) {
      switch (pSingleConfigFileName.annotation().orElseThrow()) {
        case "supply-reached":
          supplyReached = true;
          supplyRefinableReached = false;
          break;
        case "supply-reached-refinable":
          supplyReached = false;
          supplyRefinableReached = true;
          break;
        default:
          throw new InvalidConfigurationException(
              String.format(
                  "Annotation %s is not valid for config %s in option"
                      + " parallelAlgorithm.configFiles",
                  pSingleConfigFileName.annotation(), pSingleConfigFileName.value()));
      }
    } else {
      supplyReached = false;
      supplyRefinableReached = false;
    }

    final ResourceLimitChecker singleAnalysisOverallLimit =
        ResourceLimitChecker.fromConfiguration(singleConfig, singleLogger, singleShutdownManager);

    final CoreComponentsFactory coreComponents =
        new CoreComponentsFactory(
            singleConfig,
            singleLogger,
            singleShutdownManager.getNotifier(),
            aggregatedReachedSetManager.asView());

    final ConfigurableProgramAnalysis cpa = coreComponents.createCPA(cfa, specification);
    final Algorithm algorithm = coreComponents.createAlgorithm(cpa, cfa, specification);
    final ReachedSet reached = coreComponents.createReachedSet(cpa);

    AtomicBoolean terminated = new AtomicBoolean(false);
    StatisticsEntry statisticsEntry =
        stats.getNewSubStatistics(
            reached,
            singleConfigFileName.toString(),
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
        return ParallelAnalysisResult.absent(singleConfigFileName.toString());
      }

      ParallelAnalysisResult r =
          runParallelAnalysis(
              singleConfigFileName.toString(),
              algorithm,
              reached,
              singleLogger,
              cpa,
              supplyReached,
              supplyRefinableReached,
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

      if (algorithm instanceof ReachedSetUpdater) {
        ReachedSetUpdater reachedSetUpdater = (ReachedSetUpdater) algorithm;
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

  @Nullable
  private Configuration createSingleConfig(Path singleConfigFileName, LogManager pLogger) {
    try {
      ConfigurationBuilder singleConfigBuilder = Configuration.builder();
      singleConfigBuilder.copyFrom(globalConfig);
      singleConfigBuilder.clearOption("parallelAlgorithm.configFiles");
      singleConfigBuilder.clearOption("analysis.useParallelAnalyses");
      singleConfigBuilder.loadFromFile(singleConfigFileName);

      Configuration singleConfig = singleConfigBuilder.build();
      NestingAlgorithm.checkConfigs(globalConfig, singleConfig, singleConfigFileName, logger);
      return singleConfig;

    } catch (IOException | InvalidConfigurationException e) {
      pLogger.logUserException(
          Level.WARNING,
          e,
          "Skipping one analysis because the configuration file "
              + singleConfigFileName
              + " could not be read");
      return null;
    }
  }

  private void initializeReachedSet(
      ConfigurableProgramAnalysis cpa, CFANode mainFunction, ReachedSet reached)
      throws InterruptedException {
    AbstractState initialState = cpa.getInitialState(mainFunction, getDefaultPartition());
    Precision initialPrecision = cpa.getInitialPrecision(mainFunction, getDefaultPartition());
    reached.add(initialState, initialPrecision);
  }

  private static class ParallelAnalysisResult {

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

  private static class ParallelAlgorithmStatistics implements Statistics {

    private final LogManager logger;
    private final List<StatisticsEntry> allAnalysesStats = new CopyOnWriteArrayList<>();
    private int noOfAlgorithmsUsed = 0;
    private String successfulAnalysisName = null;

    ParallelAlgorithmStatistics(LogManager pLogger) {
      logger = checkNotNull(pLogger);
    }

    public synchronized StatisticsEntry getNewSubStatistics(
        ReachedSet pReached,
        String pName,
        @Nullable ThreadCpuTimeLimit pRLimit,
        AtomicBoolean pTerminated) {
      Collection<Statistics> subStats = new CopyOnWriteArrayList<>();
      StatisticsEntry entry = new StatisticsEntry(subStats, pReached, pName, pRLimit, pTerminated);
      allAnalysesStats.add(entry);
      return entry;
    }

    @Override
    public String getName() {
      return "Parallel Algorithm";
    }

    @Override
    public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
      out.println("Number of algorithms used:        " + noOfAlgorithmsUsed);
      if (successfulAnalysisName != null) {
        out.println("Successful analysis: " + successfulAnalysisName);
      }
      printSubStatistics(out, result);
    }

    private void printSubStatistics(PrintStream pOut, Result pResult) {
      for (StatisticsEntry subStats : allAnalysesStats) {
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
      StatisticsEntry successfullAnalysisStats = null;
      for (StatisticsEntry subStats : allAnalysesStats) {
        if (isSuccessfulAnalysis(subStats)) {
          successfullAnalysisStats = subStats;
        } else {
          writeSubOutputFiles(pResult, subStats);
        }
      }
      if (successfullAnalysisStats != null) {
        writeSubOutputFiles(pResult, successfullAnalysisStats);
      }
    }

    private void writeSubOutputFiles(Result pResult, StatisticsEntry pSubStats) {
      if (pSubStats.terminated.get()) {
        Result result = determineAnalysisResult(pResult, pSubStats.name);
        for (Statistics s : pSubStats.subStatistics) {
          StatisticsUtils.writeOutputFiles(s, logger, result, pSubStats.reachedSet.get());
        }
      }
    }

    private boolean isSuccessfulAnalysis(StatisticsEntry pStatEntry) {
      return successfulAnalysisName != null && successfulAnalysisName.equals(pStatEntry.name);
    }

    private Result determineAnalysisResult(Result pResult, String pActualAnalysisName) {
      if (successfulAnalysisName != null && !successfulAnalysisName.equals(pActualAnalysisName)) {
        if (pResult == Result.TRUE) {
          // we need this to let the invariant analysis write a correctness witness if we use
          // k-induction. TODO: find a better fix for this mess.
          return Result.UNKNOWN;
        } else {
          // Signal that this analysis is not important for the final verdict:
          // (especially, do NOT generate a correctness witness)
          return Result.DONE;
        }
      }
      return pResult;
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

  private static class StatisticsEntry {

    private final Collection<Statistics> subStatistics;

    private final AtomicReference<ReachedSet> reachedSet;

    private final String name;

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
}
