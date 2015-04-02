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

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.sosy_lab.common.LazyFutureTask;
import org.sosy_lab.common.concurrency.Threads;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.BMCAlgorithm;
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
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Throwables;

/**
 * Generate invariants using k-induction.
 */
public class KInductionInvariantGenerator implements InvariantGenerator, StatisticsProvider {

  private final BMCAlgorithm bmcAlgorithm;

  private final ConfigurableProgramAnalysis cpa;

  private final ReachedSetFactory reachedSetFactory;

  private final LogManager logger;

  private final ShutdownNotifier shutdownNotifier;

  private final boolean async;

  private Future<InvariantSupplier> invariantGenerationFuture = null;

  private final Timer invariantGeneration = new Timer();

  private final PathFormulaManager clientPFM;

  private final ExecutorService executorService = Executors.newSingleThreadExecutor(Threads.threadFactory());

  private final ShutdownRequestListener shutdownListener;

  public static KInductionInvariantGenerator create(final Configuration pConfig,
      final LogManager pLogger, final ShutdownNotifier pShutdownNotifier,
      final CFA pCFA, final ReachedSetFactory pReachedSetFactory,
      final ConfigurableProgramAnalysis pStepCaseCPA)
      throws InvalidConfigurationException, CPAException {

    LogManager logger = pLogger.withComponentName("KInductionInvariantGenerator");
    ShutdownNotifier invGenBMCShutdownNotfier = ShutdownNotifier.createWithParent(pShutdownNotifier);
    CPABuilder invGenBMCBuilder = new CPABuilder(pConfig, logger, invGenBMCShutdownNotfier, pReachedSetFactory);
    ConfigurableProgramAnalysis invGenBMCCPA = invGenBMCBuilder.buildCPAWithSpecAutomatas(pCFA);
    Algorithm invGenBMCCPAAlgorithm = CPAAlgorithm.create(invGenBMCCPA, logger, pConfig, invGenBMCShutdownNotfier);
    BMCAlgorithm invGenBMC = new BMCAlgorithm(invGenBMCCPAAlgorithm, invGenBMCCPA, pConfig, logger, pReachedSetFactory, invGenBMCShutdownNotfier, pCFA, true);

    PredicateCPA stepCasePredicateCPA = CPAs.retrieveCPA(pStepCaseCPA, PredicateCPA.class);

    KInductionInvariantGenerator kIndInvGen =
        new KInductionInvariantGenerator(
            invGenBMC,
            pReachedSetFactory,
            invGenBMCCPA, logger,
            invGenBMCShutdownNotfier,
            stepCasePredicateCPA.getPathFormulaManager(),
            true);
    return kIndInvGen;
  }

  private KInductionInvariantGenerator(
      BMCAlgorithm pBMCAlgorithm,
      ReachedSetFactory pReachedSetFactory,
      ConfigurableProgramAnalysis pCPA,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      PathFormulaManager pClientPFM,
      boolean pAsync) throws InvalidConfigurationException {
    bmcAlgorithm = checkNotNull(pBMCAlgorithm);
    async = pAsync;
    PredicateCPA predicateCPA = CPAs.retrieveCPA(pCPA, PredicateCPA.class);
    if (predicateCPA == null) {
      throw new InvalidConfigurationException("Predicate CPA required");
    }
    if (async && !predicateCPA.getSolver().getFormulaManager().getVersion().toLowerCase().contains("smtinterpol")) {
      throw new InvalidConfigurationException("Solver does not support concurrent execution, use SMTInterpol instead.");
    }
    cpa = pCPA;
    reachedSetFactory = pReachedSetFactory;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    clientPFM = pClientPFM;

    shutdownListener = new ShutdownRequestListener() {

      @Override
      public void shutdownRequested(String pReason) {
        executorService.shutdownNow();
      }
    };
  }

  @Override
  public void start(final CFANode pInitialLocation) {

    checkNotNull(pInitialLocation);
    checkState(invariantGenerationFuture == null);


    Callable<InvariantSupplier> task = new Callable<InvariantSupplier>() {

      @Override
      public InvariantSupplier call() throws InterruptedException, CPAException {
        invariantGeneration.start();
        shutdownNotifier.shutdownIfNecessary();

        try {
          ReachedSet reachedSet = reachedSetFactory.create();
          AbstractState initialState = cpa.getInitialState(pInitialLocation, StateSpacePartition.getDefaultPartition());
          Precision initialPrecision = cpa.getInitialPrecision(pInitialLocation, StateSpacePartition.getDefaultPartition());
          reachedSet.add(initialState, initialPrecision);
          bmcAlgorithm.run(reachedSet);
          return getResults();

        } finally {
          CPAs.closeCpaIfPossible(cpa, logger);
          CPAs.closeIfPossible(bmcAlgorithm, logger);
          invariantGeneration.stop();
        }
      }

    };

    if (async) {
      shutdownNotifier.registerAndCheckImmediately(shutdownListener);
      if (!executorService.isShutdown() && !shutdownNotifier.shouldShutdown()) {
        invariantGenerationFuture = executorService.submit(task);
      } else {
        invariantGenerationFuture = new LazyFutureTask<>(task);
      }
    } else {
      invariantGenerationFuture = new LazyFutureTask<>(task);
    }
  }

  private InvariantSupplier getResults() {
    return new InvariantSupplier() {
      @Override
      public BooleanFormula getInvariantFor(CFANode pNode, FormulaManagerView pFmgr) {
        return bmcAlgorithm.getCurrentLocationInvariants(pNode, pFmgr, clientPFM);
      }
    };
  }

  @Override
  public void cancel() {
    checkState(invariantGenerationFuture != null);
    shutdownNotifier.requestShutdown("Invariant generation cancel requested.");
  }

  @Override
  public InvariantSupplier get() throws CPAException, InterruptedException {
    if (async) {
      return getResults();
    } else {
      try {
        return invariantGenerationFuture.get();
      } catch (ExecutionException e) {
        Throwables.propagateIfPossible(e.getCause(), CPAException.class, InterruptedException.class);
        throw Throwables.propagate(e);
      }
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    bmcAlgorithm.collectStatistics(pStatsCollection);
  }
}
