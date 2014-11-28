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
package org.sosy_lab.cpachecker.cpa.wp.segkro.rules;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Set;

import org.junit.Test;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.SolverBasedTest;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ArrayFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType.NumeralType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;

import com.google.common.collect.Lists;


public class RulesTest0 extends SolverBasedTest {

  @Override
  public void setUp() throws Exception {
    super.setUp();

  }

  @Test
  public void testElim1() throws SolverException, InterruptedException {

    IntegerFormula _c1 = fmgr.makeVariable(NumeralType.IntegerType, "c1");
    IntegerFormula _c2 = fmgr.makeVariable(NumeralType.IntegerType, "c2");
    IntegerFormula _e1 = fmgr.makeVariable(NumeralType.IntegerType, "e1");
    IntegerFormula _e2 = fmgr.makeVariable(NumeralType.IntegerType, "e2");
    IntegerFormula _eX = fmgr.makeVariable(NumeralType.IntegerType, "eX");
    IntegerFormula _0 = ifm.makeNumber(0);

    // Formulas for the premise
    BooleanFormula _c1_GT_0 = ifm.greaterThan(_c1, _0);
    BooleanFormula _c2_GT_0 = ifm.greaterThan(_c2, _0);
    BooleanFormula _c1_times_ex_plus_e1_GEQ_0
      = ifm.greaterOrEquals(
          ifm.add(
              ifm.multiply(_c1, _eX),
              _e1),
          _0);
    BooleanFormula _minus_c2_times_ex_plus_e2_GEQ_0
      = ifm.greaterOrEquals(
          ifm.add(
              ifm.multiply(
                  ifm.subtract(_0, _c2),
                  _eX),
              _e2),
          _0);

    // The formula that is expected as conclusion
    BooleanFormula _c2_times_e1_minus_c1_times_e2_GEQ_0
      = ifm.greaterOrEquals(
          ifm.subtract(
              ifm.multiply(_c2, _e1),
              ifm.multiply(_c1, _e2)),
          _0);

    // Check if the expected conclusion is implied by the conjunction of the premises
    ArrayList<BooleanFormula> premiseList = Lists.newArrayList(bfm.not(_c1_GT_0), bfm.not(_c2_GT_0), _c1_times_ex_plus_e1_GEQ_0, _minus_c2_times_ex_plus_e2_GEQ_0);
    BooleanFormula premise = bfm.and(premiseList);

    BooleanFormula check = bfm.and(premise, _c2_times_e1_minus_c1_times_e2_GEQ_0);
    assertFalse(solver.isUnsat(check));

    EliminationRule elimRule = new EliminationRule(formulaManager, solver);
    Set<BooleanFormula> concluded = elimRule.apply(Lists.newArrayList(_c1_GT_0, _c2_GT_0, _c1_times_ex_plus_e1_GEQ_0, _minus_c2_times_ex_plus_e2_GEQ_0));
  }

  @Test
  public void testEquivalence1() throws SolverException, InterruptedException {
    IntegerFormula _x = fmgr.makeVariable(NumeralType.IntegerType, "x");
    IntegerFormula _e = fmgr.makeVariable(NumeralType.IntegerType, "e");
    IntegerFormula _0 = ifm.makeNumber(0);

    // Formulas for the premise
    BooleanFormula _x_minus_e_GEQ_0 = ifm.greaterOrEquals(ifm.subtract(_x, _e), _0);
    BooleanFormula _minus_x_plus_e_GEQ_0 = ifm.greaterOrEquals(ifm.add(ifm.subtract(_0, _x), _e), _0);

    // The conclusion
    BooleanFormula _x_EQ_e = ifm.equal(_x, _e);

    // Check
    BooleanFormula premise = bfm.and(Lists.newArrayList(_x_minus_e_GEQ_0, _minus_x_plus_e_GEQ_0));
    BooleanFormula check = bfm.not(bfm.implication(premise, _x_EQ_e));

    assertTrue(solver.isUnsat(check));
  }

  @Test
  public void testSubstitution1() {
    IntegerFormula _x = fmgr.makeVariable(NumeralType.IntegerType, "x");
    IntegerFormula _e = fmgr.makeVariable(NumeralType.IntegerType, "e");
    IntegerFormula _0 = ifm.makeNumber(0);

    // Formulas for the premise
    BooleanFormula _x_EQ_e = ifm.equal(_x, _e);
    ArrayFormula<IntegerFormula, IntegerFormula> _a = afm.makeArray("a", NumeralType.IntegerType, NumeralType.IntegerType);
    ArrayFormula<IntegerFormula, IntegerFormula> _a_store_0_at_x = afm.store(_a, _x, _0);

    // The conclusion
    BooleanFormula _a_at_e_EQ_0 = ifm.equal(afm.select(_a, _e), _0);

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
