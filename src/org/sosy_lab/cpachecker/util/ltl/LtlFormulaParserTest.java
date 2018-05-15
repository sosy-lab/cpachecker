/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.ltl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sosy_lab.cpachecker.util.ltl.formulas.BooleanConstant;
import org.sosy_lab.cpachecker.util.ltl.formulas.Conjunction;
import org.sosy_lab.cpachecker.util.ltl.formulas.Disjunction;
import org.sosy_lab.cpachecker.util.ltl.formulas.Finally;
import org.sosy_lab.cpachecker.util.ltl.formulas.Formula;
import org.sosy_lab.cpachecker.util.ltl.formulas.Globally;
import org.sosy_lab.cpachecker.util.ltl.formulas.Literal;
import org.sosy_lab.cpachecker.util.ltl.formulas.Next;
import org.sosy_lab.cpachecker.util.ltl.formulas.Release;
import org.sosy_lab.cpachecker.util.ltl.formulas.StrongRelease;
import org.sosy_lab.cpachecker.util.ltl.formulas.Until;
import org.sosy_lab.cpachecker.util.ltl.formulas.WeakUntil;

public class LtlFormulaParserTest {

  @Test
  public void test_parse_appliesRandomSyntaxCorrectly() {
    String[] in = {
      "!a",
      "G a",
      "F a & X b",
      "(a -> b) U c",
      "true U b",
      "a S b",
      "a R b",
      "!(a R b)",
      "a W b U c R a"
    };

    Formula[] out = {
      new Literal("a", true),
      new Globally(new Literal("a")),
      new Conjunction(new Finally(new Literal("a")), new Next(new Literal("b"))),
      new Until(new Disjunction(new Literal("a", true), new Literal("b")), new Literal("c")),
      new Finally(new Literal("b")),
      new StrongRelease(new Literal("a"), new Literal("b")),
      new Release(new Literal("a"), new Literal("b")),
      new Until(new Literal("a", true), new Literal("b", true)),
      new WeakUntil(
          new Literal("a"),
          new Until(new Literal("b"), new Release(new Literal("c"), new Literal("a"))))
    };

    for (int i = 0; i < in.length; i++) {
      assertEquals(in[i], out[i], LtlFormulaParser.parse(in[i]));
    }
  }

  @Test
  public void test_parse_APs_appliesTrivialIdentities() {
    String[] in = {
      "!false", "!true", "!!true", "true -> a", "false -> a", "a -> true", "a -> false", "a -> a"
    };

    Formula[] out = {
      BooleanConstant.TRUE,
      BooleanConstant.FALSE,
      BooleanConstant.TRUE,
      new Literal("a"),
      BooleanConstant.TRUE,
      BooleanConstant.TRUE,
      new Literal("a", true),
      BooleanConstant.TRUE
    };

    for (int i = 0; i < in.length; i++) {
      assertEquals(in[i], out[i], LtlFormulaParser.parse(in[i]));
    }
  }

  @Test
  public void test_parse_APs_appliesCommutativeIdentities() {
    String[] in = {
      "false && a",
      "true && a",
      "a && false",
      "a && true",
      "a && a",
      "false || a",
      "true || a",
      "a || false",
      "a || true",
      "a || a",
      "false XOR a",
      "true XOR a",
      "a XOR false",
      "a XOR true",
      "a XOR a",
      "false <-> a",
      "true <-> a",
      "a <-> false",
      "a <-> true",
      "a <-> a"
    };

    Formula[] out = {
      BooleanConstant.FALSE,
      new Literal("a"),
      BooleanConstant.FALSE,
      new Literal("a"),
      new Literal("a"),
      new Literal("a"),
      BooleanConstant.TRUE,
      new Literal("a"),
      BooleanConstant.TRUE,
      new Literal("a"),
      new Literal("a"),
      new Literal("a", true),
      new Literal("a"),
      new Literal("a", true),
      BooleanConstant.FALSE,
      new Literal("a", true),
      new Literal("a"),
      new Literal("a", true),
      new Literal("a"),
      BooleanConstant.TRUE
    };

    for (int i = 0; i < in.length; i++) {
      assertEquals(in[i], out[i], LtlFormulaParser.parse(in[i]));
    }
  }

  @Test
  public void test_parse_temporalOperators_appliesTrivialIdentities() {
    String[] in = {
      "X false",
      "X true",
      "F false",
      "F true",
      "F F a",
      "G false",
      "G true",
      "G G a",
      "a U true",
      "false U a",
      "a U false",
      "a U a",
      "a W true",
      "false W a",
      "true W a",
      "a W a",
      "a S false",
      "false S a",
      "true S a",
      "a S a",
      "a R true",
      "a R false",
      "true R a",
      "a R a"
    };

    Formula[] out = {
      BooleanConstant.FALSE,
      BooleanConstant.TRUE,
      BooleanConstant.FALSE,
      BooleanConstant.TRUE,
      new Finally(new Literal("a")),
      BooleanConstant.FALSE,
      BooleanConstant.TRUE,
      new Globally(new Literal("a")),
      BooleanConstant.TRUE,
      new Literal("a"),
      BooleanConstant.FALSE,
      new Literal("a"),
      BooleanConstant.TRUE,
      new Literal("a"),
      BooleanConstant.TRUE,
      new Literal("a"),
      BooleanConstant.FALSE,
      BooleanConstant.FALSE,
      new Literal("a"),
      new Literal("a"),
      BooleanConstant.TRUE,
      BooleanConstant.FALSE,
      new Literal("a"),
      new Literal("a")
    };

    for (int i = 0; i < in.length; i++) {
      assertEquals(in[i], out[i], LtlFormulaParser.parse(in[i]));
    }
  }
}
