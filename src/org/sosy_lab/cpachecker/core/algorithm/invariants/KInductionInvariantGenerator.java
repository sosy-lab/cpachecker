/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.invariants;

import static com.google.common.base.Preconditions.*;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.common.LazyFutureTask;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.common.concurrency.Threads;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.BMCAlgorithmForInvariantGeneration;
import org.sosy_lab.cpachecker.core.algorithm.bmc.BMCStatistics;
import org.sosy_lab.cpachecker.core.algorithm.bmc.CandidateGenerator;
import org.sosy_lab.cpachecker.core.algorithm.bmc.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.EdgeFormulaNegation;
import org.sosy_lab.cpachecker.core.algorithm.bmc.StaticCandidateProvider;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;
import org.sosy_lab.solver.SolverException;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;

/**
 * Generate invariants using k-induction.
 */
public class KInductionInvariantGenerator extends AbstractInvariantGenerator implements StatisticsProvider {

  private static class KInductionInvariantGeneratorStatistics extends BMCStatistics {

    final Timer invariantGeneration = new Timer();

    @Override
    public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
      out.println("Time for invariant generation:   " + invariantGeneration);
      super.printStatistics(out, result, reached);
    }

    @Override
    public String getName() {
      return "k-Induction-based invariant generator";
    }
  }

  private final KInductionInvariantGeneratorStatistics stats = new KInductionInvariantGeneratorStatistics();

  private final BMCAlgorithmForInvariantGeneration algorithm;
  private final ConfigurableProgramAnalysis cpa;
  private final ReachedSetFactory reachedSetFactory;

  private final LogManager logger;
  private final ShutdownManager shutdownManager;

  private final boolean async;

  // After start(), this will hold a Future for the final result of the invariant generation.
  // We use a Future instead of just the atomic reference below
  // to be able to ask for termination and see thrown exceptions.
  private Future<InvariantSupplier> invariantGenerationFuture = null;

  private final ShutdownRequestListener shutdownListener = new ShutdownRequestListener() {

    @Override
    public void shutdownRequested(String pReason) {
      invariantGenerationFuture.cancel(true);
    }
  };

  public static KInductionInvariantGenerator create(final Configuration pConfig,
      final LogManager pLogger, final ShutdownManager pShutdownNotifier,
      final CFA pCFA, final ReachedSetFactory pReachedSetFactory, TargetLocationProvider pTargetLocationProvider)
          throws InvalidConfigurationException, CPAException {

    return new KInductionInvariantGenerator(
            pConfig,
            pLogger.withComponentName("KInductionInvariantGenerator"),
            pShutdownNotifier,
            pCFA,
            pReachedSetFactory,
            true,
            new StaticCandidateProvider(getCandidateInvariants(pCFA, pTargetLocationProvider)));
  }

  public static KInductionInvariantGenerator create(final Configuration pConfig,
      final LogManager pLogger, final ShutdownManager pShutdownNotifier,
      final CFA pCFA, final ReachedSetFactory pReachedSetFactory, CandidateGenerator candidateGenerator, boolean pAsync)
          throws InvalidConfigurationException, CPAException {

    return new KInductionInvariantGenerator(
            pConfig,
            pLogger.withComponentName("KInductionInvariantGenerator"),
            pShutdownNotifier,
            pCFA,
            pReachedSetFactory,
            pAsync,
            candidateGenerator);
  }

  private KInductionInvariantGenerator(final Configuration config, final LogManager pLogger,
      final ShutdownManager pShutdownNotifier, final CFA cfa,
      final ReachedSetFactory pReachedSetFactory, final boolean pAsync,
      final CandidateGenerator pCandidateGenerator)
          throws InvalidConfigurationException, CPAException {
    logger = pLogger;
    shutdownManager = pShutdownNotifier;

    reachedSetFactory = pReachedSetFactory;
    async = pAsync;

    CPABuilder invGenBMCBuilder =
        new CPABuilder(config, logger, shutdownManager.getNotifier(), pReachedSetFactory);
    cpa = invGenBMCBuilder.buildCPAWithSpecAutomatas(cfa);
    Algorithm cpaAlgorithm = CPAAlgorithm.create(cpa, logger, config, shutdownManager.getNotifier());
    algorithm = new BMCAlgorithmForInvariantGeneration(
        cpaAlgorithm, cpa, config, logger, pReachedSetFactory,
        shutdownManager, cfa, stats, pCandidateGenerator);

    PredicateCPA predicateCPA = CPAs.retrieveCPA(cpa, PredicateCPA.class);
    if (predicateCPA == null) {
      throw new InvalidConfigurationException("Predicate CPA required");
    }
    if (async && !predicateCPA.getSolver().getVersion().toLowerCase().contains("smtinterpol")) {
      throw new InvalidConfigurationException("Solver does not support concurrent execution, use SMTInterpol instead.");
    }
  }

  @Override
  public void start(final CFANode initialLocation) {
    checkState(invariantGenerationFuture == null);

    Callable<InvariantSupplier> task = new InvariantGenerationTask(initialLocation);

    if (async) {
      // start invariant generation asynchronously
      ExecutorService executor = Executors.newSingleThreadExecutor(Threads.threadFactory());
      invariantGenerationFuture = executor.submit(task);
      executor.shutdown(); // will shutdown after task is finished

    } else {
      // create future for lazy synchronous invariant generation
      invariantGenerationFuture = new LazyFutureTask<>(task);
    }

    shutdownManager.getNotifier().registerAndCheckImmediately(shutdownListener);
  }

  @Override
  public void cancel() {
    checkState(invariantGenerationFuture != null);
    shutdownManager.requestShutdown("Invariant generation cancel requested.");
  }

  @Override
  public InvariantSupplier get() throws CPAException, InterruptedException {
    checkState(invariantGenerationFuture != null);

    if (async && !invariantGenerationFuture.isDone()) {
      // grab intermediate result that is available so far
      return algorithm.getCurrentInvariants();

    } else {
      try {
        return invariantGenerationFuture.get();
      } catch (ExecutionException e) {
        Throwables.propagateIfPossible(e.getCause(), CPAException.class, InterruptedException.class);
        throw new UnexpectedCheckedException("invariant generation", e.getCause());
      } catch (CancellationException e) {
        shutdownManager.getNotifier().shutdownIfNecessary();
        throw e;
      }
    }
  }

  @Override
  public ExpressionTreeSupplier getAsExpressionTree() throws CPAException, InterruptedException {
    get();
    return algorithm.getCurrentInvariantsAsExpressionTree();
  }

  @Override
  public boolean isProgramSafe() {
    return algorithm.isProgramSafe();
  }

  @Override
  public void injectInvariant(CFANode pLocation, AssumeEdge pAssumption) {
    // ignore for now (never called anyway)
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    algorithm.collectStatistics(pStatsCollection);
    pStatsCollection.add(stats);
  }

  private class InvariantGenerationTask implements Callable<InvariantSupplier> {

    private final CFANode initialLocation;

    private InvariantGenerationTask(final CFANode pInitialLocation) {
      initialLocation = checkNotNull(pInitialLocation);
    }

    @Override
    public InvariantSupplier call() throws InterruptedException, CPAException {
      stats.invariantGeneration.start();
      shutdownManager.getNotifier().shutdownIfNecessary();

      try {
        ReachedSet reachedSet = reachedSetFactory.create();
        AbstractState initialState = cpa.getInitialState(initialLocation, StateSpacePartition.getDefaultPartition());
        Precision initialPrecision = cpa.getInitialPrecision(initialLocation, StateSpacePartition.getDefaultPartition());
        reachedSet.add(initialState, initialPrecision);
        algorithm.run(reachedSet);
        return algorithm.getCurrentInvariants();

      } catch (SolverException e) {
        throw new CPAException("Solver Failure", e);
      } finally {
        stats.invariantGeneration.stop();
        CPAs.closeCpaIfPossible(cpa, logger);
        CPAs.closeIfPossible(algorithm, logger);
      }
    }
  }

  private static Set<CandidateInvariant> getCandidateInvariants(CFA pCFA, TargetLocationProvider pTargetLocationProvider) {

    final Set<CandidateInvariant> candidates = Sets.newLinkedHashSet();

    for (AssumeEdge assumeEdge : getRelevantAssumeEdges(pTargetLocationProvider.tryGetAutomatonTargetLocations(pCFA.getMainFunction()))) {
      candidates.add(new EdgeFormulaNegation(pCFA.getLoopStructure().get().getAllLoopHeads(), assumeEdge));
    }

    return candidates;
  }

  /**
   * Gets the relevant assume edges.
   *
   * @param pTargetLocations the predetermined target locations.
   *
   * @return the relevant assume edges.
   */
  private static Set<AssumeEdge> getRelevantAssumeEdges(Collection<CFANode> pTargetLocations) {
    final Set<AssumeEdge> assumeEdges = Sets.newLinkedHashSet();
    Set<CFANode> visited = Sets.newHashSet(pTargetLocations);
    Queue<CFANode> waitlist = new ArrayDeque<>(pTargetLocations);
    while (!waitlist.isEmpty()) {
      CFANode current = waitlist.poll();
      for (CFAEdge enteringEdge : CFAUtils.enteringEdges(current)) {
        CFANode predecessor = enteringEdge.getPredecessor();
        if (enteringEdge.getEdgeType() == CFAEdgeType.AssumeEdge) {
          assumeEdges.add((AssumeEdge)enteringEdge);
        } else if (visited.add(predecessor)) {
          waitlist.add(predecessor);
        }
      }
    }
    return assumeEdges;
  }
}
