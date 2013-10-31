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

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.sosy_lab.cpachecker.cpa.invariants.CompoundState;

/**
 * The singleton instance of this class is a compound state invariants formula
 * visitor used to partially evaluate compound state invariants formulae to
 * eliminate complex expressions consisting only of constants.
 */
public class PartialEvaluator implements ParameterizedInvariantsFormulaVisitor<CompoundState, FormulaEvaluationVisitor<CompoundState>, InvariantsFormula<CompoundState>> {

  /**
   * An invariants formula representing the top state.
   */
  private static final InvariantsFormula<CompoundState> TOP =
      CompoundStateFormulaManager.INSTANCE.asConstant(CompoundState.top());

  /**
   * An invariants formula representing the bottom state.
   */
  private static final InvariantsFormula<CompoundState> BOTTOM =
      CompoundStateFormulaManager.INSTANCE.asConstant(CompoundState.bottom());

  /**
   * A visitor for compound state invariants formulae used to determine whether
   * or not the visited formula is a genuine boolean formula.
   */
  private static final IsBooleanFormulaVisitor<CompoundState> IS_BOOLEAN_FORMULA_VISITOR =
      new IsBooleanFormulaVisitor<>();

  /**
   * A map representing an empty environment; since only constants are
   * evaluated, no real environment is required.
   */
  private final Map<? extends String, ? extends InvariantsFormula<CompoundState>> environment;

  public PartialEvaluator() {
    this(Collections.<String, InvariantsFormula<CompoundState>>emptyMap());
  }

  public PartialEvaluator(Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    this.environment = pEnvironment;
  }

