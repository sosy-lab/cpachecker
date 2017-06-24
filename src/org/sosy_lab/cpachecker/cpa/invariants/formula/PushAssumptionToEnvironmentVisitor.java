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

import java.math.BigInteger;
import java.util.Map;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundBitVectorIntervalManagerFactory;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundIntegralInterval;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundIntervalManager;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundIntervalManagerFactory;
import org.sosy_lab.cpachecker.cpa.invariants.NonRecursiveEnvironment;
import org.sosy_lab.cpachecker.cpa.invariants.TypeInfo;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Instances of this class are parameterized compound state invariants formula
 * visitors used to push information from an assumption into the environment.
 * The visited formulae are the expressions for which the states provided as
 * the additional parameters are assumed.
 */
public class PushAssumptionToEnvironmentVisitor implements ParameterizedBooleanFormulaVisitor<CompoundInterval, BooleanConstant<CompoundInterval>, Boolean> {

  private final PushValueToEnvironmentVisitor pushValueToEnvironmentVisitor;

  /**
   * The environment to push the gained information into.
   */
  private final NonRecursiveEnvironment.Builder environment;

  /**
   * The evaluation visitor used to evaluate compound state invariants formulae
   * to compound states.
   */
  private final FormulaEvaluationVisitor<CompoundInterval> evaluationVisitor;

  private final CompoundIntervalManagerFactory compoundIntervalManagerFactory;

  private final CompoundIntervalFormulaManager compoundIntervalFormulaManager;

  /**
   * Creates a new visitor for pushing information obtained from assuming given
   * states for the visited formulae into the given environment.
   *
   * @param pCompoundIntervalManagerFactory a factory for compound interval
   * managers.
   * @param pEvaluationVisitor the evaluation visitor used to evaluate compound
   * state invariants formulae to compound states.
   * @param pEnvironment the environment to push the gained information into.
   * Obviously, this environment must be mutable.
   */
  public PushAssumptionToEnvironmentVisitor(
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor,
      NonRecursiveEnvironment.Builder pEnvironment) {
    this.pushValueToEnvironmentVisitor = new PushValueToEnvironmentVisitor(this, pCompoundIntervalManagerFactory ,pEvaluationVisitor, pEnvironment);
    this.evaluationVisitor = pEvaluationVisitor;
    this.environment = pEnvironment;
    this.compoundIntervalManagerFactory = pCompoundIntervalManagerFactory;
    this.compoundIntervalFormulaManager = new CompoundIntervalFormulaManager(compoundIntervalManagerFactory);
  }

  private CompoundIntervalManager createCompoundIntervalManager(TypeInfo pTypeInfo) {
    if (compoundIntervalManagerFactory instanceof CompoundBitVectorIntervalManagerFactory) {
      CompoundBitVectorIntervalManagerFactory compoundBitVectorIntervalManagerFactory = (CompoundBitVectorIntervalManagerFactory) compoundIntervalManagerFactory;
      return compoundBitVectorIntervalManagerFactory.createCompoundIntervalManager(pTypeInfo, false);
    }
    return compoundIntervalManagerFactory.createCompoundIntervalManager(pTypeInfo);
  }

  /**
   * Creates a new visitor for pushing information obtained from assuming given
   * states for the visited formulae into the given environment.
   *
   * @param pPushValueToEnvironmentVisitor the visitor used to push numeral
   * values into the environment.
   * @param pCompoundIntervalManagerFactory a factory for compound interval
   * managers.
   * @param pEvaluationVisitor the evaluation visitor used to evaluate compound
   * state invariants formulae to compound states.
   * @param pEnvironment the environment to push the gained information into.
   * Obviously, this environment must be mutable.
   */
  public PushAssumptionToEnvironmentVisitor(
      PushValueToEnvironmentVisitor pPushValueToEnvironmentVisitor,
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor,
      NonRecursiveEnvironment.Builder pEnvironment) {
    this.pushValueToEnvironmentVisitor = pPushValueToEnvironmentVisitor;
    this.evaluationVisitor = pEvaluationVisitor;
    this.environment = pEnvironment;
    this.compoundIntervalManagerFactory = pCompoundIntervalManagerFactory;
    this.compoundIntervalFormulaManager = new CompoundIntervalFormulaManager(compoundIntervalManagerFactory);
  }

  private CompoundInterval evaluate(NumeralFormula<CompoundInterval> pFormula) {
    return pFormula.accept(evaluationVisitor, environment);
  }

  private BooleanConstant<CompoundInterval> evaluate(BooleanFormula<CompoundInterval> pFormula) {
    return pFormula.accept(evaluationVisitor, environment);
  }

