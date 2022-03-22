// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.parallel_bam;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm.CPAAlgorithmFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.bam.BAMCPAWithBreakOnMissingBlock;
import org.sosy_lab.cpachecker.cpa.bam.BAMReachedSetValidator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CompoundException;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatHist;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsSeries;
import org.sosy_lab.cpachecker.util.statistics.StatisticsSeries.NoopStatisticsSeries;
import org.sosy_lab.cpachecker.util.statistics.StatisticsSeries.StatisticsSeriesWithNumbers;
import org.sosy_lab.cpachecker.util.statistics.StatisticsUtils;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer;

@Options(prefix = "algorithm.parallelBam")
public class ParallelBAMAlgorithm implements Algorithm, StatisticsProvider {

  @Option(
      description =
          "number of threads, positive values match exactly, "
              + "with -1 we use the number of available cores or the machine automatically.",
      secure = true)
  private int numberOfThreads = -1;

  @Option(description = "export number of running RSE instances as CSV", secure = true)
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path runningRSESeriesFile = Path.of("RSESeries.csv");

  private final ParallelBAMStatistics stats = new ParallelBAMStatistics();
  private final LogManager logger;
  private final LogManagerWithoutDuplicates oneTimeLogger;
  private final BAMCPAWithBreakOnMissingBlock bamcpa;
  private final AlgorithmFactory algorithmFactory;
  private final ShutdownNotifier shutdownNotifier;

  public ParallelBAMAlgorithm(
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    bamcpa = (BAMCPAWithBreakOnMissingBlock) pCpa;
    logger = pLogger;
    oneTimeLogger = new LogManagerWithoutDuplicates(pLogger);
    shutdownNotifier = pShutdownNotifier;
    algorithmFactory = new CPAAlgorithmFactory(bamcpa, logger, pConfig, pShutdownNotifier);
  }

  @Override
  public AlgorithmStatus run(final ReachedSet mainReachedSet)
      throws CPAException, InterruptedException {
    stats.wallTime.start();
    try {
      return run0(mainReachedSet);
    } finally {
      stats.wallTime.stop();
    }
  }

  private AlgorithmStatus run0(final ReachedSet mainReachedSet)
      throws CPAException, InterruptedException {

    final ConcurrentMap<ReachedSet, ReachedSetExecutor> reachedSetMapping =
        new ConcurrentHashMap<>();
    final int numberOfCores = getNumberOfCores();
    oneTimeLogger.logfOnce(Level.INFO, "creating pool for %d threads", numberOfCores);

    ThreadFactory threadFactory =
        new ThreadFactoryBuilder()
            .setDaemon(true) // for killing hanging threads at program exit
            .setNameFormat("ParallelBAM-thread-%d")
            .build();
    final ExecutorService pool = Executors.newFixedThreadPool(numberOfCores, threadFactory);
    final List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
    final AtomicBoolean terminateAnalysis = new AtomicBoolean(false);
    final AtomicInteger scheduledJobs = new AtomicInteger(0);

    {
      int running = stats.numActiveThreads.get();
      assert running == 0;
      stats.runningRSESeries.add(running);
    }

    ReachedSetExecutor rse =
        new ReachedSetExecutor(
            bamcpa,
            mainReachedSet,
            bamcpa.getBlockPartitioning().getMainBlock(),
            true,
            reachedSetMapping,
            pool,
            algorithmFactory,
            shutdownNotifier,
            stats,
            errors,
            terminateAnalysis,
            scheduledJobs,
            logger);
    reachedSetMapping.put(mainReachedSet, rse); // backwards reference

    // start analysis
    rse.addNewTask(rse.asRunnable());

    boolean isSound = true;
    try {
      // TODO Shutown hook seems to never be called
      // ShutdownRequestListener hook = new ShutdownRequestListener() {
      // @Override
      // public void shutdownRequested(String pReason) {pool.shutdownNow();}};
      // shutdownNotifier.register(hook);
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

    } finally {
      int maxAssassinations = 5;
      for (int i = 0; i < maxAssassinations && !pool.isTerminated(); i++) {
        // in case of problems we must kill the thread pool,
        // otherwise we have a running daemon thread and CPAchecker does not terminate.
        try {
          logger.logf(
              Level.INFO,
              "threadpool did not terminate, killing threadpool now (try %d of %d).",
              i + 1,
              maxAssassinations);
          logger.log(Level.ALL, "remaining dependencies:\n", rse.getDependenciesAsDot());
          pool.shutdown();
          pool.awaitTermination(100, TimeUnit.MILLISECONDS);
        } finally {
          pool.shutdownNow();
        }
        isSound = false;
      }
      if (!pool.isTerminated()) {
        logger.log(
            Level.WARNING,
            "threadpool is not yet dead, some thread is alive and we cannot interupt it.");
      }
    }

    collectExceptions(reachedSetMapping, errors, mainReachedSet);

    //    assert targetStateFound
    //        || (dependencyGraph.dependsOn.isEmpty()
    //            && dependencyGraph.dependsFrom.isEmpty()) : "dependencyGraph:" + dependencyGraph;

    //    readdStatesToWaitlists(dependencyGraph);

    assert BAMReachedSetValidator.validateData(
        bamcpa.getData(), bamcpa.getBlockPartitioning(), new ARGReachedSet(mainReachedSet));

    {
      int running = stats.numActiveThreads.get();
      assert running == 0;
      stats.runningRSESeries.add(running);
    }

    return AlgorithmStatus.SOUND_AND_PRECISE.withSound(isSound);
  }

