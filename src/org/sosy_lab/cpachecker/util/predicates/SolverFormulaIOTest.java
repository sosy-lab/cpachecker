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
import static com.google.common.truth.TruthJUnit.assume;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.AssertionFailedError;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory.Solvers;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.UninterpretedFunctionDeclaration;
import org.sosy_lab.cpachecker.util.test.SolverBasedTest0;

import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;

@RunWith(Parameterized.class)
public class SolverFormulaIOTest extends SolverBasedTest0 {
  private static final String MATHSAT_DUMP1 = "(set-info :source |printed by MathSAT|)\n(declare-fun a () Bool)\n(declare-fun b () Bool)\n(declare-fun d () Bool)\n(declare-fun e () Bool)\n(define-fun .def_9 () Bool (= a b))\n(define-fun .def_10 () Bool (not .def_9))\n(define-fun .def_13 () Bool (and .def_10 d))\n(define-fun .def_14 () Bool (or e .def_13))\n(assert .def_14)";
  private static final String MATHSAT_DUMP2 = "(set-info :source |printed by MathSAT|)\n(declare-fun a () Int)\n(declare-fun b () Int)\n(declare-fun c () Int)\n(declare-fun q () Bool)\n(declare-fun u () Bool)\n(define-fun .def_15 () Int (* (- 1) c))\n(define-fun .def_16 () Int (+ b .def_15))\n(define-fun .def_17 () Int (+ a .def_16))\n(define-fun .def_19 () Bool (= .def_17 0))\n(define-fun .def_27 () Bool (= .def_19 q))\n(define-fun .def_28 () Bool (not .def_27))\n(define-fun .def_23 () Bool (<= b a))\n(define-fun .def_29 () Bool (and .def_23 .def_28))\n(define-fun .def_11 () Bool (= a b))\n(define-fun .def_34 () Bool (and .def_11 .def_29))\n(define-fun .def_30 () Bool (or u .def_29))\n(define-fun .def_31 () Bool (and q .def_30))\n(define-fun .def_35 () Bool (and .def_31 .def_34))\n(assert .def_35)";
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
  public void basicBoolTest() throws Exception {
    BooleanFormula a = bmgr.makeVariable("a");
    BooleanFormula b = bmgr.makeBoolean(false);
    BooleanFormula c = bmgr.xor(a, b);
    BooleanFormula d = bmgr.makeVariable("b");
    BooleanFormula e = bmgr.xor(a, d);

    BooleanFormula notImpl = bmgr.and(a, bmgr.not(e));

    assert_().about(BooleanFormula()).that(a).implies(c);
    assert_().about(BooleanFormula()).that(notImpl).isSatisfiable();
  }

  @Test
  public void basicIntTest() {
    IntegerFormula a = imgr.makeVariable("a");
    IntegerFormula b = imgr.makeVariable("b");
    assertThat(a).isNotEqualTo(b);
  }
  @Test
  public void basisRatTest() throws Exception {
    requireRationals();

    RationalFormula a = rmgr.makeVariable("int_c");
    RationalFormula num = rmgr.makeNumber(4);

    BooleanFormula f = rmgr.equal(rmgr.add(a, a), num);
    assert_().about(BooleanFormula()).that(f).isSatisfiable();
  }

  @Test
  public void varDumpTest() {
    BooleanFormula a = bmgr.makeVariable("a");
    BooleanFormula b = bmgr.makeVariable("b");
    BooleanFormula c1 = bmgr.xor(a, b);
    BooleanFormula c2 = bmgr.xor(a, b);
    BooleanFormula d = bmgr.and(c1, c2);

    String formDump = mgr.dumpFormula(d).toString();
    assert_().that(formDump).contains("(declare-fun a () Bool)");
    assert_().that(formDump).contains("(declare-fun b () Bool)");
    String[] lines = formDump.split("\n");
    checkThatAssertIsInLastLine(lines);
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
    assert_().that(formDump).contains("(declare-fun a () Bool)");
    assert_().that(formDump).contains("(declare-fun b () Bool)");
    String[] lines = formDump.split("\n");
    checkThatAssertIsInLastLine(lines);
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
    String[] lines = formDump.split("\n");
    checkThatAssertIsInLastLine(lines);
  }

