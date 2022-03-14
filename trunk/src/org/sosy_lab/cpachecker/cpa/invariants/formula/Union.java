// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

/**
 * Instances of this class represent unions of other invariants formulae.
 *
 * @param <ConstantType> the type of the constants used in the formulae.
 */
public final class Union<ConstantType> extends AbstractBinaryFormula<ConstantType>
    implements NumeralFormula<ConstantType> {

  /**
   * Creates a new union of the given formulae.
   *
   * @param pOperand1 the first operand of the union.
   * @param pOperand2 the second operand of the union.
   */
  private Union(NumeralFormula<ConstantType> pOperand1, NumeralFormula<ConstantType> pOperand2) {
    super("u", true, pOperand1, pOperand2);
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
   * Gets an invariants formula representing the union of the given invariants formulae.
   *
   * @param pOperand1 the first operand.
   * @param pOperand2 the second operand.
   * @return an invariants formula representing the union of the given invariants formulae.
   */
  public static <ConstantType> Union<ConstantType> of(
      NumeralFormula<ConstantType> pOperand1, NumeralFormula<ConstantType> pOperand2) {
    return new Union<>(pOperand1, pOperand2);
  }
}
