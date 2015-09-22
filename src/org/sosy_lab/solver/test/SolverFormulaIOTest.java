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
package org.sosy_lab.solver.test;

import static com.google.common.collect.Iterables.getLast;
import static com.google.common.truth.Truth.*;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.solver.FormulaManagerFactory.Solvers;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.FormulaType;
import org.sosy_lab.solver.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.solver.api.UninterpretedFunctionDeclaration;

import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;

import junit.framework.AssertionFailedError;

@RunWith(Parameterized.class)
public class SolverFormulaIOTest extends SolverBasedTest0 {
  private static final String MATHSAT_DUMP1 = "(set-info :source |printed by MathSAT|)\n(declare-fun a () Bool)\n(declare-fun b () Bool)\n(declare-fun d () Bool)\n(declare-fun e () Bool)\n(define-fun .def_9 () Bool (= a b))\n(define-fun .def_10 () Bool (not .def_9))\n(define-fun .def_13 () Bool (and .def_10 d))\n(define-fun .def_14 () Bool (or e .def_13))\n(assert .def_14)";
  private static final String MATHSAT_DUMP2 = "(set-info :source |printed by MathSAT|)\n(declare-fun a () Int)\n(declare-fun b () Int)\n(declare-fun c () Int)\n(declare-fun q () Bool)\n(declare-fun u () Bool)\n(define-fun .def_15 () Int (* (- 1) c))\n(define-fun .def_16 () Int (+ b .def_15))\n(define-fun .def_17 () Int (+ a .def_16))\n(define-fun .def_19 () Bool (= .def_17 0))\n(define-fun .def_27 () Bool (= .def_19 q))\n(define-fun .def_28 () Bool (not .def_27))\n(define-fun .def_23 () Bool (<= b a))\n(define-fun .def_29 () Bool (and .def_23 .def_28))\n(define-fun .def_11 () Bool (= a b))\n(define-fun .def_34 () Bool (and .def_11 .def_29))\n(define-fun .def_30 () Bool (or u .def_29))\n(define-fun .def_31 () Bool (and q .def_30))\n(define-fun .def_35 () Bool (and .def_31 .def_34))\n(assert .def_35)";
  private static final String MATHSAT_DUMP3 = "(set-info :source |printed by MathSAT|)\n(declare-fun fun_b (Int) Bool)\n(define-fun .def_11 () Bool (fun_b 1))\n(assert .def_11)";
  private static final String SMTINTERPOL_DUMP1 = "(declare-fun d () Bool)\n(declare-fun b () Bool)\n(declare-fun a () Bool)\n(declare-fun e () Bool)\n(assert (or e (and (xor a b) d)))";
  private static final String SMTINTERPOL_DUMP2 = "(declare-fun b () Int)(declare-fun a () Int)\n(declare-fun c () Int)\n(declare-fun q () Bool)\n(declare-fun u () Bool)\n(assert (let ((.cse0 (xor q (= (+ a b) c))) (.cse1 (>= a b))) (and (or (and .cse0 .cse1) u) q (= a b) .cse0 .cse1)))";
  private static final String Z3_DUMP1 = "(declare-fun d () Bool)\n(declare-fun b () Bool)\n(declare-fun a () Bool)\n(declare-fun e () Bool)\n(assert  (or e (and (xor a b) d)))";
  private static final String Z3_DUMP2 = "(declare-fun b () Int)\n(declare-fun a () Int)\n(declare-fun c () Int)\n(declare-fun q () Bool)\n(declare-fun u () Bool)\n(assert  (let (($x35 (and (xor q (= (+ a b) c)) (>= a b)))) (let (($x9 (= a b))) (and (and (or $x35 u) q) (and $x9 $x35)))))";
  private Supplier<BooleanFormula> boolExprGen1 = new Supplier<BooleanFormula>() {
    @Override
    public BooleanFormula get() {
      return genBoolExpr();
    }
  };

  private Supplier<BooleanFormula> boolExprGen2 = new Supplier<BooleanFormula>() {
    @Override
    public BooleanFormula get() {
      return redundancyExprGen();
    }
  };

  private Supplier<BooleanFormula> boolExprGen3 = new Supplier<BooleanFormula>() {
    @Override
    public BooleanFormula get() {
      return functionExprGen();
    }
  };

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

