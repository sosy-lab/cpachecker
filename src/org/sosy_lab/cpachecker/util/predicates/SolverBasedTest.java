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

import org.junit.After;
import org.junit.Before;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.TestLogManager;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.ArrayFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;

public abstract class SolverBasedTest {

  // TODO: Merge this class with SolverTest

  protected Solver solver;
  protected LogManager log;
  protected FormulaManager formulaManager;
  protected FormulaManagerView fmgr;
  protected ArrayFormulaManagerView afm;
  protected BooleanFormulaManagerView bfm;
  protected NumeralFormulaManagerView<IntegerFormula, IntegerFormula> ifm;

  protected String getSolverToUse() {
    return "Z3";
  }

  @Before
  public void setupSolver() throws Exception {
    Configuration.defaultConfiguration();

    Configuration config = Configuration
        .builder()
        .setOption("cpa.predicate.solver", getSolverToUse())
        .build();

    FormulaManagerFactory factory = new FormulaManagerFactory(config, log, ShutdownNotifier.create());
    formulaManager = factory.getFormulaManager();
    fmgr    = new FormulaManagerView(formulaManager, config, TestLogManager.getInstance());
    afm     = fmgr.getArrayFormulaManager();
    bfm     = fmgr.getBooleanFormulaManager();
    ifm     = fmgr.getIntegerFormulaManager();
    solver  = new Solver(fmgr, factory);
    log = TestLogManager.getInstance();
  }

  @After
  public void closeSolver() throws Exception {
    if (formulaManager instanceof AutoCloseable) {
      ((AutoCloseable) formulaManager).close();
    }
  }

}
