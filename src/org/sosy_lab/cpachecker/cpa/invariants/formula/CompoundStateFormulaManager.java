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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cpa.invariants.CompoundState;


public enum CompoundStateFormulaManager {

  /**
   * The invariants formula manager singleton instance.
   */
  INSTANCE;

  private static final FormulaEvaluationVisitor<CompoundState> FORMULA_EVALUATION_VISITOR = new FormulaCompoundStateEvaluationVisitor();

  private static final Map<String, InvariantsFormula<CompoundState>> EMPTY_ENVIRONMENT = Collections.emptyMap();

  private static final CachingEvaluationVisitor<CompoundState> CACHING_EVALUATION_VISITOR = new CachingEvaluationVisitor<>(EMPTY_ENVIRONMENT, FORMULA_EVALUATION_VISITOR);

  private static final InvariantsFormula<CompoundState> BOTTOM = InvariantsFormulaManager.INSTANCE.asConstant(CompoundState.bottom());

  private static final InvariantsFormula<CompoundState> TOP = InvariantsFormulaManager.INSTANCE.asConstant(CompoundState.top());

  private static final InvariantsFormula<CompoundState> FALSE = InvariantsFormulaManager.INSTANCE.asConstant(CompoundState.logicalFalse());

  private static final InvariantsFormula<CompoundState> TRUE = InvariantsFormulaManager.INSTANCE.asConstant(CompoundState.logicalTrue());

  private static final CollectVarsVisitor<CompoundState> COLLECT_VARS_VISITOR = new CollectVarsVisitor<>();

  private static final ContainsOnlyEnvInfoVisitor<CompoundState> CONTAINS_ONLY_ENV_INFO_VISITOR = new ContainsOnlyEnvInfoVisitor<>();

  private static final SplitConjunctionsVisitor<CompoundState> SPLIT_CONJUNCTIONS_VISITOR = new SplitConjunctionsVisitor<>();

  private static final SplitDisjunctionsVisitor<CompoundState> SPLIT_DISJUNCTIONS_VISITOR = new SplitDisjunctionsVisitor<>();

  public static CompoundState evaluate(InvariantsFormula<CompoundState> pFormula) {
    return pFormula.accept(CACHING_EVALUATION_VISITOR);
  }

  public static boolean isDefinitelyTrue(InvariantsFormula<CompoundState> pFormula) {
    return evaluate(pFormula).isDefinitelyTrue();
  }

  public static boolean isDefinitelyFalse(InvariantsFormula<CompoundState> pFormula) {
    return evaluate(pFormula).isDefinitelyFalse();
  }

  public static boolean isDefinitelyBottom(InvariantsFormula<CompoundState> pFormula) {
    return evaluate(pFormula).isBottom();
  }

  public static boolean isDefinitelyTop(InvariantsFormula<CompoundState> pFormula) {
    return (pFormula instanceof Constant<?>) && ((Constant<CompoundState>) pFormula).getValue().isTop();
  }

  public static boolean definitelyImplies(Collection<InvariantsFormula<CompoundState>> pFormulas, InvariantsFormula<CompoundState> pFormula) {
    return definitelyImplies(pFormulas, pFormula, true, new HashMap<String, InvariantsFormula<CompoundState>>());
  }

  public static boolean definitelyImplies(Collection<InvariantsFormula<CompoundState>> pFormulas, InvariantsFormula<CompoundState> pFormula, Map<String, InvariantsFormula<CompoundState>> pBaseEnvironment) {
    return definitelyImplies(pFormulas, pFormula, true, new HashMap<>(pBaseEnvironment));
  }