  @Test
  public void varDumpTest() {
    BooleanFormula a = bmgr.makeVariable("a");
    BooleanFormula b = bmgr.makeVariable("b");
    BooleanFormula c1 = bmgr.xor(a, b);
    BooleanFormula c2 = bmgr.xor(a, b);
    BooleanFormula d = bmgr.and(c1, c2);

    String formDump = mgr.dumpFormula(d).toString();
    assertThat(formDump).contains("(declare-fun a () Bool)");
    assertThat(formDump).contains("(declare-fun b () Bool)");
    checkThatAssertIsInLastLine(formDump);
  }

  @Test
  public void varDumpTest2() {
    //always true
    BooleanFormula a = bmgr.makeVariable("a");
    BooleanFormula b = bmgr.makeVariable("b");
    BooleanFormula c1 = bmgr.xor(a, b);
    BooleanFormula c2 = bmgr.and(a, b);
    BooleanFormula d = bmgr.or(c1, c2);
    BooleanFormula e = bmgr.and(a, d);

    BooleanFormula x1 = bmgr.xor(a, b);
    BooleanFormula x2 = bmgr.and(a, b);
    BooleanFormula w = bmgr.or(x1, x2);
    BooleanFormula v = bmgr.or(x1, b);

    BooleanFormula branch1 = bmgr.and(d, e);
    BooleanFormula branch2 = bmgr.and(w, v);
    BooleanFormula branchComp = bmgr.or(branch1, branch2);

    String formDump = mgr.dumpFormula(branchComp).toString();
    assertThat(formDump).contains("(declare-fun a () Bool)");
    assertThat(formDump).contains("(declare-fun b () Bool)");
    checkThatAssertIsInLastLine(formDump);
  }

  @Test
  public void valDumpTest() {
    BooleanFormula tr1 = bmgr.makeBoolean(true);
    BooleanFormula tr2 = bmgr.makeBoolean(true);
    BooleanFormula fl1 = bmgr.makeBoolean(false);
    BooleanFormula fl2 = bmgr.makeBoolean(false);
    BooleanFormula valComp = bmgr.and(fl1, tr1);
    BooleanFormula valComp2 = bmgr.and(fl1, tr1);
    BooleanFormula valComp3 = bmgr.and(tr2, valComp);
    BooleanFormula valComp4 = bmgr.and(fl2, valComp2);
    BooleanFormula valComp5 = bmgr.or(valComp3, valComp4);

    String formDump = mgr.dumpFormula(valComp5).toString();
    checkThatAssertIsInLastLine(formDump);
  }

  @Test
  public void intsDumpTest() {
    IntegerFormula f1 = imgr.makeVariable("a");
    IntegerFormula val = imgr.makeNumber(1);
    BooleanFormula formula = imgr.equal(f1, val);

    String formDump = mgr.dumpFormula(formula).toString();

    // check that int variable is declared correctly + necessary assert that has to be there
    assertThat(formDump).contains("(declare-fun a () Int)");
    checkThatAssertIsInLastLine(formDump);
  }

  @Test
  public void funcsDumpTest() {
    IntegerFormula int1 = imgr.makeNumber(1);
    IntegerFormula var = imgr.makeVariable("var_a");
    List<IntegerFormula> args1 = new LinkedList<>();
    args1.add(int1);
    args1.add(var);

    UninterpretedFunctionDeclaration<IntegerFormula> funA = fmgr.declareUninterpretedFunction("fun_a", FormulaType.IntegerType, FormulaType.IntegerType, FormulaType.IntegerType);
    IntegerFormula res1 = fmgr.callUninterpretedFunction(funA, args1);
    BooleanFormula formula = imgr.equal(res1, var);

    String formDump = mgr.dumpFormula(formula).toString();

    // check that function is dumped correctly + necessary assert that has to be there
    assertThat(formDump).contains("(declare-fun fun_a (Int Int) Int)");
    checkThatAssertIsInLastLine(formDump);
  }

  @Test
  public void parseMathSatTestParseFirst1() throws SolverException, InterruptedException {
    compareParseWithOrgParseFirst(MATHSAT_DUMP1, boolExprGen1);
  }

  @Test
  public void parseMathSatTestExprFirst1() throws SolverException, InterruptedException {
    compareParseWithOrgExprFirst(MATHSAT_DUMP1, boolExprGen1);
  }

