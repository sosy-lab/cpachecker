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

import org.junit.Test;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.cpachecker.util.precondition.segkro.rules.LinkRule;
import org.sosy_lab.solver.api.ArrayFormula;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.FormulaType.NumeralType;
import org.sosy_lab.solver.api.NumeralFormula.IntegerFormula;

import com.google.common.collect.Lists;


public class LinkRuleTest0 extends AbstractRuleTest0 {

  private LinkRule lr;

  private IntegerFormula _0;
  private IntegerFormula _1;
  private IntegerFormula _i;
  private ArrayFormula<IntegerFormula, IntegerFormula> _b;

  private IntegerFormula _x;
  private IntegerFormula _i_plus_1;
  private BooleanFormula _b_at_x_NOTEQ_0;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    lr = new LinkRule(solver, matcher);

    _0 = ifm.makeNumber(0);
    _1 = ifm.makeNumber(1);
    _i = ifm.makeVariable("i");
    _x = ifm.makeVariable("x");
    _b = afm.makeArray("b", NumeralType.IntegerType, NumeralType.IntegerType);

    _i_plus_1 = ifm.add(_i, _1);

    _b_at_x_NOTEQ_0 = bfm.not(ifm.equal(afm.select(_b, _x), _0));
  }

  @Test
  public void testConclusion1() throws SolverException, InterruptedException {

    // forall x in [i+1] . b[x] != 0
    // forall x in [i] .   b[x] != 0

    final BooleanFormula _FORALL_i_plus_1 = qfm.forall(_x, _i_plus_1, _i_plus_1, _b_at_x_NOTEQ_0);
    final BooleanFormula _FORALL_i = qfm.forall(_x, _i, _i, _b_at_x_NOTEQ_0);

    final Set<BooleanFormula> result = lr.applyWithInputRelatingPremises(
        Lists.newArrayList(
            _FORALL_i,
            _FORALL_i_plus_1));

    assertThat(result).isNotEmpty();
  }

  @Test
  public void testConclusion3() throws SolverException, InterruptedException {

    // forall x in [0] . b[x] != 0
    // forall x in [1] . b[x] != 0

    final BooleanFormula _FORALL_i = qfm.forall(_x, _0, _0, _b_at_x_NOTEQ_0);
    final BooleanFormula _FORALL_i_plus = qfm.forall(_x, _1, _1, _b_at_x_NOTEQ_0);

    final Set<BooleanFormula> result = lr.applyWithInputRelatingPremises(
        Lists.newArrayList(
            _FORALL_i, _FORALL_i_plus));

    assertThat(result).isNotEmpty();
  }


  @Test
  public void testConclusion2() throws SolverException, InterruptedException {
    //  (forall ((x Int))
    //      (and (not (= (select |copy::b@1| x) 0))
    //           (>= x (+ (- 3) al@1))
    //           (<= x |copy::i@4|)))

    //  (forall ((x Int))
    //      (and (not (= (select |copy::b@1| x) 0))
    //           (>= x (+ 1 |copy::i@4|))
    //           (<= x (+ 1 |copy::i@4|))))
    // ------- should not result in ------
    //  (exists ((x Int))
    //      (and (not (= (select |copy::b@1| x) 0))
    //           (>= x (+ (- 3) al@1))
    //           (<= x (+ 1 |copy::i@4|))))


  }


}
