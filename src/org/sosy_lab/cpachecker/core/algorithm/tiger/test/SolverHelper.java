/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.tiger.test;

import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;

import com.google.common.collect.ImmutableMap;

public class SolverHelper {

  private Solver solver;
  private FormulaManagerView fmgr;

  public SolverHelper() throws InvalidConfigurationException {
    Configuration config = TestDataTools.configurationForTest().setOptions(
        ImmutableMap.of(
            "cpa.predicate.solver", "SMTInterpol",
            "log.consoleLevel", "FINE",
            // For easier debugging.
            "cpa.predicate.handlePointerAliasing", "false",
            "analysis.interprocedural", "false"))
        .build();

    BasicLogManager logger = null;
    logger = new BasicLogManager(config, new StreamHandler(System.out, new SimpleFormatter()));

    ShutdownNotifier notifier = ShutdownNotifier.createDummy();
    solver = Solver.create(config, logger, notifier);
    fmgr = solver.getFormulaManager();
  }

  public boolean isSatisfiable(BooleanFormula formula)
      throws SolverException, InterruptedException {
    return !solver.isUnsat(formula);
  }

  public FormulaManagerView getFormulaManager() {
    return fmgr;
  }

  public boolean implies(BooleanFormula formula1, BooleanFormula formula2)
      throws SolverException, InterruptedException {
    return solver.implies(formula1, formula2);
  }

  public boolean equivalent(BooleanFormula formula1, BooleanFormula formula2)
      throws SolverException, InterruptedException {
    return implies(formula1, formula2)
        && implies(formula2, formula1);
  }

  public BooleanFormula parseFormula(String formula) {
    return PresenceConditionParser.parseFormula(formula, fmgr.getBooleanFormulaManager());
  }

}