  private int getNumberOfCores() {
    if (numberOfThreads > 0) {
      return numberOfThreads;
    }
    Preconditions.checkState(
        numberOfThreads == -1, "number of threads can only be a positive number or -1.");
    return Runtime.getRuntime().availableProcessors();
  }

  /**
   * We check here whether an error occured in a CompletableFuture. We could also ignore this step,
   * but that might be dangerous and error-prone.
   */
  private void collectExceptions(
      Map<ReachedSet, ReachedSetExecutor> pReachedSetMapping,
      List<Throwable> errors,
      final ReachedSet mainReachedSet)
      throws CPAException, InterruptedException {

    final AtomicBoolean mainRScontainsTarget = new AtomicBoolean(false);
    final AtomicBoolean otherRScontainsTarget = new AtomicBoolean(false);

    pReachedSetMapping.entrySet().parallelStream()
        .forEach(
            entry -> {
              ReachedSetExecutor rse = entry.getValue();
              CompletableFuture<Void> job = rse.getWaitingTasks();
              try {
                job.get(5, TimeUnit.SECONDS);
                stats.executionCounter.insertValue(entry.getValue().execCounter);
                stats.unfinishedRSEcounter.inc();

                if (rse.isTargetStateFound()) {
                  if (entry.getKey() == mainReachedSet) {
                    mainRScontainsTarget.set(true);
                  } else {
                    otherRScontainsTarget.set(true);
                  }
                }

              } catch (RejectedExecutionException
                  | ExecutionException
                  | InterruptedException
                  | TimeoutException e) {
                errors.add(e);
              }
              logger.log(Level.ALL, "finishing", rse, job.isCompletedExceptionally());
            });

    if (!errors.isEmpty()) {
      logger.log(Level.ALL, "The following errors appeared in the analysis:", errors);
      List<CPAException> cpaExceptions = new ArrayList<>();
      for (Throwable toThrow : errors) {
        if (toThrow instanceof Error) { // something very serious
          addSuppressedAndThrow((Error) toThrow, errors);
        } else if (toThrow instanceof RuntimeException) { // something less serious
          addSuppressedAndThrow((RuntimeException) toThrow, errors);
        } else if (toThrow instanceof InterruptedException) {
          addSuppressedAndThrow((InterruptedException) toThrow, errors);
        } else if (toThrow instanceof CPAException) {
          cpaExceptions.add((CPAException) toThrow);
        } else {
          // here, we add one suppressed too much, but that should be irrelevant
          addSuppressedAndThrow(new UnexpectedCheckedException("ParallelBAM", toThrow), errors);
        }
      }
      // if there was no other type of exception, we can throw the CPAException directly.
      if (cpaExceptions.size() == 1) {
        throw cpaExceptions.get(0);
      } else {
        throw new CompoundException(cpaExceptions);
      }
    }

    checkState(
        mainRScontainsTarget.get() == otherRScontainsTarget.get(),
        "when a target is found in a sub-analysis (%s), we expect a target in the main-reached-set"
            + " (%s)",
        otherRScontainsTarget.get(),
        mainRScontainsTarget.get());
  }

