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
package org.sosy_lab.cpachecker.util.predicates.smt;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.util.predicates.interpolation.SeparateInterpolatingProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView.ConjunctionSplitter;
import org.sosy_lab.cpachecker.util.predicates.ufCheckingProver.UFCheckingBasicProverEnvironment.UFCheckingProverOptions;
import org.sosy_lab.cpachecker.util.predicates.ufCheckingProver.UFCheckingInterpolatingProverEnvironmentWithAssumptions;
import org.sosy_lab.cpachecker.util.predicates.ufCheckingProver.UFCheckingProverEnvironment;
import org.sosy_lab.solver.SolverContextFactory;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FormulaManager;
import org.sosy_lab.solver.api.InterpolatingProverEnvironment;
import org.sosy_lab.solver.api.InterpolatingProverEnvironmentWithAssumptions;
import org.sosy_lab.solver.api.OptEnvironment;
import org.sosy_lab.solver.api.ProverEnvironment;
import org.sosy_lab.solver.api.SolverContext;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import com.google.common.base.Verify;
import com.google.common.collect.Maps;

/**
 * Encapsulation of an SMT solver.
 * This class is the central entry point to everything related to an SMT solver:
 * formula creation and manipulation (via the {@link #getFormulaManager()} method),
 * and checking for satisfiability (via the remaining methods).
 * In addition to the low-level methods provided by {@link FormulaManager},
 * this class and {@link FormulaManagerView} provide additional higher-level utility methods,
 * and additional features such as
 * replacing one SMT theory transparently with another,
 * or using different SMT solvers for different tasks such as solving and interpolation.
 */
@Options(deprecatedPrefix="cpa.predicate.solver", prefix="solver")
public final class Solver implements AutoCloseable {

  @Option(secure=true, name="checkUFs",
      description="improve sat-checks with additional constraints for UFs")
  private boolean checkUFs = false;

  private final UFCheckingProverOptions ufCheckingProverOptions;

  private final Supplier<ShutdownNotifier> notifierSupplier;

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;

  private final SolverContext solvingContext;
  private final SolverContext interpolatingContext;

  private final Map<BooleanFormula, Boolean> unsatCache = Maps.newHashMap();

  private final LogManager logger;

  // stats
  public final Timer solverTime = new Timer();
  public int satChecks = 0;
  public int trivialSatChecks = 0;
  public int cachedSatChecks = 0;

  /**
   * Please use {@link #create(Configuration, LogManager, ShutdownNotifier)} in normal code.
   * This constructor is primarily for test code.
   *
   * Please note that calling {@link #close()} on the returned instance
   * will also close the formula managers created by the passed {@link SolverContextFactory}.
   */
  @VisibleForTesting
  public Solver(SolverContextFactory pSolverFactory,
      Configuration config, LogManager pLogger,
      Supplier<ShutdownNotifier> pNotifierSupplier) throws InvalidConfigurationException {

    config.inject(this);

    notifierSupplier = pNotifierSupplier;
    solvingContext = pSolverFactory.getSolverContext();
    interpolatingContext = pSolverFactory.getSolverContextForInterpolation();
    fmgr = new FormulaManagerView(solvingContext.getFormulaManager(),
        config,
        pLogger
    );
    bfmgr = fmgr.getBooleanFormulaManager();
    logger = pLogger;

    if (checkUFs) {
      ufCheckingProverOptions = new UFCheckingProverOptions(config);
    } else {
      ufCheckingProverOptions = null;
    }
  }

  /**
   * Load and instantiate an SMT solver.
   * The returned instance should be closed by calling {@link #close}
   * when it is not used anymore.
   */
  public static Solver create(Configuration pConfig, LogManager pLogger,
      Supplier<ShutdownNotifier> pNotifierSupplier)
          throws InvalidConfigurationException {

    SolverContextFactory factory = new SolverContextFactory(pConfig, pLogger, pNotifierSupplier.get());
    return new Solver(factory, pConfig, pLogger, pNotifierSupplier);
  }

  public static Solver create(Configuration config, LogManager logger,
      final ShutdownNotifier pShutdownNotifier)
          throws InvalidConfigurationException {

    return create(config, logger, new Supplier<ShutdownNotifier>() {
      @Override
      public ShutdownNotifier get() {
        return pShutdownNotifier;
      }
    });
  }

  /**
   * Return the underlying {@link FormulaManagerView}
   * that can be used for creating and manipulating formulas.
   */
  public FormulaManagerView getFormulaManager() {
    return fmgr;
  }

  /**
   * Direct reference to the underlying SMT solver for more complicated queries.
   * This creates a fresh, new, environment in the solver.
   * This environment needs to be closed after it is used by calling {@link ProverEnvironment#close()}.
   * It is recommended to use the try-with-resources syntax.
   */
  public ProverEnvironment newProverEnvironment() {
    return newProverEnvironment(false, false, notifierSupplier.get());
  }

  /**
   * Direct reference to the underlying SMT solver for more complicated queries.
   * This creates a fresh, new, environment in the solver.
   * This environment needs to be closed after it is used by calling {@link ProverEnvironment#close()}.
   * It is recommended to use the try-with-resources syntax.
   *
   * The solver is told to enable model generation.
   */
  public ProverEnvironment newProverEnvironmentWithModelGeneration() {
    return newProverEnvironment(true, false, notifierSupplier.get());
  }

