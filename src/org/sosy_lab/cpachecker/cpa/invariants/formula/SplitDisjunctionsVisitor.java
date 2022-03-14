// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Instances of this class are invariants formula visitors used to split the visited formulae on
 * their outer conjunctions into lists of the conjunction operands. This is done recursively so that
 * nested conjunctions are split as well.
 *
 * @param <T> the type of the constants used in the formulae.
 */
class SplitDisjunctionsVisitor<T>
    implements BooleanFormulaVisitor<T, ImmutableList<BooleanFormula<T>>> {

  @Override
  public ImmutableList<BooleanFormula<T>> visit(Equal<T> pEqual) {
    return ImmutableList.of(pEqual);
  }

  @Override
  public ImmutableList<BooleanFormula<T>> visit(LessThan<T> pLessThan) {
    return ImmutableList.of(pLessThan);
  }

  @Override
  public ImmutableList<BooleanFormula<T>> visit(LogicalAnd<T> pAnd) {
    return ImmutableList.of(pAnd);
  }

  @Override
  public ImmutableList<BooleanFormula<T>> visit(LogicalNot<T> pNot) {
    if (pNot.getNegated() instanceof LogicalAnd<?>) {
      List<BooleanFormula<T>> result = new ArrayList<>();
      LogicalAnd<T> formula = (LogicalAnd<T>) pNot.getNegated();
      BooleanFormula<T> term = formula.getOperand1();
      if (!(term instanceof LogicalNot<?>)) {
        term = LogicalNot.of(term);
      } else {
        term = ((LogicalNot<T>) term).getNegated();
      }
      result.addAll(term.accept(this));
      // If the left operand is true, return true
      if (result.contains(BooleanConstant.<T>getTrue())) {
        return visitTrue();
      }
      term = formula.getOperand2();
      if (!(term instanceof LogicalNot<?>)) {
        term = LogicalNot.of(term);
      } else {
        term = ((LogicalNot<T>) term).getNegated();
      }
      Collection<BooleanFormula<T>> toAdd = term.accept(this);
      // If the right operand is true, return true
      if (toAdd.contains(BooleanConstant.<T>getTrue())) {
        return visitTrue();
      }
      result.addAll(toAdd);
      return ImmutableList.copyOf(result);
    }
    return ImmutableList.of(pNot);
  }

  @Override
  public ImmutableList<BooleanFormula<T>> visitFalse() {
    return ImmutableList.of();
  }

  @Override
  public ImmutableList<BooleanFormula<T>> visitTrue() {
    return ImmutableList.of(BooleanConstant.getTrue());
  }
}
