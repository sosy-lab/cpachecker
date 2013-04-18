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

import java.util.Collections;
import java.util.Map;

import org.sosy_lab.cpachecker.cpa.invariants.CompoundState;


public class PushAssumptionToEnvironmentVisitor implements ParameterizedInvariantsFormulaVisitor<CompoundState, CompoundState, Boolean> {

  private final Map<String, InvariantsFormula<CompoundState>> environment;

  private final FormulaEvaluationVisitor<CompoundState> evaluationVisitor;

  public PushAssumptionToEnvironmentVisitor(FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor, Map<String, InvariantsFormula<CompoundState>> pEnvironment) {
    this.evaluationVisitor = pEvaluationVisitor;
    this.environment = pEnvironment;
  }

  @Override
  public Boolean visit(Add<CompoundState> pAdd, CompoundState pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    CompoundState parameter = pAdd.accept(evaluationVisitor, getUnmodifiableEnvironment()).intersectWith(pParameter);
    if (parameter.isBottom()) {
      return false;
    }
    CompoundState leftValue = pAdd.getSummand1().accept(evaluationVisitor, getUnmodifiableEnvironment());
    CompoundState rightValue = pAdd.getSummand2().accept(evaluationVisitor, getUnmodifiableEnvironment());
    CompoundState pushLeftValue = parameter.add(rightValue.negate());
    CompoundState pushRightValue = parameter.add(leftValue.negate());
    if (!pAdd.getSummand1().accept(this, pushLeftValue)
        || !pAdd.getSummand2().accept(this, pushRightValue)) {
      return false;
    }
    return true;
  }

  @Override
  public Boolean visit(BinaryAnd<CompoundState> pAnd, CompoundState pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    return pAnd.accept(evaluationVisitor, getUnmodifiableEnvironment()).intersectsWith(pParameter);
  }

  @Override
  public Boolean visit(BinaryNot<CompoundState> pNot, CompoundState pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    CompoundState parameter = pNot.accept(evaluationVisitor, getUnmodifiableEnvironment()).intersectWith(pParameter);
    if (parameter.isBottom()) {
      return false;
    }
    if (!pNot.getFlipped().accept(this, parameter.invert())) {
      return false;
    }
    return true;
  }

  @Override
  public Boolean visit(BinaryOr<CompoundState> pOr, CompoundState pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    return pOr.accept(evaluationVisitor, getUnmodifiableEnvironment()).intersectsWith(pParameter);
  }

  @Override
  public Boolean visit(BinaryXor<CompoundState> pXor, CompoundState pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    return pXor.accept(evaluationVisitor, getUnmodifiableEnvironment()).intersectsWith(pParameter);
  }

  @Override
  public Boolean visit(Constant<CompoundState> pConstant, CompoundState pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    return pConstant.getValue().intersectsWith(pParameter);
  }

  @Override
  public Boolean visit(Divide<CompoundState> pDivide, CompoundState pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    CompoundState parameter = pDivide.accept(evaluationVisitor, getUnmodifiableEnvironment()).intersectWith(pParameter);
    if (parameter.isBottom()) {
      return false;
    }
    CompoundState leftValue = pDivide.getNumerator().accept(evaluationVisitor, getUnmodifiableEnvironment());
    CompoundState rightValue = pDivide.getDenominator().accept(evaluationVisitor, getUnmodifiableEnvironment());
    CompoundState pushLeftValue = parameter.multiply(rightValue.negate());
    CompoundState pushRightValue = leftValue.divide(parameter);
    if (!pDivide.getNumerator().accept(this, pushLeftValue)
        || !pDivide.getDenominator().accept(this, pushRightValue)) {
      return false;
    }
    return true;
  }

  @Override
  public Boolean visit(Equal<CompoundState> pEqual, CompoundState pParameter) {
    CompoundState parameter = pEqual.accept(evaluationVisitor, getUnmodifiableEnvironment()).intersectWith(pParameter);
    // If the truth of the equation is undecided, anything is possible and
    // no information can be gained
    if (!parameter.isDefinitelyTrue() && !parameter.isDefinitelyFalse()) {
      return !parameter.isBottom();
    }
    CompoundState leftValue = pEqual.getOperand1().accept(evaluationVisitor, getUnmodifiableEnvironment());
    CompoundState rightValue = pEqual.getOperand2().accept(evaluationVisitor, getUnmodifiableEnvironment());
    // If the equation is definitely true, push right to left and vice versa
    if (parameter.isDefinitelyTrue()) {
      return pEqual.getOperand1().accept(this, rightValue)
          && pEqual.getOperand2().accept(this, leftValue);
    }
    // If the equation is definitely false, push inverted singletons (if any)
    if (rightValue.isSingleton() && !pEqual.getOperand1().accept(this, rightValue.invert())) {
      return false;
    }
    if (leftValue.isSingleton() && !pEqual.getOperand2().accept(this, leftValue.invert())) {
      return false;
    }
    return true;
  }

