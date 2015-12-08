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
package org.sosy_lab.cpachecker.util.predicates;

import static com.google.common.truth.Truth.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.log.TestLogManager;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.FormulaManagerFactory.Solvers;
import org.sosy_lab.solver.api.ArrayFormula;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.FormulaType;
import org.sosy_lab.solver.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.solver.test.SolverBasedTest0;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.ArrayFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.QuantifiedFormulaManagerView;

import com.google.common.collect.Lists;


@RunWith(Parameterized.class)
public class SolverQuantifierTest extends SolverBasedTest0 {

  @Parameters(name="{0}")
  public static Object[] getAllSolvers() {
    return Solvers.values();
  }

  @Parameter(0)
  public Solvers solverUnderTest;

  private Solver solver;
  private FormulaManagerView mgrv;
  private ArrayFormulaManagerView afm;
  private BooleanFormulaManagerView bfm;
  private QuantifiedFormulaManagerView qfm;
  private NumeralFormulaManagerView<IntegerFormula, IntegerFormula> ifm;

  private IntegerFormula _x;
  private ArrayFormula<IntegerFormula, IntegerFormula> _b;
  private BooleanFormula _b_at_x_eq_1;
  private BooleanFormula _b_at_x_eq_0;
  private BooleanFormula _forall_x_bx_1;
  private BooleanFormula _forall_x_bx_0;

  @Override
  protected Solvers solverToUse() {
    return solverUnderTest;
  }

  @Before
  public void setUp() throws Exception {
    requireArrays();
    requireQuantifiers();

    this.mgrv = new FormulaManagerView(factory.getFormulaManager(),
        config, TestLogManager.getInstance());
    this.solver = new Solver(mgrv, factory, config, TestLogManager.getInstance());
    this.afm = mgrv.getArrayFormulaManager();
    this.bfm = mgrv.getBooleanFormulaManager();
    this.ifm = mgrv.getIntegerFormulaManager();
    this.qfm = mgrv.getQuantifiedFormulaManager();

    _x = ifm.makeVariable("x");
    _b = afm.makeArray("b", FormulaType.IntegerType, FormulaType.IntegerType);

    _b_at_x_eq_1 = ifm.equal(afm.select(_b, _x), ifm.makeNumber(1));
    _b_at_x_eq_0 = ifm.equal(afm.select(_b, _x), ifm.makeNumber(0));

    _forall_x_bx_1 = qfm.forall(_x, _b_at_x_eq_1);
    _forall_x_bx_0 = qfm.forall(_x, _b_at_x_eq_0);
  }

  @Test
  public void testForallArrayConjunct() throws SolverException, InterruptedException {

    BooleanFormula f;

    // (forall x . b[x] = 0) AND (b[123] = 1) is UNSAT
    f = bfm.and(
        qfm.forall(_x, _b_at_x_eq_0),
        ifm.equal(afm.select(_b, ifm.makeNumber(123)), ifm.makeNumber(1))
      );
    assertThat(solver.isUnsat(f)).isTrue();

    // (forall x . b[x] = 0) AND (b[123] = 0) is SAT
    f = bfm.and(
          qfm.forall(_x, _b_at_x_eq_0),
          ifm.equal(afm.select(_b, ifm.makeNumber(123)), ifm.makeNumber(0))
        );
    assertThat(solver.isUnsat(f)).isFalse();
  }

  @Test
  public void testForallArrayDisjunct() throws SolverException, InterruptedException {
    BooleanFormula f;

    // (forall x . b[x] = 0) AND (b[123] = 1 OR b[123] = 0) is SAT
    f = bfm.and(
        qfm.forall(_x, _b_at_x_eq_0),
        bfm.or(
            ifm.equal(afm.select(_b, ifm.makeNumber(123)), ifm.makeNumber(1)),
            ifm.equal(afm.select(_b, ifm.makeNumber(123)), ifm.makeNumber(0))

    ));
    assertThat(solver.isUnsat(f)).isFalse();

    // (forall x . b[x] = 0) OR (b[123] = 1) is SAT
    f = bfm.or(
          qfm.forall(_x, _b_at_x_eq_0),
          ifm.equal(afm.select(_b, ifm.makeNumber(123)), ifm.makeNumber(1))
    );
    assertThat(solver.isUnsat(f)).isFalse();
  }

  @Test
  public void testNotExistsArrayConjunct() throws SolverException, InterruptedException {
    BooleanFormula f;

    // (not exists x . not b[x] = 0) AND (b[123] = 1) is UNSAT
    f = bfm.and(Lists.newArrayList(
          bfm.not(qfm.exists(_x, bfm.not(_b_at_x_eq_0))),
          ifm.equal(afm.select(_b, ifm.makeNumber(123)), ifm.makeNumber(1))
      ));
    assertThat(solver.isUnsat(f)).isTrue();

    // (not exists x . not b[x] = 0) AND (b[123] = 0) is SAT
    f = bfm.and(
        bfm.not(qfm.exists(_x, bfm.not(_b_at_x_eq_0))),
        ifm.equal(afm.select(_b, ifm.makeNumber(123)), ifm.makeNumber(0))
    );
    assertThat(solver.isUnsat(f)).isFalse();

    // (not exists x . b[x] = 0) AND (b[123] = 0) is UNSAT
    f = bfm.and(
        bfm.not(qfm.exists(_x, _b_at_x_eq_0)),
        ifm.equal(afm.select(_b, ifm.makeNumber(123)), ifm.makeNumber(0))
    );
    assertThat(solver.isUnsat(f)).isTrue();
  }

