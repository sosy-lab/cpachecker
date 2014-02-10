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
 * Instances of this class represent the binary negation of an invariants formula.
 *
 * @param <ConstantType> the type of the constants used in the formula.
 */
public class BinaryNot<ConstantType> extends AbstractFormula<ConstantType> implements InvariantsFormula<ConstantType> {

  /**
   * The operand of the bit flip operation.
   */
  final InvariantsFormula<ConstantType> flipped;

  /**
   * Creates a new binary negation formula over the given operand.
   *
   * @param pToFlip the operand of the bit flip operation.
   */
  private BinaryNot(InvariantsFormula<ConstantType> pToFlip) {
    this.flipped = pToFlip;
  }

  /**
   * Gets the operand of the bit flip operation.
   *
   * @return the operand of the bit flip operation.
   */
  public InvariantsFormula<ConstantType> getFlipped() {
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
  public <ReturnType> ReturnType accept(InvariantsFormulaVisitor<ConstantType, ReturnType> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public <ReturnType, ParamType> ReturnType accept(
      ParameterizedInvariantsFormulaVisitor<ConstantType, ParamType, ReturnType> pVisitor, ParamType pParameter) {
    return pVisitor.visit(this, pParameter);
  }

  /**
   * Gets the binary negation of the given formula.
   *
   * @param pToFlip the operand of the bit flip operation.
   *
   * @return the binary negation of the given formula.
   */
  static <ConstantType> BinaryNot<ConstantType> of(InvariantsFormula<ConstantType> pToFlip) {
    return new BinaryNot<>(pToFlip);
  }

}
