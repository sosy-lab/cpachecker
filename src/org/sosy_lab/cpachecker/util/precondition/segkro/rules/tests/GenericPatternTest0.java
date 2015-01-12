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

import org.junit.Test;
import org.sosy_lab.cpachecker.util.precondition.segkro.rules.GenericPatterns;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstMatchResult;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstPatternSelection;


public class GenericPatternTest0 extends AbstractRuleTest0 {

  @Test
  public void testMatchSubtraction1() {
    // b - c
    // (+ |foo::b| (* (- 1) |foo::c|))

    IntegerFormula subst1 = ifm.add(
        ifm.makeVariable("b"),
        ifm.multiply(
            ifm.makeNumber(-1),
            ifm.makeVariable("c")));

    SmtAstPatternSelection pattern = GenericPatterns.substraction("x", "y");
    SmtAstMatchResult result = matcher.perform(pattern, subst1);

    assertThat(result.matches()).isTrue();
  }

  @Test
  public void testMatchSubtraction2() {
    // b - c
    // (- |foo::b| |foo::c|)

    IntegerFormula subst1 = ifm.subtract(
        ifm.makeVariable("b"),
        ifm.makeVariable("c"));

    SmtAstPatternSelection pattern = GenericPatterns.substraction("x", "y");
    SmtAstMatchResult result = matcher.perform(pattern, subst1);

    assertThat(result.matches()).isTrue();
  }

  @Test
  public void testMatchSubtraction3() {
    // b - c
    // (+ |foo::b| (- 0 |foo::c|)

    IntegerFormula subst1 = ifm.add(
        ifm.makeVariable("b"),
        ifm.subtract(
            ifm.makeNumber(0),
            ifm.makeVariable("c")));

    SmtAstPatternSelection pattern = GenericPatterns.substraction("x", "y");
    SmtAstMatchResult result = matcher.perform(pattern, subst1);

    assertThat(result.matches()).isTrue();
  }

}
