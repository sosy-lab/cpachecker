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

import org.sosy_lab.cpachecker.cpa.invariants.BitVectorInfo;
import org.sosy_lab.cpachecker.cpa.invariants.BitVectorType;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundIntervalManager;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundIntervalManagerFactory;
import org.sosy_lab.cpachecker.cpa.invariants.NonRecursiveEnvironment;
import org.sosy_lab.cpachecker.cpa.invariants.NonRecursiveEnvironment.Builder;

import com.google.common.collect.FluentIterable;


public class CompoundIntervalFormulaManager {

  private static final Map<String, NumeralFormula<CompoundInterval>> EMPTY_ENVIRONMENT = Collections.emptyMap();

  private static final CollectVarsVisitor<CompoundInterval> COLLECT_VARS_VISITOR = new CollectVarsVisitor<>();

  private static final ContainsOnlyEnvInfoVisitor<CompoundInterval> CONTAINS_ONLY_ENV_INFO_VISITOR = new ContainsOnlyEnvInfoVisitor<>();

  private static final SplitConjunctionsVisitor<CompoundInterval> SPLIT_CONJUNCTIONS_VISITOR = new SplitConjunctionsVisitor<>();

  private static final SplitDisjunctionsVisitor<CompoundInterval> SPLIT_DISJUNCTIONS_VISITOR = new SplitDisjunctionsVisitor<>();

  private final CompoundIntervalManagerFactory compoundIntervalManagerFactory;

  private final FormulaEvaluationVisitor<CompoundInterval> evaluationVisitor;

  private final PartialEvaluator partialEvaluator;

  public CompoundIntervalFormulaManager(CompoundIntervalManagerFactory pCompoundIntervalManagerFactory) {
    this.compoundIntervalManagerFactory = pCompoundIntervalManagerFactory;
    this.evaluationVisitor = new FormulaCompoundStateEvaluationVisitor(pCompoundIntervalManagerFactory);
    this.partialEvaluator = new PartialEvaluator(compoundIntervalManagerFactory, this);
  }

  public static Set<String> collectVariableNames(NumeralFormula<CompoundInterval> pFormula) {
    return pFormula.accept(COLLECT_VARS_VISITOR);
  }

  public static Set<String> collectVariableNames(BooleanFormula<CompoundInterval> pFormula) {
    return pFormula.accept(COLLECT_VARS_VISITOR);
  }

  public CompoundInterval evaluate(NumeralFormula<CompoundInterval> pFormula) {
    return pFormula.accept(evaluationVisitor, EMPTY_ENVIRONMENT);
  }

  public boolean isDefinitelyTrue(NumeralFormula<CompoundInterval> pFormula) {
    if (evaluate(pFormula).isDefinitelyTrue()) {
      return true;
    }
    return isDefinitelyTrue(fromNumeral(pFormula));
  }

  private boolean isDefinitelyTrue(BooleanFormula<CompoundInterval> pFormula) {
    return BooleanConstant.isTrue(pFormula)
        || definitelyImplies(Collections.<BooleanFormula<CompoundInterval>>singleton(BooleanConstant.<CompoundInterval>getTrue()), pFormula);
  }

  public boolean isDefinitelyFalse(NumeralFormula<CompoundInterval> pFormula) {
    return evaluate(pFormula).isDefinitelyFalse();
  }

  private boolean isDefinitelyFalse(BooleanFormula<CompoundInterval> pFormula) {
    return BooleanConstant.isFalse(pFormula)
        || isDefinitelyFalse(fromBoolean(BitVectorInfo.from(1, false), pFormula));
  }

  public boolean isDefinitelyBottom(NumeralFormula<CompoundInterval> pFormula) {
    return evaluate(pFormula).isBottom();
  }

  public boolean containsAllPossibleValues(NumeralFormula<CompoundInterval> pFormula) {
    return (pFormula instanceof Constant<?>)
        && ((Constant<CompoundInterval>) pFormula).getValue().containsAllPossibleValues();
  }

  public boolean definitelyImplies(Iterable<BooleanFormula<CompoundInterval>> pFormulas, BooleanFormula<CompoundInterval> pFormula) {
    return definitelyImplies(pFormulas, pFormula, new HashMap<String, NumeralFormula<CompoundInterval>>());
  }

