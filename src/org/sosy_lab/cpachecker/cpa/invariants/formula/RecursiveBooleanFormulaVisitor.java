// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

/** Instances of extending classes traverse a structure of invariants formula in post order. */
abstract class RecursiveBooleanFormulaVisitor<T>
    implements BooleanFormulaVisitor<T, BooleanFormula<T>> {

  private final RecursiveNumeralFormulaVisitor<T> recursiveNumeralFormulaVisitor;

  protected RecursiveBooleanFormulaVisitor() {
    this(null);
  }

  protected RecursiveBooleanFormulaVisitor(
      RecursiveNumeralFormulaVisitor<T> pRecursiveNumeralFormulaVisitor) {
    this.recursiveNumeralFormulaVisitor = pRecursiveNumeralFormulaVisitor;
  }

  /**
   * Visits the (possibly modified) formula after its child formulae were visited by this visitor.
   *
   * @param pFormula the formula to visit.
   * @return the (possible modified) visited formula.
   */
  protected abstract BooleanFormula<T> visitPost(BooleanFormula<T> pFormula);

  private NumeralFormula<T> visitNumeralChildIfVisitorExists(NumeralFormula<T> pChild) {
    return recursiveNumeralFormulaVisitor == null
        ? pChild
        : pChild.accept(recursiveNumeralFormulaVisitor);
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
