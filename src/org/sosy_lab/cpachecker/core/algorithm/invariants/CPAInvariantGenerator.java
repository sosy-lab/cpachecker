// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.invariants;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Throwables;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
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
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.invariants.ExpressionTreeInvariantSupplier;
import org.sosy_lab.cpachecker.util.predicates.invariants.FormulaInvariantsSupplier;

/**
 * Class that encapsulates invariant generation by using the CPAAlgorithm with an appropriate
 * configuration.
 */
@Options(prefix = "invariantGeneration")
public class CPAInvariantGenerator extends AbstractInvariantGenerator
    implements StatisticsProvider {

  public static class CPAInvariantGeneratorStatistics implements Statistics {

    private final Timer invariantGeneration = new Timer();

    @Override
    public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
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

  @Option(
      secure = true,
      name = "config",
      required = true,
      description = "configuration file for invariant generation")
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
  private Future<AggregatedReachedSets> invariantGenerationFuture = null;

  private volatile boolean programIsSafe = false;

  @SuppressWarnings("UnnecessaryAnonymousClass") // ShutdownNotifier needs a strong reference
  private final ShutdownRequestListener shutdownListener =
      new ShutdownRequestListener() {

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
   * @param pShutdownOnSafeManager optional shutdown notifier that will be notified if the invariant
   *     generator proves safety.
   * @param pCFA the CFA to run the CPA on.
   * @param additionalAutomata additional specification automata that should be used during
   *     invariant generation
   * @return a new {@link CPAInvariantGenerator}.
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

    final ShutdownManager childShutdownManager =
        ShutdownManager.createWithParent(pShutdownManager.getNotifier());

    return new CPAInvariantGenerator(
        pConfig,
        pLogger.withComponentName("CPAInvariantGenerator"),
        childShutdownManager,
        pShutdownOnSafeManager,
        1,
        pCFA,
        specification,
        additionalAutomata);
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
      throw new InvalidConfigurationException(
          "could not read configuration file for invariant generation: " + e.getMessage(), e);
    }

    reachedSetFactory = new ReachedSetFactory(invariantConfig, logger);
    cfa = pCFA;
    cpa =
        new CPABuilder(invariantConfig, logger, shutdownManager.getNotifier(), reachedSetFactory)
            .buildCPAs(cfa, pSpecification, pAdditionalAutomata, AggregatedReachedSets.empty());
    algorithm = CPAAlgorithm.create(cpa, logger, invariantConfig, shutdownManager.getNotifier());
  }

  private CPAInvariantGenerator(
      final Configuration config,
      final LogManager pLogger,
      final ShutdownManager pShutdownManager,
      Optional<ShutdownManager> pShutdownOnSafeManager,
      final int pIteration,
      final CFA pCFA,
      ReachedSetFactory pReachedSetFactory,
      ConfigurableProgramAnalysis pCPA,
      CPAAlgorithm pAlgorithm,
      CPAInvariantGeneratorStatistics pStats)
      throws InvalidConfigurationException {
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
  protected void startImpl(final CFANode initialLocation) {
    checkState(invariantGenerationFuture == null);

    Callable<AggregatedReachedSets> task = new InvariantGenerationTask(initialLocation);
    // create future for lazy synchronous invariant generation
    invariantGenerationFuture = new LazyFutureTask<>(task);

    shutdownManager.getNotifier().registerAndCheckImmediately(shutdownListener);
  }

  @Override
  public void cancel() {
    checkState(invariantGenerationFuture != null);
    shutdownManager.requestShutdown("Invariant generation cancel requested.");
  }

  private AggregatedReachedSets getAggregatedReachedSets()
      throws CPAException, InterruptedException {

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
  public InvariantSupplier getSupplier() throws CPAException, InterruptedException {

    return new FormulaInvariantsSupplier(getAggregatedReachedSets());
  }

  @Override
  public ExpressionTreeSupplier getExpressionTreeSupplier()
      throws CPAException, InterruptedException {
    return new ExpressionTreeInvariantSupplier(getAggregatedReachedSets(), cfa);
  }

  @Override
  public boolean isProgramSafe() {
    return programIsSafe;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (cpa instanceof StatisticsProvider) {
      ((StatisticsProvider) cpa).collectStatistics(pStatsCollection);
    }
    algorithm.collectStatistics(pStatsCollection);
    pStatsCollection.add(stats);
  }

  /**
   * Callable for creating invariants by running the CPAAlgorithm, potentially in a loop with
   * increasing precision. Returns the final invariants.
   */
  private class InvariantGenerationTask implements Callable<AggregatedReachedSets> {

    private static final String SAFE_MESSAGE =
        "Invariant generation with abstract interpretation proved specification to hold.";
    private final CFANode initialLocation;

    private InvariantGenerationTask(final CFANode pInitialLocation) {
      initialLocation = checkNotNull(pInitialLocation);
    }

    @Override
    public AggregatedReachedSets call() throws Exception {
      stats.invariantGeneration.start();
      try {

        shutdownManager.getNotifier().shutdownIfNecessary();
        logger.log(
            Level.INFO,
            "Starting iteration",
            iteration,
            "of invariant generation with abstract interpretation.");

        return runInvariantGeneration(initialLocation);

      } finally {
        stats.invariantGeneration.stop();
      }
    }

    private AggregatedReachedSets runInvariantGeneration(CFANode pInitialLocation)
        throws CPAException, InterruptedException {

      ReachedSet taskReached =
          reachedSetFactory.createAndInitialize(
              cpa, pInitialLocation, StateSpacePartition.getDefaultPartition());

      while (taskReached.hasWaitingState()) {
        if (!algorithm.run(taskReached).isSound()) {
          // ignore unsound invariant and abort
          return AggregatedReachedSets.empty();
        }
      }

      if (!taskReached.wasTargetReached()
          && !from(taskReached).anyMatch(CPAInvariantGenerator::hasAssumption)) {
        // program is safe (waitlist is empty, algorithm was sound, no target states present)
        logger.log(Level.INFO, SAFE_MESSAGE);
        programIsSafe = true;
        if (shutdownOnSafeNotifier.isPresent()) {
          shutdownOnSafeNotifier.orElseThrow().requestShutdown(SAFE_MESSAGE);
        }
      }

      checkState(!taskReached.hasWaitingState());
      checkState(!taskReached.isEmpty());
      return AggregatedReachedSets.singleton(taskReached);
    }
  }

  private static boolean hasAssumption(AbstractState state) {
    AssumptionStorageState assumption =
        AbstractStates.extractStateByType(state, AssumptionStorageState.class);
    return assumption != null && !assumption.isStopFormulaTrue() && !assumption.isAssumptionTrue();
  }
}
