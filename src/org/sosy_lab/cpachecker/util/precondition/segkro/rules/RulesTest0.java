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
package org.sosy_lab.cpachecker.util.precondition.segkro.rules;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory.Solvers;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ArrayFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType.NumeralType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.test.SolverBasedTest0;

import com.google.common.collect.Lists;


public class RulesTest0 extends SolverBasedTest0 {

  @Override
  protected Solvers solverToUse() {
    return Solvers.Z3;
  }

  private Solver solver;
  private FormulaManagerView mgrv;

  @Before
  public void setup() throws InvalidConfigurationException {
    mgrv = new FormulaManagerView(mgr, config, logger);
    solver = new Solver(mgrv, factory);
  }

  @Test
  public void testElim1() throws SolverException, InterruptedException {

    IntegerFormula _c1 = mgrv.makeVariable(NumeralType.IntegerType, "c1");
    IntegerFormula _c2 = mgrv.makeVariable(NumeralType.IntegerType, "c2");
    IntegerFormula _e1 = mgrv.makeVariable(NumeralType.IntegerType, "e1");
    IntegerFormula _e2 = mgrv.makeVariable(NumeralType.IntegerType, "e2");
    IntegerFormula _eX = mgrv.makeVariable(NumeralType.IntegerType, "eX");
    IntegerFormula _0 = imgr.makeNumber(0);

    // Formulas for the premise
    BooleanFormula _c1_GT_0 = imgr.greaterThan(_c1, _0);
    BooleanFormula _c2_GT_0 = imgr.greaterThan(_c2, _0);
    BooleanFormula _c1_times_ex_plus_e1_GEQ_0
      = imgr.greaterOrEquals(
          imgr.add(
              imgr.multiply(_c1, _eX),
              _e1),
          _0);
    BooleanFormula _minus_c2_times_ex_plus_e2_GEQ_0
      = imgr.greaterOrEquals(
          imgr.add(
              imgr.multiply(
                  imgr.subtract(_0, _c2),
                  _eX),
              _e2),
          _0);

    // The formula that is expected as conclusion
    BooleanFormula _c2_times_e1_minus_c1_times_e2_GEQ_0
      = imgr.greaterOrEquals(
          imgr.subtract(
              imgr.multiply(_c2, _e1),
              imgr.multiply(_c1, _e2)),
          _0);

    // Check if the expected conclusion is implied by the conjunction of the premises
    ArrayList<BooleanFormula> premiseList = Lists.newArrayList(bmgr.not(_c1_GT_0), bmgr.not(_c2_GT_0), _c1_times_ex_plus_e1_GEQ_0, _minus_c2_times_ex_plus_e2_GEQ_0);
    BooleanFormula premise = bmgr.and(premiseList);

    BooleanFormula check = bmgr.and(premise, _c2_times_e1_minus_c1_times_e2_GEQ_0);
    assertFalse(solver.isUnsat(check));

    EliminationRule elimRule = new EliminationRule(mgr, mgrv, solver);
    Set<BooleanFormula> concluded = elimRule.apply(Lists.newArrayList(_c1_GT_0, _c2_GT_0, _c1_times_ex_plus_e1_GEQ_0, _minus_c2_times_ex_plus_e2_GEQ_0));
  }

  @Test
  public void testEquivalence1() throws SolverException, InterruptedException {
    IntegerFormula _x = mgrv.makeVariable(NumeralType.IntegerType, "x");
    IntegerFormula _e = mgrv.makeVariable(NumeralType.IntegerType, "e");
    IntegerFormula _0 = imgr.makeNumber(0);

    // Formulas for the premise
    BooleanFormula _x_minus_e_GEQ_0 = imgr.greaterOrEquals(imgr.subtract(_x, _e), _0);
    BooleanFormula _minus_x_plus_e_GEQ_0 = imgr.greaterOrEquals(imgr.add(imgr.subtract(_0, _x), _e), _0);

    // The conclusion
    BooleanFormula _x_EQ_e = imgr.equal(_x, _e);

    // Check
    BooleanFormula premise = bmgr.and(Lists.newArrayList(_x_minus_e_GEQ_0, _minus_x_plus_e_GEQ_0));
    BooleanFormula check = bmgr.not(bmgr.or(bmgr.not(premise), _x_EQ_e));

    assertTrue(solver.isUnsat(check));
  }

  @Test
  public void testSubstitution1() {
    IntegerFormula _x = mgrv.makeVariable(NumeralType.IntegerType, "x");
    IntegerFormula _e = mgrv.makeVariable(NumeralType.IntegerType, "e");
    IntegerFormula _0 = imgr.makeNumber(0);

    // Formulas for the premise
    BooleanFormula _x_EQ_e = imgr.equal(_x, _e);
    ArrayFormula<IntegerFormula, IntegerFormula> _a = amgr.makeArray("a", NumeralType.IntegerType, NumeralType.IntegerType);
    ArrayFormula<IntegerFormula, IntegerFormula> _a_store_0_at_x = amgr.store(_a, _x, _0);

    // The conclusion
    BooleanFormula _a_at_e_EQ_0 = imgr.equal(amgr.select(_a, _e), _0);

    // Check

  }

  @Test
  public void testExist1() {
    assertTrue(false);
  }

  @Test
  public void testExtendLeft1() {
    assertTrue(false);
  }

  @Test
  public void testExtendRight1() {
    assertTrue(false);
  }

  @Test
  public void testLink1() {
    assertTrue(false);
  }

  @Test
  public void testUniv1() {
    assertTrue(false);
  }

}
