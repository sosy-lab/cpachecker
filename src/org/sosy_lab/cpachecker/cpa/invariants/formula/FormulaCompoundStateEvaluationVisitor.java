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

import java.util.Map;

import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;

/**
 * Instances of this class are visitors for compound state invariants formulae
 * which are used to evaluate the visited formulae to compound states. This
 * visitor uses a stronger evaluation strategy than a
 * {@link FormulaAbstractionVisitor} in order to enable the CPA strategy to
 * obtain very exact values for the expressions in the analyzed code.
 */
public class FormulaCompoundStateEvaluationVisitor implements FormulaEvaluationVisitor<CompoundInterval> {

  /**
   * A visitor for compound state invariants formulae used to determine whether
   * or not the visited formula is a genuine boolean formula.
   */
  private static final IsBooleanFormulaVisitor<CompoundInterval> IS_BOOLEAN_FORMULA_VISITOR =
      new IsBooleanFormulaVisitor<>();

  @Override
  public CompoundInterval visit(Add<CompoundInterval> pAdd, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return pAdd.getSummand1().accept(this, pEnvironment).add(pAdd.getSummand2().accept(this, pEnvironment));
  }

  @Override
  public CompoundInterval visit(BinaryAnd<CompoundInterval> pAnd, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return pAnd.getOperand1().accept(this, pEnvironment).binaryAnd(pAnd.getOperand2().accept(this, pEnvironment));
  }

  @Override
  public CompoundInterval visit(BinaryNot<CompoundInterval> pNot, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return pNot.getFlipped().accept(this, pEnvironment).binaryNot();
  }

  @Override
  public CompoundInterval visit(BinaryOr<CompoundInterval> pOr, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return pOr.getOperand1().accept(this, pEnvironment).binaryOr(pOr.getOperand2().accept(this, pEnvironment));
  }

  @Override
  public CompoundInterval visit(BinaryXor<CompoundInterval> pXor, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return pXor.getOperand1().accept(this, pEnvironment).binaryXor(pXor.getOperand2().accept(this, pEnvironment));
  }

  @Override
  public CompoundInterval visit(Constant<CompoundInterval> pConstant, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return pConstant.getValue();
  }

  @Override
  public CompoundInterval visit(Divide<CompoundInterval> pDivide, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return pDivide.getNumerator().accept(this, pEnvironment).divide(pDivide.getDenominator().accept(this, pEnvironment));
  }

  @Override
  public CompoundInterval visit(Equal<CompoundInterval> pEqual, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    CompoundInterval operand1 = pEqual.getOperand1().accept(this, pEnvironment);
    CompoundInterval operand2 = pEqual.getOperand2().accept(this, pEnvironment);
    /*
     *  If both operands of the equation are boolean formulae and each of their
     *  evaluations is either definitely true or definitely false, the
     *  corresponding boolean value can be returned, which is more exact than
     *  using the logical equality on compound states which considers numbers
     *  instead of boolean values and would return top for true == true.
     */
    if (pEqual.getOperand1().accept(IS_BOOLEAN_FORMULA_VISITOR)
        && pEqual.getOperand2().accept(IS_BOOLEAN_FORMULA_VISITOR)) {
      if (operand1.isDefinitelyTrue() || operand1.isDefinitelyFalse()) {
        if (operand2.isDefinitelyTrue()) {
          return operand1;
        } else if (operand2.isDefinitelyFalse()) {
          return operand2;
        }
      }
    }
    return operand1.logicalEquals(operand2);
  }

  @Override
  public CompoundInterval visit(LessThan<CompoundInterval> pLessThan, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return pLessThan.getOperand1().accept(this, pEnvironment).lessThan(pLessThan.getOperand2().accept(this, pEnvironment));
  }

  @Override
  public CompoundInterval visit(LogicalAnd<CompoundInterval> pAnd, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return pAnd.getOperand1().accept(this, pEnvironment).logicalAnd(pAnd.getOperand2().accept(this, pEnvironment));
  }

  @Override
  public CompoundInterval visit(LogicalNot<CompoundInterval> pNot, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return pNot.getNegated().accept(this, pEnvironment).logicalNot();
  }

  @Override
  public CompoundInterval visit(Modulo<CompoundInterval> pModulo, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return pModulo.getNumerator().accept(this, pEnvironment).modulo(pModulo.getDenominator().accept(this, pEnvironment));
  }

  @Override
  public CompoundInterval visit(Multiply<CompoundInterval> pMultiply, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return pMultiply.getFactor1().accept(this, pEnvironment).multiply(pMultiply.getFactor2().accept(this, pEnvironment));
  }

  @Override
  public CompoundInterval visit(ShiftLeft<CompoundInterval> pShiftLeft, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return pShiftLeft.getShifted().accept(this, pEnvironment).shiftLeft(pShiftLeft.getShiftDistance().accept(this, pEnvironment));
  }

  @Override
  public CompoundInterval visit(ShiftRight<CompoundInterval> pShiftRight, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return pShiftRight.getShifted().accept(this, pEnvironment).shiftRight(pShiftRight.getShiftDistance().accept(this, pEnvironment));
  }

  @Override
  public CompoundInterval visit(Union<CompoundInterval> pUnion, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return pUnion.getOperand1().accept(this, pEnvironment).unionWith(pUnion.getOperand2().accept(this, pEnvironment));
  }

  @Override
  public CompoundInterval visit(Variable<CompoundInterval> pVariable, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    InvariantsFormula<CompoundInterval> varState = pEnvironment.get(pVariable.getName());
    if (varState == null) {
      return CompoundInterval.top();
    }
    return varState.accept(this, pEnvironment);
  }

}