  private static boolean definitelyImplies(Collection<InvariantsFormula<CompoundState>> pFormulas, InvariantsFormula<CompoundState> pFormula, boolean extend, Map<String, InvariantsFormula<CompoundState>> pEnvironment) {
    final Collection<InvariantsFormula<CompoundState>> formulas;
    if (extend) {
      formulas = new HashSet<>();
      for (InvariantsFormula<CompoundState> formula : pFormulas) {
        formulas.addAll(formula.accept(SPLIT_CONJUNCTIONS_VISITOR));
      }
    } else {
      formulas = pFormulas;
    }
    Map<String, InvariantsFormula<CompoundState>> tmpEnvironment = pEnvironment;
    PushAssumptionToEnvironmentVisitor patev = new PushAssumptionToEnvironmentVisitor(FORMULA_EVALUATION_VISITOR, tmpEnvironment);
    for (InvariantsFormula<CompoundState> leftFormula : formulas) {
      if (!leftFormula.accept(patev, CompoundState.logicalTrue())) {
        return false;
      }
    }
    if (pFormula.accept(FORMULA_EVALUATION_VISITOR, tmpEnvironment).isDefinitelyTrue()) {
      return true;
    }

    Map<String, InvariantsFormula<CompoundState>> tmpEnvironment2 = new HashMap<>();
    CachingEvaluationVisitor<CompoundState> cachingEvaluationVisitor = new CachingEvaluationVisitor<>(tmpEnvironment, FORMULA_EVALUATION_VISITOR);
    outer:
    for (InvariantsFormula<CompoundState> formula2Part : pFormula.accept(SPLIT_CONJUNCTIONS_VISITOR)) {
      if (!formulas.contains(formula2Part)) {
        Collection<InvariantsFormula<CompoundState>> disjunctions = formula2Part.accept(SPLIT_DISJUNCTIONS_VISITOR);
        if (disjunctions.size() > 1) {
          for (InvariantsFormula<CompoundState> disjunctionPart : disjunctions) {
            if (definitelyImplies(formulas, disjunctionPart, false, pEnvironment)) { // Potential for optimization: Do not extract environment information again
              continue outer;
            }
          }
        }
        if (!tmpEnvironment.isEmpty() && formula2Part.accept(CONTAINS_ONLY_ENV_INFO_VISITOR)) {
          tmpEnvironment2.clear();
          Set<String> varNames = formula2Part.accept(COLLECT_VARS_VISITOR);
          if (!tmpEnvironment.keySet().containsAll(varNames)) {
            return false;
          }
          patev = new PushAssumptionToEnvironmentVisitor(FORMULA_EVALUATION_VISITOR, tmpEnvironment2);
          formula2Part.accept(patev, CompoundState.logicalTrue());
          for (String varName : varNames) {
            InvariantsFormula<CompoundState> leftFormula = tmpEnvironment.get(varName);
            InvariantsFormula<CompoundState> rightFormula = tmpEnvironment2.get(varName);
            CompoundState leftValue = leftFormula == null ? CompoundState.top() : leftFormula.accept(cachingEvaluationVisitor);
            CompoundState rightValue = rightFormula == null ? CompoundState.top() : rightFormula.accept(FORMULA_EVALUATION_VISITOR, tmpEnvironment2);
            if (rightValue.isTop() || !rightValue.contains(leftValue)) {
              return false;
            }
          }
        } else {
          return false;
        }
      }
    }
    return true;
  }

  public static boolean definitelyImplies(InvariantsFormula<CompoundState> pFormula1, InvariantsFormula<CompoundState> pFormula2) {
    if (isDefinitelyFalse(pFormula1) || pFormula1.equals(pFormula2)) {
      return true;
    }
    if (pFormula1 instanceof Equal<?> && pFormula2 instanceof Equal<?>) {
      Equal<CompoundState> p1 = (Equal<CompoundState>) pFormula1;
      Equal<CompoundState> p2 = (Equal<CompoundState>) pFormula2;
      Variable<CompoundState> var = null;
      CompoundState value = null;
      if (p1.getOperand1() instanceof Variable<?> && p1.getOperand2() instanceof Constant<?>) {
        var = (Variable<CompoundState>) p1.getOperand1();
        value = ((Constant<CompoundState>)p1.getOperand2()).getValue();
      } else  if (p1.getOperand2() instanceof Variable<?> && p1.getOperand1() instanceof Constant<?>) {
        var = (Variable<CompoundState>) p1.getOperand2();
        value = ((Constant<CompoundState>)p1.getOperand1()).getValue();
      }
      if (var != null && value != null) {
        CompoundState newValue = null;
        if (var.equals(p2.getOperand1()) && p2.getOperand2() instanceof Constant<?>) {
          newValue = (((Constant<CompoundState>) p2.getOperand2()).getValue());
        } else if (var.equals(p2.getOperand2()) && p2.getOperand1() instanceof Constant<?>) {
          newValue = (((Constant<CompoundState>) p2.getOperand1()).getValue());
        }
        if (newValue != null) {
          return newValue.contains(value);
        }
      }
    }

    Collection<InvariantsFormula<CompoundState>> leftFormulas = pFormula1.accept(SPLIT_CONJUNCTIONS_VISITOR);

    return definitelyImplies(leftFormulas, pFormula2, false, new HashMap<String, InvariantsFormula<CompoundState>>());
  }

