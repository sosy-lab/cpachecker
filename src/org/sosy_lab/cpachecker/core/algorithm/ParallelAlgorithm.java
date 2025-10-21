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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.Uninterruptibles;
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
import java.util.concurrent.ThreadFactory;
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
import org.sosy_lab.common.time.Tickers;
import org.sosy_lab.common.time.Tickers.TickerWithUnit;
import org.sosy_lab.common.time.TimeSpan;
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
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;
import org.sosy_lab.cpachecker.util.statistics.StatisticsUtils;

@Options(prefix = "parallelAlgorithm")
public class ParallelAlgorithm implements Algorithm, StatisticsProvider {

  @Option(
      secure = true,
      required = true,
      description =
          "List of files with configurations to use. Files can be suffixed with"
              + " ::refinable to enable iterative refinement of the analysis precision"
              + " (one of the CPAs has to be instanceof ReachedSetAdjustingCPA),"
              + " ::supply-reached to enabled sharing of the (parial or finished) reached set"
              + " for use in other analyses (e.g. for invariants computation),"
              + " or ::supply-reached-refinable for both.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private List<AnnotatedValue<Path>> configFiles;

  @Option(
      secure = true,
      description = "toggle to write all the files also for the unsuccessful analyses")
  private boolean writeUnsuccessfulAnalysisFiles = false;

  protected static final String SUCCESS_MESSAGE =
      "One of the parallel analyses has finished successfully, cancelling all other runs.";

  private final Configuration globalConfig;
  protected final LogManager logger;
  protected final ShutdownManager shutdownManager;
  private final CFA cfa;
  private final Specification specification;
  protected final ParallelAlgorithmStatistics stats;

  protected ParallelAnalysisResult finalResult = null;
  protected CFANode mainEntryNode = null;
  protected final AggregatedReachedSetManager aggregatedReachedSetManager;

  protected final List<ConditionAdjustmentEventSubscriber> conditionAdjustmentEventSubscribers =
      new CopyOnWriteArrayList<>();

  protected ImmutableList<Callable<ParallelAnalysisResult>> analyses;

  public ParallelAlgorithm(
      Configuration config,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa,
      AggregatedReachedSets pAggregatedReachedSets)
      throws InvalidConfigurationException, InterruptedException {
    config.inject(this);

    stats = new ParallelAlgorithmStatistics(pLogger, writeUnsuccessfulAnalysisFiles);
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
      analysesBuilder.add(createParallelAnalysis(p));
    }
    analyses = analysesBuilder.build();
  }

