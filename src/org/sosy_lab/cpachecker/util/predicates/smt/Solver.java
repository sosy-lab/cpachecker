// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsWriter.writingStatisticsTo;
import static org.sosy_lab.java_smt.api.SolverContext.ProverOptions.GENERATE_UNSAT_CORE;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.util.predicates.interpolation.SeparateInterpolatingProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.ufCheckingProver.UFCheckingBasicProverEnvironment.UFCheckingProverOptions;
import org.sosy_lab.cpachecker.util.predicates.ufCheckingProver.UFCheckingInterpolatingProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.ufCheckingProver.UFCheckingProverEnvironment;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
import org.sosy_lab.java_smt.api.OptimizationProverEnvironment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.delegate.statistics.SolverStatistics;
import org.sosy_lab.java_smt.delegate.statistics.StatisticsSolverContext;

/**
 * Encapsulation of an SMT solver. This class is the central entry point to everything related to an
 * SMT solver: formula creation and manipulation (via the {@link #getFormulaManager()} method), and
 * checking for satisfiability (via the remaining methods). In addition to the low-level methods
 * provided by {@link FormulaManager}, this class and {@link FormulaManagerView} provide additional
 * higher-level utility methods, and additional features such as replacing one SMT theory
 * transparently with another, or using different SMT solvers for different tasks such as solving
 * and interpolation.
 */
@Options(deprecatedPrefix = "cpa.predicate.solver", prefix = "solver")
public final class Solver implements AutoCloseable {

  private static final String SOLVER_OPTION_NON_LINEAR_ARITHMETIC = "solver.nonLinearArithmetic";

  @Option(
      secure = true,
      name = "checkUFs",
      description = "improve sat-checks with additional constraints for UFs")
  private boolean checkUFs = false;

  @Option(secure = true, description = "Which SMT solver to use.")
  private Solvers solver = Solvers.MATHSAT5;

  @Option(
      secure = true,
      description =
          "Which solver to use specifically for interpolation (default is to use the main one).")
  @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NULL_VALUE")
  private @Nullable Solvers interpolationSolver = null;

  @Option(secure = true, description = "Extract and cache unsat cores for satisfiability checking")
  private boolean cacheUnsatCores = true;

  @Option(
      secure = true,
      description =
          "whether CPAchecker's logger should be used as logger for the solver, "
              + "otherwise nothing is logged from the solver.")
  private boolean enableLoggingInSolver = false;

  private final @Nullable UFCheckingProverOptions ufCheckingProverOptions;

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;

  private final SolverContext solvingContext;
  private final SolverContext interpolatingContext;

  private final Map<BooleanFormula, Boolean> unsatCache = new HashMap<>();

  /**
   * More complex unsat cache, grouped by an arbitrary key.
   *
   * <p>For each node, map a set of constraints to whether it is unsatisfiable or satisfiable (maps
   * to |true| <=> |UNSAT|). If a set of constraints is satisfiable, any subset of it is also
   * satisfiable. If a set of constraints is unsatisfiable, any superset of it is also
   * unsatisfiable.
   */
  private final Map<Object, Map<Set<BooleanFormula>, Boolean>> groupedUnsatCache = new HashMap<>();

  private final LogManager logger;

  // stats
  public final Timer solverTime = new Timer();
  public int satChecks = 0;
  public int trivialSatChecks = 0;
  public int cachedSatChecks = 0;

  private Solver(Configuration config, LogManager pLogger, ShutdownNotifier shutdownNotifier)
      throws InvalidConfigurationException {
    config.inject(this);

    if (enableLoggingInSolver) {
      logger = pLogger;
    } else {
      logger = LogManager.createNullLogManager();
    }

    SolverContextFactory solverFactory = new SolverContextFactory(config, logger, shutdownNotifier);

    if (solver.equals(interpolationSolver)) {
      // If interpolationSolver is not null, we use SeparateInterpolatingProverEnvironment
      // which copies formula from and to the main solver using string serialization.
      // We don't need this if the solvers are the same anyway.
      interpolationSolver = null;
    }

    solvingContext = solverFactory.generateContext(solver);

    // Instantiate another SMT solver for interpolation if requested.
    if (interpolationSolver != null) {
      interpolatingContext = solverFactory.generateContext(interpolationSolver);
    } else {
      interpolatingContext = solvingContext;
    }

    fmgr = new FormulaManagerView(solvingContext.getFormulaManager(), config, pLogger);
    bfmgr = fmgr.getBooleanFormulaManager();

    if (checkUFs) {
      ufCheckingProverOptions = new UFCheckingProverOptions(config);
    } else {
      ufCheckingProverOptions = null;
    }
  }

