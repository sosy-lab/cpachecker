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

import java.util.Map;

import org.sosy_lab.cpachecker.cpa.invariants.CompoundState;

public class ChangeAnalyzeVisitor extends DefaultParameterizedFormulaVisitor<CompoundState, Map<? extends String, ? extends InvariantsFormula<CompoundState>>, ChangeAnalyzeVisitor.Result> implements ParameterizedInvariantsFormulaVisitor<CompoundState, Map<? extends String, ? extends InvariantsFormula<CompoundState>>, ChangeAnalyzeVisitor.Result> {

  private final InvariantsFormula<CompoundState> comparisonFormula;

  private CompoundState comparisonValue = null;

  private Result compValueToZero = null;

  private final FormulaEvaluationVisitor<CompoundState> evaluationVisitor;

  private StateEqualsVisitor cachedStateEqualsVisitor;

  private Object stateEqualsVisitorCacheKey;

  public ChangeAnalyzeVisitor(InvariantsFormula<CompoundState> pComparisonFormula, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    this.comparisonFormula = pComparisonFormula;
    this.evaluationVisitor = pEvaluationVisitor;
  }

  private StateEqualsVisitor getStateEqualsVisitor(Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    if (stateEqualsVisitorCacheKey == pEnvironment) {
      return cachedStateEqualsVisitor;
    }
    stateEqualsVisitorCacheKey = pEnvironment;
    cachedStateEqualsVisitor = new StateEqualsVisitor(evaluationVisitor, pEnvironment);
    return cachedStateEqualsVisitor;
  }

  private CompoundState getComparisonValue(Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    if (this.comparisonValue == null) {
      this.comparisonValue = comparisonFormula.accept(evaluationVisitor, pEnvironment);
    }
    return this.comparisonValue;
  }

  private Result getCompValueToZero(Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    if (this.compValueToZero == null) {
      if (isCompValueDefinitelyNegative(pEnvironment)) {
        compValueToZero = Result.LESS_THAN;
      } else if (isCompValueDefinitelyNonPositive(pEnvironment)) {
        compValueToZero = Result.LESS_THAN_OR_EQUAL;
      } else if (isCompValueDefinitelyZero(pEnvironment)) {
        compValueToZero = Result.EQUAL;
      } else if (isCompValueDefinitelyNonNegative(pEnvironment)) {
        compValueToZero = Result.GREATER_THAN_OR_EQUAL;
      } else if (isCompValueDefinitelyPositive(pEnvironment)) {
        compValueToZero = Result.GREATER_THAN;
      } else if (isCompValueDefinitelyNonZero(pEnvironment)) {
        compValueToZero = Result.INEQUAL;
      } else {
        compValueToZero = Result.UNKNOWN;
      }
    }
    return compValueToZero;
  }

  private boolean isCompValueDefinitelyNegative(Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    CompoundState comparisonValue = getComparisonValue(pEnvironment);
    if (comparisonValue.hasUpperBound()) {
      return comparisonValue.getUpperBound().signum() < 0;
    }
    return false;
  }

  private boolean isCompValueDefinitelyNonPositive(Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    CompoundState comparisonValue = getComparisonValue(pEnvironment);
    if (comparisonValue.hasUpperBound()) {
      return comparisonValue.getUpperBound().signum() <= 0;
    }
    return false;
  }

  private boolean isCompValueDefinitelyPositive(Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    CompoundState comparisonValue = getComparisonValue(pEnvironment);
    if (comparisonValue.hasLowerBound()) {
      return comparisonValue.getLowerBound().signum() > 0;
    }
    return false;
  }

  private boolean isCompValueDefinitelyNonNegative(Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    CompoundState comparisonValue = getComparisonValue(pEnvironment);
    if (comparisonValue.hasLowerBound()) {
      return comparisonValue.getLowerBound().signum() >= 0;
    }
    return false;
  }

  private boolean isCompValueDefinitelyZero(Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    CompoundState comparisonValue = getComparisonValue(pEnvironment);
    return comparisonValue.isSingleton() && comparisonValue.containsZero();
  }

  private boolean isCompValueDefinitelyNonZero(Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return !getComparisonValue(pEnvironment).containsZero();
  }

  @Override
  public Result visit(Add<CompoundState> pAdd, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    Result s1Result = pAdd.getSummand1().accept(this, pEnvironment);
    Result s2Result = pAdd.getSummand2().accept(this, pEnvironment);
    if (s1Result == Result.UNKNOWN && s2Result == Result.UNKNOWN) {
      return Result.UNKNOWN;
    }
    if (s1Result != Result.UNKNOWN && s2Result != Result.UNKNOWN) {
      InvariantsFormula<CompoundState> a = pAdd.getSummand1();
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
          CompoundState other = a.accept(evaluationVisitor, pEnvironment);
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
  public Result visit(Variable<CompoundState> pVariable, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    if (!(comparisonFormula instanceof Variable<?>)) {
      Result inverted = comparisonFormula.accept(new ChangeAnalyzeVisitor(pVariable, evaluationVisitor), pEnvironment);
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
  protected Result visitDefault(InvariantsFormula<CompoundState> pFormula, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    if (pFormula.accept(getStateEqualsVisitor(pEnvironment), comparisonFormula)) {
      return Result.EQUAL;
    }
    CompoundState evaluated = pFormula.accept(evaluationVisitor, pEnvironment);
    CompoundState comparisonValue = getComparisonValue(pEnvironment);
    CompoundState equation = evaluated.logicalEquals(comparisonValue);
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

  public enum Result {

    LESS_THAN,

    LESS_THAN_OR_EQUAL,

    EQUAL,

    GREATER_THAN_OR_EQUAL,

    GREATER_THAN,

    INEQUAL,

    UNKNOWN;

  }

}