  /**
   * Gets the sum of the given formulae as a formula.
   *
   * @param pSummand1 the first summand.
   * @param pSummand2 the second summand.
   *
   * @return the sum of the given formulae.
   */
  public InvariantsFormula<CompoundState> add(InvariantsFormula<CompoundState> pSummand1, InvariantsFormula<CompoundState> pSummand2) {
    if (isDefinitelyBottom(pSummand1) || isDefinitelyBottom(pSummand2)) {
      return BOTTOM;
    }
    if (isDefinitelyTop(pSummand1) || isDefinitelyTop(pSummand2)) {
      return TOP;
    }
    return InvariantsFormulaManager.INSTANCE.add(pSummand1, pSummand2);
  }

  /**
   * Gets the binary and operation over the given operands.
   *
   * @param pOperand1 the first operand.
   * @param pOperand2 the second operand.
   *
   * @return the binary and operation over the given operands.
   */
  public InvariantsFormula<CompoundState> binaryAnd(InvariantsFormula<CompoundState> pOperand1, InvariantsFormula<CompoundState> pOperand2) {
    if (isDefinitelyBottom(pOperand1) || isDefinitelyBottom(pOperand2)) {
      return BOTTOM;
    }
    if (isDefinitelyTop(pOperand1) || isDefinitelyTop(pOperand2)) {
      return TOP;
    }
    return InvariantsFormulaManager.INSTANCE.binaryAnd(pOperand1, pOperand2);
  }

  /**
   * Gets the binary negation of the given formula.
   *
   * @param pToFlip the operand of the bit flip operation.
   *
   * @return the binary negation of the given formula.
   */
  public InvariantsFormula<CompoundState> binaryNot(InvariantsFormula<CompoundState> pToFlip) {
    if (isDefinitelyBottom(pToFlip)) {
      return BOTTOM;
    }
    if (isDefinitelyTop(pToFlip)) {
      return TOP;
    }
    return InvariantsFormulaManager.INSTANCE.binaryNot(pToFlip);
  }

  /**
   * Gets an invariants formula representing the binary or operation over the
   * given operands.
   *
   * @param pOperand1 the first operand.
   * @param pOperand2 the second operand.
   *
   * @return an invariants formula representing the binary or operation over the
   * given operands.
   */
  public InvariantsFormula<CompoundState> binaryOr(InvariantsFormula<CompoundState> pOperand1, InvariantsFormula<CompoundState> pOperand2) {
    if (isDefinitelyBottom(pOperand1) || isDefinitelyBottom(pOperand2)) {
      return BOTTOM;
    }
    if (isDefinitelyTop(pOperand1) || isDefinitelyTop(pOperand2)) {
      return TOP;
    }
    return InvariantsFormulaManager.INSTANCE.binaryOr(pOperand1, pOperand2);
  }

  /**
   * Gets an invariants formula representing the binary exclusive or operation
   * over the given operands.
   *
   * @param pOperand1 the first operand.
   * @param pOperand2 the second operand.
   *
   * @return an invariants formula representing the binary exclusive or operation
   * over the given operands.
   */
  public InvariantsFormula<CompoundState> binaryXor(InvariantsFormula<CompoundState> pOperand1, InvariantsFormula<CompoundState> pOperand2) {
    if (isDefinitelyBottom(pOperand1) || isDefinitelyBottom(pOperand2)) {
      return BOTTOM;
    }
    if (isDefinitelyTop(pOperand1) || isDefinitelyTop(pOperand2)) {
      return TOP;
    }
    return InvariantsFormulaManager.INSTANCE.binaryXor(pOperand1, pOperand2);
  }

