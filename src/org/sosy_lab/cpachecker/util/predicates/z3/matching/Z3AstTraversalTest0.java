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
package org.sosy_lab.cpachecker.util.predicates.z3.matching;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.SolverBasedTest;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType.NumeralType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.z3.Z3FormulaManager;

import com.google.common.collect.Lists;


public class Z3AstTraversalTest0 extends SolverBasedTest {

  private Z3AstTraversal atv;

  @Before
  public void setUp() throws Exception {
    atv = new Z3AstTraversal(((Z3FormulaManager)formulaManager));
  }

  @Test @Ignore
  public void test() {
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

    atv.traverse(_c2_times_e1_minus_c1_times_e2_GEQ_0);
  }

  @Test
  public void test2() throws SolverException, InterruptedException {
    IntegerFormula i1 = fmgr.makeVariable(NumeralType.IntegerType, "i@1");
    IntegerFormula j1 = fmgr.makeVariable(NumeralType.IntegerType, "j@1");
    IntegerFormula j2 = fmgr.makeVariable(NumeralType.IntegerType, "j@2");
    IntegerFormula a1 = fmgr.makeVariable(NumeralType.IntegerType, "a@1");

    IntegerFormula _1 = ifm.makeNumber(1);
    IntegerFormula _minus1 = ifm.makeNumber(-1);

    IntegerFormula _1_plus_a1 = ifm.add(_1, a1);
    BooleanFormula not_j1_eq_minus1 = bfm.not(fmgr.getIntegerFormulaManager().equal(j1, _minus1));
    BooleanFormula i1_eq_1_plus_a1 = ifm.equal(i1, _1_plus_a1);

    IntegerFormula j2_plus_a1 = ifm.add(j2, a1);
    BooleanFormula j1_eq_j2_plus_a1 = ifm.equal(j1, j2_plus_a1);

    BooleanFormula fm = bfm.and(Lists.newArrayList(
            i1_eq_1_plus_a1,
            not_j1_eq_minus1,
            j1_eq_j2_plus_a1));

    BooleanFormula q = fmgr.getQuantifiedFormulaManager().exists(Lists.<Formula>newArrayList(j1), fm);

    atv.traverse(q);

  }

}