  @Override
  public Boolean visit(LessThan<CompoundState> pLessThan, CompoundState pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    CompoundState parameter = pLessThan.accept(evaluationVisitor, getUnmodifiableEnvironment()).intersectWith(pParameter);
    if (parameter.isBottom()) {
      return false;
    }
    // If the truth of the equation is undecided, anything is possible and
    // no information can be gained
    if (!parameter.isDefinitelyTrue() && !parameter.isDefinitelyFalse()) {
      return !parameter.isBottom();
    }
    CompoundState leftValue = pLessThan.getOperand1().accept(evaluationVisitor, getUnmodifiableEnvironment());
    CompoundState rightValue = pLessThan.getOperand2().accept(evaluationVisitor, getUnmodifiableEnvironment());

    final CompoundState leftPushValue;
    final CompoundState rightPushValue;
    // If the equation is definitely true, push
    // (negative infinity to ((right upper bound) - 1)) to the left and
    // (((left lower bound) + 1) to infinity) to the right,
    // if the equation is definitely false, push
    // ((right lower bound) to infinity) to the left and
    // (negative infinity to (left upper bound)) to the right.
    if (parameter.isDefinitelyTrue()) {
      leftPushValue = rightValue.extendToNegativeInfinity().add(-1);
      rightPushValue = leftValue.extendToPositiveInfinity().add(1);
    } else {
      leftPushValue = rightValue.extendToPositiveInfinity();
      rightPushValue = leftValue.extendToNegativeInfinity();
    }
    return pLessThan.getOperand1().accept(this, leftPushValue)
        && pLessThan.getOperand2().accept(this, rightPushValue);
  }

  @Override
  public Boolean visit(LogicalAnd<CompoundState> pAnd, CompoundState pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    CompoundState parameter = pAnd.accept(evaluationVisitor, getUnmodifiableEnvironment()).intersectWith(pParameter);
    if (parameter.isBottom()) {
      return false;
    }
    // If the parameter is evaluates to a unique boolean value, hand it on
    if (parameter.isDefinitelyTrue() || parameter.isDefinitelyFalse()) {
      return pAnd.getOperand1().accept(this, parameter)
          && pAnd.getOperand2().accept(this, parameter);
    }
    return true;
  }

  @Override
  public Boolean visit(LogicalNot<CompoundState> pNot, CompoundState pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    CompoundState parameter = pNot.accept(evaluationVisitor, getUnmodifiableEnvironment()).intersectWith(pParameter);
    if (parameter.isBottom()) {
      return false;
    }
    // If the parameter is evaluates to a unique boolean value, hand it on
    if (parameter.isDefinitelyTrue() || parameter.isDefinitelyFalse()) {
      return pNot.getNegated().accept(this, parameter.logicalNot());
    }
    return true;
  }

  @Override
  public Boolean visit(Modulo<CompoundState> pModulo, CompoundState pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    return pModulo.accept(evaluationVisitor, getUnmodifiableEnvironment()).intersectsWith(pParameter);
  }

  @Override
  public Boolean visit(Multiply<CompoundState> pMultiply, CompoundState pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    CompoundState parameter = pMultiply.accept(evaluationVisitor, getUnmodifiableEnvironment()).intersectWith(pParameter);
    if (parameter.isBottom()) {
      return false;
    }
    CompoundState leftValue = pMultiply.getFactor1().accept(evaluationVisitor, getUnmodifiableEnvironment());
    CompoundState rightValue = pMultiply.getFactor2().accept(evaluationVisitor, getUnmodifiableEnvironment());
    CompoundState pushLeftValue = parameter.divide(rightValue);
    CompoundState pushRightValue = parameter.divide(leftValue);
    if (!pMultiply.getFactor1().accept(this, pushLeftValue)
        || !pMultiply.getFactor2().accept(this, pushRightValue)) {
      return false;
    }
    return true;
  }

  @Override
  public Boolean visit(Negate<CompoundState> pNegate, CompoundState pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    CompoundState parameter = pNegate.accept(evaluationVisitor, getUnmodifiableEnvironment()).intersectWith(pParameter);
    if (parameter.isBottom()) {
      return false;
    }
    return pNegate.getNegated().accept(this, parameter.negate());
  }

  @Override
  public Boolean visit(ShiftLeft<CompoundState> pShiftLeft, CompoundState pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    return pShiftLeft.accept(evaluationVisitor, getUnmodifiableEnvironment()).intersectsWith(pParameter);
  }

  @Override
  public Boolean visit(ShiftRight<CompoundState> pShiftRight, CompoundState pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    return pShiftRight.accept(evaluationVisitor, getUnmodifiableEnvironment()).intersectsWith(pParameter);
  }

  @Override
  public Boolean visit(Union<CompoundState> pUnion, CompoundState pParameter) {
    return pUnion.getOperand1().accept(this, pParameter) && pUnion.getOperand2().accept(this, pParameter);
  }

  @Override
  public Boolean visit(Variable<CompoundState> pVariable, CompoundState pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    CompoundState parameter = pVariable.accept(evaluationVisitor, getUnmodifiableEnvironment()).intersectWith(pParameter);
    if (parameter.isBottom()) {
      return false;
    }
    if (parameter.isTop()) {
      return true;
    }
    String varName = pVariable.getName();
    InvariantsFormula<CompoundState> resolved = getFromEnvironment(varName);
    if (!(resolved instanceof Constant<?>)) {
      return resolved.accept(this, parameter);
    }
    CompoundState resolvedValue = ((Constant<CompoundState>) resolved).getValue();
    CompoundState newValue = resolvedValue.intersectWith(parameter);
    if (newValue.isBottom()) {
      return false;
    }
    if (newValue.isTop()) {
      environment.remove(varName);
    } else {
      InvariantsFormulaManager ifm = InvariantsFormulaManager.INSTANCE;
      environment.put(varName, ifm.asConstant(newValue));
    }
    return true;
  }

  private InvariantsFormula<CompoundState> getFromEnvironment(String pVarName) {
    InvariantsFormula<CompoundState> result = environment.get(pVarName);
    if (result == null) {
      return InvariantsFormulaManager.INSTANCE.asConstant(evaluationVisitor.top());
    }
    return result;
  }

  private Map<? extends String, ? extends InvariantsFormula<CompoundState>> getUnmodifiableEnvironment() {
    return Collections.unmodifiableMap(environment);
  }

}