  @Test
  public void parseSmtinterpolTestParseFirst1() throws SolverException, InterruptedException {
    compareParseWithOrgParseFirst(SMTINTERPOL_DUMP1, boolExprGen1);
  }

  @Test
  public void parseSmtinterpolTestExprFirst1() throws SolverException, InterruptedException {
    compareParseWithOrgExprFirst(SMTINTERPOL_DUMP1, boolExprGen1);
  }

  @Test
  public void parseZ3TestParseFirst1() throws SolverException, InterruptedException {
    compareParseWithOrgParseFirst(Z3_DUMP1, boolExprGen1);
  }

  @Test
  public void parseZ3TestExprFirst1() throws SolverException, InterruptedException {
    compareParseWithOrgExprFirst(Z3_DUMP1, boolExprGen1);
  }

  @Test
  public void parseMathSatTestParseFirst2() throws SolverException, InterruptedException {
    compareParseWithOrgParseFirst(MATHSAT_DUMP2, boolExprGen2);
  }

  @Test
  public void parseMathSatTestExprFirst2() throws SolverException, InterruptedException {
    compareParseWithOrgExprFirst(MATHSAT_DUMP2, boolExprGen2);
  }

  @Test
  public void parseSmtinterpolSatTestParseFirst2() throws SolverException, InterruptedException {
    compareParseWithOrgParseFirst(SMTINTERPOL_DUMP2, boolExprGen2);
  }

  @Test
  public void parseSmtinterpolSatTestExprFirst2() throws SolverException, InterruptedException {
    compareParseWithOrgExprFirst(SMTINTERPOL_DUMP2, boolExprGen2);
  }

  @Test
  public void parseZ3SatTestParseFirst2() throws SolverException, InterruptedException {
    compareParseWithOrgParseFirst(Z3_DUMP2, boolExprGen2);
  }

  @Test
  public void parseZ3SatTestExprFirst2() throws SolverException, InterruptedException {
    compareParseWithOrgExprFirst(Z3_DUMP2, boolExprGen2);
  }

  @Test
  public void parseMathSatTestExprFirst3() throws SolverException, InterruptedException {
    compareParseWithOrgExprFirst(MATHSAT_DUMP3, boolExprGen3);
  }

  public void parseMathSatTestParseFirst3() throws SolverException, InterruptedException {
    compareParseWithOrgParseFirst(MATHSAT_DUMP3, boolExprGen3);
  }

  @Test
  public void redundancyTest() {
    String formDump = mgr.dumpFormula(redundancyExprGen()).toString();
    int count = Iterables.size(Splitter.on(">=").split(formDump)) - 1;
    int count2 = Iterables.size(Splitter.on("<=").split(formDump)) - 1;
    if (!(count == 1 || count2 == 1)) {
      throw new AssertionFailedError(formDump + " does not contain <= or >= only once.");
    }
  }

  @Test
  public void funDeclareTest() {
    IntegerFormula int1 = imgr.makeNumber(1);
    IntegerFormula int2 = imgr.makeNumber(2);
    List<IntegerFormula> args1 = new LinkedList<>();
    List<IntegerFormula> args2 = new LinkedList<>();
    args1.add(int1);
    args2.add(int2);

    UninterpretedFunctionDeclaration<IntegerFormula> funA = fmgr.declareUninterpretedFunction("fun_a", FormulaType.IntegerType, FormulaType.IntegerType);
    UninterpretedFunctionDeclaration<IntegerFormula> funB = fmgr.declareUninterpretedFunction("fun_b", FormulaType.IntegerType, FormulaType.IntegerType);
    IntegerFormula res1 = fmgr.callUninterpretedFunction(funA, args1);
    IntegerFormula res2 = fmgr.callUninterpretedFunction(funB, args2);

    IntegerFormula calc = imgr.add(res1, res2);
    String formDump = mgr.dumpFormula(imgr.equal(calc, int1)).toString();

    // check if dumped formula fits our specification
    checkThatFunOnlyDeclaredOnce(formDump);
    checkThatAssertIsInLastLine(formDump);
  }

