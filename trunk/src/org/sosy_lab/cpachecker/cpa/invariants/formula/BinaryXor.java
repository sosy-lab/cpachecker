// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

/**
 * Instances of this class represent the binary exclusive or operation over invariants formulae.
 *
 * @param <ConstantType> the type of the constants used in the formula.
 */
final class BinaryXor<ConstantType> extends AbstractBinaryFormula<ConstantType>
    implements NumeralFormula<ConstantType> {

  private BinaryXor(
      NumeralFormula<ConstantType> pOperand1, NumeralFormula<ConstantType> pOperand2) {
    super("^", true, pOperand1, pOperand2);
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
   * Gets an invariants formula representing the binary exclusive or operation over the given
   * operands.
   *
   * @param pOperand1 the first operand.
   * @param pOperand2 the second operand.
   * @return an invariants formula representing the binary exclusive or operation over the given
   *     operands.
   */
  static <ConstantType> BinaryXor<ConstantType> of(
      NumeralFormula<ConstantType> pOperand1, NumeralFormula<ConstantType> pOperand2) {
    return new BinaryXor<>(pOperand1, pOperand2);
  }
}
