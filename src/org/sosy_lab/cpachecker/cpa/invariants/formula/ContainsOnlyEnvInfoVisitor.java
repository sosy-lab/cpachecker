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
    return referencesOneVariableOnOneSide(pEqual.getOperand1(), pEqual.getOperand2());
  }

  @Override
  public Boolean visit(LessThan<T> pLessThan) {
    return referencesOneVariableOnOneSide(pLessThan.getOperand1(), pLessThan.getOperand2());
  }

  /**
   * Checks whether the given operands, taken together, reference exactly one variable, and that
   * variable occurs on only one side of the relation.
   *
   * <p>The environment-based reasoning this check gates works by pushing each operand's value range
   * into the environment independently, which is only sound if the operands don't interact through
   * a shared variable. A formula like {@code INT_MAX - a < a} also references only one distinct
   * variable, but {@code a} occurs on both sides and the two occurrences are not independent, so it
   * must be rejected here even though the naive variable count is 1.
   */
  private boolean referencesOneVariableOnOneSide(
      NumeralFormula<T> pOperand1, NumeralFormula<T> pOperand2) {
    int vars1 = pOperand1.accept(collectVarsVisitor).size();
    int vars2 = pOperand2.accept(collectVarsVisitor).size();
    // A single variable total can't occur on both sides, so this also rejects the case where
    // it's split across them.
    return vars1 + vars2 == 1;
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
