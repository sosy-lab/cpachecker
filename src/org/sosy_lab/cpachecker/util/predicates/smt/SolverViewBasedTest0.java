/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.After;
import org.junit.Before;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
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
