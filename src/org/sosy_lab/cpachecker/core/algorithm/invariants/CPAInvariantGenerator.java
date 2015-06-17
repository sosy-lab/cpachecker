/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
import static com.google.common.base.Verify.verifyNotNull;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.common.LazyFutureTask;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.common.concurrency.Threads;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier.TrivialInvariantSupplier;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AdjustableConditionCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSetWrapper;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Throwables;

/**
 * Class that encapsulates invariant generation by using the CPAAlgorithm
 * with an appropriate configuration.
 * Supports synchronous and asynchronous execution,
 * and continuously-refined invariants.
 */
@Options(prefix="invariantGeneration")
public class CPAInvariantGenerator implements InvariantGenerator, StatisticsProvider {

  private static class CPAInvariantGeneratorStatistics implements Statistics {

    final Timer invariantGeneration = new Timer();

    @Override
    public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
      out.println("Time for invariant generation:   " + invariantGeneration);
    }

    @Override
    public String getName() {
      return "CPA-based invariant generator";
    }
  }

  @Option(secure=true, name="config",
          required=true,
          description="configuration file for invariant generation")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path configFile;

  @Option(secure=true, description="generate invariants in parallel to the normal analysis")
  private boolean async = false;

  @Option(secure=true, description="adjust invariant generation conditions if supported by the analysis")
  private boolean adjustConditions = false;

  private final CPAInvariantGeneratorStatistics stats = new CPAInvariantGeneratorStatistics();
  private final LogManager logger;
  private final CPAAlgorithm algorithm;
  private final ConfigurableProgramAnalysis cpa;
  private final ReachedSetFactory reachedSetFactory;

  private final ShutdownNotifier shutdownNotifier;

  // After start(), this will hold a Future for the final result of the invariant generation.
  // We use a Future instead of just the atomic reference below
  // to be able to ask for termination and see thrown exceptions.
  private Future<InvariantSupplier> invariantGenerationFuture = null;

  // In case of (async & adjustConditions), this will point to the last invariant
  // that the continuously-refining invariant generation produced so far.
  private final AtomicReference<InvariantSupplier> latestInvariant = new AtomicReference<>();

  private volatile boolean programIsSafe = false;

  private final ShutdownRequestListener shutdownListener = new ShutdownRequestListener() {

    @Override
    public void shutdownRequested(String pReason) {
      invariantGenerationFuture.cancel(true);
    }
  };

  public static CPAInvariantGenerator create(final Configuration pConfig,
      final LogManager pLogger, final ShutdownNotifier pShutdownNotifier,
      final CFA pCFA)
          throws InvalidConfigurationException, CPAException {

    return new CPAInvariantGenerator(
            pConfig,
            pLogger.withComponentName("CPAInvariantGenerator"),
            ShutdownNotifier.createWithParent(pShutdownNotifier),
            pCFA);
  }

  private CPAInvariantGenerator(final Configuration config, final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier, final CFA cfa)
          throws InvalidConfigurationException, CPAException {
    config.inject(this);
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;

    Configuration invariantConfig;
    try {
      ConfigurationBuilder configBuilder = Configuration.builder().copyOptionFrom(config, "specification");
      configBuilder.loadFromFile(configFile);
      invariantConfig = configBuilder.build();
    } catch (IOException e) {
      throw new InvalidConfigurationException("could not read configuration file for invariant generation: " + e.getMessage(), e);
    }

    reachedSetFactory = new ReachedSetFactory(invariantConfig, logger);
    cpa = new CPABuilder(invariantConfig, logger, shutdownNotifier, reachedSetFactory).buildCPAWithSpecAutomatas(cfa);
    algorithm = CPAAlgorithm.create(cpa, logger, invariantConfig, shutdownNotifier);
  }

  @Override
  public void start(final CFANode initialLocation) {
    checkState(invariantGenerationFuture == null);

    Callable<InvariantSupplier> task = new InvariantGenerationTask(initialLocation);

    if (async) {
      if (adjustConditions) {
        latestInvariant.set(InvariantSupplier.TrivialInvariantSupplier.INSTANCE);
      }

      // start invariant generation asynchronously
      ExecutorService executor = Executors.newSingleThreadExecutor(Threads.threadFactory());
      invariantGenerationFuture = executor.submit(task);
      executor.shutdown(); // will shutdown after task is finished

    } else {
      // create future for lazy synchronous invariant generation
      invariantGenerationFuture = new LazyFutureTask<>(task);
    }

    shutdownNotifier.registerAndCheckImmediately(shutdownListener);
  }

  @Override
  public void cancel() {
    checkState(invariantGenerationFuture != null);
    shutdownNotifier.requestShutdown("Invariant generation cancel requested.");
  }

  @Override
  public InvariantSupplier get() throws CPAException, InterruptedException {
    checkState(invariantGenerationFuture != null);

    if (async && adjustConditions && !invariantGenerationFuture.isDone()) {
      // grab intermediate result that is available so far
      return verifyNotNull(latestInvariant.get());

    } else {
      try {
        return invariantGenerationFuture.get();
      } catch (ExecutionException e) {
        Throwables.propagateIfPossible(e.getCause(), CPAException.class, InterruptedException.class);
        throw new UnexpectedCheckedException("invariant generation", e.getCause());
      } catch (CancellationException e) {
        shutdownNotifier.shutdownIfNecessary();
        throw e;
      }
    }
  }

  @Override
  public boolean isProgramSafe() {
    return programIsSafe;
  }

  @Override
  public void injectInvariant(CFANode pLocation, AssumeEdge pAssumption) throws UnrecognizedCodeException {
    InvariantsCPA invariantCPA = CPAs.retrieveCPA(cpa, InvariantsCPA.class);
    if (invariantCPA != null) {
      invariantCPA.injectInvariant(pLocation, pAssumption);
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (cpa instanceof StatisticsProvider) {
      ((StatisticsProvider)cpa).collectStatistics(pStatsCollection);
    }
    algorithm.collectStatistics(pStatsCollection);
    pStatsCollection.add(stats);
  }

  /**
   * {@link InvariantSupplier} that extracts invariants from a {@link ReachedSet}
   * with {@link FormulaReportingState}s.
   */
  private static class ReachedSetBasedInvariantSupplier implements InvariantSupplier {

    private final LogManager logger;
    private final UnmodifiableReachedSet reached;

    private ReachedSetBasedInvariantSupplier(UnmodifiableReachedSet pReached,
        LogManager pLogger) {
      checkArgument(!pReached.hasWaitingState());
      checkArgument(!pReached.isEmpty());
      reached = pReached;
      logger = pLogger;
    }

    @Override
    public BooleanFormula getInvariantFor(CFANode pLocation, FormulaManagerView fmgr, PathFormulaManager pfmgr) {
      BooleanFormulaManager bfmgr = fmgr.getBooleanFormulaManager();
      BooleanFormula invariant = bfmgr.makeBoolean(false);

      for (AbstractState locState : AbstractStates.filterLocation(reached, pLocation)) {
        BooleanFormula f = AbstractStates.extractReportedFormulas(fmgr, locState, pfmgr);
        logger.log(Level.ALL, "Invariant for", pLocation+":", f);

        invariant = bfmgr.or(invariant, f);
      }
      return invariant;
    }
  }

  /**
   * Callable for creating invariants by running the CPAAlgorithm,
   * potentially in a loop with increasing precision.
   * Returns the final invariants,
   * and publishes intermediate results to {@link CPAInvariantGenerator#latestInvariant}.
   */
  private class InvariantGenerationTask implements Callable<InvariantSupplier> {

    private final List<AdjustableConditionCPA> conditionCPAs;
    private final CFANode initialLocation;

    private InvariantGenerationTask(final CFANode pInitialLocation) {
      initialLocation = checkNotNull(pInitialLocation);
      conditionCPAs = CPAs.asIterable(cpa).filter(AdjustableConditionCPA.class).toList();

      if (adjustConditions && conditionCPAs.isEmpty()) {
        logger.log(Level.WARNING, "Cannot adjust invariant generation: No adjustable CPAs.");
      }
    }

    @Override
    public InvariantSupplier call() throws Exception {
      stats.invariantGeneration.start();
      try {

        int i = 0;
        InvariantSupplier invariant;
        do {
          shutdownNotifier.shutdownIfNecessary();
          logger.log(Level.INFO, "Starting iteration", ++i, "of invariant generation with abstract interpretation.");

          invariant = runInvariantGeneration(initialLocation);
          latestInvariant.set(invariant);
        } while (!programIsSafe && adjustConditions());

        return invariant;

      } finally {
        stats.invariantGeneration.stop();
        CPAs.closeCpaIfPossible(cpa, logger);
        CPAs.closeIfPossible(algorithm, logger);
      }
    }

    private InvariantSupplier runInvariantGeneration(CFANode pInitialLocation)
        throws CPAException, InterruptedException {

      ReachedSet taskReached = reachedSetFactory.create();
      taskReached.add(cpa.getInitialState(pInitialLocation, StateSpacePartition.getDefaultPartition()),
          cpa.getInitialPrecision(pInitialLocation, StateSpacePartition.getDefaultPartition()));

      while (taskReached.hasWaitingState()) {
        if (!algorithm.run(taskReached).isSound()) {
          // ignore unsound invariant and abort
          return TrivialInvariantSupplier.INSTANCE;
        }
      }

      if (!from(taskReached).anyMatch(IS_TARGET_STATE)) {
        // program is safe (waitlist is empty, algorithm was sound, no target states present)
        logger.log(Level.INFO, "Invariant generation with abstract interpretation proved specification to hold.");
        programIsSafe = true;
      }

      return new ReachedSetBasedInvariantSupplier(
          new UnmodifiableReachedSetWrapper(taskReached), logger);
    }

    private boolean adjustConditions() {
      if (!adjustConditions || conditionCPAs.isEmpty()) {
        return false;
      }

      for (AdjustableConditionCPA cpa : conditionCPAs) {
        if (!cpa.adjustPrecision()) {
          logger.log(Level.INFO, "Further invariant generation adjustments denied by", cpa.getClass().getSimpleName());
          return false;
        }
      }
      return true;
    }
  }
}