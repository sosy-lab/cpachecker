/*
\ *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.util.predicates.matching;

import static com.google.common.truth.Truth.assertThat;
import static org.sosy_lab.cpachecker.util.predicates.matching.SmtAstPatternBuilder.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.TestLogManager;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.solver.FormulaManagerFactory.Solvers;
import org.sosy_lab.solver.api.ArrayFormula;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.FormulaType.NumeralType;
import org.sosy_lab.solver.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.solver.test.SolverBasedTest0;

import com.google.common.collect.Lists;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressWarnings("unused")
@SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
public class AstMatchingTest0 extends SolverBasedTest0 {

  private IntegerFormula _0;
  private IntegerFormula _al;
  private IntegerFormula _bl;
  private IntegerFormula _c1;
  private IntegerFormula _c2;
  private IntegerFormula _e1;
  private IntegerFormula _e2;
  private IntegerFormula _eX;
  private BooleanFormula _0_GEQ_al;
  private BooleanFormula _0_LESSTHAN_bl;
  private BooleanFormula _c1_times_ex_plus_e1_GEQ_0;
  private BooleanFormula _minus_c2_times_ex_plus_e2_GEQ_0;

  private Solver solver;
  private SmtAstMatcher matcher;

  private IntegerFormula _i1;
  private IntegerFormula _j1;
  private IntegerFormula _j2;
  private IntegerFormula _a1;
  private IntegerFormula _1;
  private IntegerFormula _minus1;
  private IntegerFormula _1_plus_a1;
  private BooleanFormula _not_j1_EQUALS_minus1;
  private BooleanFormula _i1_EQUALS_1_plus_a1;
  private IntegerFormula _j12_plus_a1;
  private BooleanFormula _j1_EQUALS_j2_plus_a1;
  private BooleanFormula _f_and_of_foo;
  private SmtAstPatternSelection elimPremisePattern1;
  private SmtAstPatternSelection elimPremisePattern2;
  private IntegerFormula _i;
  private IntegerFormula _x;
  private ArrayFormula<IntegerFormula, IntegerFormula> _b;
  private BooleanFormula _b_at_i_NOTEQ_0;
  private BooleanFormula _i_plus_1_LESSEQ_al;
  private BooleanFormula _b_at_i_plus_1_EQ_0;
  private BooleanFormula _0_EQ_b_at_i_plus_1_;
  private BooleanFormula _b_at_i_plus_1_NOTEQ_0;
  private IntegerFormula _i_plus_1;
  private BooleanFormula _b_at_i_EQ_0;
  private IntegerFormula _b_at_i;

  @Override
  protected Solvers solverToUse() {
    return Solvers.Z3;
  }

  @Before
  public void setupEnvironment() throws Exception {
    setupMatcher();
    setupTestFormulas();
    setupTestPatterns();
  }

  @After
  public void closeEnvironment() throws Exception {
    solver.close();
  }

  public void setupMatcher() throws InvalidConfigurationException {
    solver = new Solver(factory, config, TestLogManager.getInstance());
    matcher = solver.getSmtAstMatcher();
  }

  public void setupTestFormulas() {
    _0 = imgr.makeNumber(0);
    _1 = imgr.makeNumber(1);
    _minus1 = imgr.makeNumber(-1);

    _al = imgr.makeVariable("al");
    _bl = imgr.makeVariable("bl");
    _c1 = imgr.makeVariable("c1");
    _c2 = imgr.makeVariable("c2");
    _e1 = imgr.makeVariable("e1");
    _e2 = imgr.makeVariable("e2");
    _eX = imgr.makeVariable("eX");

    _i1 = imgr.makeVariable("i@1");
    _j1 = imgr.makeVariable("j@1");
    _j2 = imgr.makeVariable("j@2");
    _a1 = imgr.makeVariable("a@1");

    _c1_times_ex_plus_e1_GEQ_0
      = imgr.greaterOrEquals(
          imgr.add(
              imgr.multiply(_c1, _eX),
              _e1),
          _0);

    _minus_c2_times_ex_plus_e2_GEQ_0
      = imgr.greaterOrEquals(
          imgr.add(
              imgr.multiply(
                  imgr.subtract(_0, _c2),
                  _eX),
              _e2),
          _0);

    _0_LESSTHAN_bl = imgr.lessThan(_0, _bl);
    _0_GEQ_al = imgr.greaterOrEquals(_0, _al);

    _1_plus_a1 = imgr.add(_1, _a1);
    _not_j1_EQUALS_minus1 = bmgr.not(imgr.equal(_j1, _minus1));
    _i1_EQUALS_1_plus_a1 = imgr.equal(_i1, _1_plus_a1);

    _j12_plus_a1 = imgr.add(_j2, _a1);
    _j1_EQUALS_j2_plus_a1 = imgr.equal(_j1, _j12_plus_a1);

    _f_and_of_foo = bmgr.and(Lists.newArrayList(
            _i1_EQUALS_1_plus_a1,
            _not_j1_EQUALS_minus1,
            _j1_EQUALS_j2_plus_a1));

    _i = imgr.makeVariable("i");
    _b = amgr.makeArray("b", NumeralType.IntegerType, NumeralType.IntegerType);
    _b_at_i_NOTEQ_0 = bmgr.not(imgr.equal(amgr.select(_b, _i), _0));
    _i_plus_1_LESSEQ_al = imgr.lessOrEquals(imgr.add(_i, _1), _0);
    _b_at_i_plus_1_EQ_0 = imgr.equal(amgr.select(_b, imgr.add(_i, _1)), _0);
    _0_EQ_b_at_i_plus_1_ = imgr.equal(_0, amgr.select(_b, imgr.add(_i, _1)));
  }

  public void setupTestPatterns() {
    elimPremisePattern1 =
        withDefaultBinding("c1", _0,
          or(
              match(">=",
                  match("+",
                      match("*",
                          matchNullaryBind("c1"),
                          matchAnyWithAnyArgsBind("eX")),
                      matchAnyWithAnyArgsBind("e1")),
                  matchNullary("0")),
              match(">=",
                  matchAnyWithAnyArgsBind("e1"),
                  matchNullary("0"))));

    elimPremisePattern2 =
        withDefaultBinding("c2", _0,
          or(
              match(">=",
                  match("+",
                      match("*",
                          match("-",
                              matchNullary("0"),
                              matchNullaryBind("c2")),
                          matchAnyWithAnyArgsBind("eX")),
                      matchAnyWithAnyArgsBind("e2")),
                  matchNullary("0")),
              match(">=",
                  matchAnyWithAnyArgsBind("e2"),
                  matchNullary("0"))));

  }

  @Test
  public void testLinearCombi1() {
    final SmtAstMatchResult result1 = matcher.perform(elimPremisePattern1, _0_GEQ_al);
    final SmtAstMatchResult result2 = matcher.perform(elimPremisePattern1, _0_LESSTHAN_bl);

    assertThat(result1.matches()).isFalse();
    assertThat(result2.matches()).isFalse();
  }

  @Test
  public void testLinearCombi2() {
    final SmtAstMatchResult result1 = matcher.perform(elimPremisePattern1, _minus_c2_times_ex_plus_e2_GEQ_0);
    assertThat(result1.matches()).isTrue();
  }

  @Test
  public void testLinearCombi5() {
    final SmtAstMatchResult result1 = matcher.perform(elimPremisePattern2, _c1_times_ex_plus_e1_GEQ_0);
    assertThat(result1.matches()).isTrue();
  }

  @Test
  public void testLinearCombi3() {
    final SmtAstMatchResult result2 = matcher.perform(elimPremisePattern1, _c1_times_ex_plus_e1_GEQ_0);
    assertThat(result2.matches()).isTrue();
    assertThat(result2.getVariableBindings("c1")).containsExactly(_c1);
    assertThat(result2.getVariableBindings("eX")).containsExactly(_eX);
  }

  @Test
  public void testLinearCombi4() {
    final SmtAstMatchResult result3 = matcher.perform(elimPremisePattern2, _minus_c2_times_ex_plus_e2_GEQ_0);
    assertThat(result3.matches()).isTrue();
  }

  @Test
  public void t1() {
    SmtAstPatternSelection p1 = or(
        match("f",
            and(
              matchAnyWithAnyArgsBind("e"),
              match("select",
                  matchAnyWithAnyArgsBind("a"),
                  matchAnyWithAnyArgsBind("i")))));

    SmtAstMatchResult r = matcher.perform(p1, _0_EQ_b_at_i_plus_1_);
    assertThat(r.matches()).isTrue();
  }

  @Test
  public void t2() {
    SmtAstPatternSelection p2 = or(
      matchBind("not", "nf",
          match(
              matchAnyWithAnyArgsBind("e"),
              match("select",
                  matchAnyWithAnyArgsBind("a"),
                  matchAnyWithAnyArgsBind("j")))));

    SmtAstMatchResult r = matcher.perform(p2, _b_at_i_NOTEQ_0);
    assertThat(r.matches()).isTrue();

    SmtAstMatchResult rz = matcher.perform(p2, _0_EQ_b_at_i_plus_1_);
    assertThat(rz.matches()).isFalse();
  }

  @Test
  public void testRotation() {
    SmtAstPatternSelection p = or(
      matchBind(">=", "f",
          matchAnyWithAnyArgsBind("a"),
          matchAnyWithAnyArgsBind("b")));

    SmtAstMatchResult r = matcher.perform(p,
        imgr.lessOrEquals(imgr.makeNumber(1),
            imgr.makeNumber(2)));

    assertThat(r.matches()).isTrue();

    SmtAstMatchResult r2 = matcher.perform(p,
        imgr.greaterOrEquals(imgr.makeNumber(2),
            imgr.makeNumber(1)));

    assertThat(r2.matches()).isTrue();

  }


  @Test
  public void testSubtreeMatching1() {
    _0 = imgr.makeNumber(0);
    _1 = imgr.makeNumber(1);
    _i = imgr.makeVariable("i");
    _b = amgr.makeArray("b", NumeralType.IntegerType, NumeralType.IntegerType);

    _i_plus_1 = imgr.add(_i, _1);
    _b_at_i = amgr.select(_b, _i);
    _b_at_i_EQ_0 = imgr.equal(_b_at_i, _0);
    _b_at_i_NOTEQ_0 = bmgr.not(_b_at_i_EQ_0);
    _b_at_i_plus_1_NOTEQ_0 = bmgr.not(imgr.equal(amgr.select(_b, _i_plus_1), _0));

    SmtAstPatternSelection pattern = or(
        matchAnyBind("f",
            matchInSubtree(
                matchAnyWithAnyArgsBind("x"))));

    {
      SmtAstMatchResult r = matcher.perform(pattern, _b_at_i_EQ_0);
      assertThat(r.matches()).isTrue();
      assertThat(r.getVariableBindings("x")).contains(_b_at_i);
    }
    {
      SmtAstMatchResult r = matcher.perform(pattern, _b_at_i_NOTEQ_0);
      assertThat(r.matches()).isTrue();
      assertThat(r.getVariableBindings("x")).contains(_b_at_i_EQ_0);
    }
  }

  @Test
  public void testSubtreeMatching2() {
    _0 = imgr.makeNumber(0);
    _1 = imgr.makeNumber(1);
    _i = imgr.makeVariable("i");
    _b = amgr.makeArray("b", NumeralType.IntegerType, NumeralType.IntegerType);

    _i_plus_1 = imgr.add(_i, _1);
    _b_at_i = amgr.select(_b, _i);
    _b_at_i_EQ_0 = imgr.equal(_b_at_i, _0);
    _b_at_i_NOTEQ_0 = bmgr.not(_b_at_i_EQ_0);
    _b_at_i_plus_1_NOTEQ_0 = bmgr.not(imgr.equal(amgr.select(_b, _i_plus_1), _0));

    SmtAstPatternSelection pattern = or(
        matchAnyBind("f",
            matchInSubtree(
                matchNullaryBind("i", "x"))));

    SmtAstMatchResult result = matcher.perform(pattern, _b_at_i_plus_1_NOTEQ_0);
    assertThat(result.matches()).isTrue();
    assertThat(result.getVariableBindings("x")).contains(_i);
  }

  @Test
  public void testSubtreeMatchingBinding1() {
    SmtAstPatternSelection pattern = or(
        matchAnyBind("f",
            matchAny(
                matchInSubtree(
                    matchAnyWithAnyArgsBind("i"))))
        );

    BooleanFormula input = imgr.lessThan(_i, _al);

    SmtAstMatchResult result = matcher.perform(pattern, input);

    assertThat(result.matches()).isTrue();
    assertThat(result.getVariableBindings("i")).contains(_i);
  }

  @Test
  public void testSubtreeMatchingBinding2() {

    BooleanFormula input = imgr.lessThan(_i, _al);

    SmtAstPatternSelection pattern = or(
        matchAnyBind("f",
            match("select", // should not match
                matchAnyWithAnyArgs(),
                matchInSubtree(
                    matchAnyWithAnyArgsBind("i"))),
            matchAnyWithAnyArgs()),
        matchAnyBind("f",  // <
            matchAny(      // either i or al
                matchInSubtree( // does not exist
                    matchAnyWithAnyArgsBind("i")))));

    SmtAstMatchResult result = matcher.perform(pattern, input);

    assertThat(result.matches()).isTrue();
    assertThat(result.getVariableBindings("i")).contains(_i);
  }

  @Test
  public void testCommutativenesOnTopLevel() {

    // Given a SmtAstPatternSelection.

   // Test on a formula al = i+1 if also i+1 = al matches
  }


}