  /**
   * Please use {@link #create(Configuration, LogManager, ShutdownNotifier)} in normal code. This
   * constructor is only for test code.
   *
   * <p>Please note that calling {@link #close()} on the returned instance will also close the
   * formula managers created by the passed {@link SolverContextFactory}.
   */
  @VisibleForTesting
  Solver(
      SolverContextFactory pSolverFactory,
      Solvers pSolver,
      SolverContext pContext,
      Configuration pConfig,
      LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    if (solver.equals(interpolationSolver)) {
      // If interpolationSolver is not null, we use SeparateInterpolatingProverEnvironment
      // which copies formula from and to the main solver using string serialization.
      // We don't need this if the solvers are the same anyway.
      interpolationSolver = null;
    }

    checkArgument(solver.equals(pSolver), "mismatching configuration");
    solvingContext = pContext;

    // Instantiate another SMT solver for interpolation if requested.
    if (interpolationSolver != null) {
      interpolatingContext = pSolverFactory.generateContext(interpolationSolver);
    } else {
      interpolatingContext = solvingContext;
    }

    fmgr = new FormulaManagerView(pContext.getFormulaManager(), pConfig, pLogger);
    bfmgr = fmgr.getBooleanFormulaManager();
    logger = pLogger;

    if (checkUFs) {
      ufCheckingProverOptions = new UFCheckingProverOptions(pConfig);
    } else {
      ufCheckingProverOptions = null;
    }
  }

  /**
   * Load and instantiate an SMT solver. The returned instance should be closed by calling {@link
   * #close} when it is not used anymore.
   */
  public static Solver create(
      Configuration config, LogManager logger, ShutdownNotifier shutdownNotifier)
      throws InvalidConfigurationException {
    return new Solver(adjustConfigForSolver(config), logger, shutdownNotifier);
  }

  /** Adjust config by overriding defaults of some JavaSMT options */
  @SuppressWarnings("deprecation")
  public static Configuration adjustConfigForSolver(Configuration config)
      throws InvalidConfigurationException {
    if (!config.hasProperty(SOLVER_OPTION_NON_LINEAR_ARITHMETIC)) {
      // Set a default for solver.nonLinearArithmetic, because with JavaSMT's default CPAchecker
      // would crash with UnsupportedOperationExceptions depending on which solver is used.
      // We could use APPROXIMATE_FALLBACK as default, but this would make comparisons between
      // solvers unfair, and at least small experiments showed no benefit in practice
      // (if non-linear arithmetic is useful, it would be better to use bitvectors than linear
      // approximation anyway).
      return Configuration.builder()
          .copyFrom(config)
          .setOption(SOLVER_OPTION_NON_LINEAR_ARITHMETIC, "APPROXIMATE_ALWAYS")
          .build();
    }
    return config;
  }

  /**
   * Load and instantiate an SMT solver. The returned instance should be closed by calling {@link
   * #close} when it is not used anymore.
   *
   * <p>Important: If possible, always use {@link Solver#create(Configuration, LogManager,
   * ShutdownNotifier)} and refrain from using this method.
   *
   * <p>Important: Refrain from calling this method and instead always try to use {@link
   * Solver#create(Configuration, LogManager, ShutdownNotifier)} first.
   */
  public static Solver create(
      SolverContextFactory pSolverFactory,
      Solvers pSolver,
      SolverContext pSolverContext,
      Configuration pConfig,
      LogManager pLogger)
      throws InvalidConfigurationException {
    return new Solver(pSolverFactory, pSolver, pSolverContext, pConfig, pLogger);
  }

  /**
   * Return the underlying {@link FormulaManagerView} that can be used for creating and manipulating
   * formulas.
   */
  public FormulaManagerView getFormulaManager() {
    return fmgr;
  }

  /**
   * Return the underlying {@link FormulaManagerView} that can be used for creating and manipulating
   * formulas.
   */
  public void printStatistics(PrintStream pOut) {
    if (solvingContext instanceof StatisticsSolverContext) {
      final SolverStatistics stats =
          ((StatisticsSolverContext) solvingContext).getSolverStatistics();
      pOut.println();
      writingStatisticsTo(pOut)
          .put("Statistics about operations", "")
          .beginLevel()
          .put("Number of boolean operations", stats.getNumberOfBooleanOperations())
          .put("Number of numeric operations", stats.getNumberOfNumericOperations())
          .put("Number of FP operations", stats.getNumberOfFPOperations())
          .put("Number of BV operations", stats.getNumberOfBVOperations())
          .put("Number of array operations", stats.getNumberOfArrayOperations())
          .endLevel()
          .put("Statistics about queries", "")
          .beginLevel()
          .put("Number of push queries", stats.getNumberOfPushQueries())
          .put("Number of addConstraint queries", stats.getNumberOfAddConstraintQueries())
          .put("Number of pop queries", stats.getNumberOfPopQueries())
          .put("Number of model queries", stats.getNumberOfModelQueries())
          .put("Number of isUnsat queries", stats.getNumberOfIsUnsatQueries())
          .put(
              "Sum time for isUnsat queries",
              stats.getSumTimeOfIsUnsatQueries().formatAs(TimeUnit.SECONDS))
          .put(
              "Max time for isUnsat queries",
              stats.getMaxTimeOfIsUnsatQueries().formatAs(TimeUnit.SECONDS))
          .put("Number of interpolation queries", stats.getNumberOfInterpolationQueries())
          .put(
              "Sum time for itp queries",
              stats.getSumTimeOfInterpolationQueries().formatAs(TimeUnit.SECONDS))
          .put(
              "Max time for itp queries",
              stats.getMaxTimeOfInterpolationQueries().formatAs(TimeUnit.SECONDS))
          .put("Number of allSat queries", stats.getNumberOfAllSatQueries())
          .put(
              "Sum time for allSat queries",
              stats.getSumTimeOfAllSatQueries().formatAs(TimeUnit.SECONDS))
          .put(
              "Max time for allSat queries",
              stats.getMaxTimeOfAllSatQueries().formatAs(TimeUnit.SECONDS));
    }
  }

