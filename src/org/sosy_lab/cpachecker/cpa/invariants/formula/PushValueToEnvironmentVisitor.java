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
import org.sosy_lab.cpachecker.cpa.invariants.BitVectorInfo;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundBitVectorInterval;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundBitVectorIntervalManagerFactory;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundIntervalManager;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundIntervalManagerFactory;
import org.sosy_lab.cpachecker.cpa.invariants.NonRecursiveEnvironment;
import org.sosy_lab.cpachecker.cpa.invariants.TypeInfo;
import org.sosy_lab.cpachecker.cpa.invariants.Typed;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Instances of this class are parameterized compound state invariants formula
 * visitors used to push information from an assumption into the environment.
 * The visited formulae are the expressions for which the states provided as
 * the additional parameters are assumed.
 */
public class PushValueToEnvironmentVisitor implements ParameterizedNumeralFormulaVisitor<CompoundInterval, CompoundInterval, Boolean> {

  private final PushAssumptionToEnvironmentVisitor pushAssumptionToEnvironmentVisitor;

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
  public PushValueToEnvironmentVisitor(
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor,
      NonRecursiveEnvironment.Builder pEnvironment) {
    this.pushAssumptionToEnvironmentVisitor = new PushAssumptionToEnvironmentVisitor(this, pCompoundIntervalManagerFactory, pEvaluationVisitor, pEnvironment);
    this.evaluationVisitor = pEvaluationVisitor;
    this.environment = pEnvironment;
    this.compoundIntervalManagerFactory = pCompoundIntervalManagerFactory;
  }

  /**
   * Creates a new visitor for pushing information obtained from assuming given
   * states for the visited formulae into the given environment.
   *
   * @param pPushAssumptionToEnvironmentVisitor the visitor for pushing
   * assumptions into boolean formulae.
   * @param pCompoundIntervalManagerFactory a factory for compound interval
   * managers.
   * @param pEvaluationVisitor the evaluation visitor used to evaluate compound
   * state invariants formulae to compound states.
   * @param pEnvironment the environment to push the gained information into.
   * Obviously, this environment must be mutable.
   */
  public PushValueToEnvironmentVisitor(
      PushAssumptionToEnvironmentVisitor pPushAssumptionToEnvironmentVisitor,
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor,
      NonRecursiveEnvironment.Builder pEnvironment) {
    this.pushAssumptionToEnvironmentVisitor = pPushAssumptionToEnvironmentVisitor;
    this.evaluationVisitor = pEvaluationVisitor;
    this.environment = pEnvironment;
    this.compoundIntervalManagerFactory = pCompoundIntervalManagerFactory;
  }

  private CompoundInterval evaluate(NumeralFormula<CompoundInterval> pFormula) {
    return pFormula.accept(evaluationVisitor, environment);
  }

  private CompoundIntervalManager getCompoundIntervalManager(Typed pBitVectorType) {
    TypeInfo typeInfo = pBitVectorType.getTypeInfo();
    if (compoundIntervalManagerFactory instanceof CompoundBitVectorIntervalManagerFactory) {
      CompoundBitVectorIntervalManagerFactory compoundBitVectorIntervalManagerFactory = (CompoundBitVectorIntervalManagerFactory) compoundIntervalManagerFactory;
      return compoundBitVectorIntervalManagerFactory.createCompoundIntervalManager(typeInfo, false);
    }
    return compoundIntervalManagerFactory.createCompoundIntervalManager(typeInfo);
  }

  @Override
  public Boolean visit(Add<CompoundInterval> pAdd, CompoundInterval pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    CompoundIntervalManager cim = getCompoundIntervalManager(pAdd);
    CompoundInterval parameter = cim.intersect(evaluate(pAdd), pParameter);
    if (parameter.isBottom()) {
      return false;
    }
    CompoundInterval leftValue = evaluate(pAdd.getSummand1());
    CompoundInterval rightValue = evaluate(pAdd.getSummand2());
    CompoundInterval pushLeftValue = cim.add(parameter, cim.negate(rightValue));
    CompoundInterval pushRightValue = cim.add(parameter, cim.negate(leftValue));
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
    return getCompoundIntervalManager(pAnd).doIntersect(evaluate(pAnd), pParameter);
  }

