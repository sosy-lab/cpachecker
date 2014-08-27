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
import java.util.Queue;
import java.util.Set;

import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;

/**
 * The singleton instance of this class is a compound state invariants formula
 * visitor used to partially evaluate compound state invariants formulae to
 * eliminate complex expressions consisting only of constants.
 */
public class PartialEvaluator implements ParameterizedInvariantsFormulaVisitor<CompoundInterval, FormulaEvaluationVisitor<CompoundInterval>, InvariantsFormula<CompoundInterval>> {

  /**
   * An invariants formula representing the top state.
   */
  private static final InvariantsFormula<CompoundInterval> TOP =
      CompoundIntervalFormulaManager.INSTANCE.asConstant(CompoundInterval.top());

  /**
   * An invariants formula representing the bottom state.
   */
  private static final InvariantsFormula<CompoundInterval> BOTTOM =
      CompoundIntervalFormulaManager.INSTANCE.asConstant(CompoundInterval.bottom());

  /**
   * A visitor for compound state invariants formulae used to determine whether
   * or not the visited formula is a genuine boolean formula.
   */
  private static final IsBooleanFormulaVisitor<CompoundInterval> IS_BOOLEAN_FORMULA_VISITOR =
      new IsBooleanFormulaVisitor<>();

  /**
   * A map representing an empty environment; since only constants are
   * evaluated, no real environment is required.
   */
  private final Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> environment;

  public PartialEvaluator() {
    this(Collections.<String, InvariantsFormula<CompoundInterval>>emptyMap());
  }

