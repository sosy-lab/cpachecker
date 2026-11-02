// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibToAstParser;

public class SmtLibLogicTest {

  /**
   * Checks the theories of the given logic: Int (integer arithmetic), BV (bitvectors), FP (floating
   * point numbers), A (arrays) and UF (functions with parameters).
   */
  private static void assertTheories(String pLogic, String... pTheories) {
    ImmutableSet<String> theories = ImmutableSet.copyOf(pTheories);
    SmtLibLogic logic = SmtLibLogic.fromString(pLogic);
    assertThat(logic.toString()).isEqualTo(pLogic);
    assertThat(logic.containsIntegerArithmetic()).isEqualTo(theories.contains("Int"));
    assertThat(logic.containsBitvectorOperations()).isEqualTo(theories.contains("BV"));
    assertThat(logic.containsFloatingPointOperations()).isEqualTo(theories.contains("FP"));
    assertThat(logic.containsArrayOperations()).isEqualTo(theories.contains("A"));
    assertThat(logic.containsUninterpretedFunctions()).isEqualTo(theories.contains("UF"));
  }

  @Test
  public void logicsOfTheTranslatedTasks() {
    assertTheories("ALL", "Int", "BV", "FP", "A", "UF");
    assertTheories("LIA", "Int");
    assertTheories("QF_LIA", "Int");
    assertTheories("QF_NIA", "Int");
    assertTheories("QF_ALIA", "Int", "A");
    assertTheories("QF_ANIA", "Int", "A");
    assertTheories("QF_UFLIA", "Int", "UF");
    assertTheories("QF_BV", "BV");
    assertTheories("QF_ABV", "BV", "A");
    assertTheories("QF_BVFP", "BV", "FP");
    assertTheories("QF_ABVFP", "BV", "FP", "A");
    assertTheories("QF_UFBVLIA", "Int", "BV", "UF");
    assertTheories("QF_AUFBVLIA", "Int", "BV", "A", "UF");
    assertTheories("UFBVFPDTNIRA", "Int", "BV", "FP", "UF");
    assertTheories("AUFBVFPDTNIRA", "Int", "BV", "FP", "A", "UF");
  }

  @Test
  public void otherLogics() {
    assertTheories("QF_AX", "A");
    assertTheories("QF_IDL", "Int");
    assertTheories("QF_LRA");
    assertTheories("QF_SLIA", "Int");
  }

  @Test
  public void unknownLogics() {
    assertThrows(IllegalArgumentException.class, () -> SmtLibLogic.fromString("QF_"));
    assertThrows(IllegalArgumentException.class, () -> SmtLibLogic.fromString("QF_XYZ"));
    assertThrows(IllegalArgumentException.class, () -> SmtLibLogic.fromString("QF_LIAX"));
    assertThrows(IllegalArgumentException.class, () -> SmtLibLogic.fromString("all"));
  }

  private static String scriptWithCheck(String pLogic, String pDeclarations, String pTerm) {
    return """
    (set-logic %s)
    %s
    (define-proc main ((x Int)) () ()
      (! (sequence) :tag check :check-true %s))
    (declare-var y Int)
    (verify-call main (y))
    """
        .formatted(pLogic, pDeclarations, pTerm);
  }

  @Test
  public void termsOfTheLogicAreAccepted() throws Exception {
    SvLibToAstParser.parseScript(scriptWithCheck("QF_ABV", "", "(= #b01 (_ bv1 2))"));
    SvLibToAstParser.parseScript(
        scriptWithCheck("QF_BVFP", "", "(fp.isNaN ((_ to_fp 8 24) RNE #x00000000))"));
    SvLibToAstParser.parseScript(
        scriptWithCheck("QF_ALIA", "", "(= 0 (select ((as const (Array Int Int)) 0) x))"));
    SvLibToAstParser.parseScript(
        scriptWithCheck("QF_UFLIA", "(declare-fun f (Int) Int)", "(= (f x) x)"));
    // A function of define-fun needs no uninterpreted functions
    SvLibToAstParser.parseScript(
        scriptWithCheck("QF_LIA", "(define-fun g ((a Int)) Int a)", "(= (g x) x)"));
  }

  private static void assertRejected(String pScript) {
    assertThrows(IllegalArgumentException.class, () -> SvLibToAstParser.parseScript(pScript));
  }

  @Test
  public void termsOutsideOfTheLogicAreRejected() {
    assertRejected(scriptWithCheck("QF_LIA", "", "(= #b01 #b01)"));
    assertRejected(scriptWithCheck("QF_BV", "", "(fp.isNaN ((_ to_fp 8 24) RNE #x00000000))"));
    assertRejected(
        scriptWithCheck("QF_LIA", "", "(= 0 (select ((as const (Array Int Int)) 0) x))"));
    assertRejected(scriptWithCheck("QF_LIA", "(declare-fun f (Int) Int)", "(= (f x) x)"));
  }

  @Test
  public void namesOfTheoriesOutsideOfTheLogicAreSymbols() throws Exception {
    SvLibToAstParser.parseScript(scriptWithCheck("QF_LIA", "(declare-const RNE Int)", "(= RNE x)"));
  }
}
