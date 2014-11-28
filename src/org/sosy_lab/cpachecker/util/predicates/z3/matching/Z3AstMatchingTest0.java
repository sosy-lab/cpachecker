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
package org.sosy_lab.cpachecker.util.predicates.z3.matching;

import static com.google.common.truth.Truth.assertThat;
import static org.sosy_lab.cpachecker.util.predicates.z3.matching.SmtAstPatternBuilder.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.util.predicates.SolverBasedTest;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType.NumeralType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;

import com.google.common.collect.Lists;


public class Z3AstMatchingTest0 extends SolverBasedTest {

  private IntegerFormula _c1;
  private IntegerFormula _c2;
  private IntegerFormula _e1;
  private IntegerFormula _e2;
  private IntegerFormula _eX;
  private IntegerFormula _0;
  private BooleanFormula _c1_times_ex_plus_e1_GEQ_0;
  private BooleanFormula _minus_c2_times_ex_plus_e2_GEQ_0;

  @Before
  public void setUp() throws Exception {
    _c1 = fmgr.makeVariable(NumeralType.IntegerType, "c1");
    _c2 = fmgr.makeVariable(NumeralType.IntegerType, "c2");
    _e1 = fmgr.makeVariable(NumeralType.IntegerType, "e1");
    _e2 = fmgr.makeVariable(NumeralType.IntegerType, "e2");
    _eX = fmgr.makeVariable(NumeralType.IntegerType, "eX");
    _0 = ifm.makeNumber(0);

    // Formulas for the premise
    _c1_times_ex_plus_e1_GEQ_0
      = ifm.greaterOrEquals(
          ifm.add(
              ifm.multiply(_c1, _eX),
              _e1),
          _0);
    _minus_c2_times_ex_plus_e2_GEQ_0
      = ifm.greaterOrEquals(
          ifm.add(
              ifm.multiply(
                  ifm.subtract(_0, _c2),
                  _eX),
              _e2),
          _0);
  }

  @Test
  public void testAstMatchingElim() {
    SmtAstPattern patternPremise1 =
        match("+",
            match("*",
                matchAnyBind("c1"),
                matchAnyBind("eX")),
            matchAnyBind("e1"));

    SmtAstPattern patternPremise2 =
        match("+",
            match("*",
                match("-",
                    match(0),
                    matchAnyBind("c2")),
                matchAnyBind("eX")),
            matchAnyBind("e2"));

    // The Strings c1, eX, e2, c2 represent bound formulas
    // that must reference to the same formula!!

    // Consider commutativity of different functions!

    // At this point: We have defined the match-patterns.
    // Now it is time to do the matching.

    Formula f = _c1_times_ex_plus_e1_GEQ_0;

    List<SmtAstPattern> matchers = Lists.newArrayList(patternPremise1, patternPremise2);

    SmtAstMatcher matcher = new Z3AstMatcher();
    SmtAstMatchResult result = matcher.perform(matchers, f);
    // A (sub-)formula represents the root of the matching AST
    Set<Formula> f1 = result.getMatchingFormula(patternPremise1);
    Set<Formula> f2 = result.getMatchingFormula(patternPremise2);
    // Every bound formula can be accessed in the result
    Set<Formula> eX = result.getBoundFormula("eX");

    assertThat(eX).contains(_eX);
    assertThat(f1).contains(_c1_times_ex_plus_e1_GEQ_0);
    assertThat(f2).contains(_minus_c2_times_ex_plus_e2_GEQ_0);
  }

  @Test
  public void testAstMatchingSubstitute() {
    SmtAstPattern patternPremise1 = match(  // The assumption that the parent function is a logical AND
        matchIfNot("=", matchAny()), // (= c 1) should not be matched
        match(matchNullaryBind("x"))); // (f c) should be matched
    // TODO: What is about (f a b c d)

    SmtAstPattern patternPremise2 =
        match("=",
            matchNullaryBind("x"),
            matchAnyBind("e"));

    List<SmtAstPattern> matchers = Lists.newArrayList(patternPremise1, patternPremise2);

    // Create the formula >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    IntegerFormula _i1 = fmgr.makeVariable(NumeralType.IntegerType, "i@1");
    IntegerFormula _j1= fmgr.makeVariable(NumeralType.IntegerType, "j@1");
    IntegerFormula _j2 = fmgr.makeVariable(NumeralType.IntegerType, "j@2");
    IntegerFormula _a1 = fmgr.makeVariable(NumeralType.IntegerType, "a@1");

    IntegerFormula _1 = ifm.makeNumber(1);
    IntegerFormula _minus1 = ifm.makeNumber(-1);

    IntegerFormula _1_plus_a1 = ifm.add(_1, _a1);
    BooleanFormula _not_j1_EQUALS_minus1 = bfm.not(ifm.equal(_j1, _minus1));
    BooleanFormula _i1_EQUALS_1_plus_a1 = ifm.equal(_i1, _1_plus_a1);

    IntegerFormula _j12_plus_a1 = ifm.add(_j2, _a1);
    BooleanFormula _j1_EQUALS_j2_plus_a1 = ifm.equal(_j1, _j12_plus_a1);

    BooleanFormula f = bfm.and(Lists.newArrayList(
            _i1_EQUALS_1_plus_a1,
            _not_j1_EQUALS_minus1,
            _j1_EQUALS_j2_plus_a1));
    // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

    // TODO: There might be multiple valid bindings to a variable ("models")

    SmtAstMatcher matcher = new Z3AstMatcher();
    SmtAstMatchResult result = matcher.perform(matchers, f);
    // A (sub-)formula represents the root of the matching AST
    Set<Formula> f1 = result.getMatchingFormula(patternPremise1);
    Set<Formula> f2 = result.getMatchingFormula(patternPremise2);

    assertThat(f1).containsNoneIn(Collections.singleton(_i1));
    assertThat(f1).contains(_j1);

    assertThat(f2).contains(_i1_EQUALS_1_plus_a1);
    assertThat(f2).contains(_j1_EQUALS_j2_plus_a1);

    // Every bound formula can be accessed in the result
    Set<Formula> x = result.getBoundFormula("x");
    assertThat(x).contains(_j1);
    assertThat(x).containsNoneIn(Collections.singleton(_i1));

  }


}