  /**
   * Direct reference to the underlying SMT solver for more complicated queries.
   * This creates a fresh, new, environment in the solver.
   * This environment needs to be closed after it is used by calling {@link ProverEnvironment#close()}.
   * It is recommended to use the try-with-resources syntax.
   *
   * The solver is told to enable unsat-core generation.
   */
  public ProverEnvironment newProverEnvironmentWithUnsatCoreGeneration() {
    return newProverEnvironment(false, true, notifierSupplier.get());
  }

  private ProverEnvironment newProverEnvironment(boolean generateModels, boolean generateUnsatCore,
      ShutdownNotifier pNotifier) {

    ProverEnvironment pe = solvingContext
        .newProverEnvironment(pNotifier, generateModels, generateUnsatCore);

    if (checkUFs) {
      pe = new UFCheckingProverEnvironment(logger, pe, fmgr, ufCheckingProverOptions);
    }

    return pe;
  }

  /**
   * Direct reference to the underlying SMT solver for interpolation queries.
   * This creates a fresh, new, environment in the solver.
   * This environment needs to be closed after it is used by calling {@link InterpolatingProverEnvironment#close()}.
   * It is recommended to use the try-with-resources syntax.
   */
  public InterpolatingProverEnvironmentWithAssumptions<?> newProverEnvironmentWithInterpolation() {
    InterpolatingProverEnvironment<?> ipe = interpolatingContext.newProverEnvironmentWithInterpolation();

    InterpolatingProverEnvironmentWithAssumptions<?> ipeA = (InterpolatingProverEnvironmentWithAssumptions<?>) ipe;

    if (solvingContext != interpolatingContext) {
      // If interpolatingContext is not the normal solver,
      // we use SeparateInterpolatingProverEnvironment
      // which copies formula back and forth using strings.
      // We don't need this if the solvers are the same anyway.
      ipeA = new SeparateInterpolatingProverEnvironment<>(
          solvingContext.getFormulaManager(),
          interpolatingContext.getFormulaManager(),
          ipeA
      );
    }

    if (checkUFs) {
      ipeA = new UFCheckingInterpolatingProverEnvironmentWithAssumptions<>(logger, ipeA, fmgr, ufCheckingProverOptions);
    }

    return ipeA;
  }

  /**
   * Direct reference to the underlying SMT solver for optimization queries.
   * This creates a fresh, new, environment in the solver.
   * This environment needs to be closed after it is used by calling {@link OptEnvironment#close()}.
   * It is recommended to use the try-with-resources syntax.
   */
  public OptEnvironment newOptEnvironment() {
    OptEnvironment environment = solvingContext.newOptEnvironment();
    environment = new OptEnvironmentView(environment, fmgr);
    return environment;
  }

  /**
   * Checks whether a formula is unsat.
   */
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
   * Helper function for UNSAT core generation.
   * Takes a single API call to perform.
   *
   * <p>Additionally, tries to give a "better" UNSAT core, by breaking up AND-
   * nodes into multiple constraints (thus an UNSAT core can contain only a
   * subset of some AND node).
   */
  public List<BooleanFormula> unsatCore(Iterable<BooleanFormula> constraints)
      throws SolverException, InterruptedException {

    try (ProverEnvironment prover = newProverEnvironmentWithUnsatCoreGeneration()) {
      for (BooleanFormula constraint : constraints) {
        addConstraint(constraint);
      }
      Verify.verify(prover.isUnsat());
      return prover.getUnsatCore();
    }
  }

  /**
   * Helper function: add the constraint, OR, if the constraint is an AND-node,
   * add children one by one. Keep going recursively.
   */
  private void addConstraint(BooleanFormula constraint) {

    List<BooleanFormula> splittedConjunction = bfmgr.visit(new ConjunctionSplitter(fmgr), constraint);
    if (splittedConjunction == null) {
      // in this case, we could not split the constraint and use it "as is".
      splittedConjunction = Collections.singletonList(constraint);
    }

    for (BooleanFormula f : splittedConjunction) {
      addConstraint(f);
    }
  }

  private boolean isUnsatUncached(BooleanFormula f) throws SolverException, InterruptedException {
    try (ProverEnvironment prover = newProverEnvironment()) {
      prover.push(f);
      return prover.isUnsat();
    }
  }

  /**
   * Checks whether a => b.
   * The result is cached.
   */
  public boolean implies(BooleanFormula a, BooleanFormula b) throws SolverException, InterruptedException {
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
   * Close this solver instance and all underlying formula managers.
   * This instance and any instance retrieved from it (including all {@link Formula}s)
   * may not be used anymore after closing.
   */
  @Override
  public void close() throws Exception {
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

  /**
   * Populate the cache for unsatisfiability queries with a formula
   * that is known to be unsat.
   * @param unsat An unsatisfiable formula.
   */
  public void addUnsatisfiableFormulaToCache(BooleanFormula unsat) {
    if (unsatCache.containsKey(unsat) || bfmgr.isFalse(unsat)) {
      return;
    }
    try {
      assert isUnsatUncached(unsat) : "formula is sat: " + unsat;
    } catch (SolverException e) {
      throw new AssertionError(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    unsatCache.put(unsat, true);
  }
}
