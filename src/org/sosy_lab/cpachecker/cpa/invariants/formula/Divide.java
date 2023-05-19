// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

/**
 * Instances of this class are invariants formulae representing division operations over other
 * invariants formulae.
 *
 * @param <ConstantType> the type of the constants used in the formula.
 */
final class Divide<ConstantType> extends AbstractBinaryFormula<ConstantType>
    implements NumeralFormula<ConstantType> {

  /**
   * Creates a new fraction invariants formula for the given numerator and denominator.
   *
   * @param pNumerator the numerator of the fraction.
   * @param pDenominator the denominator of the fraction.
   */
  private Divide(
      NumeralFormula<ConstantType> pNumerator, NumeralFormula<ConstantType> pDenominator) {
    super("/", false, pNumerator, pDenominator);
  }

  /**
   * Gets the numerator of the fraction.
   *
   * @return the numerator of the fraction.
   */
  public NumeralFormula<ConstantType> getNumerator() {
    return super.getOperand1();
  }

  /**
   * Gets the denominator of the fraction.
   *
   * @return the denominator of the fraction.
   */
  public NumeralFormula<ConstantType> getDenominator() {
    return super.getOperand2();
  }

  @Override
  public <ReturnType> ReturnType accept(NumeralFormulaVisitor<ConstantType, ReturnType> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public <ReturnType, ParamType> ReturnType accept(
      ParameterizedNumeralFormulaVisitor<ConstantType, ParamType, ReturnType> pVisitor,
      ParamType pParameter) {
    return pVisitor.visit(this, pParameter);
  }

  /**
   * Gets an invariants formula representing the division of the given numerator formula by the
   * given denominator formula.
   *
   * @param pNumerator the numerator of the fraction.
   * @param pDenominator the denominator of the fraction.
   * @return an invariants formula representing the division of the given numerator formula by the
   *     given denominator formula.
   */
  static <ConstantType> Divide<ConstantType> of(
      NumeralFormula<ConstantType> pNumerator, NumeralFormula<ConstantType> pDenominator) {
    return new Divide<>(pNumerator, pDenominator);
  }
}
