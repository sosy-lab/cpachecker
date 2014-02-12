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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;

import com.google.common.collect.FluentIterable;


public enum CompoundIntervalFormulaManager {

  /**
   * The invariants formula manager singleton instance.
   */
  INSTANCE;

  private static final FormulaEvaluationVisitor<CompoundInterval> FORMULA_EVALUATION_VISITOR = new FormulaCompoundStateEvaluationVisitor();

  private static final Map<String, InvariantsFormula<CompoundInterval>> EMPTY_ENVIRONMENT = Collections.emptyMap();

  private static final CachingEvaluationVisitor<CompoundInterval> CACHING_EVALUATION_VISITOR = new CachingEvaluationVisitor<>(EMPTY_ENVIRONMENT, FORMULA_EVALUATION_VISITOR);

  private static final InvariantsFormula<CompoundInterval> BOTTOM = InvariantsFormulaManager.INSTANCE.asConstant(CompoundInterval.bottom());

  private static final InvariantsFormula<CompoundInterval> TOP = InvariantsFormulaManager.INSTANCE.asConstant(CompoundInterval.top());

  private static final InvariantsFormula<CompoundInterval> FALSE = InvariantsFormulaManager.INSTANCE.asConstant(CompoundInterval.logicalFalse());

  private static final InvariantsFormula<CompoundInterval> TRUE = InvariantsFormulaManager.INSTANCE.asConstant(CompoundInterval.logicalTrue());

  private static final InvariantsFormula<CompoundInterval> MINUS_ONE = InvariantsFormulaManager.INSTANCE.asConstant(CompoundInterval.minusOne());

  private static final CollectVarsVisitor<CompoundInterval> COLLECT_VARS_VISITOR = new CollectVarsVisitor<>();

  private static final ContainsOnlyEnvInfoVisitor<CompoundInterval> CONTAINS_ONLY_ENV_INFO_VISITOR = new ContainsOnlyEnvInfoVisitor<>();

  private static final SplitConjunctionsVisitor<CompoundInterval> SPLIT_CONJUNCTIONS_VISITOR = new SplitConjunctionsVisitor<>();

  private static final SplitDisjunctionsVisitor<CompoundInterval> SPLIT_DISJUNCTIONS_VISITOR = new SplitDisjunctionsVisitor<>();

  public static CompoundInterval evaluate(InvariantsFormula<CompoundInterval> pFormula) {
    return pFormula.accept(CACHING_EVALUATION_VISITOR);
  }

  public static boolean isDefinitelyTrue(InvariantsFormula<CompoundInterval> pFormula) {
    return evaluate(pFormula).isDefinitelyTrue();
  }

  public static boolean isDefinitelyFalse(InvariantsFormula<CompoundInterval> pFormula) {
    return evaluate(pFormula).isDefinitelyFalse();
  }

  public static boolean isDefinitelyBottom(InvariantsFormula<CompoundInterval> pFormula) {
    return evaluate(pFormula).isBottom();
  }

  public static boolean isDefinitelyTop(InvariantsFormula<CompoundInterval> pFormula) {
    return (pFormula instanceof Constant<?>) && ((Constant<CompoundInterval>) pFormula).getValue().isTop();
  }

  public static boolean definitelyImplies(Iterable<InvariantsFormula<CompoundInterval>> pFormulas, InvariantsFormula<CompoundInterval> pFormula) {
    return definitelyImplies(pFormulas, pFormula, new HashMap<String, InvariantsFormula<CompoundInterval>>());
  }

  public static boolean definitelyImplies(Iterable<InvariantsFormula<CompoundInterval>> pFormulas, InvariantsFormula<CompoundInterval> pFormula, Map<String, InvariantsFormula<CompoundInterval>> pBaseEnvironment) {
    Map<String, InvariantsFormula<CompoundInterval>> newMap = new HashMap<>(pBaseEnvironment);
    if (pFormula instanceof Collection<?>) {
      return definitelyImplies((Collection<InvariantsFormula<CompoundInterval>>) pFormulas, pFormula, true, newMap, false);
    }
    return definitelyImplies(FluentIterable.from(pFormulas).toSet(), pFormula, true, newMap, false);
  }

