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
package org.sosy_lab.cpachecker.util.precondition.segkro;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.log.TestLogManager;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.cpachecker.util.precondition.segkro.rules.RuleEngine;
import org.sosy_lab.solver.FormulaManagerFactory.Solvers;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.solver.api.ArrayFormula;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.FormulaType.NumeralType;
import org.sosy_lab.solver.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.solver.test.SolverBasedTest0;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.ArrayFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.QuantifiedFormulaManagerView;

import com.google.common.collect.Lists;

@SuppressWarnings("unused")
public class ExtractNewPredsTest0 extends SolverBasedTest0 {

  private ExtractNewPreds enp;
  private RuleEngine rulesEngine;

  private FormulaManagerView mgrv;
  private ArrayFormulaManagerView afm;
  private QuantifiedFormulaManagerView qfm;
  private BooleanFormulaManagerView bfm;
  private NumeralFormulaManagerView<IntegerFormula, IntegerFormula> ifm;

  private BooleanFormula _i1_EQUAL_0;
  private BooleanFormula _b0_at_i1_NOTEQUAL_0;
  private IntegerFormula _a0_at_i1;
  private IntegerFormula _b0_at_i1;
  private BooleanFormula _i1_GEQ_al0;
  private BooleanFormula _a0_at_i1_EQUAL_b0_at_i1;
  private IntegerFormula _i1_plus_1;
  private BooleanFormula _i0_EQUAL_i1_plus_1;
  private BooleanFormula _b0_at_i0_EQUAL_0;
  private IntegerFormula _b0_at_i0;
  private BooleanFormula _i2_EQUAL_0;
  private IntegerFormula _b0_at_i2;
  private BooleanFormula _b0_at_i2_NOTEQUAL_0;
  private BooleanFormula _i2_GEQ_al0;
  private BooleanFormula _a1_at_i2_EQUAL_b0_at_i2;
  private IntegerFormula _i2_plus_1;
  private BooleanFormula _i1_EQUAL_i2_plus_1;
  private BooleanFormula _b0_at_i0_NOTEQUAL_0;
  private BooleanFormula _i0_LESS_al0;
  private IntegerFormula _a1_at_i2;

  private BooleanFormula _safeTrace;
  private BooleanFormula _errorTrace;
  private IntegerFormula _0;
  private IntegerFormula _1;
  private IntegerFormula _i;
  private IntegerFormula _n;
  private IntegerFormula _al;
  private IntegerFormula _bl;
  private ArrayFormula<IntegerFormula, IntegerFormula> _b0;
  private ArrayFormula<IntegerFormula, IntegerFormula> _b;
  private IntegerFormula _k;


  @Override
  protected Solvers solverToUse() {
    return Solvers.Z3;
  }

  @Before
  public void setUp() throws Exception {
    mgrv = new FormulaManagerView(factory, config, TestLogManager.getInstance());
    Solver solver = new Solver(mgrv, factory, config, TestLogManager.getInstance());

    afm = mgrv.getArrayFormulaManager();
    bfm = mgrv.getBooleanFormulaManager();
    ifm = mgrv.getIntegerFormulaManager();
    qfm = mgrv.getQuantifiedFormulaManager();

    rulesEngine = new RuleEngine(logger, solver);
    enp = new ExtractNewPreds(solver, rulesEngine);

    setupTestdata();
  }

