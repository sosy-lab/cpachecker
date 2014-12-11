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

import static com.google.common.truth.Truth.assertThat;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.precondition.segkro.rules.EquivalenceRule;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory.Solvers;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType.NumeralType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.z3.matching.SmtAstMatcher;
import org.sosy_lab.cpachecker.util.predicates.z3.matching.Z3AstMatcher;
import org.sosy_lab.cpachecker.util.test.SolverBasedTest0;

import com.google.common.collect.Lists;

public class EquivalenceRuleTest0 extends SolverBasedTest0 {

  private SmtAstMatcher matcher;
  private Solver solver;
  private FormulaManagerView mgrv;

  private EquivalenceRule er;

  @Override
  protected Solvers solverToUse() {
    return Solvers.Z3;
  }

  @Before
  public void setUp() throws Exception {
    mgrv = new FormulaManagerView(mgr, config, logger);
    solver = new Solver(mgrv, factory);

    matcher = new Z3AstMatcher(logger, mgr, mgrv);
    er = new EquivalenceRule(mgr, mgrv, solver, matcher);
  }

  @Test
  public void testEquivalence1() throws SolverException, InterruptedException {
    IntegerFormula _x = mgrv.makeVariable(NumeralType.IntegerType, "x");
    IntegerFormula _e = mgrv.makeVariable(NumeralType.IntegerType, "e");
    IntegerFormula _0 = imgr.makeNumber(0);

    // Formulas for the premise
    BooleanFormula _x_minus_e_GEQ_0 = imgr.greaterOrEquals(imgr.subtract(_x, _e), _0);
    BooleanFormula _minus_x_plus_e_GEQ_0 = imgr.greaterOrEquals(imgr.add(imgr.subtract(_0, _x), _e), _0);

    // The expected conclusion
    BooleanFormula expectedConclusion = imgr.equal(_x, _e);

    Set<BooleanFormula> conclusion = er.applyWithInputRelatingPremises(
        Lists.newArrayList(
            _x_minus_e_GEQ_0,
            _minus_x_plus_e_GEQ_0
        ));

    assertThat(conclusion).isNotEmpty();
    assertThat(conclusion.iterator().next().toString()).isEqualTo(expectedConclusion.toString());
  }

}

