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
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.precondition.segkro.rules.ExtendLeftRule;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ArrayFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType.NumeralType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;

import com.google.common.collect.Lists;


public class ExtendLeftRuleTest0 extends AbstractRuleTest0 {

  private ExtendLeftRule elr;

  private IntegerFormula _0;
  private IntegerFormula _i;
  private IntegerFormula _j;
  private IntegerFormula _x;

  private ArrayFormula<IntegerFormula, IntegerFormula> _b;

  private BooleanFormula _b_at_x_NOTEQ_0;
  private IntegerFormula _k;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    elr = new ExtendLeftRule(mgr, mgrv, solver, matcher);

    setupTestData();
  }

  private void setupTestData() {
    _0 = ifm.makeNumber(0);
    _i = ifm.makeVariable("i");
    _j = ifm.makeVariable("j");
    _x = ifm.makeVariable("x");
    _k = ifm.makeVariable("k");
    _b = afm.makeArray("b", NumeralType.IntegerType, NumeralType.IntegerType);

    _b_at_x_NOTEQ_0 = bfm.not(ifm.equal(afm.select(_b, _x), _0));
  }

  @Test
  public void testConclusion1() throws SolverException, InterruptedException {

    BooleanFormula _x_range = bfm.and(
        ifm.greaterOrEquals(_x, _i),
        ifm.lessOrEquals(_x, _j));

    BooleanFormula _right_ext = ifm.lessOrEquals(_j, _k);

    BooleanFormula _EXISTS_x = qmgr.exists(
        Lists.newArrayList(_x),
        bfm.and(Lists.newArrayList(
            _b_at_x_NOTEQ_0,
            _x_range)));

    Set<BooleanFormula> result = elr.applyWithInputRelatingPremises(
        Lists.newArrayList(
            _EXISTS_x,
            _right_ext));

    assertThat(result).isEmpty();
  }

  @Test
  public void testConclusion2() throws SolverException, InterruptedException {

    BooleanFormula _x_range = bfm.and(
        ifm.greaterOrEquals(_x, _i),
        ifm.lessOrEquals(_x, _j));

    BooleanFormula _right_ext = ifm.lessOrEquals(_k, _j);

    BooleanFormula _EXISTS_x = qmgr.exists(
        Lists.newArrayList(_x),
        bfm.and(Lists.newArrayList(
            _b_at_x_NOTEQ_0,
            _x_range)));

    Set<BooleanFormula> result = elr.applyWithInputRelatingPremises(
        Lists.newArrayList(
            _EXISTS_x,
            _right_ext));

    assertThat(result).isEmpty();
  }

  @Test
  public void testConclusion3() throws SolverException, InterruptedException {

    BooleanFormula _x_range = bfm.and(
        ifm.greaterOrEquals(_x, _i),
        ifm.lessOrEquals(_x, _j));

    BooleanFormula _right_ext = ifm.lessOrEquals(_k, _i);

    BooleanFormula _EXISTS_x = qmgr.exists(
        Lists.newArrayList(_x),
        bfm.and(Lists.newArrayList(
            _b_at_x_NOTEQ_0,
            _x_range)));

    Set<BooleanFormula> result = elr.applyWithInputRelatingPremises(
        Lists.newArrayList(
            _EXISTS_x,
            _right_ext));

    assertThat(result).isNotEmpty();
  }



}
