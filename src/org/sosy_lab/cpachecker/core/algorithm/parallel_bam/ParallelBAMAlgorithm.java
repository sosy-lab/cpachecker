/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.algorithm.parallel_bam;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
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
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatHist;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsSeries;
import org.sosy_lab.cpachecker.util.statistics.StatisticsSeries.NoopStatisticsSeries;
import org.sosy_lab.cpachecker.util.statistics.StatisticsUtils;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer;

@Options(prefix="algorithm.parallelBam")
public class ParallelBAMAlgorithm implements Algorithm, StatisticsProvider {

  @Option(
    description =
        "number of threads, positive values match exactly, "
            + "with -1 we use the number of available cores or the machine automatically.",
    secure = true
  )
  private int numberOfThreads = -1;

  @Option(description = "export number of running RSE instances as CSV", secure = true)
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path runningRSESeriesFile = Paths.get("RSESeries.csv");

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
    final ExecutorService pool = Executors.newFixedThreadPool(numberOfCores);
    final AtomicReference<Throwable> error = new AtomicReference<>(null);
    final AtomicBoolean terminateAnalysis = new AtomicBoolean(false);

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
            error,
            terminateAnalysis,
            logger);
    reachedSetMapping.put(mainReachedSet, rse); // backwards reference

    // start analysis
    rse.addNewTask(rse.asRunnable());

    boolean isSound = true;
    try {
      // TODO set timelimit to global limit minus overhead?
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

    } finally {
      if (!pool.isTerminated()) {
        // in case of problems we must kill the thread pool,
        // otherwise we have a running daemon thread and CPAchecker does not terminate.
        try {
          logger.log(Level.WARNING, "threadpool did not terminate, killing threadpool now.");
          logger.log(Level.ALL, "remaining dependencies:\n", rse.getDependenciesAsDot());
        } finally {
          pool.shutdownNow();
        }
        isSound = false;
      }
    }

    collectExceptions(reachedSetMapping, error, mainReachedSet);

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
      AtomicReference<Throwable> error,
      final ReachedSet mainReachedSet)
      throws CPAException {

    final AtomicBoolean mainRScontainsTarget = new AtomicBoolean(false);
    final AtomicBoolean otherRScontainsTarget = new AtomicBoolean(false);

    pReachedSetMapping
        .entrySet()
        .parallelStream()
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

              } catch (RejectedExecutionException | ExecutionException e) {
                logger.logException(Level.SEVERE, e, e.getMessage());
                error.compareAndSet(null, e);
              } catch (InterruptedException | TimeoutException e) {
                error.compareAndSet(null, e);
              }
              logger.log(Level.ALL, "finishing", rse, job.isCompletedExceptionally());
            });

    Throwable toThrow = error.get();
    if (toThrow != null) {
      // just re-throw plain errors, this results in better stack traces
      if (toThrow instanceof RuntimeException || toThrow instanceof Error) {
        throw new RuntimeException(toThrow.getMessage(), toThrow);
      } else {
        throw new CPAException(toThrow.getMessage(), toThrow);
      }
    }

    Preconditions.checkState(
        mainRScontainsTarget.get() == otherRScontainsTarget.get(),
        "when a target is found in a sub-analysis ("
            + otherRScontainsTarget.get()
            + "), we exspect a target in the main-reached-set ("
            + mainRScontainsTarget.get()
            + ")");
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

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
        (runningRSESeriesFile == null) ? new NoopStatisticsSeries<>() : new StatisticsSeries<>();

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
