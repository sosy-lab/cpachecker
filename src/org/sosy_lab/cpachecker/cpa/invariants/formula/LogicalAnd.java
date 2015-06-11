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
 * Instances of this class represent logical conjunctions over invariants
 * formulae.
 *
 * @param <ConstantType> the type of the constant used in the formulae.
 */
public class LogicalAnd<ConstantType> extends AbstractBinaryFormula<ConstantType> implements InvariantsFormula<ConstantType> {

  /**
   * Creates a new conjunction over the given operands.
   *
   * @param pOperand1 the first operand of the conjunction.
   * @param pOperand2 the second operand of the conjunction.
   */
  private LogicalAnd(InvariantsFormula<ConstantType> pOperand1,
      InvariantsFormula<ConstantType> pOperand2) {
    super("&&", true, pOperand1, pOperand2);
    // TODO is LogicalAnd really commutative in this context?
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
   * Gets an invariants formula representing the logical conjunction over the
   * given operands.
   *
   * @param pOperand1 the first operand of the conjunction.
   * @param pOperand2 the second operand of the conjunction.
   *
   * @return an invariants formula representing the logical conjunction over the
   * given operands.
   */
  static <ConstantType> LogicalAnd<ConstantType> of(InvariantsFormula<ConstantType> pOperand1, InvariantsFormula<ConstantType> pOperand2) {
    return new LogicalAnd<>(pOperand1, pOperand2);
  }

}
