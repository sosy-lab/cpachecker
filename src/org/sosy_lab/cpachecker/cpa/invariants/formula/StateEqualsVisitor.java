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
 * Instances of this class are parameterized compound state invariants formula
 * visitors used to determine whether a visited formula represents the same
 * state as the formula given as the second parameter in the context of the
 * environment provided by the visitor.
 */
public class StateEqualsVisitor extends DefaultParameterizedFormulaVisitor<CompoundInterval, InvariantsFormula<CompoundInterval>, Boolean> {

  /**
   * The evaluation visitor used to evaluate formulae to states that can be
   * compared when all else fails.
   */
  private final FormulaEvaluationVisitor<CompoundInterval> evaluationVisitor;

  /**
   * The environment providing the context for the analysis of state equality.
   */
  private final Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> environment;

  /**
   * Creates a new visitor for determining the equality of states represented
   * by formulae with the given evaluation visitor and environment.
   *
   * @param pEvaluationVisitor the evaluation visitor used to evaluate formulae
   * to states that can be compared when all else fails.
   * @param pEnvironment the environment providing the context for the analysis
   * of state equality.
   */
  public StateEqualsVisitor(FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    this.evaluationVisitor = pEvaluationVisitor;
    this.environment = pEnvironment;
  }

  @Override
  protected Boolean visitDefault(InvariantsFormula<CompoundInterval> pFormula, InvariantsFormula<CompoundInterval> pOther) {
    CompoundInterval leftValue = pFormula.accept(evaluationVisitor, environment);
    CompoundInterval rightValue = pOther.accept(evaluationVisitor, environment);
    return leftValue.logicalEquals(rightValue).isDefinitelyTrue();
  }

  /**
   * Tries to determine the state equality of two commutative formulae given
   * as each two of their operands.
   *
   * @param pO1 the first operand of the first formula.
   * @param pO2 the second operand of the first formula.
   * @param pOtherO1 the first operand of the second formula.
   * @param pOtherO2 the second operand of the second formula.
   *
   * @return <code>true</code> if both formulae definitely represent equal
   * states, <code>false</code> if the represented states are not equal or
   * their equality could not be determined.
   */
  private Boolean visitCommutative(InvariantsFormula<CompoundInterval> pO1,
      InvariantsFormula<CompoundInterval> pO2,
      InvariantsFormula<CompoundInterval> pOtherO1,
      InvariantsFormula<CompoundInterval> pOtherO2) {
    return visitNonCommutative(pO1, pO2, pOtherO1, pOtherO2)
        || pO1.accept(this, pOtherO2)
        && pO2.accept(this, pOtherO1);
  }

  /**
   * Tries to determine the state equality of two non-commutative formulae
   * given as each two of their operands.
   *
   * @param pO1 the first operand of the first formula.
   * @param pO2 the second operand of the first formula.
   * @param pOtherO1 the first operand of the second formula.
   * @param pOtherO2 the second operand of the second formula.
   *
   * @return <code>true</code> if both formulae definitely represent equal
   * states, <code>false</code> if the represented states are not equal or
   * their equality could not be determined.
   */
  private Boolean visitNonCommutative(InvariantsFormula<CompoundInterval> pO1,
      InvariantsFormula<CompoundInterval> pO2,
      InvariantsFormula<CompoundInterval> pOtherO1,
      InvariantsFormula<CompoundInterval> pOtherO2) {
    return pO1.accept(this, pOtherO1)
        && pO2.accept(this, pOtherO2);
  }

  @Override
  public Boolean visit(Add<CompoundInterval> pAdd, InvariantsFormula<CompoundInterval> pOther) {
    if (pOther instanceof Add<?>) {
      Add<CompoundInterval> other = (Add<CompoundInterval>) pOther;
      if (visitCommutative(pAdd.getSummand1(), pAdd.getSummand2(),
          other.getSummand1(), other.getSummand2())) {
        return true;
      }
    }
    return visitDefault(pAdd, pOther);
  }

