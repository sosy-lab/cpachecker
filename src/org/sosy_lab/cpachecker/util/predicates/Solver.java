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
package org.sosy_lab.cpachecker.util.predicates;

import java.util.Map;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.OptEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.OptEnvironmentView;
import org.sosy_lab.cpachecker.util.predicates.logging.LoggingInterpolatingProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.logging.LoggingOptEnvironment;
import org.sosy_lab.cpachecker.util.predicates.logging.LoggingProverEnvironment;

import com.google.common.collect.Maps;

/**
 * Abstraction of an SMT solver that also provides some higher-level methods.
 */
@Options(prefix="cpa.predicate")
public final class Solver {

  @Option(secure=true, name="solver.useLogger",
      description="log some solver actions, this may be slow!")
  private boolean useLogger = false;

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final FormulaManagerFactory factory;

  private final Map<BooleanFormula, Boolean> unsatCache = Maps.newHashMap();

  private final LogManager logger;

  // stats
  public final Timer solverTime = new Timer();
  public int satChecks = 0;
  public int trivialSatChecks = 0;
  public int cachedSatChecks = 0;

  public Solver(FormulaManagerView pFmgr, FormulaManagerFactory pFactory,
      Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    config.inject(this);
    fmgr = pFmgr;
    bfmgr = fmgr.getBooleanFormulaManager();
    factory = pFactory;
    logger = pLogger;
  }

  /**
   * Direct reference to the underlying SMT solver for more complicated queries.
   * This creates a fresh, new, environment in the solver.
   * This environment needs to be closed after it is used by calling {@link ProverEnvironment#close()}.
   * It is recommended to use the try-with-resources syntax.
   */
  public ProverEnvironment newProverEnvironment() {
    return newProverEnvironment(false, false);
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
    return newProverEnvironment(true, false);
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
    return newProverEnvironment(false, true);
  }

  private ProverEnvironment newProverEnvironment(boolean generateModels, boolean generateUnsatCore) {
    ProverEnvironment pe = factory.getFormulaManager().newProverEnvironment(generateModels, generateUnsatCore);

    if (useLogger) {
      return new LoggingProverEnvironment(logger, pe);
    } else {
      return pe;
    }
  }

  /**
   * Direct reference to the underlying SMT solver for interpolation queries.
   * This creates a fresh, new, environment in the solver.
   * This environment needs to be closed after it is used by calling {@link InterpolatingProverEnvironment#close()}.
   * It is recommended to use the try-with-resources syntax.
   */
  public InterpolatingProverEnvironment<?> newProverEnvironmentWithInterpolation() {
    InterpolatingProverEnvironment<?> ipe = factory.newProverEnvironmentWithInterpolation(false);

    if (useLogger) {
      return new LoggingInterpolatingProverEnvironment<>(logger, ipe);
    } else {
      return ipe;
    }
  }

  /**
   * Direct reference to the underlying SMT solver for optimization queries.
   * This creates a fresh, new, environment in the solver.
   * This environment needs to be closed after it is used by calling {@link OptEnvironment#close()}.
   * It is recommended to use the try-with-resources syntax.
   */
  public OptEnvironment newOptEnvironment() {
    OptEnvironment environment = factory.getFormulaManager().newOptEnvironment();
    environment = new OptEnvironmentView(environment, fmgr);

    if (useLogger) {
      return new LoggingOptEnvironment(logger, environment);
    } else {
      return environment;
    }
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