  /**
   * Gets a invariants formula representing a constant with the given value.
   *
   * @param pValue the value of the constant.
   *
   * @return a invariants formula representing a constant with the given value.
   */
  public InvariantsFormula<CompoundState> asConstant(CompoundState pValue) {
    return InvariantsFormulaManager.INSTANCE.asConstant(pValue);
  }

  /**
   * Gets an invariants formula representing the division of the given
   * numerator formula by the given denominator formula.
   *
   * @param pNumerator the numerator of the fraction.
   * @param pDenominator the denominator of the fraction.
   *
   * @return an invariants formula representing the division of the given
   * numerator formula by the given denominator formula.
   */
  public InvariantsFormula<CompoundState> divide(InvariantsFormula<CompoundState> pNumerator, InvariantsFormula<CompoundState> pDenominator) {
    if (isDefinitelyBottom(pNumerator) || isDefinitelyBottom(pDenominator)) {
      return BOTTOM;
    }
    if (isDefinitelyTop(pNumerator) || isDefinitelyTop(pDenominator)) {
      return TOP;
    }
    return InvariantsFormulaManager.INSTANCE.divide(pNumerator, pDenominator);
  }

  /**
   * Gets an invariants formula representing the equation over the given
   * formulae.
   *
   * @param pOperand1 the first operand of the equation.
   * @param pOperand2 the second operand of the equation.
   *
   * @return an invariants formula representing the equation of the given
   * operands.
   */
  public InvariantsFormula<CompoundState> equal(InvariantsFormula<CompoundState> pOperand1, InvariantsFormula<CompoundState> pOperand2) {
    if (isDefinitelyBottom(pOperand1) || isDefinitelyBottom(pOperand2)) {
      return BOTTOM;
    }
    if (isDefinitelyTop(pOperand1) || isDefinitelyTop(pOperand2)) {
      return TOP;
    }
    if (pOperand1 instanceof Union<?>) {
      Union<CompoundState> union = (Union<CompoundState>) pOperand1;
      return logicalOr(equal(union.getOperand1(), pOperand2), equal(union.getOperand2(), pOperand2));
    }
    if (pOperand2 instanceof Union<?>) {
      Union<CompoundState> union = (Union<CompoundState>) pOperand2;
      return logicalOr(equal(pOperand1, union.getOperand1()), equal(pOperand1, union.getOperand2()));
    }
    return InvariantsFormulaManager.INSTANCE.equal(pOperand1, pOperand2);
  }

  /**
   * Gets an invariants formula representing a greater-than inequation over the
   * given operands.
   *
   * @param pOperand1 the left operand of the inequation.
   * @param pOperand2 the right operand of the inequation.
   *
   * @return an invariants formula representing a greater-than inequation over
   * the given operands.
   */
  public InvariantsFormula<CompoundState> greaterThan(InvariantsFormula<CompoundState> pOperand1, InvariantsFormula<CompoundState> pOperand2) {
    if (isDefinitelyBottom(pOperand1) || isDefinitelyBottom(pOperand2)) {
      return BOTTOM;
    }
    if (isDefinitelyTop(pOperand1) || isDefinitelyTop(pOperand2)) {
      return TOP;
    }
    if (pOperand1 instanceof Union<?>) {
      Union<CompoundState> union = (Union<CompoundState>) pOperand1;
      return logicalOr(greaterThan(union.getOperand1(), pOperand2), greaterThan(union.getOperand2(), pOperand2));
    }
    if (pOperand2 instanceof Union<?>) {
      Union<CompoundState> union = (Union<CompoundState>) pOperand2;
      return logicalOr(greaterThan(pOperand1, union.getOperand1()), greaterThan(pOperand1, union.getOperand2()));
    }
    return InvariantsFormulaManager.INSTANCE.greaterThan(pOperand1, pOperand2);
  }

