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
import org.sosy_lab.cpachecker.cpa.invariants.formula.ComparisonVisitor.Result;

/**
 * Instances of this class are parameterized visitors used to determine if an
 * assumption over compound state invariants formulae definitely still holds
 * after a variable changed.
 */
public class DefinitelyStillHoldsVisitor extends DefaultParameterizedFormulaVisitor<CompoundInterval, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>>, Boolean> implements ParameterizedInvariantsFormulaVisitor<CompoundInterval, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>>, Boolean> {

  /**
   * The visitor used to check if a formula contains a specified variable.
   */
  private static final ContainsVarVisitor<CompoundInterval> CONTAINS_VAR_VISITOR = new ContainsVarVisitor<>();

  /**
   * The formula representing the new value of the changed variable.
   */
  private final InvariantsFormula<CompoundInterval> newValue;

  /**
   * The evaluation visitor used to evaluate compound state invariants formulae
   * to compound states.
   */
  private final FormulaEvaluationVisitor<CompoundInterval> evaluationVisitor;

  /**
   * The name of the changed variable.
   */
  private final String changedVariableName;

  /**
   * The visitor used to compare compound state invariants formulae.
   */
  private final ComparisonVisitor changeAnalyzeVisitor;

  /**
   * Creates a new visitor used to visit compound state invariants formulae to
   * check if they, considered as assumptions, still hold after the variable
   * with the given name is assigned the given new value.
   *
   * @param pChangedVariableName the name of the newly assigned variable.
   * @param pNewValue the formula representing the expression assigned to the
   * variable.
   * @param pEvaluationVisitor the evaluation visitor used to evaluate compound
   * state invariants formulae to compound states.
   */
  public DefinitelyStillHoldsVisitor(String pChangedVariableName,
      InvariantsFormula<CompoundInterval> pNewValue,
      FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    this.newValue = pNewValue;
    this.evaluationVisitor = pEvaluationVisitor;
    this.changedVariableName = pChangedVariableName;
    this.changeAnalyzeVisitor = new ComparisonVisitor(newValue, evaluationVisitor);
  }

  @Override
  public Boolean visit(Equal<CompoundInterval> pEqual, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    if (visitDefault(pEqual, pEnvironment)) {
      return true;
    }
    boolean leftContains = containsChangedVariable(pEqual.getOperand1());
    boolean rightContains = containsChangedVariable(pEqual.getOperand2());
    if (leftContains) {
      if (rightContains) {
        ComparisonVisitor cav = new ComparisonVisitor(pEqual.getOperand1(), evaluationVisitor);
        return pEqual.getOperand2().accept(cav, pEnvironment) == Result.EQUAL;
      }
      return pEqual.getOperand1().accept(changeAnalyzeVisitor, pEnvironment) == Result.EQUAL;
    }
    if (rightContains) {
      return pEqual.getOperand2().accept(changeAnalyzeVisitor, pEnvironment) == Result.EQUAL;
    }
    return true;
  }

  @Override
  public Boolean visit(LessThan<CompoundInterval> pLessThan, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    if (visitDefault(pLessThan, pEnvironment)) {
      return true;
    }
    boolean leftContains = containsChangedVariable(pLessThan.getOperand1());
    boolean rightContains = containsChangedVariable(pLessThan.getOperand2());
    if (leftContains) {
      if (rightContains) {
        ComparisonVisitor cav = new ComparisonVisitor(pLessThan.getOperand1(), evaluationVisitor);
        return pLessThan.getOperand2().accept(cav, pEnvironment) == Result.LESS_THAN;
      }
      Result compLeftToNew = pLessThan.getOperand1().accept(changeAnalyzeVisitor, pEnvironment);
      return compLeftToNew == Result.GREATER_THAN
          || compLeftToNew == Result.GREATER_THAN_OR_EQUAL
          || compLeftToNew == Result.EQUAL;
    }
    if (rightContains) {
      Result compRightToNew = pLessThan.getOperand2().accept(changeAnalyzeVisitor, pEnvironment);
      return compRightToNew == Result.LESS_THAN
          || compRightToNew == Result.LESS_THAN_OR_EQUAL
          || compRightToNew == Result.EQUAL;
    }
    return true;
  }

  @Override
  public Boolean visit(LogicalAnd<CompoundInterval> pAnd, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    if (visitDefault(pAnd, pEnvironment)) {
      return true;
    }
    return pAnd.getOperand1().accept(this, pEnvironment) && pAnd.getOperand2().accept(this, pEnvironment);
  }

  @Override
  protected Boolean visitDefault(InvariantsFormula<CompoundInterval> pFormula, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return pFormula.accept(evaluationVisitor, pEnvironment).isDefinitelyTrue();
  }

  /**
   * Checks if the given formula contains the changed variable.
   *
   * @param pFormula the formula to check.
   * @return <code>true</code> if the given formula contains the changed
   * variable, <code>false</code> otherwise.
   */
  private boolean containsChangedVariable(InvariantsFormula<CompoundInterval> pFormula) {
    return containsVariable(pFormula, changedVariableName);
  }

  /**
   * Checks if the given formula contains the variable with the given name.
   *
   * @param pFormula the formula to check.
   * @param pVarName the name of the variable in question.
   * @return <code>true</code> if the given formula contains the  variable,
   * <code>false</code> otherwise.
   */
  private static boolean containsVariable(InvariantsFormula<CompoundInterval> pFormula, String pVarName) {
    return pFormula.accept(CONTAINS_VAR_VISITOR, pVarName);
  }

}