  @Override
  public Boolean visit(BinaryNot<CompoundInterval> pNot, CompoundInterval pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    CompoundIntervalManager compoundIntervalManager = getCompoundIntervalManager(pNot);
    CompoundInterval parameter = compoundIntervalManager.intersect(evaluate(pNot), pParameter);
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
    return getCompoundIntervalManager(pOr).doIntersect(evaluate(pOr), pParameter);
  }

  @Override
  public Boolean visit(BinaryXor<CompoundInterval> pXor, CompoundInterval pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    return getCompoundIntervalManager(pXor).doIntersect(evaluate(pXor), pParameter);
  }

  @Override
  public Boolean visit(Constant<CompoundInterval> pConstant, CompoundInterval pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    return getCompoundIntervalManager(pConstant).doIntersect(pConstant.getValue(), pParameter);
  }

  @Override
  public Boolean visit(Divide<CompoundInterval> pDivide, CompoundInterval pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    CompoundIntervalManager cim = getCompoundIntervalManager(pDivide);
    CompoundInterval parameter = cim.intersect(evaluate(pDivide), pParameter);
    if (parameter.isBottom()) {
      return false;
    }
    CompoundInterval leftValue = evaluate(pDivide.getNumerator());
    CompoundInterval rightValue = evaluate(pDivide.getDenominator());

    // Determine the numerator but consider integer division
    CompoundInterval computedLeftValue = cim.multiply(parameter, rightValue);
    for (CompoundInterval interval : computedLeftValue.splitIntoIntervals()) {
      CompoundInterval borderA = interval;
      CompoundInterval borderB = cim.add(borderA, cim.add(rightValue, cim.negate(rightValue.signum())));
      computedLeftValue = cim.union(computedLeftValue, cim.span(borderA, borderB));
    }

    CompoundInterval pushLeftValue = cim.intersect(leftValue, computedLeftValue);
    CompoundInterval pushRightValue = parameter.isSingleton() && parameter.contains(BigInteger.ZERO)
        ? cim.allPossibleValues()
        : cim.divide(leftValue, parameter);
    if (!pDivide.getNumerator().accept(this, pushLeftValue)
        || !pDivide.getDenominator().accept(this, pushRightValue)) {
      return false;
    }
    return true;
  }

  @Override
  public Boolean visit(Exclusion<CompoundInterval> pExclusion, CompoundInterval pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    CompoundIntervalManager compoundIntervalManager = getCompoundIntervalManager(pExclusion);
    return compoundIntervalManager.doIntersect(evaluate(pExclusion), pParameter);
  }

  @Override
  public Boolean visit(Modulo<CompoundInterval> pModulo, CompoundInterval pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    CompoundIntervalManager compoundIntervalManager = getCompoundIntervalManager(pModulo);
    return compoundIntervalManager.doIntersect(evaluate(pModulo), pParameter);
  }

  @Override
  public Boolean visit(Multiply<CompoundInterval> pMultiply, CompoundInterval pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    CompoundIntervalManager compoundIntervalManager = getCompoundIntervalManager(pMultiply);
    CompoundInterval parameter = compoundIntervalManager.intersect(evaluate(pMultiply), pParameter);
    if (parameter.isBottom()) {
      return false;
    }
    CompoundInterval leftValue = evaluate(pMultiply.getFactor1());
    CompoundInterval rightValue = evaluate(pMultiply.getFactor2());
    CompoundInterval pushLeftValue = compoundIntervalManager.divide(parameter, rightValue);
    CompoundInterval pushRightValue = compoundIntervalManager.divide(parameter, leftValue);
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
    CompoundIntervalManager compoundIntervalManager = getCompoundIntervalManager(pShiftLeft);
    return compoundIntervalManager.doIntersect(evaluate(pShiftLeft), pParameter);
  }