  public ParallelAlgorithm(
      Configuration config,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa,
      AggregatedReachedSets pAggregatedReachedSets,
      ImmutableList<Callable<ParallelAnalysisResult>> pAnalyses)
      throws InvalidConfigurationException {
    config.inject(this, ParallelAlgorithm.class);

    stats = new ParallelAlgorithmStatistics(pLogger, writeUnsuccessfulAnalysisFiles);
    globalConfig = config;
    logger = checkNotNull(pLogger);
    shutdownManager = ShutdownManager.createWithParent(checkNotNull(pShutdownNotifier));
    specification = checkNotNull(pSpecification);
    cfa = checkNotNull(pCfa);

    aggregatedReachedSetManager = new AggregatedReachedSetManager();
    aggregatedReachedSetManager.addAggregated(pAggregatedReachedSets);

    analyses = pAnalyses;
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    mainEntryNode = AbstractStates.extractLocation(pReachedSet.getFirstState());
    ForwardingReachedSet forwardingReachedSet = (ForwardingReachedSet) pReachedSet;

    ThreadFactory threadFactory =
        Thread.ofPlatform().name(getClass().getSimpleName() + "-thread-", 0).factory();
    ListeningExecutorService exec =
        listeningDecorator(newFixedThreadPool(analyses.size(), threadFactory));

    List<ListenableFuture<ParallelAnalysisResult>> futures = new ArrayList<>(analyses.size());
    for (Callable<ParallelAnalysisResult> call : analyses) {
      futures.add(exec.submit(call));
    }

    // shut down the executor service,
    exec.shutdown();

    try {
      handleFutureResults(futures);

    } finally {
      // Wait some time so that all threads are shut down and we have a happens-before relation
      // (necessary for statistics).
      // Time limit here should be somewhat shorter than in ForceTerminationOnShutdown.
      if (!Uninterruptibles.awaitTerminationUninterruptibly(exec, 8, TimeUnit.SECONDS)) {
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

  @SuppressWarnings("checkstyle:IllegalThrows")
  protected void handleFutureResults(List<ListenableFuture<ParallelAnalysisResult>> futures)
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
        if (cause instanceof CPAException cPAException) {
          if (cause.getMessage().contains("recursion")) {
            logger.logUserException(
                Level.WARNING, cause, "Analysis not completed due to recursion");
          }
          if (cause.getMessage().contains("pthread_create")) {
            logger.logUserException(
                Level.WARNING, cause, "Analysis not completed due to concurrency");
          }
          exceptions.add(cPAException);

        } else {
          // runParallelAnalysis only declares CPAException, so this is unchecked or unexpected.
          // Cancel other computations and propagate.
          futures.forEach(future -> future.cancel(true));
          shutdownManager.requestShutdown("cancelling all remaining analyses");
          Throwables.throwIfUnchecked(cause);
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
      final AnnotatedValue<Path> pSingleConfigFileName)
      throws InvalidConfigurationException, InterruptedException {
    final Path singleConfigFileName = pSingleConfigFileName.value();
    final boolean supplyReached;
    final boolean refineAnalysis;

    final Configuration singleConfig = createSingleConfig(singleConfigFileName, logger);

    if (singleConfig == null) {
      return () -> ParallelAnalysisResult.absent(singleConfigFileName.toString());
    }

    if (pSingleConfigFileName.annotation().isPresent()) {
      switch (pSingleConfigFileName.annotation().orElseThrow()) {
        case "refinable" -> {
          supplyReached = false;
          refineAnalysis = true;
        }
        case "supply-reached" -> {
          supplyReached = true;
          refineAnalysis = false;
        }
        case "supply-reached-refinable" -> {
          supplyReached = true;
          refineAnalysis = true;
        }
        default ->
            throw new InvalidConfigurationException(
                String.format(
                    "Annotation %s is not valid for config %s in option"
                        + " parallelAlgorithm.configFiles",
                    pSingleConfigFileName.annotation(), pSingleConfigFileName.value()));
      }
    } else {
      supplyReached = false;
      refineAnalysis = false;
    }
    return createParallelAnalysis(
        singleConfig,
        ++stats.noOfAlgorithmsUsed,
        supplyReached,
        refineAnalysis,
        singleConfigFileName.toString());
  }

  protected Callable<ParallelAnalysisResult> createParallelAnalysis(
      @Nullable Configuration singleConfig,
      final int analysisNumber,
      boolean supplyReached,
      boolean supplyRefinableReached,
      String singleConfigFileName)
      throws InvalidConfigurationException, InterruptedException {

    if (singleConfig == null) {
      return () -> ParallelAnalysisResult.absent(singleConfigFileName);
    }
    final ShutdownManager singleShutdownManager =
        ShutdownManager.createWithParent(shutdownManager.getNotifier());

    final LogManager singleLogger = logger.withComponentName("Parallel analysis " + analysisNumber);

    final ResourceLimitChecker singleAnalysisOverallLimit =
        ResourceLimitChecker.fromConfiguration(singleConfig, singleLogger, singleShutdownManager);

    final CoreComponentsFactory coreComponents =
        new CoreComponentsFactory(
            singleConfig,
            singleLogger,
            singleShutdownManager.getNotifier(),
            aggregatedReachedSetManager.asView(),
            cfa);

    final ConfigurableProgramAnalysis cpa;
    final Algorithm algorithm;
    final ReachedSet reached;
    try {
      cpa = coreComponents.createCPA(specification);
      algorithm = coreComponents.createAlgorithm(cpa, specification);
      reached = coreComponents.createReachedSet(cpa);
    } catch (CPAException e) {
      singleLogger.logfUserException(Level.WARNING, e, "Failed to initialize analysis");
      return () -> ParallelAnalysisResult.absent(singleConfigFileName);
    }

    AtomicBoolean terminated = new AtomicBoolean(false);

    StatisticsEntry statisticsEntry =
        stats.getNewSubStatistics(reached, singleConfigFileName, terminated);

    return () ->
        runParallelAnalysis(
            singleConfigFileName,
            algorithm,
            reached,
            singleLogger,
            cpa,
            supplyReached,
            supplyRefinableReached,
            coreComponents,
            singleAnalysisOverallLimit,
            terminated,
            statisticsEntry);
  }

  protected ParallelAnalysisResult runParallelAnalysis(
      final String analysisName,
      final Algorithm algorithm,
      final ReachedSet reached,
      final LogManager singleLogger,
      final ConfigurableProgramAnalysis cpa,
      final boolean supplyReached,
      final boolean refineAnalysis,
      final CoreComponentsFactory coreComponents,
      final ResourceLimitChecker singleAnalysisOverallLimit,
      final AtomicBoolean terminated,
      final StatisticsEntry pStatisticsEntry)
      throws CPAException { // handleFutureResults needs to handle all the exceptions declared here
    try {
      if (algorithm
          instanceof ConditionAdjustmentEventSubscriber conditionAdjustmentEventSubscriber) {
        conditionAdjustmentEventSubscribers.add(conditionAdjustmentEventSubscriber);
      }

      singleAnalysisOverallLimit.start();

      if (cpa instanceof StatisticsProvider statisticsProvider) {
        statisticsProvider.collectStatistics(pStatisticsEntry.subStatistics);
      }

      if (algorithm instanceof StatisticsProvider statisticsProvider) {
        statisticsProvider.collectStatistics(pStatisticsEntry.subStatistics);
      }

      try {
        initializeReachedSet(cpa, mainEntryNode, reached);
      } catch (InterruptedException e) {
        singleLogger.logUserException(
            Level.INFO, e, "Initializing reached set took too long, analysis cannot be started");
        return ParallelAnalysisResult.absent(analysisName);
      }

      AlgorithmStatus status = null;
      ReachedSet currentReached = reached;

      if (!refineAnalysis) {
        if (supplyReached && algorithm instanceof ReachedSetUpdater reachedSetUpdater) {
          AtomicReference<ReachedSet> oldReached = new AtomicReference<>();
          reachedSetUpdater.register(
              new ReachedSetUpdateListener() {

                @Override
                public void updated(ReachedSet pReachedSet) {
                  singleLogger.log(Level.INFO, "Updating reached set provided to other analyses");
                  ReachedSet newReached = coreComponents.createReachedSet(pReachedSet.getCPA());
                  for (AbstractState as : pReachedSet) {
                    newReached.addNoWaitlist(as, pReachedSet.getPrecision(as));
                  }

                  updateOrAddReachedSetToReachedSetManager(oldReached.get(), newReached);
                  oldReached.set(newReached);
                }
              });
        }

        status = algorithm.run(currentReached);

        // Only add to aggregated reached set if we haven't done so, and all necessary requirements
        // are fulfilled. We should likely not do this here if the "ReachedSetUpdateListener" above
        // was used, but we have no reliable way of detecting whether the actually used algorithm
        // implements ReachedSetUpdateListener because there are some ReachedSetUpdateListener
        // classes
        // that just optionally pass through the calls.
        if (!currentReached.hasWaitingState()
            && supplyReached
            && status.isPrecise()
            && status.isSound()) {
          aggregatedReachedSetManager.addReachedSet(currentReached);
        }

      } else {
        boolean stopAnalysis = true;
        @Nullable ReachedSet oldReached = null;
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
            if (supplyReached) {
              updateOrAddReachedSetToReachedSetManager(oldReached, currentReached);
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

          if (supplyReached && status.isSound()) {
            singleLogger.log(Level.INFO, "Updating reached set provided to other analyses");
            updateOrAddReachedSetToReachedSetManager(oldReached, currentReached);
            oldReached = currentReached;
          }

          if (!stopAnalysis) {
            currentReached = coreComponents.createReachedSet(cpa);
            pStatisticsEntry.reachedSet.set(currentReached);
            initializeReachedSet(cpa, mainEntryNode, currentReached);
          }
        } while (!stopAnalysis);
      }

      return ParallelAnalysisResult.of(currentReached, status, analysisName);

    } catch (InterruptedException e) {
      singleLogger.logUserException(Level.INFO, e, "Analysis was terminated");
      return ParallelAnalysisResult.absent(analysisName);
    } finally {
      try {
        TickerWithUnit threadCputime = Tickers.getCurrentThreadCputime();
        pStatisticsEntry.threadCpuTime = TimeSpan.of(threadCputime.read(), threadCputime.unit());
      } catch (UnsupportedOperationException e) {
        singleLogger.logDebugException(e);
      }
      terminated.set(true);
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
      // TODO: log/return the config that triggers this!
      pLogger.logUserException(
          Level.WARNING,
          e,
          "Skipping one analysis in building a parallel analysis because the configuration file "
              + singleConfigFileName
              + " could not be read");
      return null;
    }
  }

  protected void initializeReachedSet(
      ConfigurableProgramAnalysis cpa, CFANode mainFunction, ReachedSet reached)
      throws InterruptedException {
    AbstractState initialState = cpa.getInitialState(mainFunction, getDefaultPartition());
    Precision initialPrecision = cpa.getInitialPrecision(mainFunction, getDefaultPartition());
    reached.add(initialState, initialPrecision);
  }

  /** Give the reached set to {@link #aggregatedReachedSetManager}. */
  private void updateOrAddReachedSetToReachedSetManager(
      @Nullable ReachedSet oldReachedSet, ReachedSet currentReached) {
    if (oldReachedSet != null) {
      aggregatedReachedSetManager.updateReachedSet(oldReachedSet, currentReached);
    } else {
      aggregatedReachedSetManager.addReachedSet(currentReached);
    }
  }

  static class ParallelAnalysisResult {

    private final @Nullable ReachedSet reached;
    private final @Nullable AlgorithmStatus status;
    private final String analysisName;

    private ParallelAnalysisResult(
        @Nullable ReachedSet pReached, @Nullable AlgorithmStatus pStatus, String pAnalysisName) {
      reached = pReached;
      status = pStatus;
      analysisName = pAnalysisName;
    }

    static ParallelAnalysisResult of(
        ReachedSet pReached, AlgorithmStatus pStatus, String pAnalysisName) {
      return new ParallelAnalysisResult(pReached, pStatus, pAnalysisName);
    }

    static ParallelAnalysisResult absent(String pAnalysisName) {
      return new ParallelAnalysisResult(null, null, pAnalysisName);
    }

    boolean hasValidReachedSet() {
      if (reached == null || status == null) {
        return false;
      }

      return (status.isPrecise() && from(reached).anyMatch(AbstractStates::isTargetState))
          || ((status.isSound() || !status.wasPropertyChecked())
              && !reached.hasWaitingState()
              && !from(reached)
                  .anyMatch(or(AbstractStates::hasAssumptions, AbstractStates::isTargetState)));
    }

    @Nullable ReachedSet getReached() {
      return reached;
    }

    @Nullable AlgorithmStatus getStatus() {
      return status;
    }

    String getAnalysisName() {
      return analysisName;
    }
  }

  protected static class ParallelAlgorithmStatistics implements Statistics {

    private final LogManager logger;
    private final List<StatisticsEntry> allAnalysesStats = new CopyOnWriteArrayList<>();
    private int noOfAlgorithmsUsed = 0;
    protected String successfulAnalysisName = null;
    private boolean writeUnsuccessfulAnalysisFiles;

    ParallelAlgorithmStatistics(LogManager pLogger, boolean pWriteUnsuccessfulAnalysisFiles) {
      logger = checkNotNull(pLogger);
      writeUnsuccessfulAnalysisFiles = pWriteUnsuccessfulAnalysisFiles;
    }

    synchronized StatisticsEntry getNewSubStatistics(
        ReachedSet pReached, String pName, AtomicBoolean pTerminated) {
      Collection<Statistics> subStats = new CopyOnWriteArrayList<>();
      StatisticsEntry entry = new StatisticsEntry(subStats, pReached, pName, pTerminated);
      allAnalysesStats.add(entry);
      return entry;
    }

    @Override
    public String getName() {
      return "Parallel Algorithm";
    }

    @Override
    public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
      out.println("Number of algorithms used:        " + allAnalysesStats.size());
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
        if (subStats.threadCpuTime != null) {
          pOut.println(
              "Time spent in analysis thread "
                  + subStats.name
                  + ": "
                  + subStats.threadCpuTime.formatAs(TimeUnit.SECONDS));
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
        } else if (writeUnsuccessfulAnalysisFiles) {
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

  protected static class StatisticsEntry {

    private final Collection<Statistics> subStatistics;

    protected final AtomicReference<ReachedSet> reachedSet;

    private final String name;

    private volatile @Nullable TimeSpan threadCpuTime;

    private final AtomicBoolean terminated;

    private StatisticsEntry(
        Collection<Statistics> pSubStatistics,
        ReachedSet pReachedSet,
        String pName,
        AtomicBoolean pTerminated) {
      subStatistics = Objects.requireNonNull(pSubStatistics);
      reachedSet = new AtomicReference<>(Objects.requireNonNull(pReachedSet));
      name = Objects.requireNonNull(pName);
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
