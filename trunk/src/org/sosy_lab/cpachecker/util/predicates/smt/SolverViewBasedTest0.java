// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import static com.google.common.truth.TruthJUnit.assume;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.After;
import org.junit.Before;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.test.SolverBasedTest0;

/**
 * Abstract base class for tests that use an SMT solver just like {@link SolverBasedTest0}, but
 * additionally providing {@link Solver} and {@link FormulaManagerView} instances.
 */
@SuppressFBWarnings(value = "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
public class SolverViewBasedTest0 extends SolverBasedTest0 {

  protected Solver solver;
  protected FormulaManagerView mgrv;
  protected BooleanFormulaManagerView bmgrv;
  protected IntegerFormulaManagerView imgrv;

  @Override
  protected ConfigurationBuilder createTestConfigBuilder() {
    ConfigurationBuilder newConfig = super.createTestConfigBuilder();

    // Automatically choose theories that are supported by the solver.
    // With unsupported theories, test would just fail.
    // We could also use the same theory (QF_AUFLIA) for all solvers,
    // but maybe testing a set of several theories is not bad after all.
    switch (solverToUse()) {
      case SMTINTERPOL:
        newConfig.setOption("cpa.predicate.encodeBitvectorAs", "INTEGER");
        newConfig.setOption("cpa.predicate.encodeFloatAs", "RATIONAL");
        break;
      case PRINCESS:
        newConfig.setOption("cpa.predicate.encodeBitvectorAs", "INTEGER");
        newConfig.setOption("cpa.predicate.encodeFloatAs", "INTEGER");
        break;
      case BOOLECTOR:
        assume()
            .withMessage("Solver %s does not support the tested features", solverToUse())
            .that(solverToUse())
            .isNotEqualTo(Solvers.BOOLECTOR);
        // newConfig.setOption("cpa.predicate.createFormulaEncodingEagerly", "false");
        // newConfig.setOption("cpa.predicate.encodeIntegerAs", "BITVECTOR");
        // newConfig.setOption("cpa.predicate.encodeBitvectorAs", "BITVECTOR");
        // newConfig.setOption("cpa.predicate.encodeFloatAs", "INTEGER");
        break;
      case YICES2:
        assume()
            .withMessage(
                "Solver %s is not available on all systems, disabling it for CPAchecker",
                solverToUse())
            .that(solverToUse())
            .isNotEqualTo(Solvers.YICES2);
        break;
      default:
        newConfig.setOption("cpa.predicate.encodeBitvectorAs", "BITVECTOR");
        newConfig.setOption("cpa.predicate.encodeFloatAs", "FLOAT");
    }
    return newConfig;
  }

  @Before
  public final void initCPAcheckerSolver() throws InvalidConfigurationException {
    solver = new Solver(factory, solverToUse(), context, config, logger);
    mgrv = solver.getFormulaManager();
    bmgrv = mgrv.getBooleanFormulaManager();
    imgrv = mgrv.getIntegerFormulaManager();
  }

  @After
  public final void closeCPAcheckerSolver() {
    // We should close the solver, but the super class does this, too,
    // and calling it twice can segfault.
    // solver.close();
  }
}