  @Override
  public Boolean visit(BinaryAnd<CompoundInterval> pAnd, InvariantsFormula<CompoundInterval> pOther) {
    if (pOther instanceof BinaryAnd<?>) {
      BinaryAnd<CompoundInterval> other = (BinaryAnd<CompoundInterval>) pOther;
      if (visitCommutative(pAnd.getOperand1(), pAnd.getOperand2(),
          other.getOperand1(), other.getOperand2())) {
        return true;
      }
    }
    return visitDefault(pAnd, pOther);
  }

  @Override
  public Boolean visit(BinaryNot<CompoundInterval> pNot, InvariantsFormula<CompoundInterval> pOther) {
    if (pOther instanceof BinaryNot<?>) {
      BinaryNot<CompoundInterval> other = (BinaryNot<CompoundInterval>) pOther;
      if (pNot.getFlipped().accept(this, other.getFlipped())) {
        return true;
      }
    }
    return visitDefault(pNot, pOther);
  }

  @Override
  public Boolean visit(BinaryOr<CompoundInterval> pOr, InvariantsFormula<CompoundInterval> pOther) {
    if (pOther instanceof BinaryOr<?>) {
      BinaryOr<CompoundInterval> other = (BinaryOr<CompoundInterval>) pOther;
      if (visitCommutative(pOr.getOperand1(), pOr.getOperand2(),
          other.getOperand1(), other.getOperand2())) {
        return true;
      }
    }
    return visitDefault(pOr, pOther);
  }

  @Override
  public Boolean visit(BinaryXor<CompoundInterval> pXor, InvariantsFormula<CompoundInterval> pOther) {
    if (pOther instanceof BinaryXor<?>) {
      BinaryXor<CompoundInterval> other = (BinaryXor<CompoundInterval>) pOther;
      if (visitCommutative(pXor.getOperand1(), pXor.getOperand2(),
          other.getOperand1(), other.getOperand2())) {
        return true;
      }
    }
    return visitDefault(pXor, pOther);
  }

  @Override
  public Boolean visit(Divide<CompoundInterval> pDivide, InvariantsFormula<CompoundInterval> pOther) {
    if (pOther instanceof Divide<?>) {
      Divide<CompoundInterval> other = (Divide<CompoundInterval>) pOther;
      if (visitNonCommutative(pDivide.getNumerator(), pDivide.getDenominator(),
          other.getNumerator(), other.getDenominator())) {
        return true;
      }
    }
    return visitDefault(pDivide, pOther);
  }

  @Override
  public Boolean visit(Equal<CompoundInterval> pEqual, InvariantsFormula<CompoundInterval> pOther) {
    if (pOther instanceof Equal<?>) {
      Equal<CompoundInterval> other = (Equal<CompoundInterval>) pOther;
      if (visitCommutative(pEqual.getOperand1(), pEqual.getOperand2(),
          other.getOperand1(), other.getOperand2())) {
        return true;
      }
    }
    return visitDefault(pEqual, pOther);
  }

  @Override
  public Boolean visit(LessThan<CompoundInterval> pLessThan, InvariantsFormula<CompoundInterval> pOther) {
    if (pOther instanceof LessThan<?>) {
      LessThan<CompoundInterval> other = (LessThan<CompoundInterval>) pOther;
      if (visitNonCommutative(pLessThan.getOperand1(), pLessThan.getOperand2(),
          other.getOperand1(), other.getOperand2())) {
        return true;
      }
    }
    return visitDefault(pLessThan, pOther);
  }

  @Override
  public Boolean visit(LogicalAnd<CompoundInterval> pAnd, InvariantsFormula<CompoundInterval> pOther) {
    if (pOther instanceof LogicalAnd<?>) {
      LogicalAnd<CompoundInterval> other = (LogicalAnd<CompoundInterval>) pOther;
      if (visitCommutative(pAnd.getOperand1(), pAnd.getOperand2(),
          other.getOperand1(), other.getOperand2())) {
        return true;
      }
    }
    return visitDefault(pAnd, pOther);
  }