  public void setupTestdata() {
    _0 = ifm.makeNumber(0);
    _1 = ifm.makeNumber(1);

    _i = mgrv.makeVariable(NumeralType.IntegerType, "i");
    _k = mgrv.makeVariable(NumeralType.IntegerType, "k");
    _al = mgrv.makeVariable(NumeralType.IntegerType, "al");
    _bl = mgrv.makeVariable(NumeralType.IntegerType, "bl");
    _n = mgrv.makeVariable(NumeralType.IntegerType, "n");
    _b = afm.makeArray("b", NumeralType.IntegerType, NumeralType.IntegerType);

    IntegerFormula _i0 = mgrv.makeVariable(NumeralType.IntegerType, "i0");
    IntegerFormula _i1 = mgrv.makeVariable(NumeralType.IntegerType, "i1");
    IntegerFormula _i2 = mgrv.makeVariable(NumeralType.IntegerType, "i2");
    IntegerFormula _al0 = mgrv.makeVariable(NumeralType.IntegerType, "al0");

    ArrayFormula<IntegerFormula, IntegerFormula> _a0 = afm.makeArray("a0", NumeralType.IntegerType, NumeralType.IntegerType);
    ArrayFormula<IntegerFormula, IntegerFormula> _a1 = afm.makeArray("a1", NumeralType.IntegerType, NumeralType.IntegerType);
    ArrayFormula<IntegerFormula, IntegerFormula> _b0 = afm.makeArray("b0", NumeralType.IntegerType, NumeralType.IntegerType);

    _i0_LESS_al0 = ifm.lessThan(_i0, _al0);
    _i1_plus_1 = ifm.add(_i1, _1);
    _i2_plus_1 = ifm.add(_i2, _1);
    _a0_at_i1 = afm.select(_a0, _i1);
    _a1_at_i2 = afm.select(_a1, _i2);
    _b0_at_i1 = afm.select(_b0, _i1);
    _b0_at_i0 = afm.select(_b0, _i0);
    _i1_EQUAL_0 = ifm.equal(_i1, _0);
    _b0_at_i1_NOTEQUAL_0 = ifm.equal(_b0_at_i1, _0);
    _i1_GEQ_al0 = ifm.greaterOrEquals(_i1, _al0);
    _a0_at_i1_EQUAL_b0_at_i1 = ifm.equal(_a0_at_i1, _b0_at_i1);
    _i0_EQUAL_i1_plus_1 = ifm.equal(_i0, _i1_plus_1);
    _b0_at_i0_EQUAL_0 = ifm.equal(_b0_at_i0, _0);
    _b0_at_i0_NOTEQUAL_0 = bfm.not(_b0_at_i0_EQUAL_0);
    _i2_EQUAL_0 = ifm.equal(_i2, _0);
    _b0_at_i2 = afm.select(_b0, _i2);
    _b0_at_i2_NOTEQUAL_0 = bfm.not(ifm.equal(_b0_at_i2, _0));
    _i2_GEQ_al0 = ifm.greaterOrEquals(_i2, _al0);
    _a1_at_i2_EQUAL_b0_at_i2 = ifm.equal(_a1_at_i2, _b0_at_i2);
    _i1_EQUAL_i2_plus_1 = ifm.equal(_i1, _i2_plus_1);

    _safeTrace = bfm.and(Lists.newArrayList(
        _i1_EQUAL_0,
        _b0_at_i1_NOTEQUAL_0,
        _i1_GEQ_al0,
        _a0_at_i1_EQUAL_b0_at_i1,
        _i0_EQUAL_i1_plus_1,
        _b0_at_i0_EQUAL_0));

    _errorTrace = bfm.and(Lists.newArrayList(
        _i2_EQUAL_0,
        _b0_at_i2_NOTEQUAL_0,
        _i2_GEQ_al0,
        _a1_at_i2_EQUAL_b0_at_i2,
        _i1_EQUAL_i2_plus_1,
        _b0_at_i1_NOTEQUAL_0,
        _i1_GEQ_al0,
        _a0_at_i1_EQUAL_b0_at_i1,
        _i0_EQUAL_i1_plus_1,
        _b0_at_i0_NOTEQUAL_0,
        _i0_LESS_al0));
  }

  private BooleanFormula rangePredicate(boolean pForall, IntegerFormula pLowerLimit, IntegerFormula pUpperLimit) {
    IntegerFormula _x = ifm.makeVariable("x");
    BooleanFormula _range = bfm.and(Lists.newArrayList(
        bfm.not(ifm.equal(afm.select(_b, _x), ifm.makeNumber(0))),
        ifm.greaterOrEquals(_x, pLowerLimit),
        ifm.lessOrEquals(_x, pUpperLimit)));

    if (pForall) {
      return qfm.forall(Lists.newArrayList(_x), _range);
    } else {
      return qfm.exists(Lists.newArrayList(_x), _range);
    }
  }

  public void testOnTraceX() throws SolverException, InterruptedException {
    BooleanFormula _safeWp1 = bfm.and(Lists.newArrayList(
        bfm.not(ifm.equal(afm.select(_b, _i), _0)),        // b[i] != 0
        ifm.lessOrEquals(ifm.add(_i, _1), _0),    // i+1 <= 0
        ifm.equal(afm.select(_b, ifm.add(_i, _1)), _0)));  // b[i+1] == 0

    List<BooleanFormula> result = enp.extractNewPreds(_safeWp1);
    assertThat(result).isNotEmpty();
  }

  @Test
  public void testOnSafeWp3() throws SolverException, InterruptedException {
    BooleanFormula wpSafe = bfm.and(Lists.newArrayList(
        bfm.not(ifm.equal(afm.select(_b, _i), _0)),     // b[i] != 0
        ifm.lessOrEquals(ifm.add(_i, _1), _al),         // i+1 <= al
        ifm.equal(afm.select(_b, ifm.add(_i, _1)), _0)  // b[i+1] = 0
        ));

    List<BooleanFormula> result = enp.extractNewPreds(wpSafe);

    assertThat(result).containsAllIn(
        Lists.newArrayList(
            rangePredicate(false, _i, ifm.add(_i, _1)),
            rangePredicate(false, _i, _al)));
  }

  @Test
  public void testOnErrorWp1() throws SolverException, InterruptedException {
    BooleanFormula wpError = bfm.and(Lists.newArrayList(
        ifm.greaterOrEquals(ifm.add(_i, _1), _al),                // i+1 >= al
        bfm.not(ifm.equal(afm.select(_b, ifm.add(_i, _1)), _0)),  // b[i+1] != 0
        ifm.lessThan(_i, _al),                                    // i < al
        bfm.not(ifm.equal(afm.select(_b, _i), _0))                // b[i] != 0
        ));

    List<BooleanFormula> result = enp.extractNewPreds(wpError);

    assertThat(result).containsAllIn(
        Lists.newArrayList(
            rangePredicate(true, _i, ifm.add(_i, _1)),
            rangePredicate(true, _i, _al)));
  }

