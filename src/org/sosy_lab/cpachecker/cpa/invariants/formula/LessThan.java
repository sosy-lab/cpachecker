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
 * Instances of this class represent less-than inequations over invariants
 * formulae.
 *
 * @param <ConstantType> the type of the constants used in the formula.
 */
public class LessThan<ConstantType> extends AbstractBinaryFormula<ConstantType> implements InvariantsFormula<ConstantType> {

  /**
   * Creates a new less-than inequation over the given operands.
   *
   * @param pOperand1 the left operand of the inequation.
   * @param pOperand2 the right operand of the inequation.
   */
  private LessThan(InvariantsFormula<ConstantType> pOperand1,
      InvariantsFormula<ConstantType> pOperand2) {
    super("<", false, pOperand1, pOperand2);
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
   * Gets an invariants formula representing a less-than inequation over the
   * given operands.
   *
   * @param pOperand1 the left operand of the inequation.
   * @param pOperand2 the right operand of the inequation.
   *
   * @return an invariants formula representing a less-than inequation over the
   * given operands.
   */
  static <ConstantType> LessThan<ConstantType> of(InvariantsFormula<ConstantType> pOperand1, InvariantsFormula<ConstantType> pOperand2) {
    return new LessThan<>(pOperand1, pOperand2);
  }

}
