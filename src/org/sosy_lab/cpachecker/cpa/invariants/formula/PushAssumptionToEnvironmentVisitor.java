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

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;
import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;

/**
 * Instances of this class are parameterized compound state invariants formula
 * visitors used to push information from an assumption into the environment.
 * The visited formulae are the expressions for which the states provided as
 * the additional parameters are assumed.
 */
public class PushAssumptionToEnvironmentVisitor implements ParameterizedInvariantsFormulaVisitor<CompoundInterval, CompoundInterval, Boolean> {

  /**
   * The environment to push the gained information into.
   */
  private final Map<String, InvariantsFormula<CompoundInterval>> environment;

  /**
   * The evaluation visitor used to evaluate compound state invariants formulae
   * to compound states.
   */
  private final FormulaEvaluationVisitor<CompoundInterval> evaluationVisitor;

  private final CachingEvaluationVisitor<CompoundInterval> cachingEvaluationVisitor;

  /**
   * Creates a new visitor for pushing information obtained from assuming given
   * states for the visited formulae into the given environment.
   *
   * @param pEvaluationVisitor the evaluation visitor used to evaluate compound
   * state invariants formulae to compound states.
   * @param pEnvironment the environment to push the gained information into.
   * Obviously, this environment must be mutable.
   */
  public PushAssumptionToEnvironmentVisitor(FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor, Map<String, InvariantsFormula<CompoundInterval>> pEnvironment) {
    this.evaluationVisitor = pEvaluationVisitor;
    this.environment = pEnvironment;
    this.cachingEvaluationVisitor = new CachingEvaluationVisitor<>(environment, evaluationVisitor);
  }

  private CompoundInterval evaluate(InvariantsFormula<CompoundInterval> pFormula) {
    return pFormula.accept(this.cachingEvaluationVisitor);
  }

  @Override
  public Boolean visit(Add<CompoundInterval> pAdd, CompoundInterval pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    CompoundInterval parameter = evaluate(pAdd).intersectWith(pParameter);
    if (parameter.isBottom()) {
      return false;
    }
    CompoundInterval leftValue = evaluate(pAdd.getSummand1());
    CompoundInterval rightValue = evaluate(pAdd.getSummand2());
    CompoundInterval pushLeftValue = parameter.add(rightValue.negate());
    CompoundInterval pushRightValue = parameter.add(leftValue.negate());
    if (!pAdd.getSummand1().accept(this, pushLeftValue)
        || !pAdd.getSummand2().accept(this, pushRightValue)) {
      return false;
    }
    return true;
  }

  @Override
  public Boolean visit(BinaryAnd<CompoundInterval> pAnd, CompoundInterval pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    return evaluate(pAnd).intersectsWith(pParameter);
  }

  @Override
  public Boolean visit(BinaryNot<CompoundInterval> pNot, CompoundInterval pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    CompoundInterval parameter = evaluate(pNot).intersectWith(pParameter);
    if (parameter.isBottom()) {
      return false;
    }
    if (!pNot.getFlipped().accept(this, parameter.invert())) {
      return false;
    }
    return true;
  }

  @Override
  public Boolean visit(BinaryOr<CompoundInterval> pOr, CompoundInterval pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    return evaluate(pOr).intersectsWith(pParameter);
  }

  @Override
  public Boolean visit(BinaryXor<CompoundInterval> pXor, CompoundInterval pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    return evaluate(pXor).intersectsWith(pParameter);
  }

  @Override
  public Boolean visit(Constant<CompoundInterval> pConstant, CompoundInterval pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    return pConstant.getValue().intersectsWith(pParameter);
  }

  @Override
  public Boolean visit(Divide<CompoundInterval> pDivide, CompoundInterval pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    CompoundInterval parameter = evaluate(pDivide).intersectWith(pParameter);
    if (parameter.isBottom()) {
      return false;
    }
    CompoundInterval leftValue = evaluate(pDivide.getNumerator());
    CompoundInterval rightValue = evaluate(pDivide.getDenominator());

    // Determine the numerator but consider integer division
    CompoundInterval computedLeftValue = parameter.multiply(rightValue);
    for (SimpleInterval interval : computedLeftValue.getIntervals()) {
      CompoundInterval borderA = CompoundInterval.of(interval);
      CompoundInterval borderB = borderA.add(rightValue.add(rightValue.signum().negate()));
      computedLeftValue = computedLeftValue.unionWith(CompoundInterval.span(borderA, borderB));
    }

    CompoundInterval pushLeftValue = leftValue.intersectWith(computedLeftValue);
    CompoundInterval pushRightValue = leftValue.divide(parameter);
    if (!pDivide.getNumerator().accept(this, pushLeftValue)
        || !pDivide.getDenominator().accept(this, pushRightValue)) {
      return false;
    }
    return true;
  }