  @Test
  public void intsDumpTest() {
    IntegerFormula f1 = imgr.makeVariable("a");
    IntegerFormula val = imgr.makeNumber(1);
    BooleanFormula formula = imgr.equal(f1, val);

    String formDump = mgr.dumpFormula(formula).toString();
    String[] lines = formDump.split("\n");

    // check that int variable is declared correctly + necessary assert that has to be there
    assert_().that(formDump).contains("(declare-fun a () Int)");
    checkThatAssertIsInLastLine(lines);
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
    String[] lines = formDump.split("\n");

    // check that function is dumped correctly + necessary assert that has to be there
    assert_().that(formDump).contains("(declare-fun fun_a (Int Int) Int)");
    checkThatAssertIsInLastLine(lines);
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
  public void redundancyTest() {
    assume().withFailureMessage("Solver does not support removing multiple occurrences yet.")
        .that(solver).isNotEqualTo(Solvers.PRINCESS);
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
    checkThatAssertIsInLastLine(formDump.split("\n"));
  }

  @Test
  public void funDeclareTest2() {
    IntegerFormula int1 = imgr.makeNumber(1);
    IntegerFormula int2 = imgr.makeNumber(2);
    List<IntegerFormula> args1 = new LinkedList<>();
    List<IntegerFormula> args2 = new LinkedList<>();
    args1.add(int1);
    args2.add(int2);

    UninterpretedFunctionDeclaration<IntegerFormula> funA = fmgr.declareUninterpretedFunction("fun_a", FormulaType.IntegerType, FormulaType.IntegerType);
    IntegerFormula res1 = fmgr.callUninterpretedFunction(funA, args1);
    IntegerFormula res2 = fmgr.callUninterpretedFunction(funA, args2);

    IntegerFormula calc = imgr.add(res1, res2);
    String formDump = mgr.dumpFormula(imgr.equal(calc, int1)).toString();

    // check if dumped formula fits our specification
    checkThatFunOnlyDeclaredOnce(formDump);
    checkThatAssertIsInLastLine(formDump.split("\n"));
  }

  private void compareParseWithOrgExprFirst(String textToParse, Supplier<BooleanFormula> fun)
      throws SolverException, InterruptedException {
    // check if input is correct
    requireSMTLibParser();
    checkThatFunOnlyDeclaredOnce(textToParse);
    checkThatAssertIsInLastLine(textToParse.split("\n"));

    // actual test
    BooleanFormula expr = fun.get();
    BooleanFormula parsedForm = mgr.parse(textToParse);
    assert_().about(BooleanFormula()).that(parsedForm).isEquivalentTo(expr);
  }

  private void compareParseWithOrgParseFirst(String textToParse, Supplier<BooleanFormula> fun)
      throws SolverException, InterruptedException {
    // check if input is correct
    requireSMTLibParser();
    checkThatFunOnlyDeclaredOnce(textToParse);
    checkThatAssertIsInLastLine(textToParse.split("\n"));

    // actual test
    BooleanFormula parsedForm = mgr.parse(textToParse);
    BooleanFormula expr = fun.get();
    assert_().about(BooleanFormula()).that(parsedForm).isEquivalentTo(expr);
  }

  private void checkThatFunOnlyDeclaredOnce(String formDump) {
    Iterable<String> lines = Splitter.on("\n").split(formDump);
    List<String> funDeclares = new LinkedList<>();

    for (String line: lines) {
      if (line.startsWith("(declare-fun ")) {
        funDeclares.add(line.replaceAll("\\s+", ""));
      }
    }

    assert_().that(findDuplicates(funDeclares)).isEmpty();
  }

  private void checkThatAssertIsInLastLine(String[] lines) {
    assert_().that(lines[lines.length - 1]).startsWith("(assert ");
  }

  public <T> List<T> findDuplicates(List<T> list) {
    List<T> duplicates = new LinkedList<>();
    Set<T> set = new HashSet<>();

    for (T element : list) {
      if (!set.add(element)) {
        duplicates.add(element);
      }
    }
    return duplicates;
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

  private void requireSMTLibParser() {
    assume().withFailureMessage("Solver does not support parsing yet.")
        .that(solver).isNotEqualTo(Solvers.PRINCESS);
  }
}