  /**
   * Gets an invariants formula representing a greater-than or equal inequation
   * over the given operands.
   *
   * @param pOperand1 the left operand of the inequation.
   * @param pOperand2 the right operand of the inequation.
   *
   * @return an invariants formula representing a greater-than or equal
   * inequation over the given operands.
   */
  public InvariantsFormula<CompoundState> greaterThanOrEqual(InvariantsFormula<CompoundState> pOperand1, InvariantsFormula<CompoundState> pOperand2) {
    if (isDefinitelyBottom(pOperand1) || isDefinitelyBottom(pOperand2)) {
      return BOTTOM;
    }
    if (isDefinitelyTop(pOperand1) || isDefinitelyTop(pOperand2)) {
      return TOP;
    }
    if (pOperand1 instanceof Union<?>) {
      Union<CompoundState> union = (Union<CompoundState>) pOperand1;
      return logicalOr(greaterThanOrEqual(union.getOperand1(), pOperand2), greaterThanOrEqual(union.getOperand2(), pOperand2));
    }
    if (pOperand2 instanceof Union<?>) {
      Union<CompoundState> union = (Union<CompoundState>) pOperand2;
      return logicalOr(greaterThanOrEqual(pOperand1, union.getOperand1()), greaterThanOrEqual(pOperand1, union.getOperand2()));
    }
    return InvariantsFormulaManager.INSTANCE.greaterThanOrEqual(pOperand1, pOperand2);
  }

  /**
   * Gets an invariants formula representing a less-than inequation over the
   * given operands.
   *
   * @param pOperand1 the left operand of the inequation.
   * @param pOperand2 the right operand of the inequation.
   *
   * @return an invariants formula representing a less-than inequation over the
   * given operands.
   */
  public InvariantsFormula<CompoundState> lessThan(InvariantsFormula<CompoundState> pOperand1, InvariantsFormula<CompoundState> pOperand2) {
    if (isDefinitelyBottom(pOperand1) || isDefinitelyBottom(pOperand2)) {
      return BOTTOM;
    }
    if (isDefinitelyTop(pOperand1) || isDefinitelyTop(pOperand2)) {
      return TOP;
    }
    if (pOperand1 instanceof Union<?>) {
      Union<CompoundState> union = (Union<CompoundState>) pOperand1;
      return logicalOr(lessThan(union.getOperand1(), pOperand2), lessThan(union.getOperand2(), pOperand2));
    }
    if (pOperand2 instanceof Union<?>) {
      Union<CompoundState> union = (Union<CompoundState>) pOperand2;
      return logicalOr(lessThan(pOperand1, union.getOperand1()), lessThan(pOperand1, union.getOperand2()));
    }
    return InvariantsFormulaManager.INSTANCE.lessThan(pOperand1, pOperand2);
  }

  /**
   * Gets an invariants formula representing a less-than or equal inequation
   * over the given operands.
   *
   * @param pOperand1 the left operand of the inequation.
   * @param pOperand2 the right operand of the inequation.
   *
   * @return an invariants formula representing a less-than or equal inequation
   * over the given operands.
   */
  public InvariantsFormula<CompoundState> lessThanOrEqual(InvariantsFormula<CompoundState> pOperand1, InvariantsFormula<CompoundState> pOperand2) {
    if (isDefinitelyBottom(pOperand1) || isDefinitelyBottom(pOperand2)) {
      return BOTTOM;
    }
    if (isDefinitelyTop(pOperand1) || isDefinitelyTop(pOperand2)) {
      return TOP;
    }
    if (pOperand1 instanceof Union<?>) {
      Union<CompoundState> union = (Union<CompoundState>) pOperand1;
      return logicalOr(lessThanOrEqual(union.getOperand1(), pOperand2), lessThanOrEqual(union.getOperand2(), pOperand2));
    }
    if (pOperand2 instanceof Union<?>) {
      Union<CompoundState> union = (Union<CompoundState>) pOperand2;
      return logicalOr(lessThanOrEqual(pOperand1, union.getOperand1()), lessThanOrEqual(pOperand1, union.getOperand2()));
    }
    return InvariantsFormulaManager.INSTANCE.lessThanOrEqual(pOperand1, pOperand2);
  }

