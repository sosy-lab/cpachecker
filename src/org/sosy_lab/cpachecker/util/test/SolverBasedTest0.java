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
package org.sosy_lab.cpachecker.util.test;

import static com.google.common.truth.TruthJUnit.assume;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.junit.After;
import org.junit.Before;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.Builder;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.converters.FileTypeConverter;
import org.sosy_lab.common.log.TestLogManager;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory.Solvers;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ArrayFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FunctionFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.QuantifiedFormulaManager;

import com.google.common.truth.FailureStrategy;
import com.google.common.truth.SubjectFactory;

/**
 * Abstract base class with helpful utilities for writing tests
 * that use an SMT solver.
 * It instantiates and closes the SMT solver before and after each test,
 * and provides fields with direct access to the most relevant instances.
 *
 * To run the tests using all available solvers, add the following code to your class:
 * <code>
 * @Parameters(name="{0}")
 *  public static List<Object[]> getAllSolvers() {
 *    return allSolversAsParameters();
 *  }
 *
 *  @Parameter(0)
 *  public Solvers solver;
 *
 *  @Override
 *  protected Solvers solverToUse() {
 *    return solver;
 *  }
 * </code>
 *
 * {@link #BooleanFormula()} can be used to easily write assertions
 * about formulas using Truth.
 *
 * Test that rely on a theory that not all solvers support
 * should call one of the require methods at the beginning.
 */
public abstract class SolverBasedTest0 {

  /**
   * Return a list of all solvers in a format suitable for a
   * {@link Parameters} method.
   */
  protected static List<Object[]> allSolversAsParameters() {
    List<Object[]> result = new ArrayList<>();
    for (Solvers solver : Solvers.values()) {
      result.add(new Object[] { solver });
    }
    return result;
  }

  protected FormulaManagerFactory factory;
  protected FormulaManager mgr;
  protected BooleanFormulaManager bmgr;
  protected FunctionFormulaManager fmgr;
  protected NumeralFormulaManager<IntegerFormula, IntegerFormula> imgr;
  protected @Nullable NumeralFormulaManager<NumeralFormula, RationalFormula> rmgr;
  protected @Nullable QuantifiedFormulaManager qmgr;
  protected @Nullable ArrayFormulaManager amgr;

  /**
   * Return the solver to use in this test.
   * The default is SMTInterpol because it's the only solver guaranteed on all platforms.
   * Overwrite to specify a different solver.
   */
  protected Solvers solverToUse() {
    return Solvers.SMTINTERPOL;
  }

  @Before
  public final void initSolver() throws Exception {
    ConfigurationBuilder builder = new Builder();
    builder.setOption("cpa.predicate.solver", solverToUse().toString());

    // FileOption-Converter for correct output-paths, otherwise files are written in current working directory.
    builder.addConverter(FileOption.class, FileTypeConverter.createWithSafePathsOnly(Configuration.defaultConfiguration()));

    Configuration config = builder.build();

    try {
      factory = new FormulaManagerFactory(config, TestLogManager.getInstance(),
          ShutdownNotifier.create());
    } catch (NoClassDefFoundError e) {
      assume().withFailureMessage("Scala is not on class path")
              .that(e.getMessage()).doesNotContain("scala");
    }
    mgr = factory.getFormulaManager();
    fmgr = mgr.getFunctionFormulaManager();
    bmgr = mgr.getBooleanFormulaManager();
    imgr = mgr.getIntegerFormulaManager();
    try {
      rmgr = mgr.getRationalFormulaManager();
    } catch (UnsupportedOperationException e) {
      rmgr = null;
    }
    try {
      qmgr = mgr.getQuantifiedFormulaManager();
    } catch (UnsupportedOperationException e) {
      qmgr = null;
    }
    try {
      amgr = mgr.getArrayFormulaManager();
    } catch (UnsupportedOperationException e) {
      amgr = null;
    }
  }

  @After
  public final void closeSolver() throws Exception {
    if (mgr instanceof AutoCloseable) {
      ((AutoCloseable)mgr).close();
    }
  }

  /**
   * Skip test if the solver does not support rationals.
   */
  protected final void requireRationals() {
    assume().withFailureMessage("Solver " + solverToUse() + "does not support the theory of rationals")
            .that(rmgr).isNotNull();
  }

  /**
   * Skip test if the solver does not support quantifiers.
   */
  protected final void requireQuantifiers() {
    assume().withFailureMessage("Solver " + solverToUse() + "does not support quantifiers")
            .that(qmgr).isNotNull();
  }

  /**
   * Skip test if the solver does not support arrays.
   */
  protected final void requireArrays() {
    assume().withFailureMessage("Solver " + solverToUse() + "does not support the theory of arrays")
            .that(amgr).isNotNull();
  }

  /**
   * Use this for checking assertions about BooleanFormulas with Truth:
   * <code>assert_().about(BooleanFormula()).that(formula).is...()</code>.
   */
  protected final SubjectFactory<BooleanFormulaSubject, BooleanFormula> BooleanFormula() {
    return BooleanFormulaOfSolver(factory);
  }

  /**
   * Use this for checking assertions about BooleanFormulas
   * (given the correspondign solver) with Truth:
   * <code>assert_().about(BooleanFormulaOfSolver(factory)).that(formula).is...()</code>.
   */
  public static final SubjectFactory<BooleanFormulaSubject, BooleanFormula> BooleanFormulaOfSolver(
      final FormulaManagerFactory factory) {
    return new SubjectFactory<BooleanFormulaSubject, BooleanFormula>() {
          @Override
          public BooleanFormulaSubject getSubject(FailureStrategy pFs, BooleanFormula pFormula) {
            return new BooleanFormulaSubject(pFs, pFormula, factory);
          }
        };
  }
}