  public PartialEvaluator(Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    this.environment = pEnvironment;
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(Add<CompoundInterval> pAdd, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    InvariantsFormula<CompoundInterval> summand1 = pAdd.getSummand1().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundInterval> summand2 = pAdd.getSummand2().accept(this, pEvaluationVisitor);
    // If both summands are constants, calculate a new constant
    if (summand1 instanceof Constant<?> && summand2 instanceof Constant<?>) {
      return CompoundIntervalFormulaManager.INSTANCE.asConstant(pAdd.accept(pEvaluationVisitor, environment));
    }
    // If one of the summands is constant zero, return the other summand
    // If one of the summands is top or bottom, return it
    Constant<CompoundInterval> c = null;
    InvariantsFormula<CompoundInterval> other = null;
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
      if (value.isTop() || value.isBottom()) {
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
    return CompoundIntervalFormulaManager.INSTANCE.add(summand1, summand2);
  }

  private static boolean isDefinitelyBottom(InvariantsFormula<CompoundInterval> pFormula) {
    if (pFormula instanceof Constant) {
      Constant<CompoundInterval> constant = (Constant<CompoundInterval>) pFormula;
      return constant.getValue().isBottom();
    }
    return false;
  }

  private static boolean isDefinitelyTop(InvariantsFormula<CompoundInterval> pFormula) {
    if (pFormula instanceof Constant) {
      Constant<CompoundInterval> constant = (Constant<CompoundInterval>) pFormula;
      return constant.getValue().isTop();
    }
    return false;
  }

  private static InvariantsFormula<CompoundInterval> extractBottomOrTop(InvariantsFormula<CompoundInterval> pFormula1, InvariantsFormula<CompoundInterval> pFormula2) {
    if (isDefinitelyBottom(pFormula1)) {
      return pFormula1;
    }
    if (isDefinitelyBottom(pFormula2)) {
      return pFormula2;
    }
    if (isDefinitelyTop(pFormula1)) {
      return pFormula1;
    }
    if (isDefinitelyTop(pFormula2)) {
      return pFormula2;
    }
    return null;
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(BinaryAnd<CompoundInterval> pAnd, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    InvariantsFormula<CompoundInterval> operand1 = pAnd.getOperand1().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundInterval> operand2 = pAnd.getOperand2().accept(this, pEvaluationVisitor);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      return CompoundIntervalFormulaManager.INSTANCE.asConstant(pAnd.accept(pEvaluationVisitor, environment));
    }
    if (operand1 == pAnd.getOperand1() && operand2 == pAnd.getOperand2()) {
      return pAnd;
    }
    return CompoundIntervalFormulaManager.INSTANCE.binaryAnd(operand1, operand2);
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(BinaryNot<CompoundInterval> pNot, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    InvariantsFormula<CompoundInterval> operand = pNot.getFlipped().accept(this, pEvaluationVisitor);
    if (operand instanceof Constant<?>) {
      return CompoundIntervalFormulaManager.INSTANCE.asConstant(pNot.accept(pEvaluationVisitor, environment));
    }
    if (operand == pNot.getFlipped()) {
      return pNot;
    }
    return CompoundIntervalFormulaManager.INSTANCE.binaryNot(operand);
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(BinaryOr<CompoundInterval> pOr, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    InvariantsFormula<CompoundInterval> operand1 = pOr.getOperand1().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundInterval> operand2 = pOr.getOperand2().accept(this, pEvaluationVisitor);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      return CompoundIntervalFormulaManager.INSTANCE.asConstant(pOr.accept(pEvaluationVisitor, environment));
    }
    if (operand1 == pOr.getOperand1() && operand2 == pOr.getOperand2()) {
      return pOr;
    }
    return CompoundIntervalFormulaManager.INSTANCE.binaryOr(operand1, operand2);
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(BinaryXor<CompoundInterval> pXor, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    InvariantsFormula<CompoundInterval> operand1 = pXor.getOperand1().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundInterval> operand2 = pXor.getOperand2().accept(this, pEvaluationVisitor);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      return CompoundIntervalFormulaManager.INSTANCE.asConstant(pXor.accept(pEvaluationVisitor, environment));
    }
    if (operand1 == pXor.getOperand1() && operand2 == pXor.getOperand2()) {
      return pXor;
    }
    return CompoundIntervalFormulaManager.INSTANCE.binaryXor(operand1, operand2);
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(Constant<CompoundInterval> pConstant, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    return pConstant;
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(Divide<CompoundInterval> pDivide, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    InvariantsFormula<CompoundInterval> numerator = pDivide.getNumerator().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundInterval> denominator = pDivide.getDenominator().accept(this, pEvaluationVisitor);
    if (numerator instanceof Constant<?> && denominator instanceof Constant<?>) {
      return CompoundIntervalFormulaManager.INSTANCE.asConstant(pDivide.accept(pEvaluationVisitor, environment));
    }
    // Division by 1 yields the numerator, by -1 the negated numerator
    if (denominator instanceof Constant<?>) {
      Constant<CompoundInterval> c = (Constant<CompoundInterval>) denominator;
      CompoundInterval value = c.getValue();
      if (value.isSingleton()) {
        if (value.getValue().equals(BigInteger.ONE)) {
          return numerator;
        } else if (value.getValue().equals(BigInteger.valueOf(-1))) {
          return CompoundIntervalFormulaManager.INSTANCE.negate(numerator);
        }
      }
    }
    if (numerator == pDivide.getNumerator() && denominator == pDivide.getDenominator()) {
      return pDivide;
    }
    return CompoundIntervalFormulaManager.INSTANCE.divide(numerator, denominator);
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(Equal<CompoundInterval> pEqual, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    InvariantsFormula<CompoundInterval> operand1 = pEqual.getOperand1().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundInterval> operand2 = pEqual.getOperand2().accept(this, pEvaluationVisitor);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      return CompoundIntervalFormulaManager.INSTANCE.asConstant(pEqual.accept(pEvaluationVisitor, environment));
    }
    // If one of the operands is bottom, the equation is bottom; if one of them is top, the equation is top
    InvariantsFormula<CompoundInterval> botOrTop = extractBottomOrTop(operand1, operand2);
    if (botOrTop != null) {
      return botOrTop;
    }
    /*
     * If a boolean formula is operand in an equation with true, the boolean
     * formula itself can be returned, because the "== true" has does not
     * change the value of the expression. Almost the same goes for equations
     * with false, only that the other operand has to be negated then.
     */
    CompoundInterval c = null;
    InvariantsFormula<CompoundInterval> other = null;
    if (operand1 instanceof Constant<?>
      && (pEqual.getOperand2().accept(IS_BOOLEAN_FORMULA_VISITOR)
          || operand2.accept(IS_BOOLEAN_FORMULA_VISITOR))) {
      c = operand1.accept(pEvaluationVisitor, environment);
      other = operand2;
    } else if (operand2 instanceof Constant<?>
      && (pEqual.getOperand1().accept(IS_BOOLEAN_FORMULA_VISITOR)
          || operand1.accept(IS_BOOLEAN_FORMULA_VISITOR))) {
      c = operand2.accept(pEvaluationVisitor, environment);
      other = operand1;
    }
    if (c != null && other != null) {
      if (c.isDefinitelyTrue()) {
        return other;
      }
      if (c.isDefinitelyFalse()) {
        return CompoundIntervalFormulaManager.INSTANCE.logicalNot(other).accept(this, pEvaluationVisitor);
      }
    }
    if (operand1 == pEqual.getOperand1() && operand2 == pEqual.getOperand2()) {
      return pEqual;
    }
    return CompoundIntervalFormulaManager.INSTANCE.equal(operand1, operand2);
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(LessThan<CompoundInterval> pLessThan, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    InvariantsFormula<CompoundInterval> operand1 = pLessThan.getOperand1().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundInterval> operand2 = pLessThan.getOperand2().accept(this, pEvaluationVisitor);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      return CompoundIntervalFormulaManager.INSTANCE.asConstant(pLessThan.accept(pEvaluationVisitor, environment));
    }
    // If one of the operands is bottom, the inequation is bottom; if one of them is top, the inequation is top
    InvariantsFormula<CompoundInterval> botOrTop = extractBottomOrTop(operand1, operand2);
    if (botOrTop != null) {
      return botOrTop;
    }
    if (operand1 == pLessThan.getOperand1() && operand2 == pLessThan.getOperand2()) {
      return pLessThan;
    }
    return CompoundIntervalFormulaManager.INSTANCE.lessThan(operand1, operand2);
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(LogicalAnd<CompoundInterval> pAnd, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    InvariantsFormula<CompoundInterval> operand1 = pAnd.getOperand1().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundInterval> operand2 = pAnd.getOperand2().accept(this, pEvaluationVisitor);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      return CompoundIntervalFormulaManager.INSTANCE.asConstant(pAnd.accept(pEvaluationVisitor, environment));
    }
    // If one of the operands is false, return false;
    // if one of the operands is true, return the other one
    // if one of the operands is top, return it
    // if one of the operands is bottom, return it
    InvariantsFormula<CompoundInterval> constant = null;
    InvariantsFormula<CompoundInterval> other = null;
    if (operand1 instanceof Constant<?>) {
      constant = operand1;
      other = operand2;
    } else if (operand2 instanceof Constant<?>) {
      constant = operand2;
      other = operand1;
    }
    if (constant != null && other != null) {
      CompoundInterval constantValue = ((Constant<CompoundInterval>) constant).getValue();
      if (constantValue.isDefinitelyFalse() || constantValue.isBottom() || constantValue.isTop()) {
        return constant;
      } else if (constantValue.isDefinitelyTrue()) {
        return other;
      }
    }
    if (operand1 == pAnd.getOperand1() && operand2 == pAnd.getOperand2()) {
      return pAnd;
    }
    return CompoundIntervalFormulaManager.INSTANCE.logicalAnd(operand1, operand2);
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(LogicalNot<CompoundInterval> pNot, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    InvariantsFormula<CompoundInterval> operand = pNot.getNegated().accept(this, pEvaluationVisitor);
    if (operand instanceof Constant<?>) {
      return CompoundIntervalFormulaManager.INSTANCE.asConstant(pNot.accept(pEvaluationVisitor, environment));
    }
    // The negation of a negation yields the inner operand
    if (operand instanceof LogicalNot<?>) {
      return ((LogicalNot<CompoundInterval>) operand).getNegated();
    }
    // The negation of a logical conjunction can be treated as an or with negated operands (De Morgan)
    if (operand instanceof LogicalAnd<?>) {
      LogicalAnd<CompoundInterval> land = (LogicalAnd<CompoundInterval>) operand;
      // If the or-operand is definitely true (the and-operand is definitely false), return true
      if (land.getOperand1().accept(pEvaluationVisitor, this.environment).isDefinitelyFalse()) {
        return CompoundIntervalFormulaManager.INSTANCE.asConstant(CompoundInterval.logicalTrue());
      }
      if (land.getOperand2().accept(pEvaluationVisitor, this.environment).isDefinitelyFalse()) {
        return CompoundIntervalFormulaManager.INSTANCE.asConstant(CompoundInterval.logicalTrue());
      }
      // If one of those operands is definitely false (true in the and-representation), return the other one (negated in the and-representation)
      if (land.getOperand1().accept(pEvaluationVisitor, this.environment).isDefinitelyTrue()) {
        return CompoundIntervalFormulaManager.INSTANCE.logicalNot(land.getOperand2()).accept(this, pEvaluationVisitor);
      }
      if (land.getOperand2().accept(pEvaluationVisitor, this.environment).isDefinitelyTrue()) {
        return CompoundIntervalFormulaManager.INSTANCE.logicalNot(land.getOperand1()).accept(this, pEvaluationVisitor);
      }
    }
    if (operand == pNot.getNegated()) {
      return pNot;
    }
    return CompoundIntervalFormulaManager.INSTANCE.logicalNot(operand);
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(Modulo<CompoundInterval> pModulo, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    InvariantsFormula<CompoundInterval> numerator = pModulo.getNumerator().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundInterval> denominator = pModulo.getDenominator().accept(this, pEvaluationVisitor);
    if (numerator instanceof Constant<?> && denominator instanceof Constant<?>) {
      return CompoundIntervalFormulaManager.INSTANCE.asConstant(pModulo.accept(pEvaluationVisitor, environment));
    }
    // If the denominator is 1 or -1, modulo must yield 0
    if (denominator instanceof Constant<?>) {
      Constant<CompoundInterval> c = (Constant<CompoundInterval>) denominator;
      CompoundInterval value = c.getValue();
      if (value.isSingleton() && (value.getValue().equals(BigInteger.ONE)
          || value.getValue().equals(BigInteger.valueOf(-1)))) {
        return CompoundIntervalFormulaManager.INSTANCE.asConstant(CompoundInterval.singleton(1));
      }
    }
    if (numerator == pModulo.getNumerator() && denominator == pModulo.getDenominator()) {
      return pModulo;
    }
    return CompoundIntervalFormulaManager.INSTANCE.modulo(numerator, denominator);
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(Multiply<CompoundInterval> pMultiply, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    InvariantsFormula<CompoundInterval> factor1 = pMultiply.getFactor1().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundInterval> factor2 = pMultiply.getFactor2().accept(this, pEvaluationVisitor);
    if (factor1 instanceof Constant<?> && factor2 instanceof Constant<?>) {
      return CompoundIntervalFormulaManager.INSTANCE.asConstant(pMultiply.accept(pEvaluationVisitor, environment));
    }
    // Multiplication by 1 yields the other factor,
    // by -1 the negated other factor, by 0 it yields 0.
    Constant<CompoundInterval> c = null;
    InvariantsFormula<CompoundInterval> otherFactor = null;
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
        BigInteger value = state.getValue();
        if (value.equals(BigInteger.ONE)) {
          return otherFactor;
        }
        if (value.equals(BigInteger.valueOf(-1))) {
          return CompoundIntervalFormulaManager.INSTANCE.negate(otherFactor);
        }
        if (value.equals(BigInteger.ZERO)) {
          return CompoundIntervalFormulaManager.INSTANCE.asConstant(CompoundInterval.singleton(BigInteger.ZERO));
        }
      }
    }
    if (factor1 == pMultiply.getFactor1() && factor2 == pMultiply.getFactor2()) {
      return pMultiply;
    }
    return CompoundIntervalFormulaManager.INSTANCE.multiply(factor1, factor2);
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(ShiftLeft<CompoundInterval> pShiftLeft, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    InvariantsFormula<CompoundInterval> shifted = pShiftLeft.getShifted().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundInterval> shiftDistance = pShiftLeft.getShiftDistance().accept(this, pEvaluationVisitor);
    if (shifted instanceof Constant<?> && shiftDistance instanceof Constant<?>) {
      return CompoundIntervalFormulaManager.INSTANCE.asConstant(pShiftLeft.accept(pEvaluationVisitor, environment));
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
    return CompoundIntervalFormulaManager.INSTANCE.shiftLeft(shifted, shiftDistance);
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(ShiftRight<CompoundInterval> pShiftRight, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    InvariantsFormula<CompoundInterval> shifted = pShiftRight.getShifted().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundInterval> shiftDistance = pShiftRight.getShiftDistance().accept(this, pEvaluationVisitor);
    if (shifted instanceof Constant<?> && shiftDistance instanceof Constant<?>) {
      return CompoundIntervalFormulaManager.INSTANCE.asConstant(pShiftRight.accept(pEvaluationVisitor, environment));
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
    return CompoundIntervalFormulaManager.INSTANCE.shiftRight(shifted, shiftDistance);
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(Union<CompoundInterval> pUnion, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    InvariantsFormula<CompoundInterval> operand1 = pUnion.getOperand1().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundInterval> operand2 = pUnion.getOperand2().accept(this, pEvaluationVisitor);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      return CompoundIntervalFormulaManager.INSTANCE.asConstant(pUnion.accept(pEvaluationVisitor, environment));
    }
    // Union with top yields top
    if (operand1.equals(TOP) || operand2.equals(TOP)) {
      return TOP;
    }
    // Union with bottom yields the other operand
    if (operand1.equals(BOTTOM)) {
      return operand2;
    }
    if (operand2.equals(BOTTOM)) {
      return operand1;
    }
    // Try reducing nested unions by temporarily representing them as a set
    Set<InvariantsFormula<CompoundInterval>> atomicUnionParts = new HashSet<>();
    Queue<InvariantsFormula<CompoundInterval>> unionParts = new ArrayDeque<>();
    CompoundInterval constantPart = CompoundInterval.bottom();
    unionParts.offer(pUnion);
    int partsFound = 0;
    while (!unionParts.isEmpty()) {
      InvariantsFormula<CompoundInterval> currentPart = unionParts.poll();
      if (currentPart instanceof Union<?>) {
        Union<CompoundInterval> currentUnion = (Union<CompoundInterval>) currentPart;
        unionParts.add(currentUnion.getOperand1());
        unionParts.add(currentUnion.getOperand2());
        partsFound += 2;
      } else if (currentPart instanceof Constant<?>) {
        constantPart = constantPart.unionWith(((Constant<CompoundInterval>) currentPart).getValue());
      } else {
        atomicUnionParts.add(currentPart);
      }
    }
    if (partsFound > atomicUnionParts.size()) {
      InvariantsFormula<CompoundInterval> result = null;
      if (atomicUnionParts.size() > 0) {
        Iterator<InvariantsFormula<CompoundInterval>> atomicUnionPartsIterator = atomicUnionParts.iterator();
        result = atomicUnionPartsIterator.next();
        while (atomicUnionPartsIterator.hasNext()) {
          result = CompoundIntervalFormulaManager.INSTANCE.union(result, atomicUnionPartsIterator.next());
        }
      }
      if (!constantPart.isBottom()) {
        InvariantsFormula<CompoundInterval> constantPartFormula = CompoundIntervalFormulaManager.INSTANCE.asConstant(constantPart);
        result = result == null ? constantPartFormula : CompoundIntervalFormulaManager.INSTANCE.union(result, constantPartFormula);
      }
      if (result != null) {
        return result;
      }
    }

    if (operand1 == pUnion.getOperand1() && operand2 == pUnion.getOperand2()) {
      return pUnion;
    }
    return CompoundIntervalFormulaManager.INSTANCE.union(operand1, operand2);
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(Variable<CompoundInterval> pVariable, FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    CompoundInterval value = pVariable.accept(pEvaluationVisitor, this.environment);
    if (value.isSingleton()) {
      return CompoundIntervalFormulaManager.INSTANCE.asConstant(value);
    }
    return pVariable;
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(Exclusion<CompoundInterval> pExclusion,
      FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    CompoundInterval value = pExclusion.accept(pEvaluationVisitor, this.environment);
    if (value.isSingleton()) {
      return CompoundIntervalFormulaManager.INSTANCE.asConstant(value);
    }
    return pExclusion;
  }

}
