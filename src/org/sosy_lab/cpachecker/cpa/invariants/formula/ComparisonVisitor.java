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
 * Instances of this class are invariants formula visitors that are used to
 * analyze how the visited compound state formulae compare to a comparison
 * formula.
 */
public class ComparisonVisitor extends DefaultParameterizedFormulaVisitor<CompoundInterval, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>>, ComparisonVisitor.Result> {

  /**
   * The comparison formula.
   */
  private final InvariantsFormula<CompoundInterval> comparisonFormula;

  /**
   * The formula evaluation visitor used.
   */
  private final FormulaEvaluationVisitor<CompoundInterval> evaluationVisitor;

  /**
   * The last visitor used to determine stateful equality over formulae.
   */
  private StateEqualsVisitor cachedStateEqualsVisitor;

  /**
   * The key identity the last stateful equality determination visitor is valid
   * for.
   */
  private Object stateEqualsVisitorCacheKey;

  public ComparisonVisitor(InvariantsFormula<CompoundInterval> pComparisonFormula, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    this.comparisonFormula = pComparisonFormula;
    this.evaluationVisitor = pEvaluationVisitor;
  }

  /**
   * Gets a visitor used to determine stateful equality over formulae for the
   * given environment.
   *
   * @param pEnvironment the environment used by the visitor.
   *
   * @return a visitor used to determine stateful equality over formulae for the
   * given environment.
   */
  private StateEqualsVisitor getStateEqualsVisitor(Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    if (stateEqualsVisitorCacheKey == pEnvironment) {
      return cachedStateEqualsVisitor;
    }
    stateEqualsVisitorCacheKey = pEnvironment;
    cachedStateEqualsVisitor = new StateEqualsVisitor(evaluationVisitor, pEnvironment);
    return cachedStateEqualsVisitor;
  }

  /**
   * Gets the value of the comparison formula within the given environment.
   *
   * @param pEnvironment the environment to evaluate the comparison formula in.
   *
   * @return the value of the comparison formula within the given environment.
   */
  private CompoundInterval getComparisonValue(Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return comparisonFormula.accept(evaluationVisitor, pEnvironment);
  }

  /**
   * Compares the comparison formula to zero within the given environment.
   *
   * @param pEnvironment the environment to evaluate the comparison formula in.
   *
   * @return the result of the comparison between the comparison formula and
   * zero.
   */
  private Result getCompValueToZero(Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    if (isCompValueDefinitelyZero(pEnvironment)) {
      return Result.EQUAL;
    }
    if (isCompValueDefinitelyNegative(pEnvironment)) {
      return Result.LESS_THAN;
    }
    if (isCompValueDefinitelyPositive(pEnvironment)) {
      return Result.GREATER_THAN;
    }
    if (isCompValueDefinitelyNonPositive(pEnvironment)) {
      return Result.LESS_THAN_OR_EQUAL;
    }
    if (isCompValueDefinitelyNonNegative(pEnvironment)) {
      return Result.GREATER_THAN_OR_EQUAL;
    }
    if (isCompValueDefinitelyNonZero(pEnvironment)) {
      return Result.INEQUAL;
    }
    return Result.UNKNOWN;
  }

  /**
   * Checks if the comparison formula definitely evaluates to a negative value
   * within the given environment.
   *
   * @param pEnvironment the environment to evaluate the comparison formula in.
   *
   * @return <code>true</code> if the comparison formula definitely evaluates
   * to a negative value within the given environment, <code>false</code>
   * otherwise.
   */
  private boolean isCompValueDefinitelyNegative(Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    CompoundInterval comparisonValue = getComparisonValue(pEnvironment);
    if (comparisonValue.hasUpperBound()) {
      return comparisonValue.getUpperBound().signum() < 0;
    }
    return false;
  }

  /**
   * Checks if the comparison formula definitely evaluates to a non-positive
   * value within the given environment.
   *
   * @param pEnvironment the environment to evaluate the comparison formula in.
   *
   * @return <code>true</code> if the comparison formula definitely evaluates
   * to a non-positive value within the given environment, <code>false</code>
   * otherwise.
   */
  private boolean isCompValueDefinitelyNonPositive(Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    CompoundInterval comparisonValue = getComparisonValue(pEnvironment);
    if (comparisonValue.hasUpperBound()) {
      return comparisonValue.getUpperBound().signum() <= 0;
    }
    return false;
  }

  /**
   * Checks if the comparison formula definitely evaluates to a positive value
   * within the given environment.
   *
   * @param pEnvironment the environment to evaluate the comparison formula in.
   *
   * @return <code>true</code> if the comparison formula definitely evaluates
   * to a positive value within the given environment, <code>false</code>
   * otherwise.
   */
  private boolean isCompValueDefinitelyPositive(Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    CompoundInterval comparisonValue = getComparisonValue(pEnvironment);
    if (comparisonValue.hasLowerBound()) {
      return comparisonValue.getLowerBound().signum() > 0;
    }
    return false;
  }

  /**
   * Checks if the comparison formula definitely evaluates to a non-negative
   * value within the given environment.
   *
   * @param pEnvironment the environment to evaluate the comparison formula in.
   *
   * @return <code>true</code> if the comparison formula definitely evaluates
   * to a non--negative value within the given environment, <code>false</code>
   * otherwise.
   */
  private boolean isCompValueDefinitelyNonNegative(Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    CompoundInterval comparisonValue = getComparisonValue(pEnvironment);
    if (comparisonValue.hasLowerBound()) {
      return comparisonValue.getLowerBound().signum() >= 0;
    }
    return false;
  }

