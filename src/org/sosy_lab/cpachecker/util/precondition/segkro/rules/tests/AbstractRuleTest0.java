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
package org.sosy_lab.cpachecker.util.precondition.segkro.rules.tests;

import org.junit.Before;
import org.sosy_lab.common.log.TestLogManager;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.FormulaManagerFactory.Solvers;
import org.sosy_lab.solver.Solver;
import org.sosy_lab.solver.api.ArrayFormula;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.FormulaType.NumeralType;
import org.sosy_lab.solver.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.ArrayFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.QuantifiedFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstMatcher;
import org.sosy_lab.solver.SolverBasedTest0;


public abstract class AbstractRuleTest0 extends SolverBasedTest0 {

  protected ArrayFormulaManagerView afm;
  protected BooleanFormulaManagerView bfm;
  protected QuantifiedFormulaManagerView qfm;
  protected NumeralFormulaManagerView<IntegerFormula, IntegerFormula> ifm;

  protected FormulaManagerView mgrv;
  protected SmtAstMatcher matcher;
  protected Solver solver;

  protected BooleanFormula rangePredicate(boolean pForall, IntegerFormula pLowerLimit, IntegerFormula pUpperLimit) {

    IntegerFormula _x = ifm.makeVariable("x");
    ArrayFormula<IntegerFormula, IntegerFormula> _b = afm.makeArray("b", NumeralType.IntegerType, NumeralType.IntegerType);
    BooleanFormula _body = bfm.not(ifm.equal(afm.select(_b, _x), ifm.makeNumber(0)));

    if (pForall) {
      return qfm.forall(_x, pLowerLimit, pUpperLimit, _body);
    } else {
      return qfm.exists(_x, pLowerLimit, pUpperLimit, _body);
    }
  }

  @Override
  protected Solvers solverToUse() {
    return Solvers.Z3;
  }

  @Before
  public void setUp() throws Exception {
    mgrv = new FormulaManagerView(factory, config, TestLogManager.getInstance());
    solver = new Solver(mgrv, factory, config, TestLogManager.getInstance());
    matcher = solver.getSmtAstMatcher();

    afm = mgrv.getArrayFormulaManager();
    bfm = mgrv.getBooleanFormulaManager();
    ifm = mgrv.getIntegerFormulaManager();
    qfm = mgrv.getQuantifiedFormulaManager();
  }

  protected boolean isFormulaEqual(BooleanFormula pFormula, BooleanFormula pIsEqualTo)
      throws SolverException, InterruptedException {

    return solver.isUnsat(bfm.notEquivalence(pFormula, pIsEqualTo));
  }

}