  @Override
  public Boolean visit(LogicalNot<CompoundInterval> pNot, InvariantsFormula<CompoundInterval> pOther) {
    if (pOther instanceof LogicalNot<?>) {
      LogicalNot<CompoundInterval> other = (LogicalNot<CompoundInterval>) pOther;
      if (pNot.getNegated().accept(this, other.getNegated())) {
        return true;
      }
    }
    return visitDefault(pNot, pOther);
  }

  @Override
  public Boolean visit(Modulo<CompoundInterval> pModulo, InvariantsFormula<CompoundInterval> pOther) {
    if (pOther instanceof Modulo<?>) {
      Modulo<CompoundInterval> other = (Modulo<CompoundInterval>) pOther;
      if (visitNonCommutative(pModulo.getNumerator(), pModulo.getDenominator(),
          other.getNumerator(), other.getDenominator())) {
        return true;
      }
    }
    return visitDefault(pModulo, pOther);
  }

  @Override
  public Boolean visit(Multiply<CompoundInterval> pMultiply, InvariantsFormula<CompoundInterval> pOther) {
    if (pOther instanceof Multiply<?>) {
      Multiply<CompoundInterval> other = (Multiply<CompoundInterval>) pOther;
      if (visitCommutative(pMultiply.getFactor1(), pMultiply.getFactor2(),
          other.getFactor1(), other.getFactor2())) {
        return true;
      }
    }
    return visitDefault(pMultiply, pOther);
  }

  @Override
  public Boolean visit(ShiftLeft<CompoundInterval> pShiftLeft, InvariantsFormula<CompoundInterval> pOther) {
    if (pOther instanceof ShiftLeft<?>) {
      ShiftLeft<CompoundInterval> other = (ShiftLeft<CompoundInterval>) pOther;
      if (visitNonCommutative(pShiftLeft.getShifted(), pShiftLeft.getShiftDistance(),
          other.getShifted(), other.getShiftDistance())) {
        return true;
      }
    }
    return visitDefault(pShiftLeft, pOther);
  }

  @Override
  public Boolean visit(ShiftRight<CompoundInterval> pShiftRight, InvariantsFormula<CompoundInterval> pOther) {
    if (pOther instanceof ShiftRight<?>) {
      ShiftRight<CompoundInterval> other = (ShiftRight<CompoundInterval>) pOther;
      if (visitNonCommutative(pShiftRight.getShifted(), pShiftRight.getShiftDistance(),
          other.getShifted(), other.getShiftDistance())) {
        return true;
      }
    }
    return visitDefault(pShiftRight, pOther);
  }

  @Override
  public Boolean visit(Union<CompoundInterval> pUnion, InvariantsFormula<CompoundInterval> pOther) {
    if (pOther instanceof Union<?>) {
      Union<CompoundInterval> other = (Union<CompoundInterval>) pOther;
      if (visitCommutative(pUnion.getOperand1(), pUnion.getOperand2(),
          other.getOperand1(), other.getOperand2())) {
        return true;
      }
    }
    return visitDefault(pUnion, pOther);
  }

  @Override
  public Boolean visit(Variable<CompoundInterval> pVariable, InvariantsFormula<CompoundInterval> pOther) {
    if (pVariable.equals(pOther)) {
      return true;
    }
    String leftVarName = pVariable.getName();
    InvariantsFormula<CompoundInterval> resolvedLeft = this.environment.get(leftVarName);
    resolvedLeft = resolvedLeft == null ? CompoundStateFormulaManager.INSTANCE.asConstant(CompoundInterval.top()) : resolvedLeft;
    if (pOther instanceof Variable<?>) {
      String rightVarName = ((Variable<?>) pOther).getName();
      if (leftVarName.equals(rightVarName)) {
        return true;
      }
      InvariantsFormula<CompoundInterval> resolvedRight = this.environment.get(rightVarName);
      resolvedRight = resolvedRight == null ? CompoundStateFormulaManager.INSTANCE.asConstant(CompoundInterval.top()) : resolvedRight;
      if (resolvedLeft.accept(this, resolvedRight)) {
        return true;
      }
      if (resolvedRight.accept(this, pVariable)) {
        return true;
      }
    }
    if (resolvedLeft.accept(this, pOther)) {
      return true;
    }
    return visitDefault(pVariable, pOther);
  }

}
