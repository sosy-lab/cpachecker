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
 * Instances of this class represent invariants formula additions.
 *
 * @param <ConstantType> the type of the constants used in the formula.
 */
public class Add<ConstantType> extends AbstractBinaryFormula<ConstantType> implements NumeralFormula<ConstantType> {

  /**
   * Creates a new addition formula for the given summands.
   *
   * @param pSummand1 the first summand.
   * @param pSummand2 the second summand.
   */
  private Add(NumeralFormula<ConstantType> pSummand1, NumeralFormula<ConstantType> pSummand2) {
    super("+", true, pSummand1, pSummand2);
  }

  /**
   * Gets the first summand.
   *
   * @return the first summand.
   */
  public NumeralFormula<ConstantType> getSummand1() {
    return super.getOperand1();
  }

  /**
   * Gets the second summand.
   *
   * @return the second summand.
   */
  public NumeralFormula<ConstantType> getSummand2() {
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
   * Gets the sum of the given formulae as a formula.
   *
   * @param pSummand1 the first summand.
   * @param pSummand2 the second summand.
   *
   * @return the sum of the given formulae.
   */
  static <ConstantType> Add<ConstantType> of(NumeralFormula<ConstantType> pSummand1, NumeralFormula<ConstantType> pSummand2) {
    return new Add<>(pSummand1, pSummand2);
  }

}
