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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
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
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.util.NativeLibraries;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interpolation.SeparateInterpolatingProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.logging.LoggingInterpolatingProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.logging.LoggingProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5InterpolatingProver;
import org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5TheoremProver;
import org.sosy_lab.cpachecker.util.predicates.z3.Z3FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.z3.Z3InterpolatingProver;
import org.sosy_lab.cpachecker.util.predicates.z3.Z3TheoremProver;

import com.google.common.base.Predicate;

@Options(prefix="cpa.predicate")
public class FormulaManagerFactory {

  private static enum Solvers {
    MATHSAT5,
    SMTINTERPOL,
    Z3,
    ;
  }

  @Option(name="solver.useLogger",
      description="log some solver actions, this may be slow!")
  private boolean useLogger = false;

  @Option(description="Whether to use MathSAT 5, SmtInterpol or Z3 as SMT solver (Z3 needs the FOCI library from http://www.kenmcmil.com/foci2/).")
  private Solvers solver = Solvers.MATHSAT5;

  @Option(description="Which solver to use specifically for interpolation (default is to use the main one).")
  private Solvers interpolationSolver = null;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  private final FormulaManager fmgr;
  private final FormulaManager itpFmgr;

  private volatile SolverFactory smtInterpolFactory = null;

  public FormulaManagerFactory(Configuration config, LogManager pLogger,
      ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;
    shutdownNotifier = checkNotNull(pShutdownNotifier);

    if (solver.equals(interpolationSolver)) {
      // If interpolationSolver is not null, we use SeparateInterpolatingProverEnvironment
      // which copies formula back and forth using strings.
      // We don't need this if the solvers are the same anyway.
      interpolationSolver = null;
    }

    fmgr = instantiateSolver(solver, config);

    // Instantiate another SMT solver for interpolation if requested.
    if (interpolationSolver != null) {
      itpFmgr = instantiateSolver(interpolationSolver, config);
    } else {
      itpFmgr = null;
    }
  }

  private FormulaManager instantiateSolver(Solvers solver, Configuration config)
      throws InvalidConfigurationException {
    try {
      switch (solver) {
      case SMTINTERPOL:
        return loadSmtInterpol().create(config, logger, shutdownNotifier);

      case MATHSAT5:
          return Mathsat5FormulaManager.create(logger, config, shutdownNotifier);

      case Z3:
        try {
          return Z3FormulaManager.create(logger, config);
        } catch (UnsatisfiedLinkError e) {
          if (e.getMessage().contains("libfoci.so")) {
            throw new InvalidConfigurationException("Z3 needs the FOCI library which is not supplied with CPAchecker."
                + " Please download it from http://www.kenmcmil.com/foci2/ for your architecture"
                + " and put it into " + NativeLibraries.getNativeLibraryPath() + "/.", e);
          } else {
            throw e;
          }
        }

      default:
        throw new AssertionError("no solver selected");
      }

    } catch (UnsatisfiedLinkError e) {
      throw new InvalidConfigurationException("The SMT solver " + solver
          + " is not available on this machine because of missing libraries"
          + " (" + e.getMessage() + ")."
          + " You may experiment with SMTInterpol by setting cpa.predicate.solver=SMTInterpol.", e);
    }
  }

  public FormulaManager getFormulaManager() {
    return fmgr;
  }

  public ProverEnvironment newProverEnvironment(boolean generateModels) {
    ProverEnvironment pe;
    switch (solver) {
    case SMTINTERPOL:
      pe = loadSmtInterpol().createProver(fmgr);
      break;
    case MATHSAT5:
      pe = new Mathsat5TheoremProver((Mathsat5FormulaManager) fmgr, generateModels);
      break;
    case Z3:
      pe = new Z3TheoremProver((Z3FormulaManager) fmgr);
      break;
    default:
      throw new AssertionError("no solver selected");
    }

    if (useLogger) {
      return new LoggingProverEnvironment(logger, pe);
    } else {
      return pe;
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
          Solvers solver, FormulaManager fmgr, boolean shared) {

    InterpolatingProverEnvironment<?> ipe;
    switch (solver) {
    case SMTINTERPOL:
      ipe = loadSmtInterpol().createInterpolatingProver(fmgr);
      break;
    case MATHSAT5:
      ipe = new Mathsat5InterpolatingProver((Mathsat5FormulaManager) fmgr, shared);
      break;
    case Z3:
      ipe = new Z3InterpolatingProver((Z3FormulaManager) fmgr);
      break;
    default:
      throw new AssertionError("no solver selected");
    }

    if (useLogger) {
      return new LoggingInterpolatingProverEnvironment<>(logger, ipe);
    } else {
      return ipe;
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
    FormulaManager create(Configuration config, LogManager logger,
        ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException;

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

      // Filter out java-cup-runtime.jar from the class path,
      // so that the class loader for SmtInterpol loads the Java CUP classes
      // from SmtInterpol's JAR file.
      URL[] urls = from(Arrays.asList(((URLClassLoader)classLoader).getURLs()))
        .filter(new Predicate<URL>() {
            @Override
            public boolean apply(URL pInput) {
              return !pInput.getPath().contains("java-cup");
            }
          })
        .toArray(URL.class);

      classLoader = new ChildFirstPatternClassLoader(SMTINTERPOL_CLASSES, urls,
          classLoader);
    }
    smtInterpolClassLoader = new WeakReference<>(classLoader);
    return classLoader;
  }
}
