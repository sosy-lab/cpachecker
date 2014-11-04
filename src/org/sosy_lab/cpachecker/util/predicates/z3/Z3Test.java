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
package org.sosy_lab.cpachecker.util.predicates.z3;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.TestLogManager;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType.NumeralType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.collect.Lists;

/**
 * Testing the custom SSA implementation.
 */
public class Z3Test {

  private FormulaManager formulaManager;
  private FormulaManagerView fmgr;
  private Solver solver;

  @Before
  public void setup() throws Exception {
    Configuration.defaultConfiguration();

    Configuration config = Configuration
        .builder()
        .setOption("cpa.predicate.solver", "Z3")
        .build();

    FormulaManagerFactory factory = new FormulaManagerFactory(config, TestLogManager.getInstance(), ShutdownNotifier.create());
    formulaManager = factory.getFormulaManager();
    fmgr = new FormulaManagerView(formulaManager, config, TestLogManager.getInstance());
    solver = new Solver(fmgr, factory);
  }

  @After
  public void closeFormulaManager() throws Exception {
    if (formulaManager instanceof AutoCloseable) {
      ((AutoCloseable)formulaManager).close();
    }
  }

  @Test @Ignore
  public void doTest1() throws InterruptedException, SolverException, IOException {
    IntegerFormula var_B = fmgr.makeVariable(NumeralType.IntegerType, "b");
    IntegerFormula var_C = fmgr.makeVariable(NumeralType.IntegerType, "c");
    IntegerFormula num_2 = fmgr.getIntegerFormulaManager().makeNumber(2);
    IntegerFormula num_1000 = fmgr.getIntegerFormulaManager().makeNumber(1000);
    BooleanFormula eq_c_2 = fmgr.getIntegerFormulaManager().equal(var_C, num_2);
    IntegerFormula minus_b_c = fmgr.getIntegerFormulaManager().subtract(var_B, var_C);
    BooleanFormula gt_bMinusC_1000 = fmgr.getIntegerFormulaManager().greaterThan(minus_b_c, num_1000);
    BooleanFormula and_cEq2_bMinusCgt1000 = fmgr.getBooleanFormulaManager().and(eq_c_2, gt_bMinusC_1000);

    BooleanFormula f = fmgr.getQuantifiedFormulaManager().exists(Lists.<Formula>newArrayList(var_C), and_cEq2_bMinusCgt1000);
    BooleanFormula result = fmgr.getQuantifiedFormulaManager().eliminateQuantifiers(f);

    org.junit.Assert.assertEquals("(>= b 1003)", result.toString());
  }

  @Test @Ignore
  public void doTest2() throws InterruptedException, SolverException, IOException {
    IntegerFormula i1 = fmgr.makeVariable(NumeralType.IntegerType, "i@1");
    IntegerFormula j1 = fmgr.makeVariable(NumeralType.IntegerType, "j@1");
    IntegerFormula j2 = fmgr.makeVariable(NumeralType.IntegerType, "j@2");
    IntegerFormula a1 = fmgr.makeVariable(NumeralType.IntegerType, "a@1");

    IntegerFormula _1 = fmgr.getIntegerFormulaManager().makeNumber(1);
    IntegerFormula _minus1 = fmgr.getIntegerFormulaManager().makeNumber(-1);

    IntegerFormula _1_plus_a1 = fmgr.getIntegerFormulaManager().add(_1, a1);
    BooleanFormula not_j1_eq_minus1 = fmgr.getBooleanFormulaManager().not(fmgr.getIntegerFormulaManager().equal(j1, _minus1));
    BooleanFormula i1_eq_1_plus_a1 = fmgr.getIntegerFormulaManager().equal(i1, _1_plus_a1);

    IntegerFormula j2_plus_a1 = fmgr.getIntegerFormulaManager().add(j2, a1);
    BooleanFormula j1_eq_j2_plus_a1 = fmgr.getIntegerFormulaManager().equal(j1, j2_plus_a1);

    BooleanFormula fm = fmgr.getBooleanFormulaManager().and(Lists.newArrayList(
            i1_eq_1_plus_a1,
            not_j1_eq_minus1,
            j1_eq_j2_plus_a1));

    BooleanFormula q = fmgr.getQuantifiedFormulaManager().exists(Lists.<Formula>newArrayList(j1), fm);
    BooleanFormula result = fmgr.getQuantifiedFormulaManager().eliminateQuantifiers(q);

    BooleanFormula expectedResult = fmgr.makeNot(fmgr.getIntegerFormulaManager().equal(i1, j2));

    org.junit.Assert.assertTrue(solver.isUnsat(fmgr.makeXor(result, expectedResult)));
  }

}