  /**
   * Tries to prove that the information base implies the given formula. The
   * information base is given as a combination of formulas and environment
   * data, because the formulas are more generic but the environment may
   * represent some data more efficiently.
   *
   * @param pInformationBaseFormulas the information base as formulas.
   * @param pFormula the formula that is checked for being implied by the
   * information base.
   * @param pExtend whether or not the information base should be further
   * extended by splitting the formulas into their conjunctive parts.
   * @param pInformationBaseEnvironment the information base as an environment.
   * @param pEnvironmentComplete whether or not the environment already
   * contains all information that can be gained from the formulas information
   * base.
   * @return {@code true} if the information base definitely implies the given
   * formula.
   */
  private static boolean definitelyImplies(Collection<InvariantsFormula<CompoundInterval>> pInformationBaseFormulas, InvariantsFormula<CompoundInterval> pFormula, boolean pExtend, Map<String, InvariantsFormula<CompoundInterval>> pInformationBaseEnvironment, boolean pEnvironmentComplete) {
    final Collection<InvariantsFormula<CompoundInterval>> formulas;
    if (pExtend) {
      formulas = new HashSet<>();
      for (InvariantsFormula<CompoundInterval> formula : pInformationBaseFormulas) {
        formulas.addAll(formula.accept(SPLIT_CONJUNCTIONS_VISITOR));
      }
    } else {
      formulas = pInformationBaseFormulas;
    }

    // If any of the conjunctive parts is a disjunction, check try each disjunctive part
    for (InvariantsFormula<CompoundInterval> formula : formulas) {
      Collection<InvariantsFormula<CompoundInterval>> disjunctions = formula.accept(SPLIT_DISJUNCTIONS_VISITOR);
      if (disjunctions.size() > 1) {
        ArrayList<InvariantsFormula<CompoundInterval>> newFormulas = new ArrayList<>(formulas);
        Map<String, InvariantsFormula<CompoundInterval>> newBaseEnvironment = new HashMap<>(pInformationBaseEnvironment);
        newFormulas.remove(formula);
        for (InvariantsFormula<CompoundInterval> disjunctivePart : disjunctions) {
          Collection<InvariantsFormula<CompoundInterval>> conjunctivePartsOfDisjunctivePart = disjunctivePart.accept(SPLIT_CONJUNCTIONS_VISITOR);
          newFormulas.addAll(conjunctivePartsOfDisjunctivePart);
          if (!definitelyImplies(newFormulas, pFormula, false, newBaseEnvironment, false)) {
            return false;
          }
          newFormulas.removeAll(conjunctivePartsOfDisjunctivePart);
        }
        return true;
      }
    }

    // Build the environment defined by the assumptions and check whether it contradicts or implies the proposed implication
    Map<String, InvariantsFormula<CompoundInterval>> tmpEnvironment = pInformationBaseEnvironment;
    PushAssumptionToEnvironmentVisitor patev = new PushAssumptionToEnvironmentVisitor(FORMULA_EVALUATION_VISITOR, tmpEnvironment);
    if (!pEnvironmentComplete) {
      for (InvariantsFormula<CompoundInterval> leftFormula : formulas) {
        if (!leftFormula.accept(patev, CompoundInterval.logicalTrue())) {
          return false;
        }
      }
    }
    if (pFormula.accept(FORMULA_EVALUATION_VISITOR, tmpEnvironment).isDefinitelyTrue()) {
      return true;
    }

    return definitelyImplies(formulas, tmpEnvironment, pFormula);
  }