  /**
   * Gets an invariants formula representing the logical conjunction over the
   * given operands.
   *
   * @param pOperand1 the first operand of the conjunction.
   * @param pOperand2 the second operand of the conjunction.
   *
   * @return an invariants formula representing the logical conjunction over the
   * given operands.
   */
  public InvariantsFormula<CompoundState> logicalAnd(InvariantsFormula<CompoundState> pOperand1, InvariantsFormula<CompoundState> pOperand2) {
    if (isDefinitelyFalse(pOperand1) || isDefinitelyFalse(pOperand2)) {
      return FALSE;
    }
    if (isDefinitelyTrue(pOperand1)) {
      return pOperand2;
    }
    if (isDefinitelyTrue(pOperand2)) {
      return pOperand1;
    }
    if (isDefinitelyBottom(pOperand1) || isDefinitelyBottom(pOperand2)) {
      return BOTTOM;
    }
    if (isDefinitelyTop(pOperand1) && isDefinitelyTop(pOperand2)) {
      return TOP;
    }
    Map<String, InvariantsFormula<CompoundState>> tmpEnvironment = new HashMap<>();
    PushAssumptionToEnvironmentVisitor patev = new PushAssumptionToEnvironmentVisitor(FORMULA_EVALUATION_VISITOR, tmpEnvironment);
    if (!pOperand1.accept(patev, CompoundState.logicalTrue())) {
      return FALSE;
    }
    if (!pOperand2.accept(patev, CompoundState.logicalTrue())) {
      return FALSE;
    }
    if (definitelyImplies(pOperand1, pOperand2)) {
      return pOperand1;
    }
    if (definitelyImplies(pOperand2, pOperand1)) {
      return pOperand2;
    }
    if (pOperand1 instanceof Equal<?> && pOperand2 instanceof Equal<?>) {
      Equal<CompoundState> p1 = (Equal<CompoundState>) pOperand1;
      Equal<CompoundState> p2 = (Equal<CompoundState>) pOperand2;
      Variable<CompoundState> var = null;
      CompoundState value = null;
      if (p1.getOperand1() instanceof Variable<?> && p1.getOperand2() instanceof Constant<?>) {
        var = (Variable<CompoundState>) p1.getOperand1();
        value = ((Constant<CompoundState>)p1.getOperand2()).getValue();
      } else  if (p1.getOperand2() instanceof Variable<?> && p1.getOperand1() instanceof Constant<?>) {
        var = (Variable<CompoundState>) p1.getOperand2();
        value = ((Constant<CompoundState>)p1.getOperand1()).getValue();
      }
      if (var != null && value != null) {
        CompoundState newValue = null;
        if (var.equals(p2.getOperand1()) && p2.getOperand2() instanceof Constant<?>) {
          newValue = value.intersectWith(((Constant<CompoundState>) p2.getOperand2()).getValue());
        } else if (var.equals(p2.getOperand2()) && p2.getOperand1() instanceof Constant<?>) {
          newValue = value.intersectWith(((Constant<CompoundState>) p2.getOperand1()).getValue());
        }
        if (newValue != null) {
          if (newValue.isTop()) {
            return TRUE;
          }
          if (newValue.isBottom()) {
            return FALSE;
          }
          return equal(var, asConstant(newValue));
        }
      }
    }
    return LogicalAnd.of(pOperand1, pOperand2);
  }

  /**
   * Gets an invariants formula representing the logical negation of the given
   * operand.
   *
   * @param pToNegate the invariants formula to negate.
   *
   * @return an invariants formula representing the logical negation of the given
   * operand.
   */
  public InvariantsFormula<CompoundState> logicalNot(InvariantsFormula<CompoundState> pToNegate) {
    if (isDefinitelyBottom(pToNegate)) {
      return BOTTOM;
    }
    if (isDefinitelyTop(pToNegate)) {
      return TOP;
    }
    if (isDefinitelyFalse(pToNegate)) {
      return TRUE;
    }
    if (isDefinitelyTrue(pToNegate)) {
      return FALSE;
    }
    if (pToNegate instanceof LogicalNot<?>) {
      return ((LogicalNot<CompoundState>) pToNegate).getNegated();
    }
    return InvariantsFormulaManager.INSTANCE.logicalNot(pToNegate);
  }