  /**
   * Direct reference to the underlying SMT solver for more complicated queries.
   *
   * <p>This creates a fresh, new, environment in the solver. This environment needs to be closed
   * after it is used by calling {@link ProverEnvironment#close()}. It is recommended to use the
   * try-with-resources syntax.
   */
  public ProverEnvironment newProverEnvironment(ProverOptions... options) {
    return newProverEnvironment0(options);
  }

  private ProverEnvironment newProverEnvironment0(ProverOptions... options) {
    ProverEnvironment pe = solvingContext.newProverEnvironment(options);

    if (checkUFs) {
      pe = new UFCheckingProverEnvironment(logger, pe, fmgr, ufCheckingProverOptions);
    }

    pe = new ProverEnvironmentView(pe, fmgr.getFormulaWrappingHandler());

    return pe;
  }

  /**
   * Direct reference to the underlying SMT solver for interpolation queries. This creates a fresh,
   * new, environment in the solver. This environment needs to be closed after it is used by calling
   * {@link InterpolatingProverEnvironment#close()}. It is recommended to use the try-with-resources
   * syntax.
   */
  public InterpolatingProverEnvironment<?> newProverEnvironmentWithInterpolation(
      ProverOptions... options) {
    InterpolatingProverEnvironment<?> ipe =
        interpolatingContext.newProverEnvironmentWithInterpolation(options);

    if (solvingContext != interpolatingContext) {
      // If interpolatingContext is not the normal solver,
      // we use SeparateInterpolatingProverEnvironment
      // which copies formula back and forth using strings.
      // We don't need this if the solvers are the same anyway.
      ipe =
          new SeparateInterpolatingProverEnvironment<>(
              solvingContext.getFormulaManager(), interpolatingContext.getFormulaManager(), ipe);
    }

    if (checkUFs) {
      ipe =
          new UFCheckingInterpolatingProverEnvironment<>(
              logger, ipe, fmgr, ufCheckingProverOptions);
    }

    ipe = new InterpolatingProverEnvironmentView<>(ipe, fmgr.getFormulaWrappingHandler());

    return ipe;
  }

  /**
   * Direct reference to the underlying SMT solver for optimization queries. This creates a fresh,
   * new, environment in the solver. This environment needs to be closed after it is used by calling
   * {@link OptimizationProverEnvironment#close()}. It is recommended to use the try-with-resources
   * syntax.
   */
  public OptimizationProverEnvironment newOptEnvironment() {
    OptimizationProverEnvironment environment =
        solvingContext.newOptimizationProverEnvironment(ProverOptions.GENERATE_MODELS);
    environment = new OptimizationProverEnvironmentView(environment, fmgr);
    return environment;
  }

  /** Checks whether a formula is unsat. */
  public boolean isUnsat(BooleanFormula f) throws SolverException, InterruptedException {
    satChecks++;

    if (bfmgr.isTrue(f)) {
      trivialSatChecks++;
      return false;
    }
    if (bfmgr.isFalse(f)) {
      trivialSatChecks++;
      return true;
    }
    Boolean result = unsatCache.get(f);
    if (result != null) {
      cachedSatChecks++;
      return result;
    }

    solverTime.start();
    try {
      result = isUnsatUncached(f);

      unsatCache.put(f, result);
      return result;

    } finally {
      solverTime.stop();
    }
  }

  /**
   * Unsatisfiability check with more complex cache look up, optionally based on unsat core.
   *
   * @param constraints Conjunction of formulas to test for satisfiability
   * @param cacheKey Key to group cached results under, usually CFANode is a good candidate.
   * @return Whether {@code f} is unsatisfiable.
   */
  public boolean isUnsat(Set<BooleanFormula> constraints, Object cacheKey)
      throws InterruptedException, SolverException {
    solverTime.start();
    try {
      return isUnsat0(constraints, cacheKey);
    } finally {
      solverTime.stop();
    }
  }

