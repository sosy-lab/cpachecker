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
package org.sosy_lab.cpachecker.util.predicates.interfaces.view;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory.Solvers;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ArrayFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType.NumeralType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.test.SolverBasedTest0;

import com.google.common.collect.Lists;

@RunWith(Parameterized.class)
public class FormulaManagerViewTest0 extends SolverBasedTest0 {

  @Parameters(name="{0}")
  public static List<Object[]> getAllSolvers() {
    return allSolversAsParameters();
  }

  @Parameter(0)
  public Solvers solver;

  @Override
  protected Solvers solverToUse() {
    return solver;
  }

  private FormulaManagerView mv;
  private ArrayFormulaManagerView amv;
  private BooleanFormulaManagerView bmv;
  private QuantifiedFormulaManagerView qmv;
  private NumeralFormulaManagerView<IntegerFormula, IntegerFormula> imv;

  private IntegerFormula _0;
  private IntegerFormula _1;
  private IntegerFormula _i;
  private IntegerFormula _j;
  private IntegerFormula _x;

  @Before
  public void setUp() throws Exception {
    mv = new FormulaManagerView(mgr, config, logger);
    imv = mv.getIntegerFormulaManager();
    amv = mv.getArrayFormulaManager();
    qmv = mv.getQuantifiedFormulaManager();
    bmv = mv.getBooleanFormulaManager();

    _0 = imv.makeNumber(0);
    _1 = imv.makeNumber(1);
    _i = imv.makeVariable("i");
    _j = imv.makeVariable("j");
    _x = imv.makeVariable("x");
  }

  @Test
  public void testExtractAtomsArrays() {
    requireArrays();
  }

  @Test
  public void testExtractAtomsQuantifiers() {
    requireQuantifiers();
  }

  @Test
  public void testExtractAtomsQuantifiersAndArrays() {
    requireQuantifiers();
    requireArrays();
  }

  @Test
  public void testExtractLiteralsArrays() {
    requireArrays();
  }

  @Test
  public void testExtractLiteralsQuantifiers() {
    requireQuantifiers();
  }

  @Test
  public void testExtractLiteralsQuantifiersAndArrays() {
    requireQuantifiers();
    requireArrays();
  }

  @Test
  public void testUninstanciateQuantifiersAndArrays() {
    requireQuantifiers();
    requireArrays();

    ArrayFormula<IntegerFormula, IntegerFormula> _b = amv.makeArray("b", NumeralType.IntegerType, NumeralType.IntegerType);
    BooleanFormula _b_at_x_NOTEQ_0 = bmv.not(imv.equal(amv.select(_b, _x), _0));
    BooleanFormula _FORALL_i = qmgr.forall(
        Lists.newArrayList(_x),
        bmv.and(
            Lists.newArrayList(
              _b_at_x_NOTEQ_0,
              imv.greaterOrEquals(_x, _j),
              imv.lessOrEquals(_x, _i)
            )));

    BooleanFormula result = mv.uninstantiate(_FORALL_i);
    assertThat(result.toString()).isEqualTo("");
  }

}
