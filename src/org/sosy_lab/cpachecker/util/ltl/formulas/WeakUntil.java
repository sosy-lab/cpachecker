// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ltl.formulas;

import org.sosy_lab.cpachecker.util.ltl.LtlFormulaVisitor;

/** Weak Until. */
public final class WeakUntil extends BinaryFormula {

  public WeakUntil(LtlFormula pLeft, LtlFormula pRight) {
    super(pLeft, pRight);
  }

  public static LtlFormula of(LtlFormula pLeft, LtlFormula pRight) {
    if (pLeft == BooleanConstant.TRUE || pRight == BooleanConstant.TRUE) {
      return BooleanConstant.TRUE;
    }

    if (pLeft == BooleanConstant.FALSE) {
      return pRight;
    }

    if (pLeft.equals(pRight)) {
      return pLeft;
    }

    if (pRight == BooleanConstant.FALSE) {
      return Globally.of(pLeft);
    }

    if (pLeft instanceof Globally) {
      return Disjunction.of(pLeft, pRight);
    }

    return new WeakUntil(pLeft, pRight);
  }

  @Override
  public String getSymbol() {
    return "W";
  }

  @Override
  public StrongRelease not() {
    return new StrongRelease(getLeft().not(), getRight().not());
  }

  @Override
  public String accept(LtlFormulaVisitor v) {
    return v.visit(this);
  }
}
