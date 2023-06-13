// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

/**
 * Instances of this class represent right shifts of invariants formulae by other invariants
 * formulae.
 *
 * @param <ConstantType> the type of the constants used in the formulae.
 */
final class ShiftRight<ConstantType> extends AbstractBinaryFormula<ConstantType>
    implements NumeralFormula<ConstantType> {

  /**
   * Creates a new right shift formula over the given operands.
   *
   * @param pToShift the formula to be shifted by this operation.
   * @param pShiftDistance the distance by which to shift the first operand to the right.
   */
  private ShiftRight(
      NumeralFormula<ConstantType> pToShift, NumeralFormula<ConstantType> pShiftDistance) {
    super(">>", false, pToShift, pShiftDistance);
  }

  /**
   * Gets the formula shifted by this operation.
   *
   * @return the formula shifted by this operation.
   */
  public NumeralFormula<ConstantType> getShifted() {
    return super.getOperand1();
  }

  /**
   * Gets the shift distance formula of this operation.
   *
   * @return the shift distance formula of this operation.
   */
  public NumeralFormula<ConstantType> getShiftDistance() {
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
   * Gets an invariants formula representing the right shift of the first given operand by the
   * second given operand.
   *
   * @param pToShift the operand to be shifted.
   * @param pShiftDistance the shift distance.
   * @return an invariants formula representing the right shift of the first given operand by the
   *     second given operand.
   */
  static <ConstantType> ShiftRight<ConstantType> of(
      NumeralFormula<ConstantType> pToShift, NumeralFormula<ConstantType> pShiftDistance) {
    return new ShiftRight<>(pToShift, pShiftDistance);
  }
}
