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
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
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
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Throwables;

/**
 * Class that encapsulates invariant generation by using the CPAAlgorithm
 * with an appropriate configuration.
 * Supports synchronous and asynchronous execution,
 * and continuously-refine invariants.
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
  private final CPAAlgorithm invariantAlgorithm;
  private final ConfigurableProgramAnalysis invariantCPAs;
  private final ReachedSetFactory reachedSetFactory;

  private final ShutdownNotifier shutdownNotifier;

  // After start(), this will hold a Future for the final result of the invariant generation.
  // We use a Future instead of just the atomic reference below
  // to be able to ask for termination and see thrown exceptions.
  private Future<InvariantSupplier> invariantGenerationFuture = null;

  // In case of (async & adjustConditions), this will point to the last invariant
  // that the continuously-refining invariant generation produced so far.
  private final AtomicReference<InvariantSupplier> latestInvariant = new AtomicReference<>();

  private final ShutdownRequestListener shutdownListener = new ShutdownRequestListener() {

    @Override
    public void shutdownRequested(String pReason) {
      invariantGenerationFuture.cancel(true);
    }
  };

  public ConfigurableProgramAnalysis getCPAs() {
    return invariantCPAs;
  }

  public CPAInvariantGenerator(final Configuration config, final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier, final CFA cfa)
          throws InvalidConfigurationException, CPAException {
    config.inject(this);
    logger = pLogger.withComponentName("CPAInvariantGenerator");
    shutdownNotifier = ShutdownNotifier.createWithParent(pShutdownNotifier);

    Configuration invariantConfig;
    try {
      ConfigurationBuilder configBuilder = Configuration.builder().copyOptionFrom(config, "specification");
      configBuilder.loadFromFile(configFile);
      invariantConfig = configBuilder.build();
    } catch (IOException e) {
      throw new InvalidConfigurationException("could not read configuration file for invariant generation: " + e.getMessage(), e);
    }

    reachedSetFactory = new ReachedSetFactory(invariantConfig, logger);
    invariantCPAs = new CPABuilder(invariantConfig, logger, shutdownNotifier, reachedSetFactory).buildCPAWithSpecAutomatas(cfa);
    invariantAlgorithm = CPAAlgorithm.create(invariantCPAs, logger, invariantConfig, shutdownNotifier);
  }

  @Override
  public void start(final CFANode initialLocation) {
    checkNotNull(initialLocation);
    checkState(invariantGenerationFuture == null);

    if (async) {
      if (adjustConditions) {
        latestInvariant.set(InvariantSupplier.TrivialInvariantSupplier.INSTANCE);
      }

      // start invariant generation asynchronously
      ExecutorService executor = Executors.newSingleThreadExecutor(Threads.threadFactory());
      invariantGenerationFuture = executor.submit(new InvariantGenerationTask(initialLocation));
      executor.shutdown(); // will shutdown after task is finished

    } else {
      // create future for lazy synchronous invariant generation
      invariantGenerationFuture = new LazyFutureTask<>(new InvariantGenerationTask(initialLocation));
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
    shutdownNotifier.shutdownIfNecessary();

    if (invariantGenerationFuture.isDone() // finished
        || !adjustConditions // without continuously-refined invariants we should wait for the result
        ) {

      try {
        return invariantGenerationFuture.get();

      } catch (ExecutionException e) {
        Throwables.propagateIfPossible(e.getCause(), CPAException.class, InterruptedException.class);
        throw new UnexpectedCheckedException("invariant generation", e.getCause());
      } catch (CancellationException e) {
        InterruptedException ie = new InterruptedException();
        ie.initCause(e);
        throw ie;
      }

    } else {
      // grab intermediate result that is available so far
      return verifyNotNull(latestInvariant.get());
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (invariantCPAs instanceof StatisticsProvider) {
      ((StatisticsProvider)invariantCPAs).collectStatistics(pStatsCollection);
    }
    invariantAlgorithm.collectStatistics(pStatsCollection);
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
    public BooleanFormula getInvariantFor(CFANode pLocation, FormulaManagerView fmgr) {
      BooleanFormulaManager bfmgr = fmgr.getBooleanFormulaManager();
      BooleanFormula invariant = bfmgr.makeBoolean(false);

      for (AbstractState locState : AbstractStates.filterLocation(reached, pLocation)) {
        BooleanFormula f = AbstractStates.extractReportedFormulas(fmgr, locState);
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

    public InvariantGenerationTask(final CFANode pInitialLocation) {
      initialLocation = checkNotNull(pInitialLocation);
      conditionCPAs = CPAs.asIterable(invariantCPAs).filter(AdjustableConditionCPA.class).toList();

      if (adjustConditions && conditionCPAs.isEmpty()) {
        logger.log(Level.WARNING, "Cannot adjust invariant generation: No adjustable CPAs.");
      }
    }

    @Override
    public InvariantSupplier call() throws Exception {
      stats.invariantGeneration.start();
      InvariantSupplier invariant;
      try {
        invariant = runInvariantGeneration(initialLocation);
        latestInvariant.set(invariant);

        int i = 0;
        while (adjustConditions()) {
          shutdownNotifier.shutdownIfNecessary();

          logger.log(Level.INFO, "Starting iteration", ++i, "of invariant generation with abstract interpretation.");

          invariant = runInvariantGeneration(initialLocation);
          latestInvariant.set(invariant);
        }
      } finally {
        stats.invariantGeneration.stop();
        CPAs.closeCpaIfPossible(invariantCPAs, logger);
        CPAs.closeIfPossible(invariantAlgorithm, logger);
      }
      return invariant;
    }

    private InvariantSupplier runInvariantGeneration(CFANode pInitialLocation)
        throws CPAException, InterruptedException {

      ReachedSet taskReached = reachedSetFactory.create();
      synchronized (invariantCPAs) {
        taskReached.add(invariantCPAs.getInitialState(pInitialLocation, StateSpacePartition.getDefaultPartition()),
            invariantCPAs.getInitialPrecision(pInitialLocation, StateSpacePartition.getDefaultPartition()));
      }

      while (!taskReached.getWaitlist().isEmpty()) {
        invariantAlgorithm.run(taskReached);
      }

      return new ReachedSetBasedInvariantSupplier(
          new UnmodifiableReachedSetWrapper(taskReached), logger);
    }

    private boolean adjustConditions() {
      if (!adjustConditions || conditionCPAs.isEmpty()) {
        return false;
      }
      synchronized (invariantCPAs) {
        for (AdjustableConditionCPA cpa : conditionCPAs) {
          if (!cpa.adjustPrecision()) {
            logger.log(Level.INFO, "Further invariant generation adjustments denied by", cpa.getClass().getSimpleName());
            return false;
          }
        }
      }
      return true;
    }
  }
}