  @Test
  public void testNotExistsArrayDisjunct() throws SolverException, InterruptedException {
    BooleanFormula f;

    // (not exists x . not b[x] = 0) AND (b[123] = 1 OR b[123] = 0) is SAT
    f = bfm.and(
          bfm.not(qfm.exists(_x, bfm.not(_b_at_x_eq_0))),
          bfm.or(
              ifm.equal(afm.select(_b, ifm.makeNumber(123)), ifm.makeNumber(1)),
              ifm.equal(afm.select(_b, ifm.makeNumber(123)), ifm.makeNumber(0))
        ));
    assertThat(solver.isUnsat(f)).isFalse();

    // (not exists x . not b[x] = 0) OR (b[123] = 1) is SAT
    f = bfm.or(Lists.newArrayList(
          bfm.not(qfm.exists(_x, bfm.not(_b_at_x_eq_0))),
          ifm.equal(afm.select(_b, ifm.makeNumber(123)), ifm.makeNumber(1))
        ));
    assertThat(solver.isUnsat(f)).isFalse();
  }

  @Test
  public void testExistsArrayConjunct() throws SolverException, InterruptedException {
    BooleanFormula f;

    // (exists x . b[x] = 0) AND (b[123] = 1) is SAT
    f = bfm.and(
        qfm.exists(_x, _b_at_x_eq_0),
        ifm.equal(afm.select(_b, ifm.makeNumber(123)), ifm.makeNumber(1)));
    assertThat(solver.isUnsat(f)).isFalse();

    // (exists x . b[x] = 1) AND  (forall x . b[x] = 0) is UNSAT
    f = bfm.and(
        qfm.exists(_x, _b_at_x_eq_1),
        _forall_x_bx_0);
    assertThat(solver.isUnsat(f)).isTrue();

    // (exists x . b[x] = 0) AND  (forall x . b[x] = 0) is SAT
    f = bfm.and(
        qfm.exists(_x, _b_at_x_eq_0),
        _forall_x_bx_0);
    assertThat(solver.isUnsat(f)).isFalse();
  }

  @Test
  public void testExistsArrayDisjunct() throws SolverException, InterruptedException {
    BooleanFormula f;

    // (exists x . b[x] = 0) OR  (forall x . b[x] = 1) is SAT
    f = bfm.or(qfm.exists(_x, _b_at_x_eq_0), qfm.forall(_x, _b_at_x_eq_1));
    assertThat(solver.isUnsat(f)).isFalse();

    // (exists x . b[x] = 1) OR (exists x . b[x] = 1) is SAT
    f = bfm.or(qfm.exists(_x, _b_at_x_eq_1), qfm.exists(_x, _b_at_x_eq_1));
    assertThat(solver.isUnsat(f)).isFalse();
  }

  @Test
  public void testExistsRestrictedRange() throws SolverException, InterruptedException {
    BooleanFormula f;

    BooleanFormula _exists_10_20_bx_0 = qfm.exists(_x,
        ifm.makeNumber(10),
        ifm.makeNumber(20),
        _b_at_x_eq_0);

    BooleanFormula _exists_10_20_bx_1 = qfm.exists(_x,
        ifm.makeNumber(10),
        ifm.makeNumber(20),
        _b_at_x_eq_1);

    // (exists x in [10..20] . b[x] = 0) AND (forall x . b[x] = 0) is SAT
    f = bfm.and(_exists_10_20_bx_0, _forall_x_bx_0);
    assertThat(solver.isUnsat(f)).isFalse();

    // (exists x in [10..20] . b[x] = 1) AND (forall x . b[x] = 0) is UNSAT
    f = bfm.and(_exists_10_20_bx_1, _forall_x_bx_0);
    assertThat(solver.isUnsat(f)).isTrue();

    // (exists x in [10..20] . b[x] = 1) AND (forall x . b[x] = 1) is SAT
    f = bfm.and(_exists_10_20_bx_1, _forall_x_bx_1);
    assertThat(solver.isUnsat(f)).isFalse();

    // (exists x in [10..20] . b[x] = 1) AND (b[10] = 0) is SAT
    f = bfm.and(_exists_10_20_bx_1, ifm.equal(afm.select(_b, ifm.makeNumber(10)), ifm.makeNumber(0)));
    assertThat(solver.isUnsat(f)).isFalse();

    // (exists x in [10..20] . b[x] = 1) AND (b[1000] = 0) is SAT
    f = bfm.and(_exists_10_20_bx_1, ifm.equal(afm.select(_b, ifm.makeNumber(1000)), ifm.makeNumber(0)));
    assertThat(solver.isUnsat(f)).isFalse();
  }