  @Test
  public void funDeclareTest2() {
    IntegerFormula int1 = imgr.makeNumber(1);
    IntegerFormula int2 = imgr.makeNumber(2);

    UninterpretedFunctionDeclaration<IntegerFormula> funA = fmgr.declareUninterpretedFunction("fun_a", FormulaType.IntegerType, FormulaType.IntegerType);
    IntegerFormula res1 = fmgr.callUninterpretedFunction(funA, ImmutableList.of(int1));
    IntegerFormula res2 = fmgr.callUninterpretedFunction(funA, ImmutableList.of(int2));

    IntegerFormula calc = imgr.add(res1, res2);
    String formDump = mgr.dumpFormula(imgr.equal(calc, int1)).toString();

    // check if dumped formula fits our specification
    checkThatFunOnlyDeclaredOnce(formDump);
    checkThatAssertIsInLastLine(formDump);
  }

  private void compareParseWithOrgExprFirst(String textToParse, Supplier<BooleanFormula> fun)
      throws SolverException, InterruptedException {
    // check if input is correct
    checkThatFunOnlyDeclaredOnce(textToParse);
    checkThatAssertIsInLastLine(textToParse);

    // actual test
    BooleanFormula expr = fun.get();
    BooleanFormula parsedForm = mgr.parse(textToParse);
    assert_().about(BooleanFormula()).that(parsedForm).isEquivalentTo(expr);
  }

  private void compareParseWithOrgParseFirst(String textToParse, Supplier<BooleanFormula> fun)
      throws SolverException, InterruptedException {
    // check if input is correct
    checkThatFunOnlyDeclaredOnce(textToParse);
    checkThatAssertIsInLastLine(textToParse);

    // actual test
    BooleanFormula parsedForm = mgr.parse(textToParse);
    BooleanFormula expr = fun.get();
    assert_().about(BooleanFormula()).that(parsedForm).isEquivalentTo(expr);
  }

  private void checkThatFunOnlyDeclaredOnce(String formDump) {
    Multiset<String> funDeclares = HashMultiset.create();

    for (String line: Splitter.on('\n').split(formDump)) {
      if (line.startsWith("(declare-fun ")) {
        funDeclares.add(line.replaceAll("\\s+", ""));
      }
    }

    // remove non-duplicates
    Iterator<Multiset.Entry<String>> it = funDeclares.entrySet().iterator();
    while (it.hasNext()) {
      if (it.next().getCount() <= 1) {
        it.remove();
      }
    }
    assertThat(funDeclares).named("duplicate function declarations").isEmpty();
  }

  private void checkThatAssertIsInLastLine(String lines) {
    lines = lines.trim();
    assertThat(getLast(Splitter.on('\n').split(lines)))
      .named("last line of <\n"+lines+">")
      .startsWith("(assert ");
  }

  private BooleanFormula genBoolExpr() {
    BooleanFormula a = bmgr.makeVariable("a");
    BooleanFormula b = bmgr.makeVariable("b");
    BooleanFormula c = bmgr.xor(a, b);
    BooleanFormula d = bmgr.makeVariable("d");
    BooleanFormula e = bmgr.makeVariable("e");
    BooleanFormula f = bmgr.and(c, d);
    return bmgr.or(e, f);
  }

  private BooleanFormula redundancyExprGen() {
    IntegerFormula i1 = imgr.makeVariable("a");
    IntegerFormula i2 = imgr.makeVariable("b");
    IntegerFormula erg = imgr.makeVariable("c");
    BooleanFormula b1 = bmgr.makeVariable("q");
    BooleanFormula b2 = bmgr.makeVariable("u");

    //1st execution
    BooleanFormula f1 = imgr.equal(imgr.add(i1, i2), erg);
    BooleanFormula comp1 = imgr.greaterOrEquals(i1, i2);
    BooleanFormula x1 = bmgr.xor(b1, f1);
    BooleanFormula comb1 = bmgr.and(x1, comp1);

    //rest
    BooleanFormula r1a = bmgr.or(comb1, b2);
    BooleanFormula r1b = bmgr.and(r1a, b1);

    //rest
    BooleanFormula r2a = imgr.equal(i1, i2);
    BooleanFormula r2b = bmgr.and(r2a, comb1);

    return bmgr.and(r1b, r2b);
  }

  private BooleanFormula functionExprGen() {
    IntegerFormula arg = imgr.makeNumber(1);
    UninterpretedFunctionDeclaration<BooleanFormula> funA = fmgr.declareUninterpretedFunction("fun_b", FormulaType.BooleanType, FormulaType.IntegerType);
    BooleanFormula res1 = fmgr.callUninterpretedFunction(funA, ImmutableList.of(arg));
    return bmgr.and(res1, bmgr.makeBoolean(true));
  }
}
