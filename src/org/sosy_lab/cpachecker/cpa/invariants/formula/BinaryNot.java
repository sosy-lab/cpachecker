// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

/**
 * Instances of this class represent the binary negation of an invariants formula.
 *
 * @param <ConstantType> the type of the constants used in the formula.
 */
class BinaryNot<ConstantType> extends AbstractFormula<ConstantType>
    implements NumeralFormula<ConstantType> {

  /** The operand of the bit flip operation. */
  final NumeralFormula<ConstantType> flipped;

  /**
   * Creates a new binary negation formula over the given operand.
   *
   * @param pToFlip the operand of the bit flip operation.
   */
  private BinaryNot(NumeralFormula<ConstantType> pToFlip) {
    super(pToFlip.getTypeInfo());
    this.flipped = pToFlip;
  }

  /**
   * Gets the operand of the bit flip operation.
   *
   * @return the operand of the bit flip operation.
   */
  public NumeralFormula<ConstantType> getFlipped() {
    return this.flipped;
  }

  @Override
  public String toString() {
    return String.format("(~%s)", getFlipped());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof BinaryNot<?>) {
      return getFlipped().equals(((BinaryNot<?>) o).getFlipped());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return ~getFlipped().hashCode();
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
   * Gets the binary negation of the given formula.
   *
   * @param pToFlip the operand of the bit flip operation.
   * @return the binary negation of the given formula.
   */
  static <ConstantType> BinaryNot<ConstantType> of(NumeralFormula<ConstantType> pToFlip) {
    return new BinaryNot<>(pToFlip);
  }
}