  private boolean isUnsat0(Set<BooleanFormula> lemmas, Object cacheKey)
      throws InterruptedException, SolverException {
    satChecks++;

    Map<Set<BooleanFormula>, Boolean> stored = groupedUnsatCache.get(cacheKey);
    if (stored != null) {
      for (Entry<Set<BooleanFormula>, Boolean> isUnsatResults : stored.entrySet()) {
        Set<BooleanFormula> cachedConstraints = isUnsatResults.getKey();
        boolean cachedIsUnsat = isUnsatResults.getValue();

        if (cachedIsUnsat && lemmas.containsAll(cachedConstraints)) {

          // Any superset of unreachable constraints is unreachable.
          cachedSatChecks++;
          return true;
        } else if (!cachedIsUnsat && cachedConstraints.containsAll(lemmas)) {

          // Any subset of reachable constraints is reachable.
          cachedSatChecks++;
          return false;
        }
      }
    }

    if (stored == null) {
      stored = new HashMap<>();
    } else {
      stored = new HashMap<>(stored);
    }

    ProverOptions[] opts;
    if (cacheUnsatCores) {
      opts = new ProverOptions[] {GENERATE_UNSAT_CORE};
    } else {
      opts = new ProverOptions[0];
    }

    try (ProverEnvironment pe = newProverEnvironment(opts)) {
      pe.push();
      for (BooleanFormula lemma : lemmas) {
        pe.addConstraint(lemma);
      }
      if (pe.isUnsat()) {
        if (cacheUnsatCores) {
          stored.put(ImmutableSet.copyOf(pe.getUnsatCore()), true);
        } else {
          stored.put(ImmutableSet.copyOf(lemmas), true);
        }
        return true;
      } else {
        stored.put(lemmas, false);
        return false;
      }
    } finally {
      groupedUnsatCache.put(cacheKey, ImmutableMap.copyOf(stored));
    }
  }

  /**
   * Helper function for UNSAT core generation. Takes a single API call to perform.
   *
   * <p>Additionally, tries to give a "better" UNSAT core, by breaking up AND- nodes into multiple
   * constraints (thus an UNSAT core can contain only a subset of some AND node).
   */
  public List<BooleanFormula> unsatCore(BooleanFormula constraints)
      throws SolverException, InterruptedException {

    return unsatCore(bfmgr.toConjunctionArgs(constraints, true));
  }

  public List<BooleanFormula> unsatCore(Set<BooleanFormula> constraints)
      throws SolverException, InterruptedException {

    try (ProverEnvironment prover = newProverEnvironment(GENERATE_UNSAT_CORE)) {
      for (BooleanFormula constraint : constraints) {
        prover.addConstraint(constraint);
      }
      Verify.verify(prover.isUnsat());
      return prover.getUnsatCore();
    }
  }

  private boolean isUnsatUncached(BooleanFormula f) throws SolverException, InterruptedException {
    try (ProverEnvironment prover = newProverEnvironment()) {
      prover.push(f);
      return prover.isUnsat();
    }
  }

  /** Checks whether a => b. The result is cached. */
  public boolean implies(BooleanFormula a, BooleanFormula b)
      throws SolverException, InterruptedException {
    if (bfmgr.isFalse(a) || bfmgr.isTrue(b)) {
      satChecks++;
      trivialSatChecks++;
      return true;
    }
    if (a.equals(b)) {
      satChecks++;
      trivialSatChecks++;
      return true;
    }

    BooleanFormula f = bfmgr.not(bfmgr.implication(a, b));

    return isUnsat(f);
  }

  /**
   * Close this solver instance and all underlying formula managers. This instance and any instance
   * retrieved from it (including all {@link Formula}s) may not be used anymore after closing.
   */
  @Override
  public void close() {
    // Reliably close both formula managers and re-throw exceptions,
    // such that no exception gets lost and both managers get closed.
    // Taken from https://stackoverflow.com/questions/24705055/wrapping-multiple-autocloseables
    // Guava has Closer, but it does not yet support AutoCloseables.
    Throwable t = null;
    try {
      solvingContext.close();
    } catch (Throwable t1) {
      t = t1;
      throw t1;
    } finally {
      if (solvingContext != interpolatingContext) {

        if (t != null) {
          try {
            interpolatingContext.close();
          } catch (Throwable t2) {
            t.addSuppressed(t2);
          }
        } else {
          interpolatingContext.close();
        }
      }
    }
  }

  public String getVersion() {
    return solvingContext.getVersion();
  }

  public String getInterpolatingVersion() {
    return interpolatingContext.getVersion();
  }
}
