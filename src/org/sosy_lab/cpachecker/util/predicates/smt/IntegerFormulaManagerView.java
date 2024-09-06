// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import java.math.BigInteger;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

public class IntegerFormulaManagerView
    extends NumeralFormulaManagerView<IntegerFormula, IntegerFormula>
    implements IntegerFormulaManager {
  private final IntegerFormulaManager integerFormulaManager;
  private final BooleanFormulaManager booleanFormulaManager;

  IntegerFormulaManagerView(
      FormulaWrappingHandler pWrappingHandler,
      BooleanFormulaManager pBooleanManager,
      IntegerFormulaManager pIntegerManager) {
    super(pWrappingHandler, pIntegerManager);
    booleanFormulaManager = pBooleanManager;
    integerFormulaManager = pIntegerManager;
  }

  @Override
  public BooleanFormula modularCongruence(IntegerFormula number1, IntegerFormula number2, long n) {
    return integerFormulaManager.modularCongruence(number1, number2, n);
  }

  @Override
  public BooleanFormula modularCongruence(
      IntegerFormula number1, IntegerFormula number2, BigInteger n) {
    return integerFormulaManager.modularCongruence(number1, number2, n);
  }

  @Override
  public IntegerFormula modulo(IntegerFormula pNumber1, IntegerFormula pNumber2) {
    return integerFormulaManager.modulo(pNumber1, pNumber2);
  }

  /* Division
   * <p>Uses truncate to round the result to the next integer, just as in C or Java.
   */
  @Override
  public IntegerFormula divide(IntegerFormula dividend, IntegerFormula divisor) {
    // TODO: Make sure division by zero is handled correctly
    IntegerFormula zero = makeNumber(0);
    IntegerFormula r = super.divide(dividend, divisor);

    return booleanFormulaManager.ifThenElse(
        lessOrEquals(divisor, zero),
        booleanFormulaManager.ifThenElse(greaterThan(r, zero), add(r, makeNumber(-1)), r),
        booleanFormulaManager.ifThenElse(lessThan(r, zero), add(r, makeNumber(1)), r));
  }

  /**
   * Returns the remainder of the division of the two given Integer formulas. The result of the
   * division (dividend/divisor) applied on the 2 Integer formulas used as quotient in the modulo
   * operation is floored for negative divisors and rounded upwards for positive divisors.
   *
   * <p>If the dividend evaluates to zero (modulo-by-zero), either directly as value or indirectly
   * via an additional constraint, then the solver is allowed to choose an arbitrary value for the
   * result of the modulo operation (cf. SMTLIB standard for the division operator in Ints or Reals
   * theory).
   *
   * <p>Note: Some solvers, e.g., Yices2, abort with an exception when exploring a modulo-by-zero
   * during the SAT-check. This is not compliant to the SMTLIB standard, but sadly happens.
   *
   * @see BitvectorFormulaManagerView#remainder(BitvectorFormula, BitvectorFormula, boolean) with
   *     signed true for the BV equivalent.
   * @param dividend the formula used as the dividend of the operation.
   * @param divisor the formula used as the divisor of the operation.
   * @param bmgr {@link BooleanFormulaManager} needed for the creation of the formula.
   * @return the remainder of the 2 given formulas.
   */
  public IntegerFormula remainder(final IntegerFormula dividend, final IntegerFormula divisor) {
    final IntegerFormula zero = makeNumber(0);
    final IntegerFormula additionalUnit =
        booleanFormulaManager.ifThenElse(greaterOrEquals(divisor, zero), negate(divisor), divisor);

    final IntegerFormula mod = modulo(dividend, divisor);

    // IF   first operand is positive or mod-result is zero
    // THEN return plain modulo --> here the result is equal to SMTlib2 Integer mod
    // ELSE modulo and add an additional unit towards the nearest infinity.

    // This resembles C99/C11/Java closely but not 100%
    return booleanFormulaManager.ifThenElse(
        booleanFormulaManager.or(greaterOrEquals(dividend, zero), equal(mod, zero)),
        mod,
        add(mod, additionalUnit));
  }
}
