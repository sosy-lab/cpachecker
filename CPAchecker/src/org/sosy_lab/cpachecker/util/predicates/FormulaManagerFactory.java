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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sosy_lab.common.ChildFirstPatternClassLoader;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathCounterTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.princess.PrincessFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.z3.Z3FormulaManager;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;

/**
 * Factory class for loading and instantiating SMT solvers.
 * Most code should not access {@link FormulaManager} instances directly
 * and instead use the class {@link Solver} and the classes from
 * the {@link org.sosy_lab.cpachecker.util.predicates.interfaces.view} package
 * (most notably {@link FormulaManagerView}).
 * The preferred way to instantiate all of this is
 * {@link Solver#create(Configuration, LogManager, ShutdownNotifier)}.
 */
@Options(deprecatedPrefix="cpa.predicate",
         prefix="solver")
public class FormulaManagerFactory {

  @VisibleForTesting
  public static enum Solvers {
    MATHSAT5,
    SMTINTERPOL,
    Z3,
    PRINCESS
  }

  @Option(secure=true,
      description = "Export solver queries in Smtlib format into a file.")
  private boolean logAllQueries = false;

  @Option(secure=true,
      description = "Export solver queries in Smtlib format into a file.")
  @FileOption(Type.OUTPUT_FILE)
  private PathCounterTemplate logfile = PathCounterTemplate.ofFormatString("smtquery.%03d.smt2");

  @Option(secure=true, description = "Random seed for SMT solver.")
  private long randomSeed = 42;

  @Option(secure=true, description="Which SMT solver to use.")
  private Solvers solver = Solvers.SMTINTERPOL;

  @Option(secure=true, description="Which solver to use specifically for interpolation (default is to use the main one).")
  private Solvers interpolationSolver = null;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  private final FormulaManager fmgr;
  private final FormulaManager itpFmgr;

  private volatile SolverFactory smtInterpolFactory = null;

  @VisibleForTesting
  public FormulaManagerFactory(Configuration config, LogManager pLogger,
      ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;
    shutdownNotifier = checkNotNull(pShutdownNotifier);

    if (!logAllQueries) {
      logfile = null;
    }

    if (solver.equals(interpolationSolver)) {
      // If interpolationSolver is not null, we use SeparateInterpolatingProverEnvironment
      // which copies formula back and forth using strings.
      // We don't need this if the solvers are the same anyway.
      interpolationSolver = null;
    }

    fmgr = instantiateSolver(solver, config);
    GlobalInfo.getInstance().storeFormulaManager(fmgr);

    // Instantiate another SMT solver for interpolation if requested.
    if (interpolationSolver != null) {
      itpFmgr = instantiateSolver(interpolationSolver, config);
    } else {
      itpFmgr = fmgr;
    }
  }

  private FormulaManager instantiateSolver(Solvers solver, Configuration config)
      throws InvalidConfigurationException {
    try {
      switch (solver) {
      case SMTINTERPOL:
        return loadSmtInterpol().create(config, logger, shutdownNotifier, logfile, randomSeed);

      case MATHSAT5:
          return Mathsat5FormulaManager.create(logger, config, shutdownNotifier, logfile, randomSeed);

      case Z3:
        return Z3FormulaManager.create(logger, config, shutdownNotifier, logfile, randomSeed);

      case PRINCESS:
        // TODO: pass randomSeed to Princess
        return PrincessFormulaManager.create(config, logger, shutdownNotifier, logfile);

      default:
        throw new AssertionError("no solver selected");
      }

    } catch (UnsatisfiedLinkError e) {
      throw new InvalidConfigurationException("The SMT solver " + solver
          + " is not available on this machine because of missing libraries"
          + " (" + e.getMessage() + ")."
          + " You may experiment with SMTInterpol by setting solver.solver=SMTInterpol.", e);
    }
  }

  public FormulaManager getFormulaManager() {
    return fmgr;
  }

  public FormulaManager getFormulaManagerForInterpolation() {
    return itpFmgr;
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
        ShutdownNotifier pShutdownNotifier,
        @Nullable PathCounterTemplate solverLogfile, long randomSeed) throws InvalidConfigurationException;
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
            public boolean apply(@Nonnull URL pInput) {
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
