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

import java.io.IOException;
import java.util.Set;

import org.junit.Test;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.precondition.segkro.interfaces.Premise;
import org.sosy_lab.cpachecker.util.precondition.segkro.rules.ExistentialRule;
import org.sosy_lab.cpachecker.util.precondition.segkro.rules.PatternBasedPremise;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ArrayFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType.NumeralType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstMatchResult;

import com.google.common.collect.Lists;


public class ExistentialRuleTest0 extends AbstractRuleTest0 {

  private ExistentialRule er;

  private IntegerFormula _0;
  private IntegerFormula _1;
  private IntegerFormula _i;
  private BooleanFormula _b_at_i_plus_1_EQ_0;
  private BooleanFormula _b_at_i_plus_1_NOTEQ_0;
  private BooleanFormula _0_EQ_b_at_i_plus_1;
  private ArrayFormula<IntegerFormula, IntegerFormula> _b;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    er = new ExistentialRule(solver, matcher);
    setupTestData();
  }

  private void setupTestData() {
    _0 = ifm.makeNumber(0);
    _1 = ifm.makeNumber(1);
    _b = afm.makeArray("b", NumeralType.IntegerType, NumeralType.IntegerType);
    _i = ifm.makeVariable("i");

    _0_EQ_b_at_i_plus_1 = ifm.equal(_0, afm.select(_b, ifm.add(_i, _1)));
    _b_at_i_plus_1_EQ_0 = ifm.equal(afm.select(_b, ifm.add(_i, _1)), _0);
    _b_at_i_plus_1_NOTEQ_0 = bfm.not(ifm.equal(afm.select(_b, ifm.add(_i, _1)), _0));
  }

  @Test
  public void testPremise2() throws SolverException, InterruptedException {

    Premise p1 = er.getPremises().get(1);
    assertThat(p1).isInstanceOf(PatternBasedPremise.class);
    PatternBasedPremise pp1 = (PatternBasedPremise) p1;

    SmtAstMatchResult r1 = matcher.perform(pp1.getPatternSelection(), _0_EQ_b_at_i_plus_1);
    assertThat(r1.matches()).isTrue();
    assertThat(r1.getBoundVariables().size()).isGreaterThan(1);
  }

  @Test
  public void testPremise2a() throws SolverException, InterruptedException, IOException {

    Premise p1 = er.getPremises().get(1);
    assertThat(p1).isInstanceOf(PatternBasedPremise.class);
    PatternBasedPremise pp1 = (PatternBasedPremise) p1;

    SmtAstMatchResult r2 = matcher.perform(pp1.getPatternSelection(), _b_at_i_plus_1_EQ_0);
    assertThat(r2.matches()).isTrue();

    assertThat(r2.getBoundVariables().size()).isGreaterThan(1);
  }

  @Test
  public void testPremise1() throws SolverException, InterruptedException {

    Premise p2 = er.getPremises().get(0);
    assertThat(p2).isInstanceOf(PatternBasedPremise.class);
    PatternBasedPremise pp2 = (PatternBasedPremise) p2;

    {
      SmtAstMatchResult result = matcher.perform(pp2.getPatternSelection(), _b_at_i_plus_1_EQ_0);
      assertThat(result.matches()).isFalse();
    }

    {
      SmtAstMatchResult result = matcher.perform(pp2.getPatternSelection(), _b_at_i_plus_1_NOTEQ_0);
      assertThat(result.matches()).isTrue();
    }
  }

  @Test
  public void testConclusion1() throws SolverException, InterruptedException {
    Set<BooleanFormula> result = er.applyWithInputRelatingPremises(
        Lists.newArrayList(
            bfm.not(ifm.equal(afm.select(_b, _i), _0)),
            ifm.equal(afm.select(_b, ifm.add(_i, _1)), _0)
          ));

    assertThat(result).contains(rangePredicate(false, _i,  ifm.add(_i, _1)));
  }

  @Test
  public void testConclusion3() throws SolverException, InterruptedException {
    Set<BooleanFormula> result = er.applyWithInputRelatingPremises(
        Lists.newArrayList(
            bfm.not(ifm.equal(afm.select(_b, _0), _0)),
            ifm.equal(afm.select(_b, _0), _0)
          ));

    assertThat(result).isEmpty();;
  }

  @Test
  public void testConclusion4() throws SolverException, InterruptedException {
    // (= (select |copy::b@1| (+ 1 |copy::i@2|)) 0)
    // (not (= (select |copy::b@1| |copy::i@2|) 0))

    Set<BooleanFormula> result = er.applyWithInputRelatingPremises(
        Lists.newArrayList(
            bfm.not(ifm.equal(afm.select(_b, _i), _0)),
            ifm.equal(afm.select(_b, ifm.add(_1, _i)), _0)
          ));

    assertThat(result).contains(rangePredicate(false, _i,  ifm.add(_1, _i)));
  }

  @Test
  public void testConclusion2() throws SolverException, InterruptedException {
    // Important !!!

    Set<BooleanFormula> result = er.applyWithInputRelatingPremises(
        Lists.newArrayList(
            bfm.not(ifm.equal(afm.select(_b, _i), _0)),
            ifm.equal(afm.select(_b, ifm.add(_1, _i)), _0)));

    assertThat(result).contains(rangePredicate(false, _i,  ifm.add(_1, _i)));
  }

  @Test
  public void testConclusion5() throws SolverException, InterruptedException {
    // Important !!!

    Set<BooleanFormula> result = er.applyWithInputRelatingPremises(
        Lists.newArrayList(
            bfm.not(ifm.equal(afm.select(_b, _0), _0)),
            ifm.equal(_0, afm.select(_b, _1))));

    assertThat(result).contains(rangePredicate(false, _0,  _1));
  }

}