  public static boolean definitelyImplies(final Map<String, InvariantsFormula<CompoundInterval>> pCompleteEnvironment,
      final InvariantsFormula<CompoundInterval> pFormula) {
    return definitelyImplies(Collections.<InvariantsFormula<CompoundInterval>>emptyList(), pCompleteEnvironment, pFormula);
  }

  private static boolean definitelyImplies(final Collection<InvariantsFormula<CompoundInterval>> pExtendedFormulas,
      final Map<String, InvariantsFormula<CompoundInterval>> pCompleteEnvironment,
      final InvariantsFormula<CompoundInterval> pFormula) {
    // Build the environment defined by the proposed implication and check for contradictions
    Map<String, InvariantsFormula<CompoundInterval>> tmpEnvironment2 = new HashMap<>();
    CachingEvaluationVisitor<CompoundInterval> cachingEvaluationVisitor = new CachingEvaluationVisitor<>(pCompleteEnvironment, FORMULA_EVALUATION_VISITOR);
    outer:
    for (InvariantsFormula<CompoundInterval> formula2Part : pFormula.accept(SPLIT_CONJUNCTIONS_VISITOR)) {
      if (!pExtendedFormulas.contains(formula2Part)) {
        Collection<InvariantsFormula<CompoundInterval>> disjunctions = formula2Part.accept(SPLIT_DISJUNCTIONS_VISITOR);
        if (disjunctions.size() > 1) {
          for (InvariantsFormula<CompoundInterval> disjunctionPart : disjunctions) {
            if (definitelyImplies(pExtendedFormulas, disjunctionPart, false, pCompleteEnvironment, true)) {
              continue outer;
            }
          }
        }
        if (!pCompleteEnvironment.isEmpty() && formula2Part.accept(CONTAINS_ONLY_ENV_INFO_VISITOR)) {
          tmpEnvironment2.clear();
          Set<String> varNames = formula2Part.accept(COLLECT_VARS_VISITOR);
          if (!pCompleteEnvironment.keySet().containsAll(varNames)) {
            return false;
          }
          PushAssumptionToEnvironmentVisitor patev = new PushAssumptionToEnvironmentVisitor(FORMULA_EVALUATION_VISITOR, tmpEnvironment2);
          formula2Part.accept(patev, CompoundInterval.logicalTrue());
          for (String varName : varNames) {
            InvariantsFormula<CompoundInterval> leftFormula = pCompleteEnvironment.get(varName);
            InvariantsFormula<CompoundInterval> rightFormula = tmpEnvironment2.get(varName);
            CompoundInterval leftValue = leftFormula == null ? CompoundInterval.top() : leftFormula.accept(cachingEvaluationVisitor);
            CompoundInterval rightValue = rightFormula == null ? CompoundInterval.top() : rightFormula.accept(FORMULA_EVALUATION_VISITOR, tmpEnvironment2);
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

  public static boolean definitelyImplies(InvariantsFormula<CompoundInterval> pFormula1, InvariantsFormula<CompoundInterval> pFormula2) {
    if (isDefinitelyFalse(pFormula1) || pFormula1.equals(pFormula2)) {
      return true;
    }
    if (pFormula1 instanceof Equal<?> && pFormula2 instanceof Equal<?>) {
      Equal<CompoundInterval> p1 = (Equal<CompoundInterval>) pFormula1;
      Equal<CompoundInterval> p2 = (Equal<CompoundInterval>) pFormula2;
      Variable<CompoundInterval> var = null;
      CompoundInterval value = null;
      if (p1.getOperand1() instanceof Variable<?> && p1.getOperand2() instanceof Constant<?>) {
        var = (Variable<CompoundInterval>) p1.getOperand1();
        value = ((Constant<CompoundInterval>)p1.getOperand2()).getValue();
      } else  if (p1.getOperand2() instanceof Variable<?> && p1.getOperand1() instanceof Constant<?>) {
        var = (Variable<CompoundInterval>) p1.getOperand2();
        value = ((Constant<CompoundInterval>)p1.getOperand1()).getValue();
      }
      if (var != null && value != null) {
        CompoundInterval newValue = null;
        if (var.equals(p2.getOperand1()) && p2.getOperand2() instanceof Constant<?>) {
          newValue = (((Constant<CompoundInterval>) p2.getOperand2()).getValue());
        } else if (var.equals(p2.getOperand2()) && p2.getOperand1() instanceof Constant<?>) {
          newValue = (((Constant<CompoundInterval>) p2.getOperand1()).getValue());
        }
        if (newValue != null) {
          return newValue.contains(value);
        }
      }
    }

    Collection<InvariantsFormula<CompoundInterval>> leftFormulas = pFormula1.accept(SPLIT_CONJUNCTIONS_VISITOR);

    return definitelyImplies(leftFormulas, pFormula2, false, new HashMap<String, InvariantsFormula<CompoundInterval>>(), false);
  }

  /**
   * Gets the sum of the given formulae as a formula.
   *
   * @param pSummand1 the first summand.
   * @param pSummand2 the second summand.
   *
   * @return the sum of the given formulae.
   */
  public InvariantsFormula<CompoundInterval> add(InvariantsFormula<CompoundInterval> pSummand1, InvariantsFormula<CompoundInterval> pSummand2) {
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
  public InvariantsFormula<CompoundInterval> binaryAnd(InvariantsFormula<CompoundInterval> pOperand1, InvariantsFormula<CompoundInterval> pOperand2) {
    if (isDefinitelyBottom(pOperand1) || isDefinitelyBottom(pOperand2)) {
      return BOTTOM;
    }
    if (isDefinitelyTop(pOperand1) || isDefinitelyTop(pOperand2)) {
      return TOP;
    }
    // Eliminate duplicate operands
    Set<InvariantsFormula<CompoundInterval>> uniqueOperands = new HashSet<>();
    Queue<InvariantsFormula<CompoundInterval>> unprocessedOperands = new ArrayDeque<>();
    unprocessedOperands.offer(pOperand1);
    unprocessedOperands.offer(pOperand2);
    while (!unprocessedOperands.isEmpty()) {
      InvariantsFormula<CompoundInterval> unprocessedOperand = unprocessedOperands.poll();
      if (unprocessedOperand instanceof BinaryAnd<?>) {
        BinaryAnd<CompoundInterval> and = (BinaryAnd<CompoundInterval>) unprocessedOperand;
        unprocessedOperands.offer(and.getOperand1());
        unprocessedOperands.offer(and.getOperand2());
      } else {
        uniqueOperands.add(unprocessedOperand);
      }
    }
    assert !uniqueOperands.isEmpty();
    Iterator<InvariantsFormula<CompoundInterval>> operandsIterator = uniqueOperands.iterator();
    InvariantsFormula<CompoundInterval> result = operandsIterator.next();
    while (operandsIterator.hasNext()) {
      result = InvariantsFormulaManager.INSTANCE.binaryOr(result, operandsIterator.next());
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
  public InvariantsFormula<CompoundInterval> binaryNot(InvariantsFormula<CompoundInterval> pToFlip) {
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
  public InvariantsFormula<CompoundInterval> binaryOr(InvariantsFormula<CompoundInterval> pOperand1, InvariantsFormula<CompoundInterval> pOperand2) {
    if (isDefinitelyBottom(pOperand1) || isDefinitelyBottom(pOperand2)) {
      return BOTTOM;
    }
    if (isDefinitelyTop(pOperand1) || isDefinitelyTop(pOperand2)) {
      return TOP;
    }
    // Eliminate duplicate operands
    Set<InvariantsFormula<CompoundInterval>> uniqueOperands = new HashSet<>();
    Queue<InvariantsFormula<CompoundInterval>> unprocessedOperands = new ArrayDeque<>();
    unprocessedOperands.offer(pOperand1);
    unprocessedOperands.offer(pOperand2);
    while (!unprocessedOperands.isEmpty()) {
      InvariantsFormula<CompoundInterval> unprocessedOperand = unprocessedOperands.poll();
      if (unprocessedOperand instanceof BinaryOr<?>) {
        BinaryOr<CompoundInterval> or = (BinaryOr<CompoundInterval>) unprocessedOperand;
        unprocessedOperands.offer(or.getOperand1());
        unprocessedOperands.offer(or.getOperand2());
      } else {
        uniqueOperands.add(unprocessedOperand);
      }
    }
    assert !uniqueOperands.isEmpty();
    Iterator<InvariantsFormula<CompoundInterval>> operandsIterator = uniqueOperands.iterator();
    InvariantsFormula<CompoundInterval> result = operandsIterator.next();
    while (operandsIterator.hasNext()) {
      result = InvariantsFormulaManager.INSTANCE.binaryOr(result, operandsIterator.next());
    }
    return result;
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
  public InvariantsFormula<CompoundInterval> binaryXor(InvariantsFormula<CompoundInterval> pOperand1, InvariantsFormula<CompoundInterval> pOperand2) {
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
  public InvariantsFormula<CompoundInterval> asConstant(CompoundInterval pValue) {
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
  public InvariantsFormula<CompoundInterval> divide(InvariantsFormula<CompoundInterval> pNumerator, InvariantsFormula<CompoundInterval> pDenominator) {
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
  public InvariantsFormula<CompoundInterval> equal(InvariantsFormula<CompoundInterval> pOperand1, InvariantsFormula<CompoundInterval> pOperand2) {
    if (isDefinitelyBottom(pOperand1) || isDefinitelyBottom(pOperand2)) {
      return BOTTOM;
    }
    if (isDefinitelyTop(pOperand1) || isDefinitelyTop(pOperand2)) {
      return TOP;
    }
    if (pOperand1 instanceof Union<?>) {
      Union<CompoundInterval> union = (Union<CompoundInterval>) pOperand1;
      return logicalOr(equal(union.getOperand1(), pOperand2), equal(union.getOperand2(), pOperand2));
    }
    if (pOperand2 instanceof Union<?>) {
      Union<CompoundInterval> union = (Union<CompoundInterval>) pOperand2;
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
  public InvariantsFormula<CompoundInterval> greaterThan(InvariantsFormula<CompoundInterval> pOperand1, InvariantsFormula<CompoundInterval> pOperand2) {
    if (isDefinitelyBottom(pOperand1) || isDefinitelyBottom(pOperand2)) {
      return BOTTOM;
    }
    if (isDefinitelyTop(pOperand1) || isDefinitelyTop(pOperand2)) {
      return TOP;
    }
    if (pOperand1 instanceof Union<?>) {
      Union<CompoundInterval> union = (Union<CompoundInterval>) pOperand1;
      return logicalOr(greaterThan(union.getOperand1(), pOperand2), greaterThan(union.getOperand2(), pOperand2));
    }
    if (pOperand2 instanceof Union<?>) {
      Union<CompoundInterval> union = (Union<CompoundInterval>) pOperand2;
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
  public InvariantsFormula<CompoundInterval> greaterThanOrEqual(InvariantsFormula<CompoundInterval> pOperand1, InvariantsFormula<CompoundInterval> pOperand2) {
    if (isDefinitelyBottom(pOperand1) || isDefinitelyBottom(pOperand2)) {
      return BOTTOM;
    }
    if (isDefinitelyTop(pOperand1) || isDefinitelyTop(pOperand2)) {
      return TOP;
    }
    if (pOperand1 instanceof Union<?>) {
      Union<CompoundInterval> union = (Union<CompoundInterval>) pOperand1;
      return logicalOr(greaterThanOrEqual(union.getOperand1(), pOperand2), greaterThanOrEqual(union.getOperand2(), pOperand2));
    }
    if (pOperand2 instanceof Union<?>) {
      Union<CompoundInterval> union = (Union<CompoundInterval>) pOperand2;
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
  public InvariantsFormula<CompoundInterval> lessThan(InvariantsFormula<CompoundInterval> pOperand1, InvariantsFormula<CompoundInterval> pOperand2) {
    if (isDefinitelyBottom(pOperand1) || isDefinitelyBottom(pOperand2)) {
      return BOTTOM;
    }
    if (isDefinitelyTop(pOperand1) || isDefinitelyTop(pOperand2)) {
      return TOP;
    }
    if (pOperand1 instanceof Union<?>) {
      Union<CompoundInterval> union = (Union<CompoundInterval>) pOperand1;
      return logicalOr(lessThan(union.getOperand1(), pOperand2), lessThan(union.getOperand2(), pOperand2));
    }
    if (pOperand2 instanceof Union<?>) {
      Union<CompoundInterval> union = (Union<CompoundInterval>) pOperand2;
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
  public InvariantsFormula<CompoundInterval> lessThanOrEqual(InvariantsFormula<CompoundInterval> pOperand1, InvariantsFormula<CompoundInterval> pOperand2) {
    if (isDefinitelyBottom(pOperand1) || isDefinitelyBottom(pOperand2)) {
      return BOTTOM;
    }
    if (isDefinitelyTop(pOperand1) || isDefinitelyTop(pOperand2)) {
      return TOP;
    }
    if (pOperand1 instanceof Union<?>) {
      Union<CompoundInterval> union = (Union<CompoundInterval>) pOperand1;
      return logicalOr(lessThanOrEqual(union.getOperand1(), pOperand2), lessThanOrEqual(union.getOperand2(), pOperand2));
    }
    if (pOperand2 instanceof Union<?>) {
      Union<CompoundInterval> union = (Union<CompoundInterval>) pOperand2;
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
  public InvariantsFormula<CompoundInterval> logicalAnd(InvariantsFormula<CompoundInterval> pOperand1, InvariantsFormula<CompoundInterval> pOperand2) {
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
    Map<String, InvariantsFormula<CompoundInterval>> tmpEnvironment = new HashMap<>();
    PushAssumptionToEnvironmentVisitor patev = new PushAssumptionToEnvironmentVisitor(FORMULA_EVALUATION_VISITOR, tmpEnvironment);
    if (!pOperand1.accept(patev, CompoundInterval.logicalTrue())) {
      return FALSE;
    }
    if (!pOperand2.accept(patev, CompoundInterval.logicalTrue())) {
      return FALSE;
    }
    if (definitelyImplies(pOperand1, pOperand2)) {
      return pOperand1;
    }
    if (definitelyImplies(pOperand2, pOperand1)) {
      return pOperand2;
    }
    if (pOperand1 instanceof Equal<?> && pOperand2 instanceof Equal<?>) {
      Equal<CompoundInterval> p1 = (Equal<CompoundInterval>) pOperand1;
      Equal<CompoundInterval> p2 = (Equal<CompoundInterval>) pOperand2;
      Variable<CompoundInterval> var = null;
      CompoundInterval value = null;
      if (p1.getOperand1() instanceof Variable<?> && p1.getOperand2() instanceof Constant<?>) {
        var = (Variable<CompoundInterval>) p1.getOperand1();
        value = ((Constant<CompoundInterval>)p1.getOperand2()).getValue();
      } else  if (p1.getOperand2() instanceof Variable<?> && p1.getOperand1() instanceof Constant<?>) {
        var = (Variable<CompoundInterval>) p1.getOperand2();
        value = ((Constant<CompoundInterval>)p1.getOperand1()).getValue();
      }
      if (var != null && value != null) {
        CompoundInterval newValue = null;
        if (var.equals(p2.getOperand1()) && p2.getOperand2() instanceof Constant<?>) {
          newValue = value.intersectWith(((Constant<CompoundInterval>) p2.getOperand2()).getValue());
        } else if (var.equals(p2.getOperand2()) && p2.getOperand1() instanceof Constant<?>) {
          newValue = value.intersectWith(((Constant<CompoundInterval>) p2.getOperand1()).getValue());
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
  public InvariantsFormula<CompoundInterval> logicalNot(InvariantsFormula<CompoundInterval> pToNegate) {
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
      return ((LogicalNot<CompoundInterval>) pToNegate).getNegated();
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
  public InvariantsFormula<CompoundInterval> logicalOr(InvariantsFormula<CompoundInterval> pOperand1, InvariantsFormula<CompoundInterval> pOperand2) {
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
    if (pOperand1 instanceof Equal<?> && pOperand2 instanceof Equal<?>) {
      Equal<CompoundInterval> p1 = (Equal<CompoundInterval>) pOperand1;
      Equal<CompoundInterval> p2 = (Equal<CompoundInterval>) pOperand2;
      Variable<CompoundInterval> var = null;
      InvariantsFormula<CompoundInterval> value = null;
      if (p1.getOperand1() instanceof Variable<?>) {
        var = (Variable<CompoundInterval>) p1.getOperand1();
        value = p1.getOperand2();
      } else  if (p1.getOperand2() instanceof Variable<?>) {
        var = (Variable<CompoundInterval>) p1.getOperand2();
        value = p1.getOperand1();
      }
      if (var != null && value != null) {
        InvariantsFormula<CompoundInterval> newValue = null;
        InvariantsFormula<CompoundInterval> otherValue = null;
        if (var.equals(p2.getOperand1())) {
          otherValue = p2.getOperand2();
        } else if (var.equals(p2.getOperand2())) {
          otherValue = p2.getOperand1();
        }
        if (otherValue != null) {
          newValue = CompoundIntervalFormulaManager.INSTANCE.union(value, p2.getOperand2());
          newValue = newValue.accept(new PartialEvaluator(), FORMULA_EVALUATION_VISITOR);
          CompoundInterval val = evaluate(newValue);
          if (val.isTop() && newValue instanceof Constant<?>) {
            return TRUE;
          }
          if (val.isBottom()) {
            return FALSE;
          }
          boolean useNewValue = true;
          if (newValue instanceof Union<?>) {
            Union<CompoundInterval> union = (Union<CompoundInterval>) newValue;
            InvariantsFormula<CompoundInterval> op1 = union.getOperand1();
            InvariantsFormula<CompoundInterval> op2 = union.getOperand2();
            useNewValue = !(op1.equals(value) && op2.equals(otherValue) || op1.equals(otherValue) && op2.equals(value));
          }
          if (useNewValue) {
            return equal(var, newValue);
          }
        }
      }
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
  public InvariantsFormula<CompoundInterval> logicalImplies(InvariantsFormula<CompoundInterval> pOperand1, InvariantsFormula<CompoundInterval> pOperand2) {
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
  public InvariantsFormula<CompoundInterval> modulo(InvariantsFormula<CompoundInterval> pNumerator, InvariantsFormula<CompoundInterval> pDenominator) {
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
  public InvariantsFormula<CompoundInterval> multiply(InvariantsFormula<CompoundInterval> pFactor1,
      InvariantsFormula<CompoundInterval> pFactor2) {
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
  public InvariantsFormula<CompoundInterval> negate(InvariantsFormula<CompoundInterval> pToNegate) {
    if (isDefinitelyBottom(pToNegate)) {
      return BOTTOM;
    }
    if (isDefinitelyTop(pToNegate)) {
      return TOP;
    }
    if (pToNegate instanceof Multiply<?>) {
      InvariantsFormula<CompoundInterval> factor1 = ((Multiply<CompoundInterval>) pToNegate).getFactor1();
      InvariantsFormula<CompoundInterval> factor2 = ((Multiply<CompoundInterval>) pToNegate).getFactor2();
      if (factor1.equals(MINUS_ONE)) {
        return factor2;
      }
      if (factor2.equals(MINUS_ONE)) {
        return factor1;
      }
    }
    return InvariantsFormulaManager.INSTANCE.multiply(pToNegate, MINUS_ONE);
  }

  /**
   * Gets the difference of the given invariants formulae as a formula.
   *
   * @param pMinuend the minuend.
   * @param pSubtrahend the subtrahend.
   *
   * @return the sum of the given formulae.
   */
  public InvariantsFormula<CompoundInterval> subtract(InvariantsFormula<CompoundInterval> pMinuend,
      InvariantsFormula<CompoundInterval> pSubtrahend) {
    if (isDefinitelyBottom(pMinuend) || isDefinitelyBottom(pSubtrahend)) {
      return BOTTOM;
    }
    if (isDefinitelyTop(pMinuend) || isDefinitelyTop(pSubtrahend)) {
      return TOP;
    }
    return InvariantsFormulaManager.INSTANCE.add(pMinuend, negate(pSubtrahend));
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
  public InvariantsFormula<CompoundInterval> shiftLeft(InvariantsFormula<CompoundInterval> pToShift,
      InvariantsFormula<CompoundInterval> pShiftDistance) {
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
  public InvariantsFormula<CompoundInterval> shiftRight(InvariantsFormula<CompoundInterval> pToShift,
      InvariantsFormula<CompoundInterval> pShiftDistance) {
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
  public InvariantsFormula<CompoundInterval> union(InvariantsFormula<CompoundInterval> pOperand1,
      InvariantsFormula<CompoundInterval> pOperand2) {
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
    // Try reducing nested unions by temporarily representing them as a set
    Set<InvariantsFormula<CompoundInterval>> atomicUnionParts = new HashSet<>();
    Queue<InvariantsFormula<CompoundInterval>> unionParts = new ArrayDeque<>();
    CompoundInterval constantPart = CompoundInterval.bottom();
    unionParts.offer(pOperand1);
    unionParts.offer(pOperand2);
    while (!unionParts.isEmpty()) {
      InvariantsFormula<CompoundInterval> currentPart = unionParts.poll();
      if (currentPart instanceof Union<?>) {
        Union<CompoundInterval> currentUnion = (Union<CompoundInterval>) currentPart;
        unionParts.add(currentUnion.getOperand1());
        unionParts.add(currentUnion.getOperand2());
      } else if (currentPart instanceof Constant<?>) {
        constantPart = constantPart.unionWith(((Constant<CompoundInterval>) currentPart).getValue());
      } else {
        atomicUnionParts.add(currentPart);
      }
    }
    return unionAll(constantPart, atomicUnionParts);
  }

  private InvariantsFormula<CompoundInterval> unionAll(CompoundInterval pConstantPart, Collection<InvariantsFormula<CompoundInterval>> pFormulas) {
    if (pFormulas.isEmpty() || pConstantPart.isTop()) {
      return asConstant(pConstantPart);
    }
    InvariantsFormula<CompoundInterval> result = null;
    Iterator<InvariantsFormula<CompoundInterval>> atomicUnionPartsIterator = pFormulas.iterator();
    result = atomicUnionPartsIterator.next();
    while (atomicUnionPartsIterator.hasNext()) {
      result = InvariantsFormulaManager.INSTANCE.union(result, atomicUnionPartsIterator.next());
    }
    if (!pConstantPart.isBottom()) {
      InvariantsFormula<CompoundInterval> constantPartFormula = asConstant(pConstantPart);
      result = InvariantsFormulaManager.INSTANCE.union(result, constantPartFormula);
    }
    return result;
  }

  /**
   * Gets an invariants formula representing the variable with the given name.
   *
   * @param pName the name of the variable.
   *
   * @return an invariants formula representing the variable with the given name.
   */
  public Variable<CompoundInterval> asVariable(String pName) {
    return InvariantsFormulaManager.INSTANCE.asVariable(pName);
  }

}