  private boolean areContradictory(BooleanFormula<CompoundInterval> pFormula, BooleanConstant<CompoundInterval> pConstant) {
    if (pFormula == null || pConstant == null) {
      return false;
    }
    BooleanConstant<CompoundInterval> formulaValue = evaluate(pFormula);
    if (formulaValue == null) {
      return false;
    }
    return !pConstant.equals(formulaValue);
  }

  @Override
  public Boolean visit(Equal<CompoundInterval> pEqual, BooleanConstant<CompoundInterval> pParameter) {
    // If the truth of the equation is undecided, anything is possible and
    // no information can be gained
    if (pParameter == null) {
      return true;
    }
    // If there is a contradiction, report it back
    if (areContradictory(pEqual, pParameter)) {
      return false;
    }
    CompoundInterval leftValue = evaluate(pEqual.getOperand1());
    CompoundInterval rightValue = evaluate(pEqual.getOperand2());
    // If the equation is definitely true, push right to left and vice versa
    if (pParameter.getValue()) {
      NumeralFormula<CompoundInterval> op1 = pEqual.getOperand1();
      NumeralFormula<CompoundInterval> op2 = pEqual.getOperand2();
      if (op1 instanceof Variable) {
        MemoryLocation memoryLocation = ((Variable<?>) op1).getMemoryLocation();
        NumeralFormula<CompoundInterval> previous = environment.get(memoryLocation);
        environment.put(memoryLocation, op2);
        if (previous != null
            && !compoundIntervalFormulaManager.definitelyImplies(
                environment,
                compoundIntervalFormulaManager.equal(op1, previous))) {
          environment.put(memoryLocation, previous);
        }
      }
      if (op2 instanceof Variable) {
        MemoryLocation memoryLocation = ((Variable<?>) op2).getMemoryLocation();
        NumeralFormula<CompoundInterval> previous = environment.get(memoryLocation);
        environment.put(memoryLocation, op1);
        if (previous != null
            && !compoundIntervalFormulaManager.definitelyImplies(
                environment,
                compoundIntervalFormulaManager.equal(op2, previous))) {
          environment.put(memoryLocation, previous);
        }
      }
      return pEqual.getOperand1().accept(this.pushValueToEnvironmentVisitor, rightValue)
          && pEqual.getOperand2().accept(this.pushValueToEnvironmentVisitor, leftValue);
    }
    // The equation is definitely false

    // Try to push an exclusion
    NumeralFormula<CompoundInterval> op1 = pEqual.getOperand1();
    NumeralFormula<CompoundInterval> op2 = pEqual.getOperand2();
    if (op1 instanceof Variable) {
      MemoryLocation memoryLocation = ((Variable<?>) op1).getMemoryLocation();
      environment.putIfAbsent(memoryLocation, compoundIntervalFormulaManager.exclude(op2));
    }
    if (op2 instanceof Variable) {
      MemoryLocation memoryLocation = ((Variable<?>) op2).getMemoryLocation();
      environment.putIfAbsent(memoryLocation, compoundIntervalFormulaManager.exclude(op1));
    }

    // Push inverted singletons, if any
    if (rightValue.isSingleton() && !pEqual.getOperand1().accept(this.pushValueToEnvironmentVisitor, rightValue.invert())) {
      return false;
    }
    if (leftValue.isSingleton() && !pEqual.getOperand2().accept(this.pushValueToEnvironmentVisitor, leftValue.invert())) {
      return false;
    }

    return true;
  }

  @Override
  public Boolean visit(LessThan<CompoundInterval> pLessThan, BooleanConstant<CompoundInterval> pParameter) {
    // If the truth of the equation is undecided, anything is possible and
    // no information can be gained
    if (pParameter == null) {
      return true;
    }
    // If there is a contradiction, report it back
    if (areContradictory(pLessThan, pParameter)) {
      return false;
    }
    CompoundInterval leftValue = evaluate(pLessThan.getOperand1());
    CompoundInterval rightValue = evaluate(pLessThan.getOperand2());

    if (leftValue instanceof CompoundIntegralInterval
        && rightValue instanceof CompoundIntegralInterval) {

      final CompoundInterval leftPushValue;
      final CompoundInterval rightPushValue;
      // If the equation is definitely true, push
      // (negative infinity to ((right upper bound) - 1)) to the left and
      // (((left lower bound) + 1) to infinity) to the right,
      // if the equation is definitely false, push
      // ((right lower bound) to infinity) to the left and
      // (negative infinity to (left upper bound)) to the right.
      if (pParameter.getValue()) {
        TypeInfo typeInfo = pLessThan.getOperand1().getTypeInfo();
        CompoundIntervalManager cim =
            createCompoundIntervalManager(typeInfo);
        leftPushValue =
            rightValue.isSingleton()
                ? cim.intersect(rightValue.invert(), rightValue.extendToMinValue())
                : (rightValue.hasUpperBound()
                    ? cim.singleton(
                            ((BigInteger) rightValue.getUpperBound()).subtract(BigInteger.ONE))
                        .extendToMinValue()
                    : rightValue.span().extendToMinValue());
        rightPushValue =
            leftValue.isSingleton()
                ? cim.intersect(leftValue.invert(), leftValue.extendToMaxValue())
                : (leftValue.hasLowerBound()
                    ? cim.singleton(((BigInteger) leftValue.getLowerBound()).add(BigInteger.ONE))
                        .extendToMaxValue()
                    : leftValue.span().extendToMaxValue());
      } else {
        leftPushValue = rightValue.span().extendToMaxValue();
        rightPushValue = leftValue.span().extendToMinValue();
      }
      return pLessThan.getOperand1().accept(this.pushValueToEnvironmentVisitor, leftPushValue)
          && pLessThan.getOperand2().accept(this.pushValueToEnvironmentVisitor, rightPushValue);
    }
    return true;
  }

