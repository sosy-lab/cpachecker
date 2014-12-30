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

import java.util.ArrayList;
import java.util.Set;

import org.junit.Test;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.precondition.segkro.rules.SubstitutionRule;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ArrayFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType.NumeralType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;

import com.google.common.collect.Lists;


public class SubstitutionRuleTest0 extends AbstractRuleTest0 {

  private SubstitutionRule sr;

  private IntegerFormula _0;
  private IntegerFormula _1;
  private IntegerFormula _i;
  private ArrayFormula<IntegerFormula, IntegerFormula> _b;

  private BooleanFormula _x_EQ_i_plus_1;
  private IntegerFormula _x;
  private BooleanFormula _b_at_x_NOTEQ_0;
  private IntegerFormula _al;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    sr = new SubstitutionRule(solver, matcher);

    _0 = ifm.makeNumber(0);
    _1 = ifm.makeNumber(1);
    _i = ifm.makeVariable("i");
    _x = ifm.makeVariable("x");
    _al = ifm.makeVariable("al");
    _b = afm.makeArray("b", NumeralType.IntegerType, NumeralType.IntegerType);

    _x_EQ_i_plus_1 = ifm.equal(_x, ifm.add(_i, _1));
    _b_at_x_NOTEQ_0 = bfm.not(ifm.equal(afm.select(_b, _x), _0));
  }

  @Test
  public void testConclusion1() throws SolverException, InterruptedException {
    //    (not (= (select b x) 0))
    //    (= x (+ i 1))
    //    -------------
    //    (not (= (select b (+ i 1)) 0))

    Set<BooleanFormula> result = sr.applyWithInputRelatingPremises(
        Lists.newArrayList(
            _b_at_x_NOTEQ_0,
            _x_EQ_i_plus_1));
    assertThat(result).isNotEmpty();
  }

  @Test
  public void testConclusion2() throws SolverException, InterruptedException {

    //    (= (select b (+ i 1)) 0)
    //    (= (select b (+ i 1)) 0)
    //     ----- should not result in -----
    //    (= 0 0)
    //    (= (select b 0) 0)

    Set<BooleanFormula> result = sr.applyWithInputRelatingPremises(
        Lists.newArrayList(
            ifm.equal(afm.select(_b, ifm.add(_i, _1)), _0),
            ifm.equal(afm.select(_b, ifm.add(_i, _1)), _0)));

    assertThat(result).doesNotContain(ifm.equal(_0, _0));
    assertThat(result).doesNotContain(ifm.equal(afm.select(_b, _0), _0));
    assertThat(result).isEmpty(); // would be sufficient
  }

  @Test
  public void testConclusion3() throws SolverException, InterruptedException {

    //    (= (select b (+ i 1)) 0)
    //    (= al (+ i 1))
    //     ----- should result in -----
    //    (= (select b al) 0)

    ArrayList<BooleanFormula> input = Lists.newArrayList(
        ifm.equal(afm.select(_b, ifm.add(_i, _1)), _0),
        ifm.equal(_al, ifm.add(_i, _1))
        );

    Set<BooleanFormula> result = sr.applyWithInputRelatingPremises(input);

    assertThat(result).isNotEmpty();
    assertThat(result).contains(ifm.equal(afm.select(_b, _al), _0));
  }

  @Test
  public void testConclusion4() throws SolverException, InterruptedException {

    //    (= (select b 1) 0)
    //    (<= 1 al)
    //     ----- should result in -----
    //    (= (select b al) 0)

    ArrayList<BooleanFormula> input = Lists.newArrayList(
        ifm.equal(afm.select(_b, _1), _0),
        ifm.lessOrEquals(_1, _al)
        );

    Set<BooleanFormula> result = sr.applyWithInputRelatingPremises(input);

    assertThat(result).isNotEmpty();
    assertThat(result).contains(ifm.equal(afm.select(_b, _al), _0));
  }

  @Test
  public void testConclusion5() throws SolverException, InterruptedException {

    //    (= (select b 1) 0)
    //    (<= al 1)
    //     ----- should result in -----
    //    (= (select b al) 0)

    ArrayList<BooleanFormula> input = Lists.newArrayList(
        ifm.equal(afm.select(_b, _1), _0),
        ifm.lessOrEquals(_al, _1)
        );

    Set<BooleanFormula> result = sr.applyWithInputRelatingPremises(input);

    assertThat(result).isNotEmpty();
    assertThat(result).contains(ifm.equal(afm.select(_b, _al), _0));
  }

  @Test
  public void testConclusion6() throws SolverException, InterruptedException {

    //    (= (select b 0) 0)
    //    (= (select b 0) 0)
    //     ----- should result in -----
    //    EMPTY

    ArrayList<BooleanFormula> input = Lists.newArrayList(
        ifm.equal(afm.select(_b, _0), _0),
        ifm.equal(afm.select(_b, _0), _0)
        );

    Set<BooleanFormula> result = sr.applyWithInputRelatingPremises(input);

    assertThat(result).isEmpty();
  }

  @Test
  public void testConclusion7() throws SolverException, InterruptedException {

    //  (< i al)
    //  (= i 0))
    //     ----- should result in -----
    //  0 < al

    ArrayList<BooleanFormula> input = Lists.newArrayList(
        ifm.lessThan(_i, _al),
        ifm.equal(_i, _0)
        );

    Set<BooleanFormula> result = sr.applyWithInputRelatingPremises(input);

    assertThat(result).contains(ifm.lessThan(_0, _al));
  }

}
