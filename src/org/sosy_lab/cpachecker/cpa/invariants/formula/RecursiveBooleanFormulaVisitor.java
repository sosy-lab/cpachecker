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

/**
 * Instances of extending classes traverse a structure of invariants formula
 * in post order.
 */
abstract class RecursiveBooleanFormulaVisitor<T> implements BooleanFormulaVisitor<T, BooleanFormula<T>> {

  private final RecursiveNumeralFormulaVisitor<T> recursiveNumeralFormulaVisitor;

  public RecursiveBooleanFormulaVisitor() {
    this(null);
  }

  public RecursiveBooleanFormulaVisitor(RecursiveNumeralFormulaVisitor<T> pRecursiveNumeralFormulaVisitor) {
    this.recursiveNumeralFormulaVisitor = pRecursiveNumeralFormulaVisitor;
  }

  /**
   * Visits the (possibly modified) formula after its child formulae were
   * visited by this visitor.
   *
   * @param pFormula the formula to visit.
   * @return the (possible modified) visited formula.
   */
  protected abstract BooleanFormula<T> visitPost(BooleanFormula<T> pFormula);

  private NumeralFormula<T> visitNumeralChildIfVisitorExists(NumeralFormula<T> pChild) {
    return recursiveNumeralFormulaVisitor == null ? pChild : pChild.accept(recursiveNumeralFormulaVisitor);
  }

  @Override
  public BooleanFormula<T> visit(Equal<T> pEqual) {
    NumeralFormula<T> operand1 = visitNumeralChildIfVisitorExists(pEqual.getOperand1());
    NumeralFormula<T> operand2 = visitNumeralChildIfVisitorExists(pEqual.getOperand2());
    final BooleanFormula<T> toVisit;
    if (operand1 == pEqual.getOperand1() && operand2 == pEqual.getOperand2()) {
      toVisit = pEqual;
    } else {
      toVisit = Equal.of(operand1, operand2);
    }
    return visitPost(toVisit);
  }

  @Override
  public BooleanFormula<T> visit(LessThan<T> pLessThan) {
    NumeralFormula<T> operand1 = visitNumeralChildIfVisitorExists(pLessThan.getOperand1());
    NumeralFormula<T> operand2 = visitNumeralChildIfVisitorExists(pLessThan.getOperand2());
    final BooleanFormula<T> toVisit;
    if (operand1 == pLessThan.getOperand1() && operand2 == pLessThan.getOperand2()) {
      toVisit = pLessThan;
    } else {
      toVisit = LessThan.of(operand1, operand2);
    }
    return visitPost(toVisit);
  }

  @Override
  public BooleanFormula<T> visit(LogicalAnd<T> pAnd) {
    BooleanFormula<T> operand1 = pAnd.getOperand1().accept(this);
    BooleanFormula<T> operand2 = pAnd.getOperand2().accept(this);
    final BooleanFormula<T> toVisit;
    if (operand1 == pAnd.getOperand1() && operand2 == pAnd.getOperand2()) {
      toVisit = pAnd;
    } else {
      toVisit = LogicalAnd.of(operand1, operand2);
    }
    return visitPost(toVisit);
  }

  @Override
  public BooleanFormula<T> visit(LogicalNot<T> pNot) {
    BooleanFormula<T> operand = pNot.getNegated().accept(this);
    final BooleanFormula<T> toVisit;
    if (operand == pNot.getNegated()) {
      toVisit = pNot;
    } else {
      toVisit = LogicalNot.of(operand);
    }
    return visitPost(toVisit);
  }

  @Override
  public BooleanFormula<T> visitFalse() {
    return visitPost(BooleanConstant.<T>getFalse());
  }

  @Override
  public BooleanFormula<T> visitTrue() {
    return visitPost(BooleanConstant.<T>getTrue());
  }

}
