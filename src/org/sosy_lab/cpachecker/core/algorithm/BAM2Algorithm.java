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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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

  public BAM2Algorithm(
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    bamcpa = (BAMCPA2) pCpa;
    logger = pLogger;
    algorithmFactory = new CPAAlgorithmFactory(bamcpa, logger, pConfig, pShutdownNotifier);
  }

  @Override
  public AlgorithmStatus run(final ReachedSet mainReachedSet)
      throws CPAException, InterruptedException {
    try {
      return run0(mainReachedSet);
    } catch (CPAException | InterruptedException e) {
      throw e;
    } catch (Throwable e) {
      logger.logException(Level.WARNING, e, this + " -- " + e.getClass().getSimpleName());
      throw new AssertionError(e);
    }
  }

  private AlgorithmStatus run0(final ReachedSet mainReachedSet)
      throws Exception {

    //    boolean targetStateFound = false;

    Map<ReachedSet, Pair<ReachedSetExecutor, CompletableFuture<Void>>> reachedSetMapping =
        new HashMap<>();
    ExecutorService pool =
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    synchronized (reachedSetMapping) {
      ReachedSetExecutor rse =
          new ReachedSetExecutor(
              mainReachedSet, mainReachedSet, reachedSetMapping, pool);
      CompletableFuture<Void> future = CompletableFuture.runAsync(rse, pool);
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
   * We could also ignore this step, but that might be dangerous and error-prone. */
  private void collectExceptions(
      Map<ReachedSet, Pair<ReachedSetExecutor, CompletableFuture<Void>>> pReachedSetMapping)
      throws InterruptedException, ExecutionException {
    for (Pair<ReachedSetExecutor, CompletableFuture<Void>> entry : pReachedSetMapping.values()) {
      entry.getSecond().get();
      logger.log(Level.INFO, "finishing", entry.getFirst(),
          entry.getSecond().isCompletedExceptionally());
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

  class ReachedSetExecutor implements Runnable {

    private final ReachedSet rs;
    private final ReachedSet mainReachedSet;
    private final Map<ReachedSet, Pair<ReachedSetExecutor, CompletableFuture<Void>>> reachedSetMapping; // important central data structure, shared over all threads.
    private final ExecutorService pool;

    private boolean targetStateFound = false;

    /** Results are added to this list and applied, when scheduled. Synchronized access needed! */
    private final List<AbstractState> statesToBeAdded = new ArrayList<>();

    /**
     * Sub-reached-sets have to be finished before the current one.
     * The state is unique, RSE is not. Synchronized access needed!
     */
    private final Map<AbstractState, ReachedSetExecutor> dependsOn =
        Collections.synchronizedMap(new LinkedHashMap<>());

    /**
     * The current reached-set has to be finished before parent reached-set.
     * The state is unique, RSE is not. Synchronized access needed!
     */
    private final Map<AbstractState, ReachedSetExecutor> dependingFrom =
        Collections.synchronizedMap(new LinkedHashMap<>());

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

    @Override
    public void run() {
      logger.logf(Level.INFO, "%s :: RSE.run starting", this);

      updateStates();
      analyzeReachedSet();

      logger.logf(Level.INFO, "%s :: RSE.run exiting", this);
    }

    private String idd() {
      return id(rs);
    }

    private void updateStates() {
      synchronized (statesToBeAdded) {
        for (AbstractState state : statesToBeAdded) {
          rs.reAddToWaitlist(state);
        }
        statesToBeAdded.clear();
      }
    }

    private void reAddState(AbstractState pState) {
      synchronized (statesToBeAdded) {
        statesToBeAdded.add(pState);
        dependsOn.remove(pState); // dependency fulfilled, thus removing it
      }
    }

    private void analyzeReachedSet() {
      try {
        CPAAlgorithm algorithm = algorithmFactory.newInstance();

        @SuppressWarnings("unused")
        AlgorithmStatus tmpStatus = algorithm.run(rs);
        handleTermination();

      } catch (BlockSummaryMissingException bsme) {
        handleMissingBlock(bsme);

      } catch (CPAException | InterruptedException e) {
        logger.logException(Level.INFO, e, e.getClass().getName());
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
        for (Entry<AbstractState, ReachedSetExecutor> parent : dependingFrom.entrySet()) {
          parent.getValue().reAddState(parent.getKey());
          registerJob(parent.getValue());
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
        dependsOn.put(pBsme.getState(), subRse);
        subRse.dependingFrom.put(pBsme.getState(), this);
        logger.logf(Level.INFO, "%s :: RSE.handleMissingBlock %s -> %s", this, this,
            id(pBsme.getReachedSet()));

        // register callback to get results of terminated analysis
        registerJob(subRse);
      }

      // register current RSE for further analysis
      synchronized (reachedSetMapping) {
        registerJob(this);
      }

      logger.logf(Level.INFO, "%s :: RSE.handleMissingBlock exiting", this);
    }

    /**
     * build a chain of jobs,
     * append a new job after the last registered job for the given reached-set.
     */
    private void registerJob(ReachedSetExecutor pRse) {
      Pair<ReachedSetExecutor, CompletableFuture<Void>> p = reachedSetMapping.get(pRse.rs);
      assert p.getFirst() == pRse;
      CompletableFuture<Void> future = p.getSecond().thenRunAsync(pRse, pool);
      reachedSetMapping.put(pRse.rs, Pair.of(pRse, future));
    }

    private void createNewSubAnalysis(ReachedSet newRs) {
      ReachedSetExecutor subRse =
          new ReachedSetExecutor(newRs, mainReachedSet, reachedSetMapping, pool);
      // register NOOP here. Callback for results is registered later, we have "lazy" computation.
      final CompletableFuture<Void> future = CompletableFuture.runAsync(NOOP, pool);
      assert !reachedSetMapping.containsKey(newRs) : "reached-set already registered";
      reachedSetMapping.put(newRs, Pair.of(subRse, future));
      logger.logf(Level.INFO, "%s :: register subRSE %s", this, id(newRs));
    }

    @Override
    public String toString() {
      return "RSE " + idd();
    }
  }
}
