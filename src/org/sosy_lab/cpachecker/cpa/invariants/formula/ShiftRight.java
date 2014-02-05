/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
public class ShiftRight<ConstantType> extends AbstractFormula<ConstantType> implements InvariantsFormula<ConstantType> {

  /**
   * The formula shifted by this operation.
   */
  private final InvariantsFormula<ConstantType> shifted;

  /**
   * The shift distance formula of this operation.
   */
  private final InvariantsFormula<ConstantType> shiftDistance;

  /**
   * Creates a new right shift formula over the given operands.
   *
   * @param pToShift the formula to be shifted by this operation.
   * @param pShiftDistance the distance by which to shift the first operand to
   * the right.
   */
  private ShiftRight(InvariantsFormula<ConstantType> pToShift,
      InvariantsFormula<ConstantType> pShiftDistance) {
    this.shifted = pToShift;
    this.shiftDistance = pShiftDistance;
  }

  /**
   * Gets the formula shifted by this operation.
   *
   * @return the formula shifted by this operation.
   */
  public InvariantsFormula<ConstantType> getShifted() {
    return this.shifted;
  }

  /**
   * Gets the shift distance formula of this operation.
   *
   * @return the shift distance formula of this operation.
   */
  public InvariantsFormula<ConstantType> getShiftDistance() {
    return this.shiftDistance;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof ShiftRight) {
      ShiftRight<?> other = (ShiftRight<?>) o;
      return getShifted().equals(other.getShifted()) && getShiftDistance().equals(other.getShiftDistance());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return getShifted().hashCode() >> getShiftDistance().hashCode();
  }

  @Override
  public String toString() {
    return String.format("(%s >> %s)", getShifted(), getShiftDistance());
  }

  @Override
  public <ReturnType> ReturnType accept(InvariantsFormulaVisitor<ConstantType, ReturnType> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public <ReturnType, ParamType> ReturnType accept(
      ParameterizedInvariantsFormulaVisitor<ConstantType, ParamType, ReturnType> pVisitor, ParamType pParameter) {
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
  static <ConstantType> ShiftRight<ConstantType> of(InvariantsFormula<ConstantType> pToShift, InvariantsFormula<ConstantType> pShiftDistance) {
    return new ShiftRight<>(pToShift, pShiftDistance);
  }

}
