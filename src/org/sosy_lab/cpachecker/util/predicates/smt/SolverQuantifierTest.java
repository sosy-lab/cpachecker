/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.smt;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.ArrayFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.test.SolverBasedTest0;

@RunWith(Parameterized.class)
@SuppressFBWarnings("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
public class SolverQuantifierTest extends SolverBasedTest0 {

  @Parameters(name="{0}")
  public static Object[] getAllSolvers() {
    return Solvers.values();
  }

  @Parameter(0)
  public Solvers solverUnderTest;

  private QuantifiedFormulaManagerView qfm;

  private IntegerFormula _x;
  private ArrayFormula<IntegerFormula, IntegerFormula> _b;
  private BooleanFormula _b_at_x_eq_1;
  private BooleanFormula _b_at_x_eq_0;

  @Override
  protected Solvers solverToUse() {
    return solverUnderTest;
  }

  @Before
  public void setUp() throws Exception {
    requireArrays();
    requireQuantifiers();
    requireRationals();

    FormulaManagerView mgrv = new FormulaManagerView(mgr, config, LogManager.createTestLogManager());
    this.qfm = mgrv.getQuantifiedFormulaManager();
    imgr = mgrv.getIntegerFormulaManager();

    _x = imgr.makeVariable("x");
    _b = amgr.makeArray("b", FormulaType.IntegerType, FormulaType.IntegerType);

    _b_at_x_eq_1 = imgr.equal(amgr.select(_b, _x), imgr.makeNumber(1));
    _b_at_x_eq_0 = imgr.equal(amgr.select(_b, _x), imgr.makeNumber(0));
  }

  @Test
  public void testExistsRestrictedRange() throws SolverException, InterruptedException {
    BooleanFormula f;

    BooleanFormula _exists_10_20_bx_0 = qfm.exists(_x,
        imgr.makeNumber(10),
        imgr.makeNumber(20),
        _b_at_x_eq_0);

    BooleanFormula _exists_10_20_bx_1 = qfm.exists(_x,
        imgr.makeNumber(10),
        imgr.makeNumber(20),
        _b_at_x_eq_1);

    BooleanFormula _forall_x_bx_1 = qfm.forall(_x, _b_at_x_eq_1);
    BooleanFormula _forall_x_bx_0 = qfm.forall(_x, _b_at_x_eq_0);

    // (exists x in [10..20] . b[x] = 0) AND (forall x . b[x] = 0) is SAT
    f = bmgr.and(_exists_10_20_bx_0, _forall_x_bx_0);
    assertThatFormula(f).isSatisfiable();

    // (exists x in [10..20] . b[x] = 1) AND (forall x . b[x] = 0) is UNSAT
    f = bmgr.and(_exists_10_20_bx_1, _forall_x_bx_0);
    assertThatFormula(f).isUnsatisfiable();

    // (exists x in [10..20] . b[x] = 1) AND (forall x . b[x] = 1) is SAT
    f = bmgr.and(_exists_10_20_bx_1, _forall_x_bx_1);
    assertThatFormula(f).isSatisfiable();

    // (exists x in [10..20] . b[x] = 1) AND (b[10] = 0) is SAT
    f = bmgr.and(_exists_10_20_bx_1, imgr.equal(amgr.select(_b, imgr.makeNumber(10)), imgr.makeNumber(0)));
    assertThatFormula(f).isSatisfiable();

    // (exists x in [10..20] . b[x] = 1) AND (b[1000] = 0) is SAT
    f = bmgr.and(_exists_10_20_bx_1, imgr.equal(amgr.select(_b, imgr.makeNumber(1000)), imgr.makeNumber(0)));
    assertThatFormula(f).isSatisfiable();
  }

  @Test
  public void testForallRestrictedRange() throws SolverException, InterruptedException {
    BooleanFormula f;

    BooleanFormula _forall_10_20_bx_0 = qfm.forall(_x,
        imgr.makeNumber(10),
        imgr.makeNumber(20),
        _b_at_x_eq_0);

    BooleanFormula _forall_10_20_bx_1 = qfm.forall(_x,
        imgr.makeNumber(10),
        imgr.makeNumber(20),
        _b_at_x_eq_1);

    // (forall x in [10..20] . b[x] = 0) AND (forall x . b[x] = 0) is SAT
    f = bmgr.and(
        _forall_10_20_bx_0,
        qfm.forall(_x, _b_at_x_eq_0));
    assertThatFormula(f).isSatisfiable();

    // (forall x in [10..20] . b[x] = 1) AND (exits x in [15..17] . b[x] = 0) is UNSAT
    f = bmgr.and(
        _forall_10_20_bx_1,
        qfm.exists(_x, imgr.makeNumber(15), imgr.makeNumber(17), _b_at_x_eq_0));
    assertThatFormula(f).isUnsatisfiable();

    // (forall x in [10..20] . b[x] = 1) AND b[10] = 0 is UNSAT
    f = bmgr.and(
        _forall_10_20_bx_1,
        imgr.equal(
            amgr.select(_b, imgr.makeNumber(10)),
            imgr.makeNumber(0)));
    assertThatFormula(f).isUnsatisfiable();

    // (forall x in [10..20] . b[x] = 1) AND b[20] = 0 is UNSAT
    f = bmgr.and(
        _forall_10_20_bx_1,
        imgr.equal(
            amgr.select(_b, imgr.makeNumber(20)),
            imgr.makeNumber(0)));
    assertThatFormula(f).isUnsatisfiable();

    // (forall x in [10..20] . b[x] = 1) AND b[9] = 0 is SAT
    f = bmgr.and(
        _forall_10_20_bx_1,
        imgr.equal(
            amgr.select(_b, imgr.makeNumber(9)),
            imgr.makeNumber(0)));
    assertThatFormula(f).isSatisfiable();

    // (forall x in [10..20] . b[x] = 1) AND b[21] = 0 is SAT
    f = bmgr.and(
        _forall_10_20_bx_1,
        imgr.equal(
            amgr.select(_b, imgr.makeNumber(21)),
            imgr.makeNumber(0)));
    assertThatFormula(f).isSatisfiable();

    // (forall x in [10..20] . b[x] = 1) AND (forall x in [0..20] . b[x] = 0) is UNSAT
    f = bmgr.and(
        _forall_10_20_bx_1,
        qfm.forall(_x, imgr.makeNumber(0), imgr.makeNumber(20), _b_at_x_eq_0));
    assertThatFormula(f).isUnsatisfiable();

    // (forall x in [10..20] . b[x] = 1) AND (forall x in [0..9] . b[x] = 0) is SAT
    f = bmgr.and(
        _forall_10_20_bx_1,
        qfm.forall(_x, imgr.makeNumber(0), imgr.makeNumber(9), _b_at_x_eq_0));
    assertThatFormula(f).isSatisfiable();
  }
}
