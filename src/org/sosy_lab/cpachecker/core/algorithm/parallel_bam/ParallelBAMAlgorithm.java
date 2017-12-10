/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
import javax.annotation.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
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
import org.sosy_lab.cpachecker.cpa.bam.BAMCPAWithoutReachedSetCreation;
import org.sosy_lab.cpachecker.cpa.bam.BAMReachedSetValidator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatHist;
import org.sosy_lab.cpachecker.util.statistics.StatisticsUtils;

@Options(prefix="algorithm.parallelBam")
public class ParallelBAMAlgorithm implements Algorithm, StatisticsProvider {

  @Option(
    description =
        "number of threads, positive values match exactly, "
            + "with -1 we use the number of available cores or the machine automatically.",
    secure = true
  )
  private int numberOfThreads = -1;

  private final ParallelBAMStatistics stats = new ParallelBAMStatistics();
  private final LogManager logger;
  private final LogManagerWithoutDuplicates oneTimeLogger;
  private final BAMCPAWithoutReachedSetCreation bamcpa;
  private final CPAAlgorithmFactory algorithmFactory;
  private final ShutdownNotifier shutdownNotifier;

  public ParallelBAMAlgorithm(
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    bamcpa = (BAMCPAWithoutReachedSetCreation) pCpa;
    logger = pLogger;
    oneTimeLogger = new LogManagerWithoutDuplicates(pLogger);
    shutdownNotifier = pShutdownNotifier;
    algorithmFactory = new CPAAlgorithmFactory(bamcpa, logger, pConfig, pShutdownNotifier);
  }

  @Override
  public AlgorithmStatus run(final ReachedSet mainReachedSet)
      throws CPAException, InterruptedException {

    //    boolean targetStateFound = false;

    Map<ReachedSet, Pair<ReachedSetExecutor, CompletableFuture<Void>>> reachedSetMapping =
        new HashMap<>();
    final int numberOfCores = getNumberOfCores();
    oneTimeLogger.logfOnce(Level.INFO, "creating pool for %d threads", numberOfCores);
    final ExecutorService pool = Executors.newFixedThreadPool(numberOfCores);
    final AtomicReference<Throwable> error = new AtomicReference<>(null);
    final AtomicBoolean terminateAnalysis = new AtomicBoolean(false);

    synchronized (reachedSetMapping) {
      ReachedSetExecutor rse =
          new ReachedSetExecutor(
              bamcpa,
              mainReachedSet,
              mainReachedSet,
              reachedSetMapping,
              pool,
              algorithmFactory,
              shutdownNotifier,
              stats,
              error,
              terminateAnalysis,
              logger);
      CompletableFuture<Void> future = CompletableFuture.runAsync(rse.asRunnable(), pool);
      reachedSetMapping.put(mainReachedSet, Pair.of(rse, future));
    }

    boolean isSound = true;
    try {
      // TODO set timelimit to global limit minus overhead?
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

    } finally {
      if (!pool.isTerminated()) {
        // in case of problems we must kill the thread pool,
        // otherwise we have a running daemon thread and CPAchecker does not terminate.
        logger.log(Level.WARNING, "threadpool did not terminate, killing threadpool now.");
        isSound = false;
        pool.shutdownNow();
      }
    }

    collectExceptions(reachedSetMapping, error, mainReachedSet);

    //    assert targetStateFound
    //        || (dependencyGraph.dependsOn.isEmpty()
    //            && dependencyGraph.dependsFrom.isEmpty()) : "dependencyGraph:" + dependencyGraph;

    //    readdStatesToWaitlists(dependencyGraph);

    assert BAMReachedSetValidator.validateData(
        bamcpa.getData(), bamcpa.getBlockPartitioning(), new ARGReachedSet(mainReachedSet));

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
      Map<ReachedSet, Pair<ReachedSetExecutor, CompletableFuture<Void>>> pReachedSetMapping,
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
              ReachedSetExecutor rse = entry.getValue().getFirst();
              CompletableFuture<Void> job = entry.getValue().getSecond();
              try {
                job.get(5, TimeUnit.SECONDS);
                stats.executionCounter.insertValue(entry.getValue().getFirst().execCounter);
                stats.unfinishedRSEcounter.inc();

                if (rse.isTargetStateFound()) {
                  if (entry.getKey() == mainReachedSet) {
                    mainRScontainsTarget.set(true);
                  } else {
                    otherRScontainsTarget.set(true);
                  }
                }

              } catch (RejectedExecutionException e) {
                logger.log(Level.SEVERE, e);
              } catch (InterruptedException | ExecutionException | TimeoutException e) {
                logger.log(Level.SEVERE, e);
                error.compareAndSet(null, e);
              }
              logger.log(Level.ALL, "finishing", rse, job.isCompletedExceptionally());
            });

    Throwable toThrow = error.get();
    if (toThrow != null) {
      throw new CPAException(toThrow.getMessage());
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

  static class ParallelBAMStatistics implements Statistics {
    final LongAccumulator numMaxRSE = new LongAccumulator(Math::max, 0);
    final AtomicInteger numActiveThreads = new AtomicInteger(0);
    final StatHist histActiveThreads = new StatHist("Active threads");
    final StatHist executionCounter = new StatHist("RSE execution counter");
    private final StatCounter unfinishedRSEcounter = new StatCounter("unfinished reached-sets");

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      StatisticsUtils.write(pOut, 0, 50, "max number of executors", numMaxRSE);
      StatisticsUtils.write(pOut, 0, 50, histActiveThreads);
      StatisticsUtils.write(pOut, 0, 50, executionCounter);
      StatisticsUtils.write(pOut, 0, 50, unfinishedRSEcounter);
    }

    @Override
    public @Nullable String getName() {
      return "BAM-parallel";
    }

  }
}
