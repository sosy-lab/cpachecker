/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007!2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE!2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy!lab.org
 */
package org.sosy_lab.cpachecker.core.algorithm.tiger.test;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;

public class SolverTest {

  @Test
  public void presenceConditionParsingTest() throws Exception {
    SolverHelper solver = new SolverHelper();

    BooleanFormula formula0 = solver.parseFormula("a");
    assertTrue(formula0.toString().equals("a"));

    BooleanFormula formula1 = solver.parseFormula("(a)");
    assertTrue(formula1.toString().equals("a"));

    BooleanFormula formula2 = solver.parseFormula("((aa))");
    assertTrue(formula2.toString().equals("aa"));

    BooleanFormula formula3 = solver.parseFormula("(a & (b))");
    assertTrue(formula3.toString().equals("(and a b)"));

    BooleanFormula formula4 = solver.parseFormula("d & (a & (b)) & c");
    assertTrue(formula4.toString().equals("(and d a b c)"));

    BooleanFormula formula5 = solver.parseFormula("d | (a & (b)) | (c & (d | e))");
    assertTrue(formula5.toString().equals("(or d (and a b) (and c (or d e)))"));

    BooleanFormula formula6 = solver.parseFormula("d | (a & (FALSE)) | (c & (TRUE | e))");
    assertTrue(formula6.toString().equals("(or d c)"));

    BooleanFormula formula7 = solver.parseFormula("(!a & !b) & c");
    assertTrue(formula7.toString().equals("(and (not a) (not b) c)"));

    BooleanFormula formula8 = solver.parseFormula("(!a & b)");
    assertTrue(formula8.toString().equals("(and (not a) b)"));

    BooleanFormula formula9 = solver.parseFormula("(a & b & !TRUE)");
    assertTrue(formula9.toString().equals("(and a b (not TRUE))"));

    BooleanFormula formula10 = solver.parseFormula("((a & b) | (c & d))");
    assertTrue(formula10.toString().equals("(or (and a b) (and c d))"));

    BooleanFormula formula11 = solver.parseFormula("a & ((c & (!d & e)) | b)");
    assertTrue(formula11.toString().equals("(and a (or (and c (not d) e) b))"));
  }

  @Test
  public void fmParsingTest() throws InvalidConfigurationException, SolverException, InterruptedException {
    SolverHelper solver = new SolverHelper();

    String fm = "__SELECTED_FEATURE_FOOBAR_SPL  &  (!__SELECTED_FEATURE_FOOBAR_SPL  |  __SELECTED_FEATURE_COMP)  &  (!__SELECTED_FEATURE_FOOBAR_SPL  |  __SELECTED_FEATURE_OP)  &  (!__SELECTED_FEATURE_COMP  |  __SELECTED_FEATURE_FOOBAR_SPL)  &  (!__SELECTED_FEATURE_OP  |  __SELECTED_FEATURE_FOOBAR_SPL)  &  (!__SELECTED_FEATURE_NOTNEGATIVE  |  __SELECTED_FEATURE_FOOBAR_SPL)  &  (!__SELECTED_FEATURE_COMP  |  __SELECTED_FEATURE_LE  |  __SELECTED_FEATURE_GR)  &  (!__SELECTED_FEATURE_LE  |  __SELECTED_FEATURE_COMP)  &  (!__SELECTED_FEATURE_GR  |  __SELECTED_FEATURE_COMP)  &  (!__SELECTED_FEATURE_LE  |  !__SELECTED_FEATURE_GR)  &  (!__SELECTED_FEATURE_OP  |  __SELECTED_FEATURE_PLUS  |  __SELECTED_FEATURE_MINUS)  &  (!__SELECTED_FEATURE_PLUS  |  __SELECTED_FEATURE_OP)  &  (!__SELECTED_FEATURE_MINUS  |  __SELECTED_FEATURE_OP)  &  (!__SELECTED_FEATURE_PLUS  |  !__SELECTED_FEATURE_MINUS)  &  (!__SELECTED_FEATURE_NOTNEGATIVE  |  __SELECTED_FEATURE_MINUS)  &  True  &  !False  &  (__SELECTED_FEATURE_LE  |  __SELECTED_FEATURE_PLUS  |  __SELECTED_FEATURE_NOTNEGATIVE  |  __SELECTED_FEATURE_GR  |  __SELECTED_FEATURE_MINUS  |  True)";

    BooleanFormula formula0 = solver.parseFormula(fm);
    assertTrue(formula0.toString().equals("(let ((.cse0 (not __SELECTED_FEATURE_FOOBAR_SPL)) (.cse1 (not __SELECTED_FEATURE_COMP)) (.cse4 (not __SELECTED_FEATURE_LE)) (.cse5 (not __SELECTED_FEATURE_GR)) (.cse2 (not __SELECTED_FEATURE_OP)) (.cse6 (not __SELECTED_FEATURE_PLUS)) (.cse7 (not __SELECTED_FEATURE_MINUS)) (.cse3 (not __SELECTED_FEATURE_NOTNEGATIVE))) (and __SELECTED_FEATURE_FOOBAR_SPL (or .cse0 __SELECTED_FEATURE_COMP) (or .cse0 __SELECTED_FEATURE_OP) (or .cse1 __SELECTED_FEATURE_FOOBAR_SPL) (or .cse2 __SELECTED_FEATURE_FOOBAR_SPL) (or .cse3 __SELECTED_FEATURE_FOOBAR_SPL) (or .cse1 __SELECTED_FEATURE_LE __SELECTED_FEATURE_GR) (or .cse4 __SELECTED_FEATURE_COMP) (or .cse5 __SELECTED_FEATURE_COMP) (or .cse4 .cse5) (or .cse2 __SELECTED_FEATURE_PLUS __SELECTED_FEATURE_MINUS) (or .cse6 __SELECTED_FEATURE_OP) (or .cse7 __SELECTED_FEATURE_OP) (or .cse6 .cse7) (or .cse3 __SELECTED_FEATURE_MINUS) (not False)))"));
  }

  @Test
  public void implicationTest() throws InvalidConfigurationException, SolverException, InterruptedException {
    SolverHelper solver = new SolverHelper();
    BooleanFormula formula1 = solver.parseFormula("a & b");
    BooleanFormula formula2 = solver.parseFormula("b & a");
    BooleanFormula formula3 = solver.parseFormula("(a & TRUE & (!b | b) & b");
    BooleanFormula formula4 = solver.parseFormula("a | b");

    assertTrue(solver.equivalent(formula1, formula2));
    assertTrue(solver.equivalent(formula1, formula3));
    assertTrue(!solver.equivalent(formula1, formula4));
  }

}
