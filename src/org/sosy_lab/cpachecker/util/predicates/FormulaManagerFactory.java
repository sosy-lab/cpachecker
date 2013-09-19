/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.net.URLClassLoader;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.sosy_lab.common.ChildFirstPatternClassLoader;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interpolation.SeparateInterpolatingProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5InterpolatingProver;
import org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5TheoremProver;

@Options(prefix="cpa.predicate")
public class FormulaManagerFactory {

  private static final String MATHSAT5 = "MATHSAT5";
  private static final String SMTINTERPOL = "SMTINTERPOL";

  @Option(name="solver.useIntegers",
      description="Encode program variables as INTEGER variables, instead of "
      + "using REALs. Not all solvers might support this.")
  private boolean useIntegers = false;

  @Option(values={MATHSAT5, SMTINTERPOL}, toUppercase=true,
      description="Whether to use MathSAT 5 or SmtInterpol as SMT solver")
  private String solver = MATHSAT5;

  @Option(values={MATHSAT5, SMTINTERPOL}, toUppercase=true,
      description="Which solver to use specifically for interpolation (default is to use the main one).")
  private String interpolationSolver = null;

  private final LogManager logger;

  private final FormulaManager fmgr;
  private final FormulaManager itpFmgr;

  private volatile SolverFactory smtInterpolFactory = null;

  public FormulaManagerFactory(Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;

    if (solver.equals(interpolationSolver)) {
      // If interpolationSolver is not null, we use SeparateInterpolatingProverEnvironment
      // which copies formula back and forth using strings.
      // We don't need this if the solvers are the same anyway.
      interpolationSolver = null;
    }

    FormulaManager lFmgr;
    if (solver.equals(SMTINTERPOL)) {
      lFmgr = loadSmtInterpol().create(config, logger, useIntegers);

    } else {
      try {
        assert solver.equals(MATHSAT5);

        lFmgr = Mathsat5FormulaManager.create(logger, config, useIntegers);

      } catch (UnsatisfiedLinkError e) {
        throw new InvalidConfigurationException("The SMT solver " + solver
            + " is not available on this machine."
            + " You may experiment with SMTInterpol by setting cpa.predicate.solver=SMTInterpol.", e);
      }
    }

    fmgr = lFmgr;

    // Instantiate another SMT solver for interpolation if requested.
    if (interpolationSolver != null) {
      if (interpolationSolver.equals(SMTINTERPOL)) {
        itpFmgr = loadSmtInterpol().create(config, logger, useIntegers);
      } else {
        assert interpolationSolver.equals(MATHSAT5);
        itpFmgr = Mathsat5FormulaManager.create(logger, config, useIntegers);
      }
    } else {
      itpFmgr = null;
    }
  }

  public FormulaManager getFormulaManager() {
    return fmgr;
  }

  public ProverEnvironment newProverEnvironment(boolean generateModels) {
    if (solver.equals(SMTINTERPOL)) {
      return loadSmtInterpol().createProver(fmgr);
    } else {
      return new Mathsat5TheoremProver((Mathsat5FormulaManager)fmgr, generateModels);
    }
  }

  public InterpolatingProverEnvironment<?> newProverEnvironmentWithInterpolation(boolean shared) {
    if (interpolationSolver != null) {
      InterpolatingProverEnvironment<?> env = newProverEnvironmentWithInterpolation(interpolationSolver, itpFmgr, shared);
      return new SeparateInterpolatingProverEnvironment<>(fmgr, itpFmgr, env);
    }

    return newProverEnvironmentWithInterpolation(solver, fmgr, shared);
  }

  private InterpolatingProverEnvironment<?> newProverEnvironmentWithInterpolation(
          String solver, FormulaManager fmgr, boolean shared) {

    if (solver.equals(SMTINTERPOL)) {
      return loadSmtInterpol().createInterpolatingProver(fmgr);
    } else {
      assert solver.equals(MATHSAT5);
      return new Mathsat5InterpolatingProver((Mathsat5FormulaManager) fmgr, shared);
    }
  }

  /**
   * Interface for completely encapsulating all accesses to a solver's package
   * to discouple the solver's package from the rest of CPAchecker.
   *
   * This interface is only meant to be implemented by SMT solvers
   * and used by this class, not by other classes.
   */
  public static interface SolverFactory {
    FormulaManager create(Configuration config, LogManager logger, boolean useIntegers) throws InvalidConfigurationException;

    ProverEnvironment createProver(FormulaManager mgr);

    InterpolatingProverEnvironment<?> createInterpolatingProver(FormulaManager mgr);
  }

  // ------------------------- SmtInterpol -------------------------
  // For SmtInterpol we need a separate class loader
  // because it needs it's own (modified) version of the Java CUP runtime
  // and we already have the normal (unmodified) version of Java CUP
  // on the class path of the normal class loader.

  private static final Pattern SMTINTERPOL_CLASSES = Pattern.compile("^("
      + "org\\.sosy_lab\\.cpachecker\\.util\\.predicates\\.smtInterpol|"
      + "de\\.uni_freiburg\\.informatik\\.ultimate|"
      + "java_cup\\.runtime|"
      + "org\\.apache\\.log4j"
      + ")\\..*");
  private static final String SMTINTERPOL_FACTORY_CLASS = "org.sosy_lab.cpachecker.util.predicates.smtInterpol.SmtInterpolSolverFactory";

  // We keep the class loader for SmtInterpol around
  // in case someone creates a seconds instance of FormulaManagerFactory
  private static WeakReference<ClassLoader> smtInterpolClassLoader = new WeakReference<>(null);
  private static final AtomicInteger smtInterpolLoadingCount = new AtomicInteger(0);

  private SolverFactory loadSmtInterpol() {
    // Double-checked locking is used here, be careful when changing something.
    SolverFactory result = smtInterpolFactory;

    if (result == null) {
      synchronized (this) {
        result = smtInterpolFactory;
        if (result == null) {
          try {
            ClassLoader classLoader = getClassLoader(logger);

            @SuppressWarnings("unchecked")
            Class<? extends SolverFactory> factoryClass = (Class<? extends SolverFactory>) classLoader.loadClass(SMTINTERPOL_FACTORY_CLASS);
            Constructor<? extends SolverFactory> factoryConstructor = factoryClass.getConstructor(new Class<?>[0]);
            smtInterpolFactory = result = factoryConstructor.newInstance();
          } catch (ReflectiveOperationException e) {
            throw new Classes.UnexpectedCheckedException("Failed to load SmtInterpol", e);
          }
        }
      }
    }

    return result;
  }

  private static ClassLoader getClassLoader(LogManager logger) {
    ClassLoader classLoader = smtInterpolClassLoader.get();
    if (classLoader != null) {
      return classLoader;
    }

    // garbage collected or first time we come here
    if (smtInterpolLoadingCount.incrementAndGet() > 1) {
      logger.log(Level.INFO, "Repeated loading of SmtInterpol");
    }

    classLoader = FormulaManagerFactory.class.getClassLoader();
    if (classLoader instanceof URLClassLoader) {
      classLoader = new ChildFirstPatternClassLoader(SMTINTERPOL_CLASSES,
          ((URLClassLoader)classLoader).getURLs(), classLoader);
    }
    smtInterpolClassLoader = new WeakReference<>(classLoader);
    return classLoader;
  }
}