  @Override
  public Boolean visit(ShiftRight<CompoundInterval> pShiftRight, CompoundInterval pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    CompoundIntervalManager compoundIntervalManager = getCompoundIntervalManager(pShiftRight);
    return compoundIntervalManager.doIntersect(evaluate(pShiftRight), pParameter);
  }

  @Override
  public Boolean visit(Union<CompoundInterval> pUnion, CompoundInterval pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    NumeralFormula<CompoundInterval> operand1 = pUnion.getOperand1();
    NumeralFormula<CompoundInterval> operand2 = pUnion.getOperand2();
    CompoundIntervalManager compoundIntervalManager = getCompoundIntervalManager(pUnion);
    if (!compoundIntervalManager.doIntersect(evaluate(operand1), pParameter)) {
      if (!operand2.accept(this, pParameter)) {
        return false;
      }
    }
    if (!compoundIntervalManager.doIntersect(evaluate(operand2), pParameter)) {
      if (!operand1.accept(this, pParameter)) {
        return false;
      }
    }

    NumeralFormula<CompoundInterval> parameter =
        InvariantsFormulaManager.INSTANCE.asConstant(pUnion.getTypeInfo(), pParameter);
    BooleanFormula<CompoundInterval> disjunctiveForm = LogicalNot.of(LogicalAnd.of(
        LogicalNot.of(Equal.of(pUnion.getOperand1(), parameter)),
        LogicalNot.of(Equal.of(pUnion.getOperand2(), parameter))));
    return disjunctiveForm.accept(this.pushAssumptionToEnvironmentVisitor, BooleanConstant.<CompoundInterval>getTrue());
  }

  @Override
  public Boolean visit(Variable<CompoundInterval> pVariable, CompoundInterval pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    CompoundIntervalManager compoundIntervalManager = getCompoundIntervalManager(pVariable);
    CompoundInterval parameter = compoundIntervalManager.intersect(evaluate(pVariable), pParameter);
    if (parameter.isBottom()) {
      return false;
    }
    if (parameter.containsAllPossibleValues()) {
      return true;
    }
    MemoryLocation memoryLocation = pVariable.getMemoryLocation();
    NumeralFormula<CompoundInterval> resolved = getFromEnvironment(pVariable);
    if (!resolved.accept(this, parameter)) {
      return false;
    }
    final CompoundInterval newValue;
    if (resolved instanceof Constant<?>) {
      CompoundInterval resolvedValue = ((Constant<CompoundInterval>) resolved).getValue();
      newValue = compoundIntervalManager.intersect(resolvedValue, parameter);
    } else if (!parameter.equals(pParameter)) {
      newValue = parameter;
    } else {
      return true;
    }
    if (newValue.isBottom()) {
      return false;
    }
    if (newValue.containsAllPossibleValues()) {
      environment.remove(memoryLocation);
    } else {
      environment.put(
          memoryLocation,
          InvariantsFormulaManager.INSTANCE.asConstant(pVariable.getTypeInfo(), newValue));
    }
    return true;
  }

