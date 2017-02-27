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

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm.CPAAlgorithmFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.bam.BAMCPA2;
import org.sosy_lab.cpachecker.cpa.bam.BlockSummaryMissingException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;

public class BAM2Algorithm implements Algorithm {

  private final static Runnable NOOP = () -> {};

  private final LogManager logger;
  private final BAMCPA2 bamcpa;
  private final CPAAlgorithmFactory algorithmFactory;
  private final ShutdownNotifier shutdownNotifier;
  private final AtomicReference<Throwable> error = new AtomicReference<>(null);

  public BAM2Algorithm(
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    bamcpa = (BAMCPA2) pCpa;
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
    ExecutorService pool =
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

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

  /** We check here whether an error occured in a CompletableFuture.
   * We could also ignore this step, but that might be dangerous and error-prone.
   */
  private void collectExceptions(
      Map<ReachedSet, Pair<ReachedSetExecutor, CompletableFuture<Void>>> pReachedSetMapping)
      throws InterruptedException, CPAException {
    for (Pair<ReachedSetExecutor, CompletableFuture<Void>> entry : pReachedSetMapping.values()) {
      try{
        entry.getSecond().get();
      } catch (RejectedExecutionException e) {
        // ignore
      } catch (ExecutionException e) {
        error.compareAndSet(null, e);
      }
      logger.log(Level.INFO, "finishing", entry.getFirst(),
          entry.getSecond().isCompletedExceptionally());
    }
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

  private class ReachedSetExecutor {

    private final ReachedSet rs;
    private final ReachedSet mainReachedSet;
    /** important central data structure, shared over all threads. */
    private final Map<ReachedSet, Pair<ReachedSetExecutor, CompletableFuture<Void>>> reachedSetMapping;
    private final ExecutorService pool;

    private boolean targetStateFound = false;

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

      logger.logf(Level.INFO, "%s :: creating RSE", this);
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
      if (shutdownNotifier.shouldShutdown()) {
        return;
      }

      try {
        apply0(pStatesToBeAdded);

      } catch (CPAException | InterruptedException e) {
        logger.logException(Level.INFO, e, e.getClass().getName());
      }
    }

    /**
     * This method should be synchronized by design of the algorithm.
     * There exists a mapping of ReachedSet to ReachedSetExecutor
     * that guarantees single-threaded access to each ReachedSet.
     */
    private void apply0(Collection<AbstractState> pStatesToBeAdded)
        throws CPAException, InterruptedException {
      logger.logf(Level.INFO, "%s :: RSE.run starting", this);

      updateStates(pStatesToBeAdded);
      analyzeReachedSet();

      logger.logf(Level.INFO, "%s :: RSE.run exiting", this);
    }

    private String idd() {
      return id(rs);
    }

    private void updateStates(Collection<AbstractState> pStatesToBeAdded) {
      for (AbstractState state : pStatesToBeAdded) {
        rs.reAddToWaitlist(state);
        dependsOn.remove(state);
      }
    }

    private void analyzeReachedSet() throws CPAException, InterruptedException {
      try {
        CPAAlgorithm algorithm = algorithmFactory.newInstance();

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
      logger.logf(Level.INFO, "%s :: RSE.handleTermination starting", this);

      boolean isFinished = isFinished();
      boolean endsWithTargetState = endsWithTargetState();

      if (endsWithTargetState) {
        targetStateFound = true;
      }

      if (isFinished || endsWithTargetState) {
        logger.logf(Level.INFO, "%s :: finished=%s, endsWithTargetState=%s", this, isFinished,
            endsWithTargetState);

        updateCache(endsWithTargetState);
        reAddStatesToDependingReachedSets();

        if (rs == mainReachedSet) {
          logger.logf(Level.INFO, "%s :: mainRS finished, shutdown threadpool", this);
          pool.shutdown();
        }
      }

      logger.logf(Level.INFO, "%s :: RSE.handleTermination exiting", this);
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
      logger.logf(Level.INFO, "%s :: RSE.handleMissingBlock starting", this);

      if (targetStateFound) {
        // ignore further sub-analyses
      }

      // remove current state from waitlist to avoid exploration until all sub-blocks are done.
      // The state was removed for exploration,
      // but re-added by CPA-algorithm when throwing the exception
      assert rs.contains(pBsme.getState()) : "parent reachedset must contain entry state";
      rs.removeOnlyFromWaitlist(pBsme.getState());

      // register sub analysis as asynchronous/parallel/future work
      synchronized (reachedSetMapping) {
        if (!reachedSetMapping.containsKey(pBsme.getReachedSet())) {
          createNewSubAnalysis(pBsme.getReachedSet());
        }
      }

      // register dependencies to wait for results and to get results, asynchronous
      synchronized (reachedSetMapping) {
        Pair<ReachedSetExecutor, CompletableFuture<Void>> p =
            reachedSetMapping.get(pBsme.getReachedSet());
        ReachedSetExecutor subRse = p.getFirst();

        // add dependencies
        dependsOn.add(pBsme.getState());
        synchronized(subRse.dependingFrom) {
          subRse.dependingFrom.put(this, pBsme.getState());
        }
        logger.logf(Level.INFO, "%s :: RSE.handleMissingBlock %s -> %s", this, this,
            id(pBsme.getReachedSet()));

        // register callback to get results of terminated analysis
        registerJob(subRse, subRse.asRunnable());
      }

      // register current RSE for further analysis
      synchronized (reachedSetMapping) {
        registerJob(this, this.asRunnable());
      }

      logger.logf(Level.INFO, "%s :: RSE.handleMissingBlock exiting", this);
    }

    /**
     * build a chain of jobs,
     * append a new job after the last registered job for the given reached-set.
     */
    private void registerJob(ReachedSetExecutor pRse, Runnable r) {
      Pair<ReachedSetExecutor, CompletableFuture<Void>> p = reachedSetMapping.get(pRse.rs);
      assert p.getFirst() == pRse;
      CompletableFuture<Void> future = p.getSecond().thenRunAsync(r, pool)
          .exceptionally(new ExceptionHandler(pool));
      reachedSetMapping.put(pRse.rs, Pair.of(pRse, future));
    }

    private void createNewSubAnalysis(ReachedSet newRs) {
      ReachedSetExecutor subRse =
          new ReachedSetExecutor(newRs, mainReachedSet, reachedSetMapping, pool);
      // register NOOP here. Callback for results is registered later, we have "lazy" computation.
      CompletableFuture<Void> future = CompletableFuture.runAsync(NOOP, pool)
          .exceptionally(new ExceptionHandler(pool));
      assert !reachedSetMapping.containsKey(newRs) : "reached-set already registered";
      reachedSetMapping.put(newRs, Pair.of(subRse, future));
      logger.logf(Level.INFO, "%s :: register subRSE %s", this, id(newRs));
    }

    @Override
    public String toString() {
      return "RSE " + idd();
    }
  }

  private class ExceptionHandler implements Function<Throwable, Void> {

    private final ExecutorService pool;

    public ExceptionHandler(ExecutorService pPool) {
      pool = pPool;
    }

    @Override
    public Void apply(Throwable e) {
      if (e instanceof RejectedExecutionException) {
        // ignore, might happen when target-state is found
        // TODO cleanup waiting states and dependencies?
      } else {
        error.compareAndSet(null, e);
      }
      pool.shutdownNow();
      return null;
    }

  }
}
