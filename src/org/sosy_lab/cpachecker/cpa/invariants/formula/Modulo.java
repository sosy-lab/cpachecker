// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

/**
 * Instances of this class represent modulo invariants formulae over other invariants formulae.
 *
 * @param <ConstantType> the type of the constants used in the formulae.
 */
final class Modulo<ConstantType> extends AbstractBinaryFormula<ConstantType>
    implements NumeralFormula<ConstantType> {

  /**
   * Creates a new modulo formula over the given numerator and denominator formulae.
   *
   * @param pNumerator the numerator of the fraction.
   * @param pDenominator the denominator of the fraction.
   */
  private Modulo(
      NumeralFormula<ConstantType> pNumerator, NumeralFormula<ConstantType> pDenominator) {
    super("%", false, pNumerator, pDenominator);
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
   * Gets an invariants formula representing the modulo operation over the given operands.
   *
   * @param pNumerator the numerator of the fraction.
   * @param pDenominator the denominator of the fraction.
   * @return an invariants formula representing the modulo operation over the given operands.
   */
  static <ConstantType> Modulo<ConstantType> of(
      NumeralFormula<ConstantType> pNumerator, NumeralFormula<ConstantType> pDenominator) {
    return new Modulo<>(pNumerator, pDenominator);
  }
}
