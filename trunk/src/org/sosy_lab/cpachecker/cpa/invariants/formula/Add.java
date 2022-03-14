// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

/**
 * Instances of this class represent invariants formula additions.
 *
 * @param <ConstantType> the type of the constants used in the formula.
 */
public final class Add<ConstantType> extends AbstractBinaryFormula<ConstantType>
    implements NumeralFormula<ConstantType> {

  /**
   * Creates a new addition formula for the given summands.
   *
   * @param pSummand1 the first summand.
   * @param pSummand2 the second summand.
   */
  private Add(NumeralFormula<ConstantType> pSummand1, NumeralFormula<ConstantType> pSummand2) {
    super("+", true, pSummand1, pSummand2);
  }

  /**
   * Gets the first summand.
   *
   * @return the first summand.
   */
  public NumeralFormula<ConstantType> getSummand1() {
    return super.getOperand1();
  }

  /**
   * Gets the second summand.
   *
   * @return the second summand.
   */
  public NumeralFormula<ConstantType> getSummand2() {
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
   * Gets the sum of the given formulae as a formula.
   *
   * @param pSummand1 the first summand.
   * @param pSummand2 the second summand.
   * @return the sum of the given formulae.
   */
  static <ConstantType> Add<ConstantType> of(
      NumeralFormula<ConstantType> pSummand1, NumeralFormula<ConstantType> pSummand2) {
    return new Add<>(pSummand1, pSummand2);
  }
}