  @Override
  public Boolean visit(Equal<CompoundInterval> pEqual, CompoundInterval pParameter) {
    CompoundInterval parameter = evaluate(pEqual).intersectWith(pParameter);
    // If the truth of the equation is undecided, anything is possible and
    // no information can be gained
    if (!parameter.isDefinitelyTrue() && !parameter.isDefinitelyFalse()) {
      return !parameter.isBottom();
    }
    CompoundInterval leftValue = evaluate(pEqual.getOperand1());
    CompoundInterval rightValue = evaluate(pEqual.getOperand2());
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
  public Boolean visit(LessThan<CompoundInterval> pLessThan, CompoundInterval pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    CompoundInterval parameter = evaluate(pLessThan).intersectWith(pParameter);
    if (parameter.isBottom()) {
      return false;
    }
    // If the truth of the equation is undecided, anything is possible and
    // no information can be gained
    if (!parameter.isDefinitelyTrue() && !parameter.isDefinitelyFalse()) {
      return !parameter.isBottom();
    }
    CompoundInterval leftValue = evaluate(pLessThan.getOperand1());
    CompoundInterval rightValue = evaluate(pLessThan.getOperand2());

    final CompoundInterval leftPushValue;
    final CompoundInterval rightPushValue;
    // If the equation is definitely true, push
    // (negative infinity to ((right upper bound) - 1)) to the left and
    // (((left lower bound) + 1) to infinity) to the right,
    // if the equation is definitely false, push
    // ((right lower bound) to infinity) to the left and
    // (negative infinity to (left upper bound)) to the right.
    if (parameter.isDefinitelyTrue()) {
      leftPushValue = rightValue.extendToNegativeInfinity().span().add(-1);
      rightPushValue = leftValue.extendToPositiveInfinity().span().add(1);
    } else {
      leftPushValue = rightValue.extendToPositiveInfinity().span();
      rightPushValue = leftValue.extendToNegativeInfinity().span();
    }
    return pLessThan.getOperand1().accept(this, leftPushValue)
        && pLessThan.getOperand2().accept(this, rightPushValue);
  }

  @Override
  public Boolean visit(LogicalAnd<CompoundInterval> pAnd, CompoundInterval pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    CompoundInterval parameter = evaluate(pAnd).intersectWith(pParameter);
    if (parameter.isBottom()) {
      return false;
    }
    // If the parameter is definitely true, both operands must be true
    // If the parameter is definitely false, at least one of the operands must be false
    if (parameter.isDefinitelyTrue()) {
      return pAnd.getOperand1().accept(this, parameter)
          && pAnd.getOperand2().accept(this, parameter);
    } else if (parameter.isDefinitelyFalse()) {
      Map<String, InvariantsFormula<CompoundInterval>> env1 = new HashMap<>(this.environment);
      boolean push1 = pAnd.getOperand1().accept(new PushAssumptionToEnvironmentVisitor(evaluationVisitor, env1), pParameter);

      // If operand1 cannot be false, operand2 must be false
      if (!push1) {
        return pAnd.getOperand2().accept(this, CompoundInterval.logicalFalse());
      }

      Map<String, InvariantsFormula<CompoundInterval>> env2 = new HashMap<>(this.environment);
      boolean push2 = pAnd.getOperand2().accept(new PushAssumptionToEnvironmentVisitor(evaluationVisitor, env2), pParameter);
      // If operand2 cannot be false, operand1 must be false
      if (!push2) {
        return pAnd.getOperand1().accept(this, CompoundInterval.logicalFalse());
      }

      // If both may be false, the effects on the environment are united
      for (Map.Entry<String, InvariantsFormula<CompoundInterval>> entry : env2.entrySet()) {
        String varName = entry.getKey();
        InvariantsFormula<CompoundInterval> value1 = env1.get(varName);
        // Only if BOTH parts produced an environment value, they can be united to a non-top value
        if (value1 != null) {
          InvariantsFormula<CompoundInterval> value2 = entry.getValue();
          final InvariantsFormula<CompoundInterval> newValueFormula = CompoundStateFormulaManager.INSTANCE.union(value1, value2).accept(new PartialEvaluator(this.environment), evaluationVisitor);
          if (newValueFormula.accept(this.evaluationVisitor, this.environment).isBottom()) {
            this.cachingEvaluationVisitor.clearCache();
            return false;
          }
          if (newValueFormula instanceof Constant<?> && ((Constant<CompoundInterval>) newValueFormula).getValue().isTop()) {
            continue;
          }
          this.environment.put(varName, newValueFormula);
          this.cachingEvaluationVisitor.clearCache();
        }
      }
    }
    return true;
  }

  @Override
  public Boolean visit(LogicalNot<CompoundInterval> pNot, CompoundInterval pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    CompoundInterval parameter = evaluate(pNot).intersectWith(pParameter);
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
  public Boolean visit(Modulo<CompoundInterval> pModulo, CompoundInterval pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    return evaluate(pModulo).intersectsWith(pParameter);
  }

  @Override
  public Boolean visit(Multiply<CompoundInterval> pMultiply, CompoundInterval pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    CompoundInterval parameter = evaluate(pMultiply).intersectWith(pParameter);
    if (parameter.isBottom()) {
      return false;
    }
    CompoundInterval leftValue = evaluate(pMultiply.getFactor1());
    CompoundInterval rightValue = evaluate(pMultiply.getFactor2());
    CompoundInterval pushLeftValue = parameter.divide(rightValue);
    CompoundInterval pushRightValue = parameter.divide(leftValue);
    if (!pMultiply.getFactor1().accept(this, pushLeftValue)
        || !pMultiply.getFactor2().accept(this, pushRightValue)) {
      return false;
    }
    return true;
  }

  @Override
  public Boolean visit(ShiftLeft<CompoundInterval> pShiftLeft, CompoundInterval pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    return evaluate(pShiftLeft).intersectsWith(pParameter);
  }

  @Override
  public Boolean visit(ShiftRight<CompoundInterval> pShiftRight, CompoundInterval pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    return evaluate(pShiftRight).intersectsWith(pParameter);
  }

  @Override
  public Boolean visit(Union<CompoundInterval> pUnion, CompoundInterval pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    InvariantsFormula<CompoundInterval> operand1 = pUnion.getOperand1();
    InvariantsFormula<CompoundInterval> operand2 = pUnion.getOperand2();
    if (!evaluate(operand1).intersectsWith(pParameter)) {
      if (!operand2.accept(this, pParameter)) {
        return false;
      }
    }
    if (!evaluate(operand2).intersectsWith(pParameter)) {
      if (!operand1.accept(this, pParameter)) {
        return false;
      }
    }

    CompoundStateFormulaManager ifm = CompoundStateFormulaManager.INSTANCE;
    InvariantsFormula<CompoundInterval> parameter = ifm.asConstant(pParameter);
    return ifm.logicalOr(ifm.equal(pUnion.getOperand1(), parameter), ifm.equal(pUnion.getOperand2(), parameter)).accept(this, CompoundInterval.logicalTrue());
  }

  @Override
  public Boolean visit(Variable<CompoundInterval> pVariable, CompoundInterval pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    CompoundInterval parameter = evaluate(pVariable).intersectWith(pParameter);
    if (parameter.isBottom()) {
      return false;
    }
    if (parameter.isTop()) {
      return true;
    }
    String varName = pVariable.getName();
    InvariantsFormula<CompoundInterval> resolved = getFromEnvironment(varName);
    if (!resolved.accept(this, parameter)) {
      return false;
    }
    final CompoundInterval newValue;
    if (resolved instanceof Constant<?>) {
      CompoundInterval resolvedValue = ((Constant<CompoundInterval>) resolved).getValue();
      newValue = resolvedValue.intersectWith(parameter);
    } else {
      newValue = parameter;
    }
    if (newValue.isBottom()) {
      return false;
    }
    if (newValue.isTop()) {
      environment.remove(varName);
    } else {
      CompoundStateFormulaManager ifm = CompoundStateFormulaManager.INSTANCE;
      environment.put(varName, ifm.asConstant(newValue));
    }
    this.cachingEvaluationVisitor.clearCache();
    return true;
  }

  /**
   * Resolves the variable with the given name.
   *
   * @param pVarName the name of the variable.
   *
   * @return the expression formula assigned to the variable.
   */
  private InvariantsFormula<CompoundInterval> getFromEnvironment(String pVarName) {
    InvariantsFormula<CompoundInterval> result = environment.get(pVarName);
    if (result == null) {
      return CompoundStateFormulaManager.INSTANCE.asConstant(CompoundInterval.top());
    }
    return result;
  }

}