  @Override
  public InvariantsFormula<CompoundState> visit(Add<CompoundState> pAdd, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> summand1 = pAdd.getSummand1().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundState> summand2 = pAdd.getSummand2().accept(this, pEvaluationVisitor);
    // If both summands are constants, calculate a new constant
    if (summand1 instanceof Constant<?> && summand2 instanceof Constant<?>) {
      return CompoundStateFormulaManager.INSTANCE.asConstant(pAdd.accept(pEvaluationVisitor, environment));
    }
    // If one of the summands is constant zero, return the other summand
    // If one of the summands is top or bottom, return it
    Constant<CompoundState> c = null;
    InvariantsFormula<CompoundState> other = null;
    if (summand1 instanceof Constant<?>) {
      c = (Constant<CompoundState>) summand1;
      other = summand2;
    } else if (summand2 instanceof Constant<?>) {
      c = (Constant<CompoundState>) summand2;
      other = summand1;
    }
    if (c != null && other != null) {
      CompoundState value = c.getValue();
      if (value.isSingleton() && value.getValue().equals(BigInteger.ZERO)) {
        return other;
      }
      if (value.isTop() || value.isBottom()) {
        return c;
      }
      PushSummandVisitor<CompoundState> psv = new PushSummandVisitor<>(pEvaluationVisitor);
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
    return CompoundStateFormulaManager.INSTANCE.add(summand1, summand2);
  }

  private static boolean isDefinitelyBottom(InvariantsFormula<CompoundState> pFormula) {
    if (pFormula instanceof Constant) {
      Constant<CompoundState> constant = (Constant<CompoundState>) pFormula;
      return constant.getValue().isBottom();
    }
    return false;
  }

  private static boolean isDefinitelyTop(InvariantsFormula<CompoundState> pFormula) {
    if (pFormula instanceof Constant) {
      Constant<CompoundState> constant = (Constant<CompoundState>) pFormula;
      return constant.getValue().isTop();
    }
    return false;
  }

  private static InvariantsFormula<CompoundState> extractBottomOrTop(InvariantsFormula<CompoundState> pFormula1, InvariantsFormula<CompoundState> pFormula2) {
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
  public InvariantsFormula<CompoundState> visit(BinaryAnd<CompoundState> pAnd, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> operand1 = pAnd.getOperand1().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundState> operand2 = pAnd.getOperand2().accept(this, pEvaluationVisitor);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      return CompoundStateFormulaManager.INSTANCE.asConstant(pAnd.accept(pEvaluationVisitor, environment));
    }
    if (operand1 == pAnd.getOperand1() && operand2 == pAnd.getOperand2()) {
      return pAnd;
    }
    return CompoundStateFormulaManager.INSTANCE.binaryAnd(operand1, operand2);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(BinaryNot<CompoundState> pNot, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> operand = pNot.getFlipped().accept(this, pEvaluationVisitor);
    if (operand instanceof Constant<?>) {
      return CompoundStateFormulaManager.INSTANCE.asConstant(pNot.accept(pEvaluationVisitor, environment));
    }
    if (operand == pNot.getFlipped()) {
      return pNot;
    }
    return CompoundStateFormulaManager.INSTANCE.binaryNot(operand);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(BinaryOr<CompoundState> pOr, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> operand1 = pOr.getOperand1().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundState> operand2 = pOr.getOperand2().accept(this, pEvaluationVisitor);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      return CompoundStateFormulaManager.INSTANCE.asConstant(pOr.accept(pEvaluationVisitor, environment));
    }
    if (operand1 == pOr.getOperand1() && operand2 == pOr.getOperand2()) {
      return pOr;
    }
    return CompoundStateFormulaManager.INSTANCE.binaryOr(operand1, operand2);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(BinaryXor<CompoundState> pXor, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> operand1 = pXor.getOperand1().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundState> operand2 = pXor.getOperand2().accept(this, pEvaluationVisitor);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      return CompoundStateFormulaManager.INSTANCE.asConstant(pXor.accept(pEvaluationVisitor, environment));
    }
    if (operand1 == pXor.getOperand1() && operand2 == pXor.getOperand2()) {
      return pXor;
    }
    return CompoundStateFormulaManager.INSTANCE.binaryXor(operand1, operand2);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(Constant<CompoundState> pConstant, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    return pConstant;
  }

  @Override
  public InvariantsFormula<CompoundState> visit(Divide<CompoundState> pDivide, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> numerator = pDivide.getNumerator().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundState> denominator = pDivide.getDenominator().accept(this, pEvaluationVisitor);
    if (numerator instanceof Constant<?> && denominator instanceof Constant<?>) {
      return CompoundStateFormulaManager.INSTANCE.asConstant(pDivide.accept(pEvaluationVisitor, environment));
    }
    // Division by 1 yields the numerator, by -1 the negated numerator
    if (denominator instanceof Constant<?>) {
      Constant<CompoundState> c = (Constant<CompoundState>) denominator;
      CompoundState value = c.getValue();
      if (value.isSingleton()) {
        if (value.getValue().equals(BigInteger.ONE)) {
          return numerator;
        } else if (value.getValue().equals(BigInteger.valueOf(-1))) {
          return CompoundStateFormulaManager.INSTANCE.negate(numerator);
        }
      }
    }
    if (numerator == pDivide.getNumerator() && denominator == pDivide.getDenominator()) {
      return pDivide;
    }
    return CompoundStateFormulaManager.INSTANCE.divide(numerator, denominator);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(Equal<CompoundState> pEqual, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> operand1 = pEqual.getOperand1().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundState> operand2 = pEqual.getOperand2().accept(this, pEvaluationVisitor);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      return CompoundStateFormulaManager.INSTANCE.asConstant(pEqual.accept(pEvaluationVisitor, environment));
    }
    // If one of the operands is bottom, the equation is bottom; if one of them is top, the equation is top
    InvariantsFormula<CompoundState> botOrTop = extractBottomOrTop(operand1, operand2);
    if (botOrTop != null) {
      return botOrTop;
    }
    /*
     * If a boolean formula is operand in an equation with true, the boolean
     * formula itself can be returned, because the "== true" has does not
     * change the value of the expression. Almost the same goes for equations
     * with false, only that the other operand has to be negated then.
     */
    CompoundState c = null;
    InvariantsFormula<CompoundState> other = null;
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
        return CompoundStateFormulaManager.INSTANCE.logicalNot(other).accept(this, pEvaluationVisitor);
      }
    }
    if (operand1 == pEqual.getOperand1() && operand2 == pEqual.getOperand2()) {
      return pEqual;
    }
    return CompoundStateFormulaManager.INSTANCE.equal(operand1, operand2);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(LessThan<CompoundState> pLessThan, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> operand1 = pLessThan.getOperand1().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundState> operand2 = pLessThan.getOperand2().accept(this, pEvaluationVisitor);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      return CompoundStateFormulaManager.INSTANCE.asConstant(pLessThan.accept(pEvaluationVisitor, environment));
    }
    // If one of the operands is bottom, the inequation is bottom; if one of them is top, the inequation is top
    InvariantsFormula<CompoundState> botOrTop = extractBottomOrTop(operand1, operand2);
    if (botOrTop != null) {
      return botOrTop;
    }
    if (operand1 == pLessThan.getOperand1() && operand2 == pLessThan.getOperand2()) {
      return pLessThan;
    }
    return CompoundStateFormulaManager.INSTANCE.lessThan(operand1, operand2);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(LogicalAnd<CompoundState> pAnd, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> operand1 = pAnd.getOperand1().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundState> operand2 = pAnd.getOperand2().accept(this, pEvaluationVisitor);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      return CompoundStateFormulaManager.INSTANCE.asConstant(pAnd.accept(pEvaluationVisitor, environment));
    }
    // If one of the operands is false, return false;
    // if one of the operands is true, return the other one
    // if one of the operands is top, return it
    // if one of the operands is bottom, return it
    InvariantsFormula<CompoundState> constant = null;
    InvariantsFormula<CompoundState> other = null;
    if (operand1 instanceof Constant<?>) {
      constant = operand1;
      other = operand2;
    } else if (operand2 instanceof Constant<?>) {
      constant = operand2;
      other = operand1;
    }
    if (constant != null && other != null) {
      CompoundState constantValue = ((Constant<CompoundState>) constant).getValue();
      if (constantValue.isDefinitelyFalse() || constantValue.isBottom() || constantValue.isTop()) {
        return constant;
      } else if (constantValue.isDefinitelyTrue()) {
        return other;
      }
    }
    if (operand1 == pAnd.getOperand1() && operand2 == pAnd.getOperand2()) {
      return pAnd;
    }
    return CompoundStateFormulaManager.INSTANCE.logicalAnd(operand1, operand2);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(LogicalNot<CompoundState> pNot, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> operand = pNot.getNegated().accept(this, pEvaluationVisitor);
    if (operand instanceof Constant<?>) {
      return CompoundStateFormulaManager.INSTANCE.asConstant(pNot.accept(pEvaluationVisitor, environment));
    }
    // The negation of a negation yields the inner operand
    if (operand instanceof LogicalNot<?>) {
      return ((LogicalNot<CompoundState>) operand).getNegated();
    }
    // The negation of a logical conjunction can be treated as an or with negated operands (De Morgan)
    if (operand instanceof LogicalAnd<?>) {
      LogicalAnd<CompoundState> land = (LogicalAnd<CompoundState>) operand;
      // If the or-operand is definitely true (the and-operand is definitely false), return true
      if (land.getOperand1().accept(pEvaluationVisitor, this.environment).isDefinitelyFalse()) {
        return CompoundStateFormulaManager.INSTANCE.asConstant(CompoundState.logicalTrue());
      }
      if (land.getOperand2().accept(pEvaluationVisitor, this.environment).isDefinitelyFalse()) {
        return CompoundStateFormulaManager.INSTANCE.asConstant(CompoundState.logicalTrue());
      }
      // If one of those operands is definitely false (true in the and-representation), return the other one (negated in the and-representation)
      if (land.getOperand1().accept(pEvaluationVisitor, this.environment).isDefinitelyTrue()) {
        return CompoundStateFormulaManager.INSTANCE.logicalNot(land.getOperand2()).accept(this, pEvaluationVisitor);
      }
      if (land.getOperand2().accept(pEvaluationVisitor, this.environment).isDefinitelyTrue()) {
        return CompoundStateFormulaManager.INSTANCE.logicalNot(land.getOperand1()).accept(this, pEvaluationVisitor);
      }
    }
    if (operand == pNot.getNegated()) {
      return pNot;
    }
    return CompoundStateFormulaManager.INSTANCE.logicalNot(operand);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(Modulo<CompoundState> pModulo, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> numerator = pModulo.getNumerator().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundState> denominator = pModulo.getDenominator().accept(this, pEvaluationVisitor);
    if (numerator instanceof Constant<?> && denominator instanceof Constant<?>) {
      return CompoundStateFormulaManager.INSTANCE.asConstant(pModulo.accept(pEvaluationVisitor, environment));
    }
    // If the denominator is 1 or -1, modulo must yield 0
    if (denominator instanceof Constant<?>) {
      Constant<CompoundState> c = (Constant<CompoundState>) denominator;
      CompoundState value = c.getValue();
      if (value.isSingleton() && (value.getValue().equals(BigInteger.ONE)
          || value.getValue().equals(BigInteger.valueOf(-1)))) {
        return CompoundStateFormulaManager.INSTANCE.asConstant(CompoundState.singleton(1));
      }
    }
    if (numerator == pModulo.getNumerator() && denominator == pModulo.getDenominator()) {
      return pModulo;
    }
    return CompoundStateFormulaManager.INSTANCE.modulo(numerator, denominator);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(Multiply<CompoundState> pMultiply, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> factor1 = pMultiply.getFactor1().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundState> factor2 = pMultiply.getFactor2().accept(this, pEvaluationVisitor);
    if (factor1 instanceof Constant<?> && factor2 instanceof Constant<?>) {
      return CompoundStateFormulaManager.INSTANCE.asConstant(pMultiply.accept(pEvaluationVisitor, environment));
    }
    // Multiplication by 1 yields the other factor,
    // by -1 the negated other factor, by 0 it yields 0.
    Constant<CompoundState> c = null;
    InvariantsFormula<CompoundState> otherFactor = null;
    if (factor1 instanceof Constant<?>) {
      c = (Constant<CompoundState>) factor1;
      otherFactor = factor2;
    } else if (factor2 instanceof Constant<?>) {
      c = (Constant<CompoundState>) factor2;
      otherFactor = factor1;
    }
    if (c != null && otherFactor != null) {
      CompoundState state = c.getValue();
      if (state.isSingleton()) {
        BigInteger value = state.getValue();
        if (value.equals(BigInteger.ONE)) {
          return otherFactor;
        }
        if (value.equals(BigInteger.valueOf(-1))) {
          return CompoundStateFormulaManager.INSTANCE.negate(otherFactor);
        }
        if (value.equals(BigInteger.ZERO)) {
          return CompoundStateFormulaManager.INSTANCE.asConstant(CompoundState.singleton(BigInteger.ZERO));
        }
      }
    }
    if (factor1 == pMultiply.getFactor1() && factor2 == pMultiply.getFactor2()) {
      return pMultiply;
    }
    return CompoundStateFormulaManager.INSTANCE.multiply(factor1, factor2);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(Negate<CompoundState> pNegate, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> operand = pNegate.getNegated().accept(this, pEvaluationVisitor);
    if (operand instanceof Constant<?>) {
      return CompoundStateFormulaManager.INSTANCE.asConstant(pNegate.accept(pEvaluationVisitor, environment));
    }
    if (operand == pNegate.getNegated()) {
      return pNegate;
    }
    return CompoundStateFormulaManager.INSTANCE.negate(operand);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(ShiftLeft<CompoundState> pShiftLeft, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> shifted = pShiftLeft.getShifted().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundState> shiftDistance = pShiftLeft.getShiftDistance().accept(this, pEvaluationVisitor);
    if (shifted instanceof Constant<?> && shiftDistance instanceof Constant<?>) {
      return CompoundStateFormulaManager.INSTANCE.asConstant(pShiftLeft.accept(pEvaluationVisitor, environment));
    }
    // If the shift distance is zero, return the left operand
    if (shiftDistance instanceof Constant<?>) {
      Constant<CompoundState> c = (Constant<CompoundState>) shiftDistance;
      if (c.getValue().isSingleton() && c.getValue().getValue().equals(BigInteger.ZERO)) {
        return shifted;
      }
    }
    if (shifted == pShiftLeft.getShifted() && shiftDistance == pShiftLeft.getShiftDistance()) {
      return pShiftLeft;
    }
    return CompoundStateFormulaManager.INSTANCE.shiftLeft(shifted, shiftDistance);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(ShiftRight<CompoundState> pShiftRight, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> shifted = pShiftRight.getShifted().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundState> shiftDistance = pShiftRight.getShiftDistance().accept(this, pEvaluationVisitor);
    if (shifted instanceof Constant<?> && shiftDistance instanceof Constant<?>) {
      return CompoundStateFormulaManager.INSTANCE.asConstant(pShiftRight.accept(pEvaluationVisitor, environment));
    }
    // If the shift distance is zero, return the left operand
    if (shiftDistance instanceof Constant<?>) {
      Constant<CompoundState> c = (Constant<CompoundState>) shiftDistance;
      if (c.getValue().isSingleton() && c.getValue().getValue().equals(BigInteger.ZERO)) {
        return shifted;
      }
    }
    if (shifted == pShiftRight.getShifted() && shiftDistance == pShiftRight.getShiftDistance()) {
      return pShiftRight;
    }
    return CompoundStateFormulaManager.INSTANCE.shiftRight(shifted, shiftDistance);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(Union<CompoundState> pUnion, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> operand1 = pUnion.getOperand1().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundState> operand2 = pUnion.getOperand2().accept(this, pEvaluationVisitor);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      return CompoundStateFormulaManager.INSTANCE.asConstant(pUnion.accept(pEvaluationVisitor, environment));
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
    Set<InvariantsFormula<CompoundState>> atomicUnionParts = new HashSet<>();
    Queue<InvariantsFormula<CompoundState>> unionParts = new ArrayDeque<>();
    CompoundState constantPart = CompoundState.bottom();
    unionParts.offer(pUnion);
    int partsFound = 0;
    while (!unionParts.isEmpty()) {
      InvariantsFormula<CompoundState> currentPart = unionParts.poll();
      if (currentPart instanceof Union<?>) {
        Union<CompoundState> currentUnion = (Union<CompoundState>) currentPart;
        unionParts.add(currentUnion.getOperand1());
        unionParts.add(currentUnion.getOperand2());
        partsFound += 2;
      } else if (currentPart instanceof Constant<?>) {
        constantPart = constantPart.unionWith(((Constant<CompoundState>) currentPart).getValue());
      } else {
        atomicUnionParts.add(currentPart);
      }
    }
    if (partsFound > atomicUnionParts.size()) {
      InvariantsFormula<CompoundState> result = null;
      if (atomicUnionParts.size() > 0) {
        Iterator<InvariantsFormula<CompoundState>> atomicUnionPartsIterator = atomicUnionParts.iterator();
        result = atomicUnionPartsIterator.next();
        while (atomicUnionPartsIterator.hasNext()) {
          result = CompoundStateFormulaManager.INSTANCE.union(result, atomicUnionPartsIterator.next());
        }
      }
      if (!constantPart.isBottom()) {
        InvariantsFormula<CompoundState> constantPartFormula = CompoundStateFormulaManager.INSTANCE.asConstant(constantPart);
        result = result == null ? constantPartFormula : CompoundStateFormulaManager.INSTANCE.union(result, constantPartFormula);
      }
      if (result != null) {
        return result;
      }
    }

    if (operand1 == pUnion.getOperand1() && operand2 == pUnion.getOperand2()) {
      return pUnion;
    }
    return CompoundStateFormulaManager.INSTANCE.union(operand1, operand2);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(Variable<CompoundState> pVariable, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    CompoundState value = pVariable.accept(pEvaluationVisitor, this.environment);
    if (value.isSingleton()) {
      return CompoundStateFormulaManager.INSTANCE.asConstant(value);
    }
    return pVariable;
  }

}