  @Test
  public void testOnErrorWp2() throws SolverException, InterruptedException {
    BooleanFormula wpError = bfm.and(Lists.newArrayList(
        ifm.greaterOrEquals(_1, _al),               // 1 >= al
        bfm.not(ifm.equal(afm.select(_b, _1), _0)), // b[1] != 0
        ifm.lessThan(_0, _al),                      // 0 < al
        bfm.not(ifm.equal(afm.select(_b, _0), _0))  // b[0] != 0
        ));

    List<BooleanFormula> result = enp.extractNewPreds(wpError);

    assertThat(result).containsAllIn(
        Lists.newArrayList(
            ifm.equal(_al,  _1),
            rangePredicate(true, _0, _1),
            rangePredicate(true, _0, _al)));
  }

  @Test
  public void testOnSafeWp4() throws SolverException, InterruptedException {
    BooleanFormula wpSafe = bfm.and(Lists.newArrayList(
        bfm.not(ifm.equal(afm.select(_b, _0), _0)),     // b[0] != 0
        ifm.lessOrEquals(_1, _al),         // 1 <= al
        ifm.equal(afm.select(_b, _1), _0)  // b[1] = 0
        ));

    List<BooleanFormula> result = enp.extractNewPreds(wpSafe);

    assertThat(result).containsAllIn(
        Lists.newArrayList(
            rangePredicate(false, _0, _1),
            rangePredicate(false, _0, _al)));
  }

  @Test
  public void testOnArrayPreds1() throws SolverException, InterruptedException {
    BooleanFormula wpSafe = bfm.and(Lists.newArrayList(
        bfm.not(ifm.equal(afm.select(_b, _0), _al)), // b[0] != al
        bfm.not(ifm.lessOrEquals(_i, _0))            // not i <= 0
        ));

    List<BooleanFormula> result = enp.extractNewPreds(wpSafe);
    assertThat(result).isNotEmpty();
  }

  @Test
  public void testOnArrayPreds2() throws SolverException, InterruptedException {
    // (not (<= n i|)),
    // (not (= (select M i) e))

    BooleanFormula wpSafe = bfm.and(Lists.newArrayList(
        bfm.not(ifm.equal(afm.select(_b, _i), _al)),
        bfm.not(ifm.lessOrEquals(_n, _i))
        ));

    List<BooleanFormula> result = enp.extractNewPreds(wpSafe);
    assertThat(result).isNotEmpty();
  }

  @Test
  public void testOnArrayPreds3() throws SolverException, InterruptedException {
    // i = 0
    // (not (= (select b i) 0))
    // (< i n)
    // (= k (+ i 1))
    // (= (select b k) 0)

    List<BooleanFormula> preds = Lists.newArrayList(
        ifm.equal(_i, _0),
        bfm.not(ifm.equal(afm.select(_b, _i), _0)),
        ifm.lessThan(_i, _n),
        ifm.equal(_k, ifm.add(_i, _1)),
        ifm.equal(afm.select(_b, _k), _0)
        );

    List<BooleanFormula> result = enp.extractNewPreds(preds);
    assertThat(result).isNotEmpty();
  }

  @Test
  public void testOnArrayPreds4() throws SolverException, InterruptedException {
    // (and (= (select |copy::b@1| (+ 1 |copy::i@2|)) 0)
    // (not (= (select |copy::b@1| |copy::i@2|) 0))
    // (not (>= |copy::i@2| al@1)))

    List<BooleanFormula> preds = Lists.newArrayList(
        ifm.equal(afm.select(_b, ifm.add(ifm.makeNumber(1), _i)), _0),
        bfm.not(ifm.equal(afm.select(_b, _i), _0)),
        bfm.not(ifm.greaterOrEquals(_i, _al))
        );

    List<BooleanFormula> result = enp.extractNewPreds(preds);
    assertThat(result).isNotEmpty();
  }

  @Test
  public void testOnArrayPreds5() throws SolverException, InterruptedException {
    // (<= (+ 0 1) al@1)
    // (not (= (select |copy::b@1| 0) 0))
    // (= (select |copy::b@1| 1) 0)

    List<BooleanFormula> preds = Lists.newArrayList(
        bfm.not(ifm.equal(afm.select(_b, _0), _0)),
        ifm.equal(afm.select(_b, _0), _0),
        ifm.lessOrEquals(ifm.add(ifm.makeNumber(0), ifm.makeNumber(1)), _al)
        );

    List<BooleanFormula> result = enp.extractNewPreds(preds);
    assertThat(result).isNotEmpty();
  }

  @Test
  public void testLinCombPreds() throws SolverException, InterruptedException {
    BooleanFormula wpSafe = bfm.and(Lists.newArrayList(
        ifm.lessThan(_0, _bl),
        ifm.greaterOrEquals(_0, _al)
        ));

    List<BooleanFormula> result = enp.extractNewPreds(wpSafe);
    assertThat(result).contains(ifm.lessThan(_al, _bl));
  }

}
