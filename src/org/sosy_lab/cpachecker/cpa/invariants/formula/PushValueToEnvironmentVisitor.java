// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
 * Instances of this class are parameterized compound state invariants formula visitors used to push
 * information from an assumption into the environment. The visited formulae are the expressions for
 * which the states provided as the additional parameters are assumed.
 */
class PushValueToEnvironmentVisitor
    implements ParameterizedNumeralFormulaVisitor<CompoundInterval, CompoundInterval, Boolean> {

  private final PushAssumptionToEnvironmentVisitor pushAssumptionToEnvironmentVisitor;

  /** The environment to push the gained information into. */
  private final NonRecursiveEnvironment.Builder environment;

  /**
   * The evaluation visitor used to evaluate compound state invariants formulae to compound states.
   */
  private final FormulaEvaluationVisitor<CompoundInterval> evaluationVisitor;

  private final CompoundIntervalManagerFactory compoundIntervalManagerFactory;

  /**
   * Creates a new visitor for pushing information obtained from assuming given states for the
   * visited formulae into the given environment.
   *
   * @param pCompoundIntervalManagerFactory a factory for compound interval managers.
   * @param pEvaluationVisitor the evaluation visitor used to evaluate compound state invariants
   *     formulae to compound states.
   * @param pEnvironment the environment to push the gained information into. Obviously, this
   *     environment must be mutable.
   */
  public PushValueToEnvironmentVisitor(
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor,
      NonRecursiveEnvironment.Builder pEnvironment) {
    pushAssumptionToEnvironmentVisitor =
        new PushAssumptionToEnvironmentVisitor(
            this, pCompoundIntervalManagerFactory, pEvaluationVisitor, pEnvironment);
    evaluationVisitor = pEvaluationVisitor;
    environment = pEnvironment;
    compoundIntervalManagerFactory = pCompoundIntervalManagerFactory;
  }

  /**
   * Creates a new visitor for pushing information obtained from assuming given states for the
   * visited formulae into the given environment.
   *
   * @param pPushAssumptionToEnvironmentVisitor the visitor for pushing assumptions into boolean
   *     formulae.
   * @param pCompoundIntervalManagerFactory a factory for compound interval managers.
   * @param pEvaluationVisitor the evaluation visitor used to evaluate compound state invariants
   *     formulae to compound states.
   * @param pEnvironment the environment to push the gained information into. Obviously, this
   *     environment must be mutable.
   */
  public PushValueToEnvironmentVisitor(
      PushAssumptionToEnvironmentVisitor pPushAssumptionToEnvironmentVisitor,
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor,
      NonRecursiveEnvironment.Builder pEnvironment) {
    pushAssumptionToEnvironmentVisitor = pPushAssumptionToEnvironmentVisitor;
    evaluationVisitor = pEvaluationVisitor;
    environment = pEnvironment;
    compoundIntervalManagerFactory = pCompoundIntervalManagerFactory;
  }

  private CompoundInterval evaluate(NumeralFormula<CompoundInterval> pFormula) {
    return pFormula.accept(evaluationVisitor, environment);
  }

  private CompoundIntervalManager getCompoundIntervalManager(Typed pBitVectorType) {
    TypeInfo typeInfo = pBitVectorType.getTypeInfo();
    if (compoundIntervalManagerFactory
        instanceof
        CompoundBitVectorIntervalManagerFactory compoundBitVectorIntervalManagerFactory) {
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

    final CompoundInterval pushLeftValue;
    final CompoundInterval pushRightValue;

    TypeInfo typeInfo = pAdd.getTypeInfo();
    if (typeInfo instanceof BitVectorInfo bitVectorInfo && pAdd.getTypeInfo().isSigned()) {
      BitVectorInfo extendedType = bitVectorInfo.extend(1);

      CompoundInterval extendedRange =
          cim.cast(extendedType, CompoundBitVectorInterval.of(bitVectorInfo.getRange()));
      CompoundInterval extendedLeftValue = cim.cast(extendedType, leftValue);
      CompoundInterval extendedRightValue = cim.cast(extendedType, rightValue);
      CompoundInterval extendedParameter = cim.cast(extendedType, parameter);

      pushLeftValue =
          cim.cast(
              bitVectorInfo,
              cim.intersect(
                  cim.add(extendedParameter, cim.negate(extendedRightValue)), extendedRange));
      pushRightValue =
          cim.cast(
              bitVectorInfo,
              cim.intersect(
                  cim.add(extendedParameter, cim.negate(extendedLeftValue)), extendedRange));
    } else {
      pushLeftValue = cim.add(parameter, cim.negate(rightValue));
      pushRightValue = cim.add(parameter, cim.negate(leftValue));
    }
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
    return getCompoundIntervalManager(pNot).doIntersect(evaluate(pNot), pParameter);
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

  /**
   * Narrows the operands of a division to the values that can produce an assumed quotient.
   *
   * <p>C integer division truncates towards zero, so a quotient {@code q} does not determine the
   * operands exactly: it only states that the numerator {@code n} and the denominator {@code d}
   * satisfy, for the remainder {@code r} that the division drops,
   *
   * <pre>n = q * d + r,  |r| &lt; |d|,  sign(r) = sign(n)</pre>
   *
   * <p>Inverting the division as if it were exact therefore yields operand ranges that are too
   * small, and narrowing to them prunes feasible branches. Both operands have to account for {@code
   * r}:
   *
   * <ul>
   *   <li>The numerator lies in {@code q * d + r}, so the product is widened by up to {@code |d| -
   *       1} — towards positive values for a positive numerator and towards negative ones for a
   *       negative numerator, since {@code r} carries the sign of {@code n}. For example {@code
   *       INT_MIN / d == -1} holds for every {@code d} in {@code [2, INT_MAX]}, although {@code -1
   *       * d} alone never reaches {@code INT_MIN}.
   *   <li>The denominator satisfies {@code |n| / (|q| + 1) < |d| <= |n| / |q|}, so every
   *       denominator down to almost half of {@code n / q} yields the same quotient, and only the
   *       upper end of that range is {@code n / q} itself. Since {@code |q| + 1 <= 2 * |q|} for
   *       {@code q != 0}, halving {@code n / q} is a sound (if slightly loose) bound for the end
   *       closest to zero. For example {@code INT_MAX / d == 1} holds for every {@code d} in {@code
   *       [1073741824, INT_MAX]}, not just for {@code d == INT_MAX}.
   * </ul>
   *
   * <p>A quotient of zero is the one case that bounds the denominator not at all: it only states
   * that the denominator is larger in magnitude than the numerator.
   */
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

    // Determine the numerator but consider integer division: widen the product by the dropped
    // remainder, whose magnitude is below that of the denominator and whose sign is the numerator's
    CompoundInterval signedDenominatorBound =
        cim.add(rightValue, cim.negate(rightValue.signum())); // sign(d) * (|d| - 1)
    CompoundInterval remainder =
        cim.intersect(
            cim.span(signedDenominatorBound, cim.negate(signedDenominatorBound)),
            cim.singleton(BigInteger.ZERO).extendToMaxValue()); // [0, |d| - 1]
    CompoundInterval computedLeftValue = cim.multiply(parameter, rightValue);
    for (CompoundInterval interval : computedLeftValue.splitIntoIntervals()) {
      CompoundInterval borderA = interval;
      CompoundInterval borderB = cim.add(borderA, cim.multiply(remainder, leftValue.signum()));
      computedLeftValue = cim.union(computedLeftValue, cim.span(borderA, borderB));
    }

    CompoundInterval pushLeftValue = cim.intersect(leftValue, computedLeftValue);

    // Determine the denominator but consider integer division: extend numerator/quotient towards
    // zero, down to half of it, since those denominators yield the same quotient
    CompoundInterval pushRightValue;
    if (parameter.contains(BigInteger.ZERO)) {
      pushRightValue = cim.allPossibleValues();
    } else {
      CompoundInterval two = cim.singleton(2);
      pushRightValue = cim.bottom();
      for (CompoundInterval quotient : parameter.splitIntoIntervals()) {
        CompoundInterval border = cim.divide(leftValue, quotient);
        pushRightValue = cim.union(pushRightValue, cim.span(border, cim.divide(border, two)));
      }
    }

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
    if (parameter.contains(BigInteger.ZERO)) {
      if (parameter.isSingleton()) {
        if (!leftValue.contains(BigInteger.ZERO)) {
          return pMultiply.getFactor2().accept(this, parameter);
        } else if (!rightValue.contains(BigInteger.ZERO)) {
          return pMultiply.getFactor1().accept(this, parameter);
        }
      }
      return true;
    }
    // Here we could potentially check more precisely whether pMultiply may intersect pParameter
    // by resolving the multiplication. But we cannot use divide() as it was used in the past
    // because for bitvectors division is not the inverse of multiplication
    // (because of truncation and overflows).
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
    BooleanFormula<CompoundInterval> disjunctiveForm =
        LogicalNot.of(
            LogicalAnd.of(
                LogicalNot.of(Equal.of(pUnion.getOperand1(), parameter)),
                LogicalNot.of(Equal.of(pUnion.getOperand2(), parameter))));
    return disjunctiveForm.accept(
        pushAssumptionToEnvironmentVisitor, BooleanConstant.<CompoundInterval>getTrue());
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
      if (!conditionFormula.accept(
          pushAssumptionToEnvironmentVisitor, BooleanConstant.<CompoundInterval>getFalse())) {
        return false;
      }
    }
    if (negativeCaseIntersection.isBottom()) {
      if (!conditionFormula.accept(
          pushAssumptionToEnvironmentVisitor, BooleanConstant.<CompoundInterval>getTrue())) {
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
    if (targetInfo instanceof BitVectorInfo targetBVInfo
        && sourceInfo instanceof BitVectorInfo sourceBVInfo) {
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
   * @return the expression formula assigned to the variable.
   */
  private NumeralFormula<CompoundInterval> getFromEnvironment(
      Variable<CompoundInterval> pVariable) {
    NumeralFormula<CompoundInterval> result = environment.get(pVariable.getMemoryLocation());
    if (result == null) {
      return InvariantsFormulaManager.INSTANCE.asConstant(
          pVariable.getTypeInfo(), getCompoundIntervalManager(pVariable).allPossibleValues());
    }
    return result;
  }
}
