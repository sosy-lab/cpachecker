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
 * Instances of this class represent binary and operations on invariants
 * formulae.
 *
 * @param <ConstantType> the type of the constants used in the formula.
 */
public class BinaryAnd<ConstantType> extends AbstractBinaryFormula<ConstantType> implements NumeralFormula<ConstantType> {

  /**
   * Creates a new binary and formula over the given operands.
   *
   * @param pOperand1 the first operand.
   * @param pOperand2 the second operand.
   */
  private BinaryAnd(NumeralFormula<ConstantType> pOperand1, NumeralFormula<ConstantType> pOperand2) {
    super("&", true, pOperand1, pOperand2);
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
   * Gets the binary and operation over the given operands.
   *
   * @param pOperand1 the first operand.
   * @param pOperand2 the second operand.
   *
   * @return the binary and operation over the given operands.
   */
  static <ConstantType> BinaryAnd<ConstantType> of(NumeralFormula<ConstantType> pOperand1, NumeralFormula<ConstantType> pOperand2) {
    return new BinaryAnd<>(pOperand1, pOperand2);
  }

}