  public boolean definitelyImplies(Iterable<BooleanFormula<CompoundInterval>> pFormulas, BooleanFormula<CompoundInterval> pFormula, Map<String, NumeralFormula<CompoundInterval>> pBaseEnvironment) {
    Map<String, NumeralFormula<CompoundInterval>> newMap = new HashMap<>(pBaseEnvironment);
    if (pFormula instanceof Collection<?>) {
      return definitelyImplies((Collection<BooleanFormula<CompoundInterval>>) pFormulas, pFormula, true, newMap, false);
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
  private boolean definitelyImplies(Collection<BooleanFormula<CompoundInterval>> pInformationBaseFormulas, BooleanFormula<CompoundInterval> pFormula, boolean pExtend, Map<String, NumeralFormula<CompoundInterval>> pInformationBaseEnvironment, boolean pEnvironmentComplete) {
    final Collection<BooleanFormula<CompoundInterval>> formulas;
    if (pExtend) {
      formulas = new HashSet<>();
      for (BooleanFormula<CompoundInterval> formula : pInformationBaseFormulas) {
        for (BooleanFormula<CompoundInterval> f : formula.accept(SPLIT_CONJUNCTIONS_VISITOR)) {
          formulas.add(f.accept(partialEvaluator, evaluationVisitor));
        }
      }
    } else {
      formulas = pInformationBaseFormulas;
    }

    // If any of the conjunctive parts is a disjunction, check try each disjunctive part
    for (BooleanFormula<CompoundInterval> formula : formulas) {
      Collection<BooleanFormula<CompoundInterval>> disjunctions = formula.accept(SPLIT_DISJUNCTIONS_VISITOR);
      if (disjunctions.size() > 1) {
        ArrayList<BooleanFormula<CompoundInterval>> newFormulas = new ArrayList<>(formulas);
        Map<String, NumeralFormula<CompoundInterval>> newBaseEnvironment = new HashMap<>(pInformationBaseEnvironment);
        newFormulas.remove(formula);
        for (BooleanFormula<CompoundInterval> disjunctivePart : disjunctions) {
          Collection<BooleanFormula<CompoundInterval>> conjunctivePartsOfDisjunctivePart = disjunctivePart.accept(SPLIT_CONJUNCTIONS_VISITOR);
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
    NonRecursiveEnvironment.Builder tmpEnvironment = NonRecursiveEnvironment.Builder.of(compoundIntervalManagerFactory, pInformationBaseEnvironment);
    PushAssumptionToEnvironmentVisitor patev = new PushAssumptionToEnvironmentVisitor(compoundIntervalManagerFactory, evaluationVisitor, tmpEnvironment);
    if (!pEnvironmentComplete) {
      for (BooleanFormula<CompoundInterval> leftFormula : formulas) {
        if (!leftFormula.accept(patev, BooleanConstant.<CompoundInterval>getTrue())) {
          return false;
        }
      }
    }

    return definitelyImplies(formulas, tmpEnvironment, pFormula);
  }

  public boolean definitelyImplies(final Map<String, NumeralFormula<CompoundInterval>> pCompleteEnvironment,
      final BooleanFormula<CompoundInterval> pFormula) {
    return definitelyImplies(Collections.<BooleanFormula<CompoundInterval>>emptyList(), pCompleteEnvironment, pFormula);
  }

  private boolean definitelyImplies(final Collection<BooleanFormula<CompoundInterval>> pExtendedFormulas,
      final Map<String, NumeralFormula<CompoundInterval>> pCompleteEnvironment,
      final BooleanFormula<CompoundInterval> pFormula) {

    /*
     * Imagine this function is sound, which it should be.
     *
     * x implies y, and y implies z, that means that x implies z
     *
     * [1] implies [1,2] implies [1,2,3]
     *
     * Now, if the right formula is overapproximated, this function is still sound:
     * If x is implied, anything that is implied by x is also implied.
     * For the same transitive reason, underapproximating the environment / left side would also be sound.
     *
     * What does "Equal"/"==" mean with intervals?
     * [1] == [1,10] means "[1] is element of [1,10]"
     * x == y means "some SPECIFIC value that is element of the set of possible values of x
     *   is equal to some SPECIFIC element of the possible values of y",
     * however, if we do not know the specific value meant,
     * then we cannot decide about the truth.
     * x != y means "some SPECIFIC value that is element of the set of possible values of x
     *   is not equal to some OTHER SPECIFIC value that is element of the set of possible values of y,
     * so we can only decide the truth if we know that the sets do not overlap or are both singletons.
     */

    PartialEvaluator variableResolver = new PartialEvaluator(compoundIntervalManagerFactory, pCompleteEnvironment);
    Builder impliedEnvironment = NonRecursiveEnvironment.Builder.of(compoundIntervalManagerFactory, EMPTY_ENVIRONMENT);

    outer:
    for (BooleanFormula<CompoundInterval> f : pFormula.accept(SPLIT_CONJUNCTIONS_VISITOR)) {
      BooleanFormula<CompoundInterval> formulaAtom = f.accept(partialEvaluator, evaluationVisitor);
      if (!pExtendedFormulas.contains(formulaAtom)) {
        Collection<BooleanFormula<CompoundInterval>> disjunctions = formulaAtom.accept(SPLIT_DISJUNCTIONS_VISITOR);
        if (disjunctions.size() > 1) {
          for (BooleanFormula<CompoundInterval> disjunctionPart : disjunctions) {
            if (definitelyImplies(pExtendedFormulas, disjunctionPart, false, pCompleteEnvironment, true)) {
              continue outer;
            }
          }
        }

        BooleanConstant<CompoundInterval> evaluated = pFormula.accept(evaluationVisitor, pCompleteEnvironment);
        if (BooleanConstant.isTrue(evaluated)) {
          continue;
        }
        if (BooleanConstant.isFalse(evaluated)) {
          return false;
        }

        BooleanFormula<CompoundInterval> resolved = formulaAtom.accept(variableResolver, evaluationVisitor);

        StateEqualsVisitor stateEqualsVisitor = new StateEqualsVisitor(evaluationVisitor, pCompleteEnvironment, compoundIntervalManagerFactory);

        if (resolved instanceof Equal) {
          Equal<CompoundInterval> equation = (Equal<CompoundInterval>) resolved;
          NumeralFormula<CompoundInterval> op1 = equation.getOperand1();
          NumeralFormula<CompoundInterval> op2 = equation.getOperand2();
          if (op1.accept(stateEqualsVisitor, op2)) {
            continue;
          }
          CompoundIntervalManager cim = compoundIntervalManagerFactory.createCompoundIntervalManager(op1.getBitVectorInfo());
          CompoundInterval leftEval = op1.accept(evaluationVisitor, pCompleteEnvironment);
          CompoundInterval rightEval = op2.accept(evaluationVisitor, pCompleteEnvironment);
          if (!cim.doIntersect(leftEval, rightEval)) {
            return false;
          }
          if (op1 instanceof Constant && rightEval.isSingleton() && cim.contains(leftEval, rightEval)
              || op2 instanceof Constant && leftEval.isSingleton() && cim.contains(rightEval, leftEval)) {
            continue;
          }
        } else if (resolved instanceof LogicalNot) {
          LogicalNot<CompoundInterval> negation = (LogicalNot<CompoundInterval>) resolved;
          BooleanFormula<CompoundInterval> negated = negation.getNegated();
          if (negated instanceof Equal) {
            Equal<CompoundInterval> equation = (Equal<CompoundInterval>) negated;
            NumeralFormula<CompoundInterval> op1 = equation.getOperand1();
            NumeralFormula<CompoundInterval> op2 = equation.getOperand2();
            if (op1.accept(stateEqualsVisitor, op2)) {
              return false;
            }
            CompoundIntervalManager cim = compoundIntervalManagerFactory.createCompoundIntervalManager(op1.getBitVectorInfo());
            CompoundInterval leftEval = op1.accept(evaluationVisitor, pCompleteEnvironment);
            CompoundInterval rightEval = op2.accept(evaluationVisitor, pCompleteEnvironment);
            if (!cim.doIntersect(leftEval, rightEval)) {
              continue;
            }
            return false;
          }
        }

        if (!pCompleteEnvironment.isEmpty() && formulaAtom.accept(CONTAINS_ONLY_ENV_INFO_VISITOR)) {
          impliedEnvironment.clear();
          Set<String> varNames = formulaAtom.accept(COLLECT_VARS_VISITOR);
          if (!pCompleteEnvironment.keySet().containsAll(varNames)) {
            return false;
          }
          PushAssumptionToEnvironmentVisitor patev = new PushAssumptionToEnvironmentVisitor(compoundIntervalManagerFactory, evaluationVisitor, impliedEnvironment);
          formulaAtom.accept(patev, BooleanConstant.<CompoundInterval>getTrue());
          for (String varName : varNames) {
            NumeralFormula<CompoundInterval> leftFormula = pCompleteEnvironment.get(varName);
            NumeralFormula<CompoundInterval> rightFormula = impliedEnvironment.get(varName);
            // If the right formula is null, we learned nothing about the variable from pushing it,
            // so continuing would be unsound.
            // If the left formula is null, it cannot imply any information about the right variable.
            if (rightFormula == null || leftFormula == null) {
              return false;
            }
            if (rightFormula.equals(leftFormula)) {
              continue;
            }
            rightFormula = rightFormula.accept(partialEvaluator, evaluationVisitor);
            leftFormula = leftFormula.accept(partialEvaluator, evaluationVisitor);
            if (rightFormula.equals(leftFormula)) {
              continue;
            }
            if (rightFormula instanceof Constant) {
              BitVectorInfo bitVectorInfo = rightFormula.getBitVectorInfo();
              CompoundIntervalManager cim = compoundIntervalManagerFactory.createCompoundIntervalManager(bitVectorInfo);
              CompoundInterval leftValue = leftFormula == null ? cim.allPossibleValues() : leftFormula.accept(evaluationVisitor, pCompleteEnvironment);
              CompoundInterval rightValue = rightFormula.accept(evaluationVisitor, impliedEnvironment);
              if (cim.contains(rightValue, leftValue)) {
                continue;
              }
            }
            return false;
          }
          continue;
        }
        return false;
      }
    }
    return true;
  }

  public boolean definitelyImplies(BooleanFormula<CompoundInterval> pFormula1, BooleanFormula<CompoundInterval> pFormula2) {
    if (BooleanConstant.isFalse(pFormula1) || pFormula1.equals(pFormula2)) {
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
          BitVectorInfo bitVectorInfo = p2.getOperand1().getBitVectorInfo();
          CompoundIntervalManager cim = compoundIntervalManagerFactory.createCompoundIntervalManager(bitVectorInfo);
          return cim.contains(newValue, value);
        }
      }
    }

    Collection<BooleanFormula<CompoundInterval>> leftFormulas = pFormula1.accept(SPLIT_CONJUNCTIONS_VISITOR);

    return definitelyImplies(leftFormulas, pFormula2, false, new HashMap<String, NumeralFormula<CompoundInterval>>(), false);
  }

  /**
   * Gets the sum of the given formulae as a formula.
   *
   * @param pSummand1 the first summand.
   * @param pSummand2 the second summand.
   *
   * @return the sum of the given formulae.
   */
  public NumeralFormula<CompoundInterval> add(NumeralFormula<CompoundInterval> pSummand1, NumeralFormula<CompoundInterval> pSummand2) {
    if (isDefinitelyBottom(pSummand1)) {
      return bottom(pSummand1);
    }
    if (isDefinitelyBottom(pSummand2)) {
      return bottom(pSummand2);
    }
    if (containsAllPossibleValues(pSummand1)) {
      return allPossibleValues(pSummand1);
    }
    if (containsAllPossibleValues(pSummand2)) {
      return allPossibleValues(pSummand2);
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
  public NumeralFormula<CompoundInterval> binaryAnd(NumeralFormula<CompoundInterval> pOperand1, NumeralFormula<CompoundInterval> pOperand2) {
    if (isDefinitelyBottom(pOperand1)) {
      return bottom(pOperand1);
    }
    if (isDefinitelyBottom(pOperand2)) {
      return bottom(pOperand2);
    }
    // Eliminate duplicate operands
    Set<NumeralFormula<CompoundInterval>> uniqueOperands = new HashSet<>();
    Queue<NumeralFormula<CompoundInterval>> unprocessedOperands = new ArrayDeque<>();
    unprocessedOperands.offer(pOperand1);
    unprocessedOperands.offer(pOperand2);
    while (!unprocessedOperands.isEmpty()) {
      NumeralFormula<CompoundInterval> unprocessedOperand = unprocessedOperands.poll();
      if (unprocessedOperand instanceof BinaryAnd<?>) {
        BinaryAnd<CompoundInterval> and = (BinaryAnd<CompoundInterval>) unprocessedOperand;
        unprocessedOperands.offer(and.getOperand1());
        unprocessedOperands.offer(and.getOperand2());
      } else {
        uniqueOperands.add(unprocessedOperand);
      }
    }
    assert !uniqueOperands.isEmpty();
    Iterator<NumeralFormula<CompoundInterval>> operandsIterator = uniqueOperands.iterator();
    NumeralFormula<CompoundInterval> result = operandsIterator.next();
    while (operandsIterator.hasNext()) {
      result = InvariantsFormulaManager.INSTANCE.binaryAnd(result, operandsIterator.next());
    }
    return result;
  }

  /**
   * Gets the binary negation of the given formula.
   *
   * @param pToFlip the operand of the bit flip operation.
   *
   * @return the binary negation of the given formula.
   */
  public NumeralFormula<CompoundInterval> binaryNot(NumeralFormula<CompoundInterval> pToFlip) {
    if (isDefinitelyBottom(pToFlip)) {
      return bottom(pToFlip);
    }
    if (containsAllPossibleValues(pToFlip)) {
      return allPossibleValues(pToFlip);
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
  public NumeralFormula<CompoundInterval> binaryOr(NumeralFormula<CompoundInterval> pOperand1, NumeralFormula<CompoundInterval> pOperand2) {
    if (isDefinitelyBottom(pOperand1)) {
      return bottom(pOperand1);
    }
    if (isDefinitelyBottom(pOperand2)) {
      return bottom(pOperand2);
    }
    // Eliminate duplicate operands
    Set<NumeralFormula<CompoundInterval>> uniqueOperands = new HashSet<>();
    Queue<NumeralFormula<CompoundInterval>> unprocessedOperands = new ArrayDeque<>();
    unprocessedOperands.offer(pOperand1);
    unprocessedOperands.offer(pOperand2);
    while (!unprocessedOperands.isEmpty()) {
      NumeralFormula<CompoundInterval> unprocessedOperand = unprocessedOperands.poll();
      if (unprocessedOperand instanceof BinaryOr<?>) {
        BinaryOr<CompoundInterval> or = (BinaryOr<CompoundInterval>) unprocessedOperand;
        unprocessedOperands.offer(or.getOperand1());
        unprocessedOperands.offer(or.getOperand2());
      } else {
        uniqueOperands.add(unprocessedOperand);
      }
    }
    assert !uniqueOperands.isEmpty();
    Iterator<NumeralFormula<CompoundInterval>> operandsIterator = uniqueOperands.iterator();
    NumeralFormula<CompoundInterval> result = operandsIterator.next();
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
  public NumeralFormula<CompoundInterval> binaryXor(NumeralFormula<CompoundInterval> pOperand1, NumeralFormula<CompoundInterval> pOperand2) {
    if (isDefinitelyBottom(pOperand1)) {
      return bottom(pOperand1);
    }
    if (isDefinitelyBottom(pOperand2)) {
      return bottom(pOperand2);
    }
    return InvariantsFormulaManager.INSTANCE.binaryXor(pOperand1, pOperand2);
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
  public NumeralFormula<CompoundInterval> divide(NumeralFormula<CompoundInterval> pNumerator, NumeralFormula<CompoundInterval> pDenominator) {
    if (isDefinitelyBottom(pNumerator)) {
      return bottom(pNumerator);
    }
    if (isDefinitelyBottom(pDenominator)) {
      return bottom(pDenominator);
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
  public BooleanFormula<CompoundInterval> equal(NumeralFormula<CompoundInterval> pOperand1, NumeralFormula<CompoundInterval> pOperand2) {
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
  public BooleanFormula<CompoundInterval> greaterThan(NumeralFormula<CompoundInterval> pOperand1, NumeralFormula<CompoundInterval> pOperand2) {
    return lessThan(pOperand2, pOperand1);
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
  public BooleanFormula<CompoundInterval> greaterThanOrEqual(NumeralFormula<CompoundInterval> pOperand1, NumeralFormula<CompoundInterval> pOperand2) {
    return lessThanOrEqual(pOperand2, pOperand1);
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
  public BooleanFormula<CompoundInterval> lessThan(NumeralFormula<CompoundInterval> pOperand1, NumeralFormula<CompoundInterval> pOperand2) {
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
  public BooleanFormula<CompoundInterval> lessThanOrEqual(NumeralFormula<CompoundInterval> pOperand1, NumeralFormula<CompoundInterval> pOperand2) {
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
  public BooleanFormula<CompoundInterval> logicalAnd(BooleanFormula<CompoundInterval> pOperand1, BooleanFormula<CompoundInterval> pOperand2) {
    if (BooleanConstant.isFalse(pOperand1) || BooleanConstant.isFalse(pOperand2)) {
      return BooleanConstant.getFalse();
    }
    if (BooleanConstant.isTrue(pOperand1)) {
      return pOperand2;
    }
    if (BooleanConstant.isTrue(pOperand2)) {
      return pOperand1;
    }
    NonRecursiveEnvironment.Builder tmpEnvironment = new NonRecursiveEnvironment.Builder(compoundIntervalManagerFactory);
    PushAssumptionToEnvironmentVisitor patev = new PushAssumptionToEnvironmentVisitor(compoundIntervalManagerFactory, evaluationVisitor, tmpEnvironment);
    if (!pOperand1.accept(patev, BooleanConstant.<CompoundInterval>getTrue())) {
      return BooleanConstant.getFalse();
    }
    if (!pOperand2.accept(patev, BooleanConstant.<CompoundInterval>getTrue())) {
      return BooleanConstant.getFalse();
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
        BitVectorInfo bitVectorInfo = p2.getOperand1().getBitVectorInfo();
        CompoundIntervalManager cim = getCompoundIntervalManager(bitVectorInfo);
        if (var.equals(p2.getOperand1()) && p2.getOperand2() instanceof Constant<?>) {
          newValue = cim.intersect(value, ((Constant<CompoundInterval>) p2.getOperand2()).getValue());
        } else if (var.equals(p2.getOperand2()) && p2.getOperand1() instanceof Constant<?>) {
          newValue = cim.intersect(value, ((Constant<CompoundInterval>) p2.getOperand1()).getValue());
        }
        if (newValue != null) {
          if (newValue.containsAllPossibleValues()) {
            return BooleanConstant.getTrue();
          }
          if (newValue.isBottom()) {
            return BooleanConstant.getFalse();
          }
          return equal(var, asConstant(bitVectorInfo, newValue));
        }
      }
    }
    return InvariantsFormulaManager.INSTANCE.logicalAnd(pOperand1, pOperand2);
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
  public BooleanFormula<CompoundInterval> logicalNot(BooleanFormula<CompoundInterval> pToNegate) {
    if (pToNegate instanceof BooleanConstant) {
      return ((BooleanConstant<CompoundInterval>) pToNegate).negate();
    }
    if (isDefinitelyTrue(pToNegate)) {
      return BooleanConstant.getFalse();
    }
    if (isDefinitelyFalse(pToNegate)) {
      return BooleanConstant.getTrue();
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
  public BooleanFormula<CompoundInterval> logicalOr(BooleanFormula<CompoundInterval> pOperand1, BooleanFormula<CompoundInterval> pOperand2) {
    if (BooleanConstant.isTrue(pOperand1) || BooleanConstant.isTrue(pOperand2)) {
      return BooleanConstant.getTrue();
    }
    if (BooleanConstant.isFalse(pOperand1)) {
      return pOperand2;
    }
    if (BooleanConstant.isFalse(pOperand2)) {
      return pOperand1;
    }
    if (definitelyImplies(pOperand1, pOperand2)) {
      return pOperand2;
    }
    if (definitelyImplies(pOperand2, pOperand1)) {
      return pOperand1;
    }
    if (pOperand1 instanceof Equal && pOperand2 instanceof Equal) {
      Equal<CompoundInterval> p1 = (Equal<CompoundInterval>) pOperand1;
      Equal<CompoundInterval> p2 = (Equal<CompoundInterval>) pOperand2;
      Variable<CompoundInterval> var = null;
      NumeralFormula<CompoundInterval> value = null;
      if (p1.getOperand1() instanceof Variable<?>) {
        var = (Variable<CompoundInterval>) p1.getOperand1();
        value = p1.getOperand2();
      } else  if (p1.getOperand2() instanceof Variable<?>) {
        var = (Variable<CompoundInterval>) p1.getOperand2();
        value = p1.getOperand1();
      }
      if (var != null && value != null) {
        NumeralFormula<CompoundInterval> newValue = null;
        NumeralFormula<CompoundInterval> otherValue = null;
        if (var.equals(p2.getOperand1())) {
          otherValue = p2.getOperand2();
        } else if (var.equals(p2.getOperand2())) {
          otherValue = p2.getOperand1();
        }
        if (otherValue != null) {
          newValue = union(value, p2.getOperand2());
          newValue = newValue.accept(new PartialEvaluator(compoundIntervalManagerFactory), evaluationVisitor);
          CompoundInterval val = evaluate(newValue);
          if (val.containsAllPossibleValues() && newValue instanceof Constant<?>) {
            return BooleanConstant.getTrue();
          }
          if (val.isBottom()) {
            return BooleanConstant.getFalse();
          }
          boolean useNewValue = true;
          if (newValue instanceof Union<?>) {
            Union<CompoundInterval> union = (Union<CompoundInterval>) newValue;
            NumeralFormula<CompoundInterval> op1 = union.getOperand1();
            NumeralFormula<CompoundInterval> op2 = union.getOperand2();
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
  public BooleanFormula<CompoundInterval> logicalImplies(BooleanFormula<CompoundInterval> pOperand1, BooleanFormula<CompoundInterval> pOperand2) {
    if (definitelyImplies(pOperand1, pOperand2)) {
      return BooleanConstant.getTrue();
    }
    return logicalNot(logicalAnd(pOperand1, logicalNot(pOperand2)));
  }

  /**
   * Gets an invariants formula representing the modulo operation over the
   * given operands.
   *
   * @param pNumerator the numerator of the fraction.
   * @param pDenominator the denominator of the fraction.
   *
   * @return an invariants formula representing the modulo operation over the
   * given operands.
   */
  public NumeralFormula<CompoundInterval> modulo(NumeralFormula<CompoundInterval> pNumerator, NumeralFormula<CompoundInterval> pDenominator) {
    if (isDefinitelyBottom(pNumerator)) {
      return bottom(pNumerator);
    }
    if (isDefinitelyBottom(pDenominator)) {
      return bottom(pDenominator);
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
  public NumeralFormula<CompoundInterval> multiply(NumeralFormula<CompoundInterval> pFactor1,
      NumeralFormula<CompoundInterval> pFactor2) {
    if (isDefinitelyBottom(pFactor1)) {
      return bottom(pFactor1);
    }
    if (isDefinitelyBottom(pFactor2)) {
      return bottom(pFactor2);
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
  public NumeralFormula<CompoundInterval> negate(NumeralFormula<CompoundInterval> pToNegate) {
    if (isDefinitelyBottom(pToNegate)) {
      return bottom(pToNegate);
    }
    if (containsAllPossibleValues(pToNegate)) {
      return allPossibleValues(pToNegate);
    }
    if (pToNegate instanceof Multiply<?>) {
      NumeralFormula<CompoundInterval> factor1 = ((Multiply<CompoundInterval>) pToNegate).getFactor1();
      NumeralFormula<CompoundInterval> factor2 = ((Multiply<CompoundInterval>) pToNegate).getFactor2();
      if (isMinusOne(factor1)) {
        return factor2;
      }
      if (isMinusOne(factor2)) {
        return factor1;
      }
    }
    BitVectorInfo bitVectorInfo = pToNegate.getBitVectorInfo();
    CompoundIntervalManager cim = getCompoundIntervalManager(bitVectorInfo);
    if (pToNegate instanceof Constant) {
      return asConstant(bitVectorInfo, cim.negate(((Constant<CompoundInterval>) pToNegate).getValue()));
    }
    if (cim.allPossibleValues().contains(BigInteger.valueOf(-1))) {
      NumeralFormula<CompoundInterval> minusOne = asConstant(bitVectorInfo, cim.singleton(-1));
      return InvariantsFormulaManager.INSTANCE.multiply(pToNegate, minusOne);
    }
    // TODO more precise implementation; maybe reintroduce "Negation" as a formula
    return allPossibleValues(bitVectorInfo);
  }

  private boolean isMinusOne(NumeralFormula<CompoundInterval> pFormula) {
    CompoundInterval value = evaluate(pFormula);
    return value.isSingleton() && value.contains(BigInteger.valueOf(-1));
  }

  /**
   * Gets the difference of the given invariants formulae as a formula.
   *
   * @param pMinuend the minuend.
   * @param pSubtrahend the subtrahend.
   *
   * @return the sum of the given formulae.
   */
  public NumeralFormula<CompoundInterval> subtract(NumeralFormula<CompoundInterval> pMinuend,
      NumeralFormula<CompoundInterval> pSubtrahend) {
    if (isDefinitelyBottom(pMinuend)) {
      return bottom(pMinuend);
    }
    if (isDefinitelyBottom(pSubtrahend)) {
      return bottom(pSubtrahend);
    }
    if (containsAllPossibleValues(pMinuend)) {
      return allPossibleValues(pMinuend);
    }
    if (containsAllPossibleValues(pSubtrahend)) {
      return allPossibleValues(pSubtrahend);
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
  public NumeralFormula<CompoundInterval> shiftLeft(NumeralFormula<CompoundInterval> pToShift,
      NumeralFormula<CompoundInterval> pShiftDistance) {
    if (isDefinitelyBottom(pToShift)) {
      return bottom(pToShift);
    }
    if (isDefinitelyBottom(pShiftDistance)) {
      return bottom(pShiftDistance);
    }
    if (containsAllPossibleValues(pShiftDistance)) {
      return allPossibleValues(pShiftDistance);
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
  public NumeralFormula<CompoundInterval> shiftRight(NumeralFormula<CompoundInterval> pToShift,
      NumeralFormula<CompoundInterval> pShiftDistance) {
    if (isDefinitelyBottom(pToShift)) {
      return bottom(pToShift);
    }
    if (isDefinitelyBottom(pShiftDistance)) {
      return bottom(pShiftDistance);
    }
    if (containsAllPossibleValues(pShiftDistance)) {
      return allPossibleValues(pShiftDistance);
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
  public NumeralFormula<CompoundInterval> union(NumeralFormula<CompoundInterval> pOperand1,
      NumeralFormula<CompoundInterval> pOperand2) {
    if (isDefinitelyBottom(pOperand1)) {
      return pOperand2;
    }
    if (isDefinitelyBottom(pOperand2)) {
      return pOperand1;
    }
    if (containsAllPossibleValues(pOperand1)) {
      return allPossibleValues(pOperand1);
    }
    if (containsAllPossibleValues(pOperand2)) {
      return allPossibleValues(pOperand2);
    }
    if (pOperand1.equals(pOperand2)) {
      return pOperand1;
    }
    // Try reducing nested unions by temporarily representing them as a set
    Set<NumeralFormula<CompoundInterval>> atomicUnionParts = new HashSet<>();
    Queue<NumeralFormula<CompoundInterval>> unionParts = new ArrayDeque<>();
    BitVectorInfo bitVectorInfo = pOperand1.getBitVectorInfo();
    CompoundIntervalManager cim = getCompoundIntervalManager(bitVectorInfo);
    CompoundInterval constantPart = cim.bottom();
    unionParts.offer(pOperand1);
    unionParts.offer(pOperand2);
    while (!unionParts.isEmpty()) {
      NumeralFormula<CompoundInterval> currentPart = unionParts.poll();
      if (currentPart instanceof Union<?>) {
        Union<CompoundInterval> currentUnion = (Union<CompoundInterval>) currentPart;
        unionParts.add(currentUnion.getOperand1());
        unionParts.add(currentUnion.getOperand2());
      } else if (currentPart instanceof Constant<?>) {
        constantPart = cim.union(constantPart, ((Constant<CompoundInterval>) currentPart).getValue());
      } else {
        atomicUnionParts.add(currentPart);
      }
    }
    return unionAll(bitVectorInfo, constantPart, atomicUnionParts);
  }

  private NumeralFormula<CompoundInterval> unionAll(BitVectorInfo pBitVectorInfo, CompoundInterval pConstantPart, Collection<NumeralFormula<CompoundInterval>> pFormulas) {
    if (pFormulas.isEmpty() || pConstantPart.containsAllPossibleValues()) {
      return asConstant(pBitVectorInfo, pConstantPart);
    }
    NumeralFormula<CompoundInterval> result = null;
    Iterator<NumeralFormula<CompoundInterval>> atomicUnionPartsIterator = pFormulas.iterator();
    result = atomicUnionPartsIterator.next();
    while (atomicUnionPartsIterator.hasNext()) {
      result = InvariantsFormulaManager.INSTANCE.union(result, atomicUnionPartsIterator.next());
    }
    if (!pConstantPart.isBottom()) {
      NumeralFormula<CompoundInterval> constantPartFormula = asConstant(pBitVectorInfo, pConstantPart);
      result = InvariantsFormulaManager.INSTANCE.union(result, constantPartFormula);
    }
    return result;
  }

  public NumeralFormula<CompoundInterval> exclude(NumeralFormula<CompoundInterval> pToExclude) {
    if (pToExclude instanceof Constant) {
      Constant<CompoundInterval> c = (Constant<CompoundInterval>) pToExclude;
      BitVectorInfo bitVectorInfo = pToExclude.getBitVectorInfo();
      if (c.getValue().isSingleton()) {
        return asConstant(bitVectorInfo, c.getValue().invert());
      }
      return allPossibleValues(bitVectorInfo);
    }
    return InvariantsFormulaManager.INSTANCE.exclude(pToExclude);
  }

  public NumeralFormula<CompoundInterval> ifThenElse(
      BooleanFormula<CompoundInterval> pCondition,
      NumeralFormula<CompoundInterval> pPositiveCase,
      NumeralFormula<CompoundInterval> pNegativeCase) {
    if (BooleanConstant.isTrue(pCondition)) {
      return pPositiveCase;
    }
    if (BooleanConstant.isFalse(pCondition)) {
      return pNegativeCase;
    }
    if (isDefinitelyBottom(pPositiveCase) && isDefinitelyBottom(pNegativeCase)) {
      return bottom(pPositiveCase);
    }
    if (pCondition instanceof LogicalNot) {
      return ifThenElse(
          ((LogicalNot<CompoundInterval>) pCondition).getNegated(),
          pNegativeCase,
          pPositiveCase);
    }
    return InvariantsFormulaManager.INSTANCE.ifThenElse(pCondition, pPositiveCase, pNegativeCase);
  }

  public NumeralFormula<CompoundInterval> fromBoolean(
      BitVectorInfo pBitVectorInfo,
      BooleanFormula<CompoundInterval> pFormula) {
    CompoundIntervalManager cim = getCompoundIntervalManager(pBitVectorInfo);
    //NumeralFormula<CompoundInterval> trueNumeralFormula = asConstant(pBitVectorInfo, cim.singleton(BigInteger.ONE));
    NumeralFormula<CompoundInterval> trueNumeralFormula = asConstant(pBitVectorInfo, cim.logicalTrue());
    NumeralFormula<CompoundInterval> falseNumeralFormula = asConstant(pBitVectorInfo, cim.logicalFalse());
    return ifThenElse(
        pFormula,
        trueNumeralFormula,
        falseNumeralFormula);
  }

  public BooleanFormula<CompoundInterval> fromNumeral(NumeralFormula<CompoundInterval> pFormula) {
    BitVectorInfo bitVectorInfo = pFormula.getBitVectorInfo();
    CompoundIntervalManager cim = getCompoundIntervalManager(bitVectorInfo);
    if (pFormula instanceof IfThenElse) {
      IfThenElse<CompoundInterval> ifThenElse = (IfThenElse<CompoundInterval>) pFormula;
      if (isDefinitelyTrue(ifThenElse.getPositiveCase()) && isDefinitelyFalse(ifThenElse.getNegativeCase())) {
        return ifThenElse.getCondition();
      }
      if (isDefinitelyFalse(ifThenElse.getPositiveCase()) && isDefinitelyTrue(ifThenElse.getNegativeCase())) {
        return logicalNot(ifThenElse.getCondition());
      }
    }
    return logicalNot(equal(pFormula, asConstant(bitVectorInfo, cim.singleton(BigInteger.ZERO))));
  }

  public NumeralFormula<CompoundInterval> cast(BitVectorInfo pBitVectorInfo, NumeralFormula<CompoundInterval> pToCast) {
    if (pToCast.getBitVectorInfo().equals(pBitVectorInfo)) {
      return pToCast;
    }
    NumeralFormula<CompoundInterval> casted = InvariantsFormulaManager.INSTANCE.cast(pBitVectorInfo, pToCast);
    if (pToCast instanceof Constant) {
      casted = asConstant(pBitVectorInfo, casted.accept(evaluationVisitor, EMPTY_ENVIRONMENT));
    }
    return casted;
  }

  private NumeralFormula<CompoundInterval> bottom(BitVectorInfo pBitVectorInfo) {
    return asConstant(pBitVectorInfo, getCompoundIntervalManager(pBitVectorInfo).bottom());
  }

  private NumeralFormula<CompoundInterval> bottom(BitVectorType pBitVectorType) {
    return bottom(pBitVectorType.getBitVectorInfo());
  }

  private NumeralFormula<CompoundInterval> allPossibleValues(BitVectorInfo pBitVectorInfo) {
    return asConstant(pBitVectorInfo, getCompoundIntervalManager(pBitVectorInfo).allPossibleValues());
  }

  private NumeralFormula<CompoundInterval> allPossibleValues(BitVectorType pBitVectorType) {
    return allPossibleValues(pBitVectorType.getBitVectorInfo());
  }

  private NumeralFormula<CompoundInterval> asConstant(BitVectorInfo pBitVectorInfo, CompoundInterval pValue) {
    return InvariantsFormulaManager.INSTANCE.asConstant(pBitVectorInfo, pValue);
  }

  private CompoundIntervalManager getCompoundIntervalManager(BitVectorInfo pBitVectorInfo) {
    return compoundIntervalManagerFactory.createCompoundIntervalManager(pBitVectorInfo);
  }

}