  @Override
  public Boolean visit(IfThenElse<CompoundInterval> pIfThenElse, CompoundInterval pParameter) {
    BooleanFormula<CompoundInterval> conditionFormula = pIfThenElse.getCondition();
    NumeralFormula<CompoundInterval> positiveCaseFormula = pIfThenElse.getPositiveCase();
    NumeralFormula<CompoundInterval> negativeCaseFormula = pIfThenElse.getNegativeCase();
    CompoundInterval positiveCaseValue = evaluate(positiveCaseFormula);
    CompoundInterval negativeCaseValue = evaluate(negativeCaseFormula);
    CompoundIntervalManager cim = getCompoundIntervalManager(pIfThenElse);
    CompoundInterval positiveCaseIntersection = cim.intersect(pParameter, positiveCaseValue);
    CompoundInterval negativeCaseIntersection = cim.intersect(pParameter, negativeCaseValue);
    if (positiveCaseIntersection.isBottom() && negativeCaseIntersection.isBottom()) {
      return false;
    }
    if (positiveCaseIntersection.isBottom()) {
       if (!conditionFormula.accept(pushAssumptionToEnvironmentVisitor, BooleanConstant.<CompoundInterval>getFalse())) {
         return false;
       }
    }
    if (negativeCaseIntersection.isBottom()) {
      if (!conditionFormula.accept(pushAssumptionToEnvironmentVisitor, BooleanConstant.<CompoundInterval>getTrue())) {
        return false;
      }
    }
    boolean positiveCaseConsistent = positiveCaseFormula.accept(this, positiveCaseIntersection);
    if (!positiveCaseConsistent && !positiveCaseIntersection.isBottom()) {
      return false;
    }
    boolean negativeCaseConsistent = negativeCaseFormula.accept(this, negativeCaseIntersection);
    if (!negativeCaseConsistent && !negativeCaseIntersection.isBottom()) {
      return false;
    }
    return true;
  }

  @Override
  public Boolean visit(Cast<CompoundInterval> pCast, CompoundInterval pParameter) {
    if (pParameter == null || pParameter.isBottom()) {
      return false;
    }
    CompoundIntervalManager targetManager = getCompoundIntervalManager(pCast);
    TypeInfo targetInfo = pCast.getTypeInfo();
    TypeInfo sourceInfo = pCast.getCasted().getTypeInfo();
    if (targetInfo instanceof BitVectorInfo && sourceInfo instanceof BitVectorInfo) {
      BitVectorInfo targetBVInfo = (BitVectorInfo) targetInfo;
      BitVectorInfo sourceBVInfo = (BitVectorInfo) sourceInfo;
      if (targetBVInfo.getRange().contains(sourceBVInfo.getRange())) {
        if (!pCast.getCasted().accept(this, targetManager.cast(sourceInfo, pParameter))) {
          return false;
        }
      } else if (!targetInfo.isSigned()) {
        BigInteger numberOfPotentialOrigins =
            sourceBVInfo.getRange().size().divide(targetBVInfo.getRange().size());
        CompoundIntervalManager sourceManager = getCompoundIntervalManager(pCast.getCasted());
        CompoundInterval originFactors =
            sourceManager.span(
                sourceManager.singleton(BigInteger.ZERO),
                sourceManager.singleton(numberOfPotentialOrigins.subtract(BigInteger.ONE)));
        if (sourceInfo.isSigned()) {
          originFactors =
              sourceManager.add(
                  originFactors,
                  sourceManager.singleton(
                      numberOfPotentialOrigins.divide(BigInteger.valueOf(2).negate())));
        }
        CompoundInterval potentialOrigins =
            sourceManager.add(
                sourceManager.multiply(
                    originFactors,
                    sourceManager.singleton(
                        targetBVInfo
                            .getRange()
                            .size()
                            .min(sourceBVInfo.getRange().getUpperBound()))),
                targetManager.cast(sourceInfo, pParameter));
        if (!pCast.getCasted().accept(this, potentialOrigins)) {
          return false;
        }
      }
      return targetManager.doIntersect(evaluate(pCast), pParameter);
    }
    // TODO try to gain more information from casts between other types
    return true;
  }

  /**
   * Resolves the variable with the given name.
   *
   * @param pVariable the name of the variable.
   *
   * @return the expression formula assigned to the variable.
   */
  private NumeralFormula<CompoundInterval> getFromEnvironment(Variable<CompoundInterval> pVariable) {
    NumeralFormula<CompoundInterval> result = environment.get(pVariable.getMemoryLocation());
    if (result == null) {
      return InvariantsFormulaManager.INSTANCE.asConstant(
          pVariable.getTypeInfo(), getCompoundIntervalManager(pVariable).allPossibleValues());
    }
    return result;
  }

}