  /**
   * Gets an invariants formula representing the logical disjunction over the
   * given operands.
   *
   * @param pOperand1 the first operand of the disjunction.
   * @param pOperand2 the second operand of the disjunction.
   *
   * @return an invariants formula representing the logical disjunction over
   * the given operands.
   */
  public InvariantsFormula<CompoundState> logicalOr(InvariantsFormula<CompoundState> pOperand1, InvariantsFormula<CompoundState> pOperand2) {
    if (isDefinitelyTrue(pOperand1) || isDefinitelyTrue(pOperand2)) {
      return TRUE;
    }
    if (isDefinitelyFalse(pOperand1)) {
      return pOperand2;
    }
    if (isDefinitelyFalse(pOperand2)) {
      return pOperand1;
    }
    if (isDefinitelyBottom(pOperand1) || isDefinitelyBottom(pOperand2)) {
      return BOTTOM;
    }
    if (isDefinitelyTop(pOperand1) && isDefinitelyTop(pOperand2)) {
      return TOP;
    }
    if (definitelyImplies(pOperand1, pOperand2)) {
      return pOperand2;
    }
    if (definitelyImplies(pOperand2, pOperand1)) {
      return pOperand1;
    }
    return logicalNot(logicalAnd(logicalNot(pOperand1), logicalNot(pOperand2)));
  }

  /**
   * Gets an invariants formula representing a logical implication over the
   * given operands, meaning that the first operand implies the second operand.
   *
   * @param pOperand1 the implication assumption.
   * @param pOperand2 the implication conclusion.
   *
   * @return an invariants formula representing a logical implication over the
   * given operands, meaning that the first operand implies the second operand.
   */
  public InvariantsFormula<CompoundState> logicalImplies(InvariantsFormula<CompoundState> pOperand1, InvariantsFormula<CompoundState> pOperand2) {
    if (definitelyImplies(pOperand1, pOperand2)) {
      return TRUE;
    }
    return logicalNot(logicalAnd(pOperand1, logicalNot(pOperand2)));
  }

  /**
   * Gets an invariants formula representing the modulo operation over the
   * given operands.
   *
   * @param pNumerator the numerator of the fraction.
   * @param pDenominator the denominator of the fraction.
   * @return an invariants formula representing the modulo operation over the
   * given operands.
   */
  public InvariantsFormula<CompoundState> modulo(InvariantsFormula<CompoundState> pNumerator, InvariantsFormula<CompoundState> pDenominator) {
    if (isDefinitelyBottom(pNumerator) || isDefinitelyBottom(pDenominator)) {
      return BOTTOM;
    }
    if (isDefinitelyTop(pNumerator) || isDefinitelyTop(pDenominator)) {
      return TOP;
    }
    return InvariantsFormulaManager.INSTANCE.modulo(pNumerator, pDenominator);
  }

  /**
   * Gets an invariants formula representing the multiplication of the given
   * factors.
   *
   * @param pFactor1 the first factor.
   * @param pFactor2 the second factor.
   *
   * @return an invariants formula representing the multiplication of the given
   * factors.
   */
  public InvariantsFormula<CompoundState> multiply(InvariantsFormula<CompoundState> pFactor1,
      InvariantsFormula<CompoundState> pFactor2) {
    if (isDefinitelyBottom(pFactor1) || isDefinitelyBottom(pFactor2)) {
      return BOTTOM;
    }
    if (isDefinitelyTop(pFactor1) || isDefinitelyTop(pFactor2)) {
      return TOP;
    }
    return InvariantsFormulaManager.INSTANCE.multiply(pFactor1, pFactor2);
  }

