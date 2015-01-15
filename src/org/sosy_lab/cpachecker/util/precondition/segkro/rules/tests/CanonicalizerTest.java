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
package org.sosy_lab.cpachecker.util.precondition.segkro.rules.tests;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.precondition.segkro.interfaces.Canonicalizer;
import org.sosy_lab.cpachecker.util.precondition.segkro.rules.DefaultCanonicalizer;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;

public class CanonicalizerTest extends AbstractRuleTest0 {

  private Canonicalizer c;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    this.c = new DefaultCanonicalizer(solver, matcher);
  }

  @Test
  public void test1() throws SolverException, InterruptedException {
    // a < b
    // a, b: Integers
    // ====>
    // b - 1 >= a
    BooleanFormula input = bfm.makeBoolean(true);
    BooleanFormula result = c.canonicalize(input);
    BooleanFormula expected = bfm.makeBoolean(true);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void test2() throws SolverException, InterruptedException {
    // a > b
    // a, b: Integers
    // ====>
    // a - 1 >= b
    BooleanFormula input = bfm.makeBoolean(true);
    BooleanFormula result = c.canonicalize(input);
    BooleanFormula expected = bfm.makeBoolean(true);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void test3() throws SolverException, InterruptedException {
    // not a <= b
    // a, b: Integers
    // ====>
    // a - 1 >= b

    BooleanFormula input = bfm.makeBoolean(true);
    BooleanFormula result = c.canonicalize(input);
    BooleanFormula expected = bfm.makeBoolean(true);

    assertThat(result).isEqualTo(expected);
  }


  @Test
  public void test4() throws SolverException, InterruptedException {
    // (not (>= 1 al@1)),
    // ====>
    // 1 + 1 <= al@1

    BooleanFormula input = bfm.not(ifm.greaterOrEquals(ifm.makeNumber(1), ifm.makeVariable("a")));
    BooleanFormula result = c.canonicalize(input);
    BooleanFormula expected = ifm.lessOrEquals(ifm.makeNumber(2), ifm.makeVariable("a"));

    assertThat(isFormulaEqual(result, expected)).isTrue();
    assertThat(result.toString()).doesNotContain("not");
  }

}