  /**
   * Checks if the comparison formula definitely evaluates to zero within the
   * given environment.
   *
   * @param pEnvironment the environment to evaluate the comparison formula in.
   *
   * @return <code>true</code> if the comparison formula definitely evaluates
   * to zero within the given environment, <code>false</code> otherwise.
   */
  private boolean isCompValueDefinitelyZero(Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    CompoundInterval comparisonValue = getComparisonValue(pEnvironment);
    return comparisonValue.isSingleton() && comparisonValue.containsZero();
  }

  /**
   * Checks if the comparison formula definitely evaluates to a non-zero value
   * within the given environment.
   *
   * @param pEnvironment the environment to evaluate the comparison formula in.
   *
   * @return <code>true</code> if the comparison formula definitely evaluates
   * to a non-zero value within the given environment, <code>false</code>
   * otherwise.
   */
  private boolean isCompValueDefinitelyNonZero(Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return !getComparisonValue(pEnvironment).containsZero();
  }

  @Override
  public Result visit(Add<CompoundInterval> pAdd, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    Result s1Result = pAdd.getSummand1().accept(this, pEnvironment);
    Result s2Result = pAdd.getSummand2().accept(this, pEnvironment);
    if (s1Result == Result.UNKNOWN && s2Result == Result.UNKNOWN) {
      return Result.UNKNOWN;
    }
    if (s1Result != Result.UNKNOWN && s2Result != Result.UNKNOWN) {
      InvariantsFormula<CompoundInterval> a = pAdd.getSummand1();
      if (s1Result.compareTo(s2Result) > 0) {
        Result tmp = s1Result;
        s1Result = s2Result;
        s2Result = tmp;
        a = pAdd.getSummand2();
      }
      switch (s1Result) {
      case EQUAL:
        switch (s2Result) {
        case EQUAL:
          // If x+x, result depends on sign of x
          return getCompValueToZero(pEnvironment);
        default:
          CompoundInterval other = a.accept(evaluationVisitor, pEnvironment);
          if (other.isSingleton() && other.containsZero()) {
            return Result.EQUAL;
          }
          if (other.containsNegative() && !other.containsPositive()) {
            if (other.containsZero()) {
              return Result.LESS_THAN_OR_EQUAL;
            }
            return Result.LESS_THAN;
          }
          if (other.containsPositive() && !other.containsNegative()) {
            if (other.containsZero()) {
              return Result.GREATER_THAN_OR_EQUAL;
            }
            return Result.GREATER_THAN;
          }
        }
        break;
      default:
        break;
      }
    }
    return visitDefault(pAdd, pEnvironment);
  }

  @Override
  public Result visit(Variable<CompoundInterval> pVariable, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    if (!(comparisonFormula instanceof Variable<?>)) {
      Result inverted = comparisonFormula.accept(new ComparisonVisitor(pVariable, evaluationVisitor), pEnvironment);
      switch (inverted) {
      case EQUAL:
        return Result.EQUAL;
      case GREATER_THAN:
        return Result.LESS_THAN;
      case GREATER_THAN_OR_EQUAL:
        return Result.LESS_THAN_OR_EQUAL;
      case INEQUAL:
        return Result.INEQUAL;
      case LESS_THAN:
        return Result.GREATER_THAN;
      case LESS_THAN_OR_EQUAL:
        return Result.GREATER_THAN_OR_EQUAL;
      default:
        break;
      }
    }
    return visitDefault(pVariable, pEnvironment);
  }

  @Override
  protected Result visitDefault(InvariantsFormula<CompoundInterval> pFormula, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    if (pFormula.accept(getStateEqualsVisitor(pEnvironment), comparisonFormula)) {
      return Result.EQUAL;
    }
    CompoundInterval evaluated = pFormula.accept(evaluationVisitor, pEnvironment);
    CompoundInterval comparisonValue = getComparisonValue(pEnvironment);
    CompoundInterval equation = evaluated.logicalEquals(comparisonValue);
    if (equation.isDefinitelyTrue()) {
      return Result.EQUAL;
    }
    if (evaluated.greaterThan(comparisonValue).isDefinitelyTrue()) {
      return Result.GREATER_THAN;
    }
    if (evaluated.greaterEqual(comparisonValue).isDefinitelyTrue()) {
      return Result.GREATER_THAN_OR_EQUAL;
    }
    if (evaluated.lessThan(comparisonValue).isDefinitelyTrue()) {
      return Result.LESS_THAN;
    }
    if (evaluated.lessEqual(comparisonValue).isDefinitelyTrue()) {
      return Result.LESS_THAN_OR_EQUAL;
    }
    if (equation.isDefinitelyFalse()) {
      return Result.INEQUAL;
    }
    return Result.UNKNOWN;
  }

  /**
   * Instances of this enumeration represent possible comparison results.
   */
  public enum Result {

    /**
     * Represents that a formula is less than a comparison formula.
     */
    LESS_THAN,

    /**
     * Represents that a formula is less than or equal to a comparison formula.
     */
    LESS_THAN_OR_EQUAL,

    /**
     * Represents that a formula is equal to a comparison formula.
     */
    EQUAL,

    /**
     * Represents that a formula is greater than or equal to a comparison
     * formula.
     */
    GREATER_THAN_OR_EQUAL,

    /**
     * Represents that a formula is greater than a comparison formula.
     */
    GREATER_THAN,

    /**
     * Represents that a formula is not equal to a comparison formula.
     */
    INEQUAL,

    /**
     * Represents that it is unknown how a formula compares to a comparison
     * formula.
     */
    UNKNOWN;

  }

}