  @Test
  public void testForallRestrictedRange() throws SolverException, InterruptedException {
    BooleanFormula f;

    BooleanFormula _forall_10_20_bx_0 = qfm.forall(_x,
        ifm.makeNumber(10),
        ifm.makeNumber(20),
        _b_at_x_eq_0);

    BooleanFormula _forall_10_20_bx_1 = qfm.forall(_x,
        ifm.makeNumber(10),
        ifm.makeNumber(20),
        _b_at_x_eq_1);

    // (forall x in [10..20] . b[x] = 0) AND (forall x . b[x] = 0) is SAT
    f = bfm.and(
        _forall_10_20_bx_0,
        qfm.forall(_x, _b_at_x_eq_0));
    assertThatFormula(f).isSatisfiable();

    // (forall x in [10..20] . b[x] = 1) AND (exits x in [15..17] . b[x] = 0) is UNSAT
    f = bfm.and(
        _forall_10_20_bx_1,
        qfm.exists(_x, ifm.makeNumber(15), ifm.makeNumber(17), _b_at_x_eq_0));
    assertThat(solver.isUnsat(f)).isTrue();

    // (forall x in [10..20] . b[x] = 1) AND b[10] = 0 is UNSAT
    f = bfm.and(
        _forall_10_20_bx_1,
        ifm.equal(
            afm.select(_b, ifm.makeNumber(10)),
            ifm.makeNumber(0)));
    assertThat(solver.isUnsat(f)).isTrue();

    // (forall x in [10..20] . b[x] = 1) AND b[20] = 0 is UNSAT
    f = bfm.and(
        _forall_10_20_bx_1,
        ifm.equal(
            afm.select(_b, ifm.makeNumber(20)),
            ifm.makeNumber(0)));
    assertThat(solver.isUnsat(f)).isTrue();

    // (forall x in [10..20] . b[x] = 1) AND b[9] = 0 is SAT
    f = bfm.and(
        _forall_10_20_bx_1,
        ifm.equal(
            afm.select(_b, ifm.makeNumber(9)),
            ifm.makeNumber(0)));
    assertThat(solver.isUnsat(f)).isFalse();

    // (forall x in [10..20] . b[x] = 1) AND b[21] = 0 is SAT
    f = bfm.and(
        _forall_10_20_bx_1,
        ifm.equal(
            afm.select(_b, ifm.makeNumber(21)),
            ifm.makeNumber(0)));
    assertThat(solver.isUnsat(f)).isFalse();

    // (forall x in [10..20] . b[x] = 1) AND (forall x in [0..20] . b[x] = 0) is UNSAT
    f = bfm.and(
        _forall_10_20_bx_1,
        qfm.forall(_x, ifm.makeNumber(0), ifm.makeNumber(20), _b_at_x_eq_0));
    assertThat(solver.isUnsat(f)).isTrue();

    // (forall x in [10..20] . b[x] = 1) AND (forall x in [0..9] . b[x] = 0) is SAT
    f = bfm.and(
        _forall_10_20_bx_1,
        qfm.forall(_x, ifm.makeNumber(0), ifm.makeNumber(9), _b_at_x_eq_0));
    assertThat(solver.isUnsat(f)).isFalse();
  }

  @Test
  public void testContradiction() throws SolverException, InterruptedException {

    // forall x . x = x+1  is UNSAT

    BooleanFormula f = qfm.forall(_x, ifm.equal(_x, ifm.add(_x, ifm.makeNumber(1))));
    assertThat(solver.isUnsat(f)).isTrue();

    BooleanFormula g = qfm.exists(_x, ifm.equal(_x, ifm.add(_x, ifm.makeNumber(1))));
    assertThat(solver.isUnsat(g)).isTrue();
  }

  @Test
  public void testSimple() throws SolverException, InterruptedException {

    // forall x . x+2 = x+1+1  is SAT
    BooleanFormula f = qfm.forall(_x,
        ifm.equal(
            ifm.add(_x, ifm.makeNumber(2)),
            ifm.add(
                ifm.add(_x, ifm.makeNumber(1)),
                ifm.makeNumber(1))));
    assertThat(solver.isUnsat(f)).isFalse();
  }

  @Test
  public void testEquals() {
    BooleanFormula f1 = qfm.exists(ifm.makeVariable("x"), ifm.makeNumber(1), ifm.makeNumber(2), _b_at_x_eq_1);
    BooleanFormula f2 = qfm.exists(ifm.makeVariable("x"), ifm.makeNumber(1), ifm.makeNumber(2), _b_at_x_eq_1);

    assertThat(f1).isEqualTo(f2);
  }

}
