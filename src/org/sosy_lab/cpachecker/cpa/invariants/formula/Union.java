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
 * Instances of this class represent unions of other invariants formulae.
 *
 * @param <ConstantType> the type of the constants used in the formulae.
 */
public class Union<ConstantType> extends AbstractFormula<ConstantType> implements InvariantsFormula<ConstantType> {

  /**
   * The first operand of the union.
   */
  private final InvariantsFormula<ConstantType> operand1;

  /**
   * The second operand of the union.
   */
  private final InvariantsFormula<ConstantType> operand2;

  /**
   * Creates a new union of the given formulae.
   *
   * @param pOperand1 the first operand of the union.
   * @param pOperand2 the second operand of the union.
   */
  private Union(InvariantsFormula<ConstantType> pOperand1,
      InvariantsFormula<ConstantType> pOperand2) {
    this.operand1 = pOperand1;
    this.operand2 = pOperand2;
  }

  /**
   * Gets the first operand of the union.
   *
   * @return the first operand of the union.
   */
  public InvariantsFormula<ConstantType> getOperand1() {
    return this.operand1;
  }

  /**
   * Gets the second operand of the union.
   *
   * @return the second operand of the union.
   */
  public InvariantsFormula<ConstantType> getOperand2() {
    return this.operand2;
  }

  @Override
  public boolean equals(Object pO) {
    if (pO == this) {
      return true;
    }
    if (pO instanceof Union<?>) {
      Union<?> other = (Union<?>) pO;
      return getOperand1().equals(other.getOperand1()) && getOperand2().equals(other.getOperand2())
          || getOperand2().equals(other.getOperand1()) && getOperand1().equals(other.getOperand2());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return getOperand1().hashCode() | getOperand2().hashCode();
  }

  @Override
  public String toString() {
    return String.format("(%s u %s)", getOperand1(), getOperand2());
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
   * Gets an invariants formula representing the union of the given invariants
   * formulae.
   *
   * @param pOperand1 the first operand.
   * @param pOperand2 the second operand.
   *
   * @return an invariants formula representing the union of the given invariants
   * formulae.
   */
  public static <ConstantType> Union<ConstantType> of(InvariantsFormula<ConstantType> pOperand1,
      InvariantsFormula<ConstantType> pOperand2) {
    return new Union<>(pOperand1, pOperand2);
  }

}