  /**
   * This method adds all other exceptions as suppressed exceptions to the given throwable element
   * and throws it.
   */
  private <T extends Throwable> void addSuppressedAndThrow(T toThrow, List<Throwable> errors)
      throws T {
    for (Throwable otherErrors : Iterables.filter(errors, e -> e != toThrow)) {
      toThrow.addSuppressed(otherErrors);
    }
    throw toThrow;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

  @SuppressWarnings("deprecation")
  class ParallelBAMStatistics implements Statistics {
    final StatTimer wallTime = new StatTimer("Time for execution of algorithm");
    final ThreadSafeTimerContainer threadTime =
        new ThreadSafeTimerContainer("Time for RSE execution");
    final ThreadSafeTimerContainer addingStatesTime =
        new ThreadSafeTimerContainer("Time for adding states to RSE");
    final ThreadSafeTimerContainer terminationCheckTime =
        new ThreadSafeTimerContainer("Time for terminating RSE");
    final LongAccumulator numMaxRSE = new LongAccumulator(Math::max, 0);
    final AtomicInteger numActiveThreads = new AtomicInteger(0);
    final StatHist histActiveThreads = new StatHist("Active threads");
    final StatHist executionCounter = new StatHist("RSE execution counter");
    private final StatCounter unfinishedRSEcounter = new StatCounter("unfinished reached-sets");

    final StatisticsSeries<Integer> runningRSESeries =
        (runningRSESeriesFile == null)
            ? new NoopStatisticsSeries<>()
            : new StatisticsSeriesWithNumbers();

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      StatisticsUtils.write(pOut, 0, 50, "max number of executors", numMaxRSE);
      StatisticsUtils.write(pOut, 0, 50, histActiveThreads);
      StatisticsUtils.write(pOut, 0, 50, executionCounter);
      StatisticsUtils.write(pOut, 0, 50, unfinishedRSEcounter);
      StatisticsUtils.write(pOut, 0, 50, wallTime);
      StatisticsUtils.write(pOut, 0, 50, threadTime);
      StatisticsUtils.write(pOut, 1, 50, addingStatesTime);
      StatisticsUtils.write(pOut, 1, 50, terminationCheckTime);
      if (runningRSESeriesFile != null) {
        final StatisticsSeriesWithNumbers sswn = (StatisticsSeriesWithNumbers) runningRSESeries;
        StatisticsUtils.write(
            pOut, 1, 50, "Avg. number of parallel RSEs w/o time", sswn.getStatsWithoutTime());
        StatisticsUtils.write(
            pOut, 1, 50, "Avg. number of parallel RSEs over time", sswn.getStatsOverTime());
      }
    }

    @Override
    public void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {
      if (runningRSESeriesFile != null) {
        try {
          IO.writeFile(runningRSESeriesFile, Charset.defaultCharset(), runningRSESeries);
        } catch (IOException e) {
          logger.logUserException(Level.WARNING, e, "Could not write data-series for RSEs to file");
        }
      }
    }

    @Override
    public @Nullable String getName() {
      return "BAM-parallel";
    }
  }
}