  /**
   * Gets an invariants formula representing the numerical negation of the
   * given invariants formula.
   *
   * @param pToNegate the invariants formula to negate.
   *
   * @return an invariants formula representing the numerical negation of the
   * given invariants formula.
   */
  public InvariantsFormula<CompoundState> negate(InvariantsFormula<CompoundState> pToNegate) {
    if (isDefinitelyBottom(pToNegate)) {
      return BOTTOM;
    }
    if (isDefinitelyTop(pToNegate)) {
      return TOP;
    }
    return InvariantsFormulaManager.INSTANCE.negate(pToNegate);
  }

  /**
   * Gets the difference of the given invariants formulae as a formula.
   *
   * @param pMinuend the minuend.
   * @param pSubtrahend the subtrahend.
   *
   * @return the sum of the given formulae.
   */
  public InvariantsFormula<CompoundState> subtract(InvariantsFormula<CompoundState> pMinuend,
      InvariantsFormula<CompoundState> pSubtrahend) {
    if (isDefinitelyBottom(pMinuend) || isDefinitelyBottom(pSubtrahend)) {
      return BOTTOM;
    }
    if (isDefinitelyTop(pMinuend) || isDefinitelyTop(pSubtrahend)) {
      return TOP;
    }
    return InvariantsFormulaManager.INSTANCE.subtract(pMinuend, pSubtrahend);
  }

  /**
   * Gets an invariants formula representing the left shift of the first given
   * operand by the second given operand.
   *
   * @param pToShift the operand to be shifted.
   * @param pShiftDistance the shift distance.
   *
   * @return an invariants formula representing the left shift of the first
   * given operand by the second given operand.
   */
  public InvariantsFormula<CompoundState> shiftLeft(InvariantsFormula<CompoundState> pToShift,
      InvariantsFormula<CompoundState> pShiftDistance) {
    if (isDefinitelyBottom(pToShift) || isDefinitelyBottom(pShiftDistance)) {
      return BOTTOM;
    }
    if (isDefinitelyTop(pToShift) || isDefinitelyTop(pShiftDistance)) {
      return TOP;
    }
    return InvariantsFormulaManager.INSTANCE.shiftLeft(pToShift, pShiftDistance);
  }

  /**
   * Gets an invariants formula representing the right shift of the first given
   * operand by the second given operand.
   *
   * @param pToShift the operand to be shifted.
   * @param pShiftDistance the shift distance.
   *
   * @return an invariants formula representing the right shift of the first
   * given operand by the second given operand.
   */
  public InvariantsFormula<CompoundState> shiftRight(InvariantsFormula<CompoundState> pToShift,
      InvariantsFormula<CompoundState> pShiftDistance) {
    if (isDefinitelyBottom(pToShift) || isDefinitelyBottom(pShiftDistance)) {
      return BOTTOM;
    }
    if (isDefinitelyTop(pToShift) || isDefinitelyTop(pShiftDistance)) {
      return TOP;
    }
    return InvariantsFormulaManager.INSTANCE.shiftRight(pToShift, pShiftDistance);
  }

  /**
   * Gets an invariants formula representing the union of the given invariants
   * formulae.
   *
   * @param pOperand1 the first operand.
   * @param pOperand2 the second operand.
   *
   * @return an invariants formula representing the union of the given invariants
   * formulae.
   */
  public InvariantsFormula<CompoundState> union(InvariantsFormula<CompoundState> pOperand1,
      InvariantsFormula<CompoundState> pOperand2) {
    if (isDefinitelyBottom(pOperand1)) {
      if (isDefinitelyBottom(pOperand2)) {
        return BOTTOM;
      } else {
        return pOperand2;
      }
    } else if (isDefinitelyBottom(pOperand2)) {
      return pOperand1;
    }
    if (isDefinitelyTop(pOperand1) || isDefinitelyTop(pOperand2)) {
      return TOP;
    }
    if (pOperand1.equals(pOperand2)) {
      return pOperand1;
    }
    return InvariantsFormulaManager.INSTANCE.union(pOperand1, pOperand2);
  }

  /**
   * Gets an invariants formula representing the variable with the given name.
   *
   * @param pName the name of the variable.
   *
   * @return an invariants formula representing the variable with the given name.
   */
  public Variable<CompoundState> asVariable(String pName) {
    return InvariantsFormulaManager.INSTANCE.asVariable(pName);
  }

}
