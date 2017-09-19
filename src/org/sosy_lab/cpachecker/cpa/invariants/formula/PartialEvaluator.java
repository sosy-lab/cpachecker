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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundIntervalManager;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundIntervalManagerFactory;
import org.sosy_lab.cpachecker.cpa.invariants.TypeInfo;
import org.sosy_lab.cpachecker.cpa.invariants.Typed;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * The singleton instance of this class is a compound state invariants formula
 * visitor used to partially evaluate compound state invariants formulae to
 * eliminate complex expressions consisting only of constants.
 */
public class PartialEvaluator implements
    ParameterizedNumeralFormulaVisitor<CompoundInterval, FormulaEvaluationVisitor<CompoundInterval>, NumeralFormula<CompoundInterval>>,
    ParameterizedBooleanFormulaVisitor<CompoundInterval, FormulaEvaluationVisitor<CompoundInterval>, BooleanFormula<CompoundInterval>> {

  /**
   * A map representing an empty environment; since only constants are
   * evaluated, no real environment is required.
   */
  private final Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> environment;

  private final CompoundIntervalManagerFactory compoundIntervalManagerFactory;

  private final CompoundIntervalFormulaManager compoundIntervalFormulaManager;

  public PartialEvaluator(CompoundIntervalManagerFactory pCompoundIntervalManagerFactory) {
    this(pCompoundIntervalManagerFactory, Collections.<MemoryLocation, NumeralFormula<CompoundInterval>>emptyMap());
  }

  public PartialEvaluator(
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      CompoundIntervalFormulaManager pCompoundIntervalFormulaManager) {
    this.compoundIntervalManagerFactory = pCompoundIntervalManagerFactory;
    this.environment = Collections.emptyMap();
    this.compoundIntervalFormulaManager = pCompoundIntervalFormulaManager;
  }

  public PartialEvaluator(
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    this.compoundIntervalManagerFactory = pCompoundIntervalManagerFactory;
    this.environment = pEnvironment;
    this.compoundIntervalFormulaManager = new CompoundIntervalFormulaManager(compoundIntervalManagerFactory);
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (pObj instanceof PartialEvaluator) {
      PartialEvaluator other = (PartialEvaluator) pObj;
      return compoundIntervalManagerFactory.equals(other.compoundIntervalManagerFactory)
          && compoundIntervalFormulaManager.equals(other.compoundIntervalFormulaManager)
          && environment.equals(other.environment);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        compoundIntervalManagerFactory, compoundIntervalFormulaManager, environment);
  }

  private CompoundIntervalManager getCompoundIntervalManager(TypeInfo pTypeInfo) {
    return compoundIntervalManagerFactory.createCompoundIntervalManager(pTypeInfo);
  }

  private CompoundIntervalManager getCompoundIntervalManager(Typed pTyped) {
    return getCompoundIntervalManager(pTyped.getTypeInfo());
  }

  private NumeralFormula<CompoundInterval> evaluateAndWrap(NumeralFormula<CompoundInterval> pFormula, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    return InvariantsFormulaManager.INSTANCE.asConstant(
        pFormula.getTypeInfo(), pFormula.accept(pEvaluationVisitor, environment));
  }

  private NumeralFormula<CompoundInterval> asConstant(Typed pTyped, CompoundInterval pValue) {
    return InvariantsFormulaManager.INSTANCE.asConstant(pTyped.getTypeInfo(), pValue);
  }

  private NumeralFormula<CompoundInterval> singleton(Typed pTyped, BigInteger pValue) {
    TypeInfo info = pTyped.getTypeInfo();
    return InvariantsFormulaManager.INSTANCE.asConstant(
        info,
        getCompoundIntervalManager(info).singleton(pValue));
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(Add<CompoundInterval> pAdd, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    NumeralFormula<CompoundInterval> summand1 = pAdd.getSummand1().accept(this, pEvaluationVisitor);
    NumeralFormula<CompoundInterval> summand2 = pAdd.getSummand2().accept(this, pEvaluationVisitor);
    // If both summands are constants, calculate a new constant
    if (summand1 instanceof Constant<?> && summand2 instanceof Constant<?>) {
      return evaluateAndWrap(pAdd, pEvaluationVisitor);
    }
    // If one of the summands is constant zero, return the other summand
    // If one of the summands is top or bottom, return it
    Constant<CompoundInterval> c = null;
    NumeralFormula<CompoundInterval> other = null;
    if (summand1 instanceof Constant<?>) {
      c = (Constant<CompoundInterval>) summand1;
      other = summand2;
    } else if (summand2 instanceof Constant<?>) {
      c = (Constant<CompoundInterval>) summand2;
      other = summand1;
    }
    if (c != null && other != null) {
      CompoundInterval value = c.getValue();
      if (value.isSingleton() && value.getValue().equals(BigInteger.ZERO)) {
        return other;
      }
      if (value.containsAllPossibleValues() || value.isBottom()) {
        return c;
      }
      PushSummandVisitor<CompoundInterval> psv = new PushSummandVisitor<>(pEvaluationVisitor);
      other = other.accept(psv, value);
      if (psv.isSummandConsumed()) {
        other = other.accept(this, pEvaluationVisitor);
      }
      return other;
    }
    // If the summands did not change, return the original statement
    if (summand1 == pAdd.getSummand1() && summand2 == pAdd.getSummand2()) {
      return pAdd;
    }
    // If one or both summands change but cannot be further evaluated,
    // return a new addition formula for both
    return compoundIntervalFormulaManager.add(summand1, summand2);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(BinaryAnd<CompoundInterval> pAnd, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    NumeralFormula<CompoundInterval> operand1 = pAnd.getOperand1().accept(this, pEvaluationVisitor);
    NumeralFormula<CompoundInterval> operand2 = pAnd.getOperand2().accept(this, pEvaluationVisitor);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      return evaluateAndWrap(pAnd, pEvaluationVisitor);
    }
    if (operand1 == pAnd.getOperand1() && operand2 == pAnd.getOperand2()) {
      return pAnd;
    }
    return compoundIntervalFormulaManager.binaryAnd(operand1, operand2);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(BinaryNot<CompoundInterval> pNot, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    NumeralFormula<CompoundInterval> operand = pNot.getFlipped().accept(this, pEvaluationVisitor);
    if (operand instanceof Constant<?>) {
      return evaluateAndWrap(pNot, pEvaluationVisitor);
    }
    if (operand == pNot.getFlipped()) {
      return pNot;
    }
    return compoundIntervalFormulaManager.binaryNot(operand);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(BinaryOr<CompoundInterval> pOr, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    NumeralFormula<CompoundInterval> operand1 = pOr.getOperand1().accept(this, pEvaluationVisitor);
    NumeralFormula<CompoundInterval> operand2 = pOr.getOperand2().accept(this, pEvaluationVisitor);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      return evaluateAndWrap(pOr, pEvaluationVisitor);
    }
    if (operand1 == pOr.getOperand1() && operand2 == pOr.getOperand2()) {
      return pOr;
    }
    return compoundIntervalFormulaManager.binaryOr(operand1, operand2);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(BinaryXor<CompoundInterval> pXor, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    NumeralFormula<CompoundInterval> operand1 = pXor.getOperand1().accept(this, pEvaluationVisitor);
    NumeralFormula<CompoundInterval> operand2 = pXor.getOperand2().accept(this, pEvaluationVisitor);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      return evaluateAndWrap(pXor, pEvaluationVisitor);
    }
    if (operand1 == pXor.getOperand1() && operand2 == pXor.getOperand2()) {
      return pXor;
    }
    return compoundIntervalFormulaManager.binaryXor(operand1, operand2);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(Constant<CompoundInterval> pConstant, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    return pConstant;
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(Divide<CompoundInterval> pDivide, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    NumeralFormula<CompoundInterval> numerator = pDivide.getNumerator().accept(this, pEvaluationVisitor);
    NumeralFormula<CompoundInterval> denominator = pDivide.getDenominator().accept(this, pEvaluationVisitor);
    if (numerator instanceof Constant<?> && denominator instanceof Constant<?>) {
      return evaluateAndWrap(pDivide, pEvaluationVisitor);
    }
    // Division by 1 yields the numerator, by -1 the negated numerator
    if (denominator instanceof Constant<?>) {
      Constant<CompoundInterval> c = (Constant<CompoundInterval>) denominator;
      CompoundInterval value = c.getValue();
      if (value.isSingleton()) {
        if (value.getValue().equals(BigInteger.ONE)) {
          return numerator;
        } else if (value.getValue().equals(BigInteger.valueOf(-1))) {
          return compoundIntervalFormulaManager.negate(numerator);
        }
      }
    }
    if (numerator == pDivide.getNumerator() && denominator == pDivide.getDenominator()) {
      return pDivide;
    }
    return compoundIntervalFormulaManager.divide(numerator, denominator);
  }

  @Override
  public BooleanFormula<CompoundInterval> visit(Equal<CompoundInterval> pEqual, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    NumeralFormula<CompoundInterval> operand1 = pEqual.getOperand1().accept(this, pEvaluationVisitor);
    NumeralFormula<CompoundInterval> operand2 = pEqual.getOperand2().accept(this, pEvaluationVisitor);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      BooleanFormula<CompoundInterval> result = pEqual.accept(pEvaluationVisitor, environment);
      if (result != null) {
        return result;
      }
    }
    if (operand1 == pEqual.getOperand1() && operand2 == pEqual.getOperand2()) {
      return pEqual;
    }
    return Equal.of(operand1, operand2);
  }

  @Override
  public BooleanFormula<CompoundInterval> visit(LessThan<CompoundInterval> pLessThan, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    NumeralFormula<CompoundInterval> operand1 = pLessThan.getOperand1().accept(this, pEvaluationVisitor);
    NumeralFormula<CompoundInterval> operand2 = pLessThan.getOperand2().accept(this, pEvaluationVisitor);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      BooleanFormula<CompoundInterval> result = pLessThan.accept(pEvaluationVisitor, environment);
      if (result != null) {
        return result;
      }
    }
    if (operand1 == pLessThan.getOperand1() && operand2 == pLessThan.getOperand2()) {
      return pLessThan;
    }
    return LessThan.of(operand1, operand2);
  }

  @Override
  public BooleanFormula<CompoundInterval> visit(LogicalAnd<CompoundInterval> pAnd, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    BooleanFormula<CompoundInterval> operand1 = pAnd.getOperand1().accept(this, pEvaluationVisitor);
    BooleanFormula<CompoundInterval> operand2 = pAnd.getOperand2().accept(this, pEvaluationVisitor);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      BooleanFormula<CompoundInterval> result = pAnd.accept(pEvaluationVisitor, environment);
      if (result != null) {
        return result;
      }
    }
    // If either operand is false, return it
    if (operand1 instanceof BooleanConstant<?> && !((BooleanConstant<?>) operand1).getValue()) {
      return operand1;
    }
    if (operand2 instanceof BooleanConstant<?> && !((BooleanConstant<?>) operand2).getValue()) {
      return operand2;
    }
    // If both operands are true, return the first one
    if (operand1 instanceof BooleanConstant<?> && ((BooleanConstant<?>) operand1).getValue()
        && operand2 instanceof BooleanConstant<?> && ((BooleanConstant<?>) operand2).getValue()) {
      return operand1;
    }
    if (operand1 == pAnd.getOperand1() && operand2 == pAnd.getOperand2()) {
      return pAnd;
    }
    return LogicalAnd.of(operand1, operand2);
  }

  @Override
  public BooleanFormula<CompoundInterval> visit(LogicalNot<CompoundInterval> pNot, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    BooleanFormula<CompoundInterval> operand = pNot.getNegated().accept(this, pEvaluationVisitor);
    if (operand instanceof Constant<?>) {
      BooleanFormula<CompoundInterval> result = pNot.accept(pEvaluationVisitor, environment);
      if (result != null) {
        return result;
      }
    }
    // The negation of a negation yields the inner operand
    if (operand instanceof LogicalNot<?>) {
      return ((LogicalNot<CompoundInterval>) operand).getNegated();
    }
    if (operand instanceof BooleanConstant<?>) {
      return ((BooleanConstant<CompoundInterval>) operand).negate();
    }
    if (operand == pNot.getNegated()) {
      return pNot;
    }
    return LogicalNot.of(operand);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(Modulo<CompoundInterval> pModulo, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    NumeralFormula<CompoundInterval> numerator = pModulo.getNumerator().accept(this, pEvaluationVisitor);
    NumeralFormula<CompoundInterval> denominator = pModulo.getDenominator().accept(this, pEvaluationVisitor);
    if (numerator instanceof Constant<?> && denominator instanceof Constant<?>) {
      return evaluateAndWrap(pModulo, pEvaluationVisitor);
    }
    // If the denominator is 1 or -1, modulo must yield 0
    if (denominator instanceof Constant<?>) {
      Constant<CompoundInterval> c = (Constant<CompoundInterval>) denominator;
      CompoundInterval value = c.getValue();
      if (value.isSingleton() && (value.getValue().equals(BigInteger.ONE)
          || value.getValue().equals(BigInteger.valueOf(-1)))) {
        return singleton(pModulo, BigInteger.ZERO);
      }
    }
    if (numerator == pModulo.getNumerator() && denominator == pModulo.getDenominator()) {
      return pModulo;
    }
    return compoundIntervalFormulaManager.modulo(numerator, denominator);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(Multiply<CompoundInterval> pMultiply, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    NumeralFormula<CompoundInterval> factor1 = pMultiply.getFactor1().accept(this, pEvaluationVisitor);
    NumeralFormula<CompoundInterval> factor2 = pMultiply.getFactor2().accept(this, pEvaluationVisitor);
    if (factor1 instanceof Constant<?> && factor2 instanceof Constant<?>) {
      return evaluateAndWrap(pMultiply, pEvaluationVisitor);
    }
    // Multiplication by 1 yields the other factor,
    // by -1 the negated other factor, by 0 it yields 0.
    Constant<CompoundInterval> c = null;
    NumeralFormula<CompoundInterval> otherFactor = null;
    if (factor1 instanceof Constant<?>) {
      c = (Constant<CompoundInterval>) factor1;
      otherFactor = factor2;
    } else if (factor2 instanceof Constant<?>) {
      c = (Constant<CompoundInterval>) factor2;
      otherFactor = factor1;
    }
    if (c != null && otherFactor != null) {
      CompoundInterval state = c.getValue();
      if (state.isSingleton()) {
        Number value = state.getValue();
        if (value.equals(BigInteger.ONE)) {
          return otherFactor;
        }
        if (value.equals(BigInteger.valueOf(-1))) {
          return compoundIntervalFormulaManager.negate(otherFactor);
        }
        if (value.equals(BigInteger.ZERO)) {
          return singleton(pMultiply, BigInteger.ZERO);
        }
      }
    }
    if (factor1 == pMultiply.getFactor1() && factor2 == pMultiply.getFactor2()) {
      return pMultiply;
    }
    return compoundIntervalFormulaManager.multiply(factor1, factor2);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(ShiftLeft<CompoundInterval> pShiftLeft, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    NumeralFormula<CompoundInterval> shifted = pShiftLeft.getShifted().accept(this, pEvaluationVisitor);
    NumeralFormula<CompoundInterval> shiftDistance = pShiftLeft.getShiftDistance().accept(this, pEvaluationVisitor);
    if (shifted instanceof Constant<?> && shiftDistance instanceof Constant<?>) {
      return evaluateAndWrap(pShiftLeft, pEvaluationVisitor);
    }
    // If the shift distance is zero, return the left operand
    if (shiftDistance instanceof Constant<?>) {
      Constant<CompoundInterval> c = (Constant<CompoundInterval>) shiftDistance;
      if (c.getValue().isSingleton() && c.getValue().getValue().equals(BigInteger.ZERO)) {
        return shifted;
      }
    }
    if (shifted == pShiftLeft.getShifted() && shiftDistance == pShiftLeft.getShiftDistance()) {
      return pShiftLeft;
    }
    return compoundIntervalFormulaManager.shiftLeft(shifted, shiftDistance);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(ShiftRight<CompoundInterval> pShiftRight, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    NumeralFormula<CompoundInterval> shifted = pShiftRight.getShifted().accept(this, pEvaluationVisitor);
    NumeralFormula<CompoundInterval> shiftDistance = pShiftRight.getShiftDistance().accept(this, pEvaluationVisitor);
    if (shifted instanceof Constant<?> && shiftDistance instanceof Constant<?>) {
      return evaluateAndWrap(pShiftRight, pEvaluationVisitor);
    }
    // If the shift distance is zero, return the left operand
    if (shiftDistance instanceof Constant<?>) {
      Constant<CompoundInterval> c = (Constant<CompoundInterval>) shiftDistance;
      if (c.getValue().isSingleton() && c.getValue().getValue().equals(BigInteger.ZERO)) {
        return shifted;
      }
    }
    if (shifted == pShiftRight.getShifted() && shiftDistance == pShiftRight.getShiftDistance()) {
      return pShiftRight;
    }
    return compoundIntervalFormulaManager.shiftRight(shifted, shiftDistance);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(Union<CompoundInterval> pUnion, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    NumeralFormula<CompoundInterval> operand1 = pUnion.getOperand1().accept(this, pEvaluationVisitor);
    NumeralFormula<CompoundInterval> operand2 = pUnion.getOperand2().accept(this, pEvaluationVisitor);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      return evaluateAndWrap(pUnion, pEvaluationVisitor);
    }
    // Union with top yields top
    CompoundIntervalManager compoundIntervalManager = getCompoundIntervalManager(pUnion);
    NumeralFormula<CompoundInterval> allPossibleValues = asConstant(pUnion, compoundIntervalManager.allPossibleValues());
    if (operand1.equals(allPossibleValues) || operand2.equals(allPossibleValues)) {
      return allPossibleValues;
    }
    // Union with bottom yields the other operand
    NumeralFormula<CompoundInterval> bottom = asConstant(pUnion, compoundIntervalManager.bottom());
    if (operand1.equals(bottom)) {
      return operand2;
    }
    if (operand2.equals(bottom)) {
      return operand1;
    }
    // Try reducing nested unions by temporarily representing them as a set
    Set<NumeralFormula<CompoundInterval>> atomicUnionParts = new HashSet<>();
    Queue<NumeralFormula<CompoundInterval>> unionParts = new ArrayDeque<>();
    CompoundInterval constantPart = compoundIntervalManager.bottom();
    unionParts.offer(pUnion);
    int partsFound = 0;
    while (!unionParts.isEmpty()) {
      NumeralFormula<CompoundInterval> currentPart = unionParts.poll();
      if (currentPart instanceof Union<?>) {
        Union<CompoundInterval> currentUnion = (Union<CompoundInterval>) currentPart;
        unionParts.add(currentUnion.getOperand1());
        unionParts.add(currentUnion.getOperand2());
        partsFound += 2;
      } else if (currentPart instanceof Constant<?>) {
        constantPart = compoundIntervalManager.union(constantPart, ((Constant<CompoundInterval>) currentPart).getValue());
      } else {
        atomicUnionParts.add(currentPart);
      }
    }
    if (partsFound > atomicUnionParts.size()) {
      NumeralFormula<CompoundInterval> result = null;
      if (atomicUnionParts.size() > 0) {
        Iterator<NumeralFormula<CompoundInterval>> atomicUnionPartsIterator = atomicUnionParts.iterator();
        result = atomicUnionPartsIterator.next();
        while (atomicUnionPartsIterator.hasNext()) {
          result = compoundIntervalFormulaManager.union(result, atomicUnionPartsIterator.next());
        }
      }
      if (!constantPart.isBottom()) {
        NumeralFormula<CompoundInterval> constantPartFormula = asConstant(pUnion, constantPart);
        result = result == null ? constantPartFormula : compoundIntervalFormulaManager.union(result, constantPartFormula);
      }
      if (result != null) {
        return result;
      }
    }

    if (operand1 == pUnion.getOperand1() && operand2 == pUnion.getOperand2()) {
      return pUnion;
    }
    return compoundIntervalFormulaManager.union(operand1, operand2);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(Variable<CompoundInterval> pVariable, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    CompoundInterval value = pVariable.accept(pEvaluationVisitor, this.environment);
    if (value.isSingleton()) {
      return asConstant(pVariable, value);
    }
    return pVariable;
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(Exclusion<CompoundInterval> pExclusion,
      FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    CompoundInterval value = pExclusion.accept(pEvaluationVisitor, this.environment);
    if (value.isSingleton() || pExclusion.getExcluded() instanceof Constant) {
      return asConstant(pExclusion, value);
    }
    return pExclusion;
  }

  @Override
  public BooleanFormula<CompoundInterval> visitFalse(FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    return pEvaluationVisitor.visitFalse(environment);
  }

  @Override
  public BooleanFormula<CompoundInterval> visitTrue(FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    return pEvaluationVisitor.visitTrue(environment);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(IfThenElse<CompoundInterval> pIfThenElse,
      FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    BooleanFormula<CompoundInterval> condition = pIfThenElse.getCondition().accept(this, pEvaluationVisitor);
    NumeralFormula<CompoundInterval> positiveCase = pIfThenElse.getPositiveCase().accept(this, pEvaluationVisitor);
    if (BooleanConstant.isTrue(condition)) {
      return positiveCase;
    }
    NumeralFormula<CompoundInterval> negativeCase = pIfThenElse.getNegativeCase().accept(this, pEvaluationVisitor);
    if (BooleanConstant.isFalse(condition)) {
      return negativeCase;
    }

    if (pIfThenElse.getCondition().equals(condition)
        && pIfThenElse.getPositiveCase().equals(positiveCase)
        && pIfThenElse.getNegativeCase().equals(negativeCase)) {
      return pIfThenElse;
    }
    return compoundIntervalFormulaManager.ifThenElse(condition, positiveCase, negativeCase);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(Cast<CompoundInterval> pCast,
      FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    CompoundInterval value = pCast.accept(pEvaluationVisitor, this.environment);
    if (value.isSingleton()) {
      return asConstant(pCast, value);
    }
    NumeralFormula<CompoundInterval> operand = pCast.getCasted().accept(this, pEvaluationVisitor);
    if (operand == pCast.getCasted()) {
      return pCast;
    }
    return compoundIntervalFormulaManager.cast(pCast.getTypeInfo(), operand);
  }

}
