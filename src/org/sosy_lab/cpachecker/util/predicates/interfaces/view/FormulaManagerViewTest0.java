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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.log.TestLogManager;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.solver.FormulaManagerFactory.Solvers;
import org.sosy_lab.solver.api.ArrayFormula;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.FormulaType.NumeralType;
import org.sosy_lab.solver.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.solver.SolverBasedTest0;

import com.google.common.collect.Lists;

@RunWith(Parameterized.class)
public class FormulaManagerViewTest0 extends SolverBasedTest0 {

  @Parameters(name="{0}")
  public static Object[] getAllSolvers() {
    return Solvers.values();
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
  private IntegerFormula _i1;
  private IntegerFormula _j;
  private IntegerFormula _j1;
  private IntegerFormula _x;
  private IntegerFormula _x1;
  private ArrayFormula<IntegerFormula, IntegerFormula> _b;

  @Before
  public void setUp() throws Exception {
    mv = new FormulaManagerView(factory, config, TestLogManager.getInstance());

    imv = mv.getIntegerFormulaManager();
    amv = mv.getArrayFormulaManager();
    qmv = mv.getQuantifiedFormulaManager();
    bmv = mv.getBooleanFormulaManager();

    _0 = imv.makeNumber(0);
    _1 = imv.makeNumber(1);
    _i = imv.makeVariable("i");
    _i1 = imv.makeVariable("i@1");
    _j = imv.makeVariable("j");
    _j1 = imv.makeVariable("j@1");
    _x = imv.makeVariable("x");
    _x1 = imv.makeVariable("x@1");
    _b = amv.makeArray("b", NumeralType.IntegerType, NumeralType.IntegerType);
  }

  @Test @Ignore
  public void testExtractAtomsArrays() {
    requireArrays();
  }

  @Test @Ignore
  public void testExtractAtomsQuantifiers() {
    requireQuantifiers();
  }

  @Test @Ignore
  public void testExtractAtomsQuantifiersAndArrays() {
    requireQuantifiers();
    requireArrays();
  }

  @Test @Ignore
  public void testExtractLiteralsArrays() {
    requireArrays();
  }

  @Test @Ignore
  public void testExtractLiteralsQuantifiers() {
    requireQuantifiers();
  }

  @Test @Ignore
  public void testExtractLiteralsQuantifiersAndArrays() {
    requireQuantifiers();
    requireArrays();
  }

  @Test
  public void testUnInstanciateQuantifiersAndArrays() {
    requireQuantifiers();
    requireArrays();

    BooleanFormula _b_at_x_NOTEQ_0 = bmv.not(imv.equal(amv.select(_b, _x), _0));

    BooleanFormula instantiated = qmv.forall(
        Lists.newArrayList(_x),
        bmv.and(
            Lists.newArrayList(
              _b_at_x_NOTEQ_0,
              imv.greaterOrEquals(_x, _j1),
              imv.lessOrEquals(_x, _i1)
            )));

    BooleanFormula uninstantiated = qmv.forall(
        Lists.newArrayList(_x),
        bmv.and(
            Lists.newArrayList(
              _b_at_x_NOTEQ_0,
              imv.greaterOrEquals(_x, _j),
              imv.lessOrEquals(_x, _i)
            )));

    SSAMapBuilder ssaBuilder = SSAMap.emptySSAMap().builder();
    ssaBuilder.setIndex("i", CNumericTypes.INT, 1);
    ssaBuilder.setIndex("j", CNumericTypes.INT, 1);

    testUnInstanciate(instantiated, uninstantiated, ssaBuilder);
  }

  @Test
  public void testUnInstanciate1() {
    requireQuantifiers();
    requireArrays();

    BooleanFormula _inst1 = imv.equal(
        imv.add(_1, _j1),
        imv.add(_0, _i1));
    BooleanFormula _inst2 = imv.equal(
        imv.add(_1, imv.subtract(_0, _i1)),
        imv.add(imv.add(_0, _x1), _i1));
    BooleanFormula _inst3 = bmv.and(
        Lists.newArrayList(_inst1, _inst2, bmv.not(_inst1)));

    BooleanFormula _uinst1 = imv.equal(
        imv.add(_1, _j),
        imv.add(_0, _i));
    BooleanFormula _uinst2 = imv.equal(
        imv.add(_1, imv.subtract(_0, _i)),
        imv.add(imv.add(_0, _x), _i));
    BooleanFormula _uinst3 = bmv.and(
        Lists.newArrayList(_uinst1, _uinst2, bmv.not(_uinst1)));

    SSAMapBuilder ssaBuilder = SSAMap.emptySSAMap().builder();
    ssaBuilder.setIndex("i", CNumericTypes.INT, 1);
    ssaBuilder.setIndex("j", CNumericTypes.INT, 1);
    ssaBuilder.setIndex("x", CNumericTypes.INT, 1);

    testUnInstanciate(_inst3, _uinst3, ssaBuilder);
  }

  private void testUnInstanciate(BooleanFormula pInstantiated, BooleanFormula pUninstantiated, SSAMapBuilder pSsaBuilder) {
    BooleanFormula r1 = mv.instantiate(pUninstantiated, pSsaBuilder.build());
    assertThat(r1.toString()).isEqualTo(pInstantiated.toString());

    BooleanFormula r2 = mv.uninstantiate(pInstantiated);
    assertThat(r2.toString()).isEqualTo(pUninstantiated.toString());
  }

}
