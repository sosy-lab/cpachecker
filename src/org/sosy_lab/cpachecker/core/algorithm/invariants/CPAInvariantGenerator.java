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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;

import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.common.LazyFutureTask;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier.TrivialInvariantSupplier;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AdjustableConditionCPA;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;

/**
 * Class that encapsulates invariant generation by using the CPAAlgorithm
 * with an appropriate configuration.
 * Supports synchronous and asynchronous execution,
 * and continuously-refined invariants.
 */
@Options(prefix="invariantGeneration")
public class CPAInvariantGenerator extends AbstractInvariantGenerator implements StatisticsProvider {

  public static class CPAInvariantGeneratorStatistics implements Statistics {

    private final Timer invariantGeneration = new Timer();

    @Override
    public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
      if (invariantGeneration.getNumberOfIntervals() > 0) {
        out.println("Time for invariant generation:   " + invariantGeneration.getSumTime());
      }
    }

    public TimeSpan getConsumedTime() {
      return invariantGeneration.getSumTime();
    }

    @Override
    public String getName() {
      return "CPA-based invariant generator";
    }
  }

  @Options(prefix="invariantGeneration")
  private static class InvariantGeneratorOptions {


    @Option(secure=true, description="generate invariants in parallel to the normal analysis")
    private boolean async = false;

    @Option(secure=true, description="adjust invariant generation conditions if supported by the analysis")
    private boolean adjustConditions = false;

  }

  @Option(secure=true, name="config",
          required=true,
          description="configuration file for invariant generation")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path configFile;

  private final CPAInvariantGeneratorStatistics stats;
  private final LogManager logger;
  private final CPAAlgorithm algorithm;
  private final ConfigurableProgramAnalysis cpa;
  private final ReachedSetFactory reachedSetFactory;
  private final CFA cfa;

  private final ShutdownManager shutdownManager;

  private final int iteration;

  // After start(), this will hold a Future for the final result of the invariant generation.
  // We use a Future instead of just the atomic reference below
  // to be able to ask for termination and see thrown exceptions.
  private Future<FormulaAndTreeSupplier> invariantGenerationFuture = null;

  private volatile boolean programIsSafe = false;

  private final ShutdownRequestListener shutdownListener = new ShutdownRequestListener() {

    @Override
    public void shutdownRequested(String pReason) {
      if (!invariantGenerationFuture.isDone() && !programIsSafe) {
        invariantGenerationFuture.cancel(true);
      }
    }
  };

  private Optional<ShutdownManager> shutdownOnSafeNotifier;

  /**
   * Creates a new {@link CPAInvariantGenerator}.
   *
   * @param pConfig the configuration options.
   * @param pLogger the logger to be used.
   * @param pShutdownManager shutdown notifier to shutdown the invariant generator.
   * @param pShutdownOnSafeManager optional shutdown notifier that will be
   * notified if the invariant generator proves safety.
   * @param pCFA the CFA to run the CPA on.
   *
   * @return a new {@link CPAInvariantGenerator}.
   *
   * @throws InvalidConfigurationException if the configuration is invalid.
   * @throws CPAException if the CPA cannot be created.
   */
  public static InvariantGenerator create(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownManager pShutdownManager,
      final Optional<ShutdownManager> pShutdownOnSafeManager,
      final CFA pCFA,
      final Specification pSpecification)
      throws InvalidConfigurationException, CPAException {
    return create(
        pConfig,
        pLogger,
        pShutdownManager,
        pShutdownOnSafeManager,
        pCFA,
        pSpecification,
        Collections.<Automaton>emptyList());
  }

  /**
   * Creates a new {@link CPAInvariantGenerator}.
   *
   * @param pConfig the configuration options.
   * @param pLogger the logger to be used.
   * @param pShutdownManager shutdown notifier to shutdown the invariant generator.
   * @param pShutdownOnSafeManager optional shutdown notifier that will be
   * notified if the invariant generator proves safety.
   * @param pCFA the CFA to run the CPA on.
   * @param additionalAutomata additional specification automata that should be used
   *                           during invariant generation
   *
   * @return a new {@link CPAInvariantGenerator}.
   *
   * @throws InvalidConfigurationException if the configuration is invalid.
   * @throws CPAException if the CPA cannot be created.
   */
  public static InvariantGenerator create(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownManager pShutdownManager,
      final Optional<ShutdownManager> pShutdownOnSafeManager,
      final CFA pCFA,
      final Specification specification,
      final List<Automaton> additionalAutomata)
      throws InvalidConfigurationException, CPAException {

    InvariantGeneratorOptions options = new InvariantGeneratorOptions();
    pConfig.inject(options);
    final ShutdownManager childShutdownManager =
        ShutdownManager.createWithParent(pShutdownManager.getNotifier());

    CPAInvariantGenerator cpaInvariantGenerator =
        new CPAInvariantGenerator(
            pConfig,
            pLogger.withComponentName("CPAInvariantGenerator"),
            childShutdownManager,
            pShutdownOnSafeManager,
            1,
            pCFA,
            specification,
            additionalAutomata);

    InvariantGenerator invariantGenerator = cpaInvariantGenerator;
    final Function<CPAInvariantGenerator, CPAInvariantGenerator> adjust;
    if (options.adjustConditions) {
      adjust =
          new Function<CPAInvariantGenerator, CPAInvariantGenerator>() {

            @Override
            public CPAInvariantGenerator apply(CPAInvariantGenerator pToAdjust) {
              ConfigurableProgramAnalysis cpa = pToAdjust.cpa;
              LogManager logger = pToAdjust.logger;
              List<AdjustableConditionCPA> conditionCPAs =
                  CPAs.asIterable(cpa).filter(AdjustableConditionCPA.class).toList();
              CPAInvariantGenerator result = pToAdjust;
              try {
                if (adjustConditions(logger, conditionCPAs)) {
                  result =
                      new CPAInvariantGenerator(
                          pConfig,
                          pLogger,
                          childShutdownManager,
                          pShutdownOnSafeManager,
                          pToAdjust.iteration + 1,
                          pCFA,
                          pToAdjust.reachedSetFactory,
                          cpa,
                          pToAdjust.algorithm,
                          pToAdjust.stats);
                }
              } catch (InvalidConfigurationException e) {
                pLogger.logUserException(
                    Level.WARNING, e, "Creating adjusted invariant generator failed");
              } finally {
                if (result == pToAdjust) {
                  CPAs.closeCpaIfPossible(pToAdjust.cpa, pToAdjust.logger);
                  CPAs.closeIfPossible(pToAdjust.algorithm, pToAdjust.logger);
                }
              }
              return result;
            }

            private boolean adjustConditions(
                LogManager pLogger, List<AdjustableConditionCPA> pConditionCPAs) {

              boolean adjusted = false;

              // Adjust precision if at least one CPA can do it.
              for (AdjustableConditionCPA cpa : pConditionCPAs) {
                if (cpa.adjustPrecision()) {
                  pLogger.log(Level.INFO, "Adjusting precision for CPA", cpa);
                  adjusted = true;
                }
              }
              if (!adjusted) {
                pLogger.log(
                    Level.INFO,
                    "None of the CPAs could adjust precision, " + "stopping invariant generation");
              }
              return adjusted;
            }
          };
    } else {
      adjust = new Function<CPAInvariantGenerator, CPAInvariantGenerator>() {

        @Override
        public CPAInvariantGenerator apply(CPAInvariantGenerator pArg0) {
          CPAs.closeCpaIfPossible(pArg0.cpa, pArg0.logger);
          CPAs.closeIfPossible(pArg0.algorithm, pArg0.logger);
          return pArg0;
        }

      };
    }
    invariantGenerator =
        new AdjustableInvariantGenerator<>(
            pShutdownManager.getNotifier(), cpaInvariantGenerator, adjust);
    if (options.async) {
      invariantGenerator =
          new AutoAdjustingInvariantGenerator<>(
              pShutdownManager.getNotifier(), cpaInvariantGenerator, adjust);
    }
    return invariantGenerator;
  }

  private CPAInvariantGenerator(
      final Configuration config,
      final LogManager pLogger,
      final ShutdownManager pShutdownManager,
      Optional<ShutdownManager> pShutdownOnSafeManager,
      final int pIteration,
      final CFA pCFA,
      final Specification pSpecification,
      final List<Automaton> pAdditionalAutomata)
      throws InvalidConfigurationException, CPAException {
    config.inject(this);
    stats = new CPAInvariantGeneratorStatistics();
    logger = pLogger;
    shutdownManager = pShutdownManager;
    shutdownOnSafeNotifier = pShutdownOnSafeManager;
    iteration = pIteration;

    Configuration invariantConfig;
    try {
      invariantConfig = Configuration.builder().loadFromFile(configFile).build();
    } catch (IOException e) {
      throw new InvalidConfigurationException("could not read configuration file for invariant generation: " + e.getMessage(), e);
    }

    reachedSetFactory = new ReachedSetFactory(invariantConfig);
    cfa = pCFA;
    cpa =
        new CPABuilder(invariantConfig, logger, shutdownManager.getNotifier(), reachedSetFactory)
            .buildCPAs(cfa, pSpecification, pAdditionalAutomata, new AggregatedReachedSets());
    algorithm = CPAAlgorithm.create(cpa, logger, invariantConfig, shutdownManager.getNotifier());
  }

  private CPAInvariantGenerator(final Configuration config,
      final LogManager pLogger,
      final ShutdownManager pShutdownManager,
      Optional<ShutdownManager> pShutdownOnSafeManager,
      final int pIteration,
      final CFA pCFA,
      ReachedSetFactory pReachedSetFactory,
      ConfigurableProgramAnalysis pCPA,
      CPAAlgorithm pAlgorithm,
      CPAInvariantGeneratorStatistics pStats) throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;
    shutdownManager = pShutdownManager;
    shutdownOnSafeNotifier = pShutdownOnSafeManager;
    iteration = pIteration;

    reachedSetFactory = pReachedSetFactory;
    cfa = pCFA;
    cpa = pCPA;
    algorithm = pAlgorithm;

    stats = pStats;
  }

  @Override
  public void start(final CFANode initialLocation) {
    checkState(invariantGenerationFuture == null);

    Callable<FormulaAndTreeSupplier> task = new InvariantGenerationTask(initialLocation);
    // create future for lazy synchronous invariant generation
    invariantGenerationFuture = new LazyFutureTask<>(task);

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

  @Override
  public ExpressionTreeSupplier getAsExpressionTree() throws CPAException, InterruptedException {
    checkState(invariantGenerationFuture != null);

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

  @Override
  public boolean isProgramSafe() {
    return programIsSafe;
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
   * Callable for creating invariants by running the CPAAlgorithm,
   * potentially in a loop with increasing precision.
   * Returns the final invariants.
   */
  private class InvariantGenerationTask implements Callable<FormulaAndTreeSupplier> {

    private static final String SAFE_MESSAGE = "Invariant generation with abstract interpretation proved specification to hold.";
    private final CFANode initialLocation;

    private InvariantGenerationTask(final CFANode pInitialLocation) {
      initialLocation = checkNotNull(pInitialLocation);
    }

    @Override
    public FormulaAndTreeSupplier call() throws Exception {
      stats.invariantGeneration.start();
      try {

        shutdownManager.getNotifier().shutdownIfNecessary();
        logger.log(Level.INFO, "Starting iteration", iteration, "of invariant generation with abstract interpretation.");

        return runInvariantGeneration(initialLocation);

      } finally {
        stats.invariantGeneration.stop();
      }
    }

    private FormulaAndTreeSupplier runInvariantGeneration(CFANode pInitialLocation)
        throws CPAException, InterruptedException {

      ReachedSet taskReached = reachedSetFactory.create();
      taskReached.add(cpa.getInitialState(pInitialLocation, StateSpacePartition.getDefaultPartition()),
          cpa.getInitialPrecision(pInitialLocation, StateSpacePartition.getDefaultPartition()));

      while (taskReached.hasWaitingState()) {
        if (!algorithm.run(taskReached).isSound()) {
          // ignore unsound invariant and abort
          return new FormulaAndTreeSupplier(
              TrivialInvariantSupplier.INSTANCE,
              org.sosy_lab.cpachecker.core.algorithm.invariants.ExpressionTreeSupplier.TrivialInvariantSupplier.INSTANCE);
        }
      }

      if (!from(taskReached).anyMatch(Predicates.<AbstractState>or(IS_TARGET_STATE, HAS_ASSUMPTIONS))) {
        // program is safe (waitlist is empty, algorithm was sound, no target states present)
        logger.log(Level.INFO, SAFE_MESSAGE);
        programIsSafe = true;
        if (shutdownOnSafeNotifier.isPresent()) {
          shutdownOnSafeNotifier.get().requestShutdown(SAFE_MESSAGE);
        }
      }

      checkState(!taskReached.hasWaitingState());
      checkState(!taskReached.isEmpty());
      return new FormulaAndTreeSupplier(new LazyLocationMapping(taskReached), cfa);
    }
  }


  private final Predicate<AbstractState> HAS_ASSUMPTIONS =
      state -> {
        AssumptionStorageState assumption =
            AbstractStates.extractStateByType(state, AssumptionStorageState.class);
        return assumption != null
            && !assumption.isStopFormulaTrue()
            && !assumption.isAssumptionTrue();
      };
}
