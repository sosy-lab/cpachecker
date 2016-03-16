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
package org.sosy_lab.cpachecker.cpa.invariants.formula;

/**
 * Instances of this class represent right shifts of invariants formulae by
 * other invariants formulae.
 *
 * @param <ConstantType> the type of the constants used in the formulae.
 */
public class ShiftRight<ConstantType> extends AbstractBinaryFormula<ConstantType> implements NumeralFormula<ConstantType> {

  /**
   * Creates a new right shift formula over the given operands.
   *
   * @param pToShift the formula to be shifted by this operation.
   * @param pShiftDistance the distance by which to shift the first operand to
   * the right.
   */
  private ShiftRight(NumeralFormula<ConstantType> pToShift,
      NumeralFormula<ConstantType> pShiftDistance) {
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
      ParameterizedNumeralFormulaVisitor<ConstantType, ParamType, ReturnType> pVisitor, ParamType pParameter) {
    return pVisitor.visit(this, pParameter);
  }

  /**
   * Gets an invariants formula representing the right shift of the first given
   * operand by the second given operand.
   *
   * @param pToShift the operand to be shifted.
   * @param pShiftDistance the shift distance.
   *
   * @return an invariants formula representing the right shift of the first
   * given operand by the second given operand.
   */
  static <ConstantType> ShiftRight<ConstantType> of(NumeralFormula<ConstantType> pToShift, NumeralFormula<ConstantType> pShiftDistance) {
    return new ShiftRight<>(pToShift, pShiftDistance);
  }

}
