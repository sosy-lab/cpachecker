// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

class ContainsOnlyEnvInfoVisitor<T> extends DefaultNumeralFormulaVisitor<T, Boolean>
    implements BooleanFormulaVisitor<T, Boolean> {

  private final CollectVarsVisitor<T> collectVarsVisitor = new CollectVarsVisitor<>();

  @Override
  public Boolean visit(Equal<T> pEqual) {
    return isPushedExactly(pEqual.getOperand1(), pEqual.getOperand2());
  }

  @Override
  public Boolean visit(LessThan<T> pLessThan) {
    return isPushedExactly(pLessThan.getOperand1(), pLessThan.getOperand2());
  }

  /**
   * Pushing a relation onto the environment yields exactly the values that satisfy it only if its
   * single variable occurs on one side, not e.g. in {@code INT_MAX - a < a}.
   */
  private boolean isPushedExactly(NumeralFormula<T> pOperand1, NumeralFormula<T> pOperand2) {
    return pOperand1.accept(collectVarsVisitor).size() + pOperand2.accept(collectVarsVisitor).size()
        == 1;
  }

  @Override
  public Boolean visit(LogicalAnd<T> pAnd) {
    return pAnd.getOperand1().accept(this) && pAnd.getOperand2().accept(this);
  }

  @Override
  public Boolean visit(LogicalNot<T> pNot) {
    return pNot.getNegated().accept(this);
  }

  @Override
  public Boolean visitFalse() {
    return true;
  }

  @Override
  public Boolean visitTrue() {
    return true;
  }

  @Override
  public Boolean visit(Union<T> pUnion) {
    return pUnion.getOperand1().accept(this) && pUnion.getOperand2().accept(this);
  }

  @Override
  protected Boolean visitDefault(NumeralFormula<T> pFormula) {
    return false;
  }
}