  @Override
  public Boolean visit(LogicalAnd<CompoundInterval> pAnd, BooleanConstant<CompoundInterval> pParameter) {
    // If the truth of the equation is undecided, anything is possible and
    // no information can be gained
    if (pParameter == null) {
      return true;
    }
    // If there is a contradiction, report it back
    if (areContradictory(pAnd, pParameter)) {
      return false;
    }
    // If the parameter is true, both operands must be true
    // If the parameter is false, at least one of the operands must be false
    if (pParameter.getValue()) {
      return pAnd.getOperand1().accept(this, pParameter)
          && pAnd.getOperand2().accept(this, pParameter);
    } else {
      NonRecursiveEnvironment.Builder env1 = new NonRecursiveEnvironment.Builder(compoundIntervalManagerFactory, this.environment);
      boolean push1 = pAnd.getOperand1().accept(
          new PushAssumptionToEnvironmentVisitor(compoundIntervalManagerFactory, evaluationVisitor, env1), pParameter);

      // If operand1 cannot be false, operand2 must be false
      if (!push1) {
        return pAnd.getOperand2().accept(this, pParameter);
      }

      NonRecursiveEnvironment.Builder env2 = new NonRecursiveEnvironment.Builder(compoundIntervalManagerFactory, this.environment);
      boolean push2 = pAnd.getOperand2().accept(
          new PushAssumptionToEnvironmentVisitor(compoundIntervalManagerFactory, evaluationVisitor, env2), pParameter);
      // If operand2 cannot be false, operand1 must be false
      if (!push2) {
        return pAnd.getOperand1().accept(this, pParameter);
      }

      // If both may be false, the effects on the environment are united
      for (Map.Entry<MemoryLocation, NumeralFormula<CompoundInterval>> entry : env2.entrySet()) {
        MemoryLocation memoryLocation = entry.getKey();
        NumeralFormula<CompoundInterval> value1 = env1.get(memoryLocation);
        // Only if BOTH parts produced an environment value, they can be united to a non-top value
        if (value1 != null) {
          NumeralFormula<CompoundInterval> value2 = entry.getValue();
          final NumeralFormula<CompoundInterval> newValueFormula = compoundIntervalFormulaManager
              .union(value1, value2)
              .accept(new PartialEvaluator(compoundIntervalManagerFactory, this.environment), evaluationVisitor);
          if (newValueFormula.accept(this.evaluationVisitor, this.environment).isBottom()) {
            return false;
          }
          if (newValueFormula instanceof Constant<?> && ((Constant<CompoundInterval>) newValueFormula).getValue().containsAllPossibleValues()) {
            continue;
          }
          this.environment.put(memoryLocation, newValueFormula);
        }
      }
    }
    return true;
  }

  @Override
  public Boolean visit(LogicalNot<CompoundInterval> pNot, BooleanConstant<CompoundInterval> pParameter) {
    // If the truth of the equation is undecided, anything is possible and
    // no information can be gained
    if (pParameter == null) {
      return true;
    }
    // If there is a contradiction, report it back
    if (areContradictory(pNot, pParameter)) {
      return false;
    }
    return pNot.getNegated().accept(this, pParameter.negate());
  }

  @Override
  public Boolean visitFalse(BooleanConstant<CompoundInterval> pParameter) {
    return pParameter == null || !pParameter.getValue();
  }

  @Override
  public Boolean visitTrue(BooleanConstant<CompoundInterval> pParameter) {
    return pParameter == null || pParameter.getValue();
  }

}
