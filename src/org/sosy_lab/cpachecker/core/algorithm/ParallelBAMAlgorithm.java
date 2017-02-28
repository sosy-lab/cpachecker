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
package org.sosy_lab.cpachecker.core.algorithm;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.function.Function;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm.CPAAlgorithmFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.bam.BAMCPAWithoutReachedSetCreation;
import org.sosy_lab.cpachecker.cpa.bam.BlockSummaryMissingException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
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

  private final LongAccumulator numMaxRSE = new LongAccumulator(Math::max, 0);
  private final AtomicInteger numActiveThreads = new AtomicInteger(0);
  private final StatHist histActiveThreads = new StatHist("Active threads");
  private final StatHist executionCounter = new StatHist("RSE execution counter");
  private final StatCounter unfinishedRSEcounter = new StatCounter("unfinished reached-sets");

  private final static Level level = Level.ALL;
  private final static Runnable NOOP = () -> {};

  private final LogManager logger;
  private final BAMCPAWithoutReachedSetCreation bamcpa;
  private final CPAAlgorithmFactory algorithmFactory;
  private final ShutdownNotifier shutdownNotifier;
  private final AtomicReference<Throwable> error = new AtomicReference<>(null);

  public ParallelBAMAlgorithm(
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    bamcpa = (BAMCPAWithoutReachedSetCreation) pCpa;
    logger = pLogger;
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
    logger.logf(Level.INFO, "creating pool for %d threads", numberOfCores);
    ExecutorService pool = Executors.newFixedThreadPool(numberOfCores);

    synchronized (reachedSetMapping) {
      ReachedSetExecutor rse =
          new ReachedSetExecutor(
              mainReachedSet, mainReachedSet, reachedSetMapping, pool);
      CompletableFuture<Void> future = CompletableFuture.runAsync(rse.asRunnable(), pool);
      reachedSetMapping.put(mainReachedSet, Pair.of(rse, future));
    }

    try {
      // TODO set timelimit to global limit minus overhead?
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

    } finally {
      if (!pool.isTerminated()) {
        // in case of problems we must kill the thread pool,
        // otherwise we have a running daemon thread and CPAchecker does not terminate.
        logger.log(Level.WARNING, "threadpool did not terminate, killing threadpool now.");
        pool.shutdownNow();
      }
    }

    collectExceptions(reachedSetMapping);

    //    assert targetStateFound
    //        || (dependencyGraph.dependsOn.isEmpty()
    //            && dependencyGraph.dependsFrom.isEmpty()) : "dependencyGraph:" + dependencyGraph;

    //    readdStatesToWaitlists(dependencyGraph);

    return AlgorithmStatus.SOUND_AND_PRECISE;
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
      Map<ReachedSet, Pair<ReachedSetExecutor, CompletableFuture<Void>>> pReachedSetMapping)
      throws CPAException {
    pReachedSetMapping.values().parallelStream().forEach(entry -> {
      try{
        entry.getSecond().get(5, TimeUnit.SECONDS);
        executionCounter.insertValue(entry.getFirst().execCounter);
        unfinishedRSEcounter.inc();
      } catch (RejectedExecutionException e) {
        // ignore
      } catch (InterruptedException | ExecutionException  | TimeoutException e) {
        error.compareAndSet(null, e);
      }
      logger.log(level, "finishing", entry.getFirst(),
          entry.getSecond().isCompletedExceptionally());
    });
    Throwable toThrow = error.get();
    if (toThrow != null) {
      throw new CPAException(toThrow.getMessage());
    }
  }

  private Block getBlockForState(AbstractState state) {
    CFANode location = extractLocation(state);
    assert bamcpa.getBlockPartitioning()
        .isCallNode(location) : "root of reached-set must be located at block entry.";
    return bamcpa.getBlockPartitioning().getBlockForCallNode(location);
  }

  private static String id(final AbstractState state) {
    return ((ARGState) state).getStateId() + "@" + AbstractStates.extractLocation(state);
  }

  private static String id(ReachedSet pRs) {
    return id(pRs.getFirstState());
  }

  /**
   * A wrapper for a single reached-set and the corresponding data-structures.
   * We assume that each reachedset is contained in only one ReachedSetExecutor and
   * that each instance of ReachedSetExecutor is only executed by a single thread,
   * because this guarantees us single-threaded access to the reached-sets.
   */
  private class ReachedSetExecutor {

    /** the working reached-set, single-threaded access. */
    private final ReachedSet rs;

    /** the working algorithm for the reached-set, single-threaded access. */
    private final CPAAlgorithm algorithm = algorithmFactory.newInstance();

    /** flag that causes termination if enabled. */
    private boolean targetStateFound = false;

    /** main reached-set is used for checking termination of the algorithm. */
    private final ReachedSet mainReachedSet;

    /** important central data structure, shared over all threads, need to be synchronized directly. */
    private final Map<ReachedSet, Pair<ReachedSetExecutor, CompletableFuture<Void>>> reachedSetMapping;
    private final ExecutorService pool;

    private int execCounter = 0; // statistics

    /**
     * Sub-reached-sets have to be finished before the current one.
     * The state is unique. Synchronized access needed!
     */
    private final Set<AbstractState> dependsOn = new LinkedHashSet<>();

    /**
     * The current reached-set has to be finished before parent reached-set.
     * The state is unique, RSE is not. Synchronized access needed!
     */
    private final Multimap<ReachedSetExecutor, AbstractState> dependingFrom = LinkedHashMultimap.create();

    ReachedSetExecutor(
        ReachedSet pRs,
        ReachedSet pMainReachedSet,
        Map<ReachedSet, Pair<ReachedSetExecutor, CompletableFuture<Void>>> pReachedSetMapping,
        ExecutorService pPool) {
      rs = pRs;
      mainReachedSet = pMainReachedSet;
      reachedSetMapping = pReachedSetMapping;
      pool = pPool;

      logger.logf(level, "%s :: creating RSE", this);
    }

    public Runnable asRunnable() {
      return asRunnable(ImmutableSet.of());
    }

    public Runnable asRunnable(Collection<AbstractState> pStatesToBeAdded) {
      // copy needed, because access to pStatesToBeAdded is done in the future
      ImmutableSet<AbstractState> copy = ImmutableSet.copyOf(pStatesToBeAdded);
      return () -> apply(copy);
    }

    /** Wrapper-method around {@link #apply0} for handling errors. */
    private void apply(Collection<AbstractState> pStatesToBeAdded) {
      int running = numActiveThreads.incrementAndGet();
      histActiveThreads.insertValue(running);
      numMaxRSE.accumulate(reachedSetMapping.size());
      execCounter++;

      try {

      if (shutdownNotifier.shouldShutdown()) {
        pool.shutdownNow();
        return;
      }

      try {
        apply0(pStatesToBeAdded);

      } catch (CPAException | InterruptedException e) {
        logger.logException(level, e, e.getClass().getName());
      }

      } finally {
        numActiveThreads.decrementAndGet();
      }
    }

    /**
     * This method should be synchronized by design of the algorithm.
     * There exists a mapping of ReachedSet to ReachedSetExecutor
     * that guarantees single-threaded access to each ReachedSet.
     */
    private void apply0(Collection<AbstractState> pStatesToBeAdded)
        throws CPAException, InterruptedException {
      logger.logf(level, "%s :: RSE.run starting", this);

      updateStates(pStatesToBeAdded);
      analyzeReachedSet();

      logger.logf(level, "%s :: RSE.run exiting", this);
    }

    private String idd() {
      return id(rs);
    }

    /**
     * This method re-adds states to the waitlist.
     * The states were removed due to missing blocks,
     * and we re-add them when the missing block is finished.
     * The states are at block-start locations.
     */
    private void updateStates(Collection<AbstractState> pStatesToBeAdded) {
      for (AbstractState state : pStatesToBeAdded) {
        rs.reAddToWaitlist(state);
        dependsOn.remove(state);
      }
    }

    private void analyzeReachedSet() throws CPAException, InterruptedException {
      try {
        @SuppressWarnings("unused")
        AlgorithmStatus tmpStatus = algorithm.run(rs);
        handleTermination();

      } catch (BlockSummaryMissingException bsme) {
        handleMissingBlock(bsme);

      }
    }

    private boolean isFinished() {
      return !rs.hasWaitingState() && dependsOn.isEmpty();
    }

    private boolean endsWithTargetState() {
      return rs.getLastState() != null && AbstractStates.isTargetState(rs.getLastState());
    }

    private void handleTermination() {
      logger.logf(level, "%s :: RSE.handleTermination starting", this);

      boolean isFinished = isFinished();
      boolean endsWithTargetState = endsWithTargetState();

      if (endsWithTargetState) {
        targetStateFound = true;
      }

      if (isFinished || endsWithTargetState) {
        logger.logf(level, "%s :: finished=%s, endsWithTargetState=%s", this, isFinished,
            endsWithTargetState);

        updateCache(endsWithTargetState);
        reAddStatesToDependingReachedSets();

        if (rs == mainReachedSet) {
          logger.logf(level, "%s :: mainRS finished, shutdown threadpool", this);
          pool.shutdown();
        }
      }

      logger.logf(level, "%s :: RSE.handleTermination exiting", this);
    }

    private void updateCache(boolean pEndsWithTargetState) {
      if (rs == mainReachedSet) {
        // we do not cache main reached set, because it should not be used internally
        return;
      }

      AbstractState reducedInitialState = rs.getFirstState();
      Precision reducedInitialPrecision = rs.getPrecision(reducedInitialState);
      Block block = getBlockForState(reducedInitialState);
      final Collection<AbstractState> exitStates;
      if (pEndsWithTargetState) {
        exitStates = Collections.singletonList(rs.getLastState());
      } else {
        exitStates =
            AbstractStates.filterLocations(rs, block.getReturnNodes())
                .filter(s -> ((ARGState) s).getChildren().isEmpty())
                .toList();
      }
      Pair<ReachedSet, Collection<AbstractState>> check =
          bamcpa.getCache().get(reducedInitialState, reducedInitialPrecision, block);
      assert check.getFirst() == rs : String.format(
          "reached-set for initial state should be unique: current rs = %s, cached entry = %s",
          id(rs), check.getFirst());
      if (!exitStates.equals(check.getSecond())) {
        assert check.getSecond() == null: String.format(
            "result-states already registered for reached-set %s: current = %s, cached = %s",
            id(rs),
            Collections2.transform(exitStates, s -> id(s)),
            Collections2.transform(check.getSecond(), s -> id(s)));
        bamcpa.getCache().put(reducedInitialState, reducedInitialPrecision, block, exitStates, null);
      }
    }

    private void reAddStatesToDependingReachedSets() {
      synchronized (dependingFrom) {
        for (Entry<ReachedSetExecutor, Collection<AbstractState>> parent :
            dependingFrom.asMap().entrySet()) {
          registerJob(parent.getKey(), parent.getKey().asRunnable(parent.getValue()));
        }
        dependingFrom.clear();
      }
    }

    private void handleMissingBlock(BlockSummaryMissingException pBsme) {
      logger.logf(level, "%s :: RSE.handleMissingBlock starting", this);

      if (targetStateFound) {
        // ignore further sub-analyses
      }

      // remove current state from waitlist to avoid exploration until all sub-blocks are done.
      // The state was removed for exploration,
      // but re-added by CPA-algorithm when throwing the exception
      assert rs.contains(pBsme.getState()) : "parent reachedset must contain entry state";
      rs.removeOnlyFromWaitlist(pBsme.getState());

      // register new sub-analysis as asynchronous/parallel/future work, if not existent
      synchronized (reachedSetMapping) {

        ReachedSet newRs = pBsme.getReachedSet();
        if (newRs == null) {
          // We are only synchronized in the current method. Thus, we need to check
          // the cache again, maybe another thread already created the needed reached-set.
          final Pair<ReachedSet, Collection<AbstractState>> pair =
              bamcpa.getCache().get(pBsme.getReducedState(), pBsme.getReducedPrecision(), pBsme.getBlock());
          newRs = pair.getFirst();
        }

        // now we can be sure, whether the sub-reached-set exists or not.
        final ReachedSetExecutor subRse;
        if (newRs == null) {
          // we have not even cached a partly computed reached-set,
          // so we must compute the subgraph specification from scratch
          newRs = bamcpa.getData().createAndRegisterNewReachedSet(
                  pBsme.getReducedState(), pBsme.getReducedPrecision(), pBsme.getBlock());

          // we have not even cached a partly computed reach-set,
          // so we must compute the subgraph specification from scratch
          subRse = new ReachedSetExecutor(newRs, mainReachedSet, reachedSetMapping, pool);
          // register NOOP here. Callback for results is registered later, we have "lazy" computation.
          CompletableFuture<Void> future = CompletableFuture.runAsync(NOOP, pool)
              .exceptionally(new ExceptionHandler(pool, subRse));
          reachedSetMapping.put(newRs, Pair.of(subRse, future));
          logger.logf(level, "%s :: register subRSE %s", this, id(newRs));

        } else {
          Pair<ReachedSetExecutor, CompletableFuture<Void>> p = reachedSetMapping.get(newRs);
          subRse = p.getFirst();
        }

        // register dependencies to wait for results and to get results, asynchronous

        // add dependencies
        dependsOn.add(pBsme.getState());
        synchronized(subRse.dependingFrom) {
          subRse.dependingFrom.put(this, pBsme.getState());
        }
        logger.logf(level, "%s :: RSE.handleMissingBlock %s -> %s", this, this, id(newRs));

        // register callback to get results of terminated analysis
        registerJob(subRse, subRse.asRunnable());
      }

      // register current RSE for further analysis
      registerJob(this, this.asRunnable());

      logger.logf(level, "%s :: RSE.handleMissingBlock exiting", this);
    }

    /**
     * build a chain of jobs,
     * append a new job after the last registered job for the given reached-set.
     */
    private void registerJob(ReachedSetExecutor pRse, Runnable r) {
      synchronized (reachedSetMapping) {
        Pair<ReachedSetExecutor, CompletableFuture<Void>> p = reachedSetMapping.get(pRse.rs);
        assert p.getFirst() == pRse;
        CompletableFuture<Void> future = p.getSecond().thenRunAsync(r, pool)
            .exceptionally(new ExceptionHandler(pool, pRse));
        reachedSetMapping.put(pRse.rs, Pair.of(pRse, future));
      }
    }

    @Override
    public String toString() {
      return "RSE " + idd();
    }
  }

  private class ExceptionHandler implements Function<Throwable, Void> {

    private final ExecutorService pool;
    private final ReachedSetExecutor rse;

    public ExceptionHandler(ExecutorService pPool, ReachedSetExecutor pRse) {
      pool = pPool;
      rse = pRse;
    }

    @Override
    public Void apply(Throwable e) {
      if (e instanceof RejectedExecutionException || e instanceof CompletionException) {
        // ignore, might happen when target-state is found
        // TODO cleanup waiting states and dependencies?
      } else {
        logger.logException(Level.WARNING, e, rse + " :: " + e.getClass().getSimpleName());
        error.compareAndSet(null, e);
      }
//      pool.shutdownNow();
      return null;
    }

  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Statistics () {

      @Override
      public void printStatistics(PrintStream pOut, Result pResult,
          UnmodifiableReachedSet pReached) {
        StatisticsUtils.write(pOut, 0, 50, "max number of executors", numMaxRSE);
        StatisticsUtils.write(pOut, 0, 50, histActiveThreads);
        StatisticsUtils.write(pOut, 0, 50, executionCounter);
        StatisticsUtils.write(pOut, 0, 50, unfinishedRSEcounter);
      }

      @Override
      public @Nullable String getName() {
        return "BAM-parallel";
      }

    });
  }
}
