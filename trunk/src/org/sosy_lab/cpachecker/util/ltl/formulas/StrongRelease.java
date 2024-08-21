// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ltl.formulas;

import org.sosy_lab.cpachecker.util.ltl.LtlFormulaVisitor;

/** Strong Release. */
public final class StrongRelease extends BinaryFormula {

  public StrongRelease(LtlFormula pLeft, LtlFormula pRight) {
    super(pLeft, pRight);
  }

  public static LtlFormula of(LtlFormula pLeft, LtlFormula pRight) {
    if (pLeft == BooleanConstant.FALSE || pRight == BooleanConstant.FALSE) {
      return BooleanConstant.FALSE;
    }

    if (pLeft == BooleanConstant.TRUE) {
      return pRight;
    }

    if (pLeft.equals(pRight)) {
      return pLeft;
    }

    if (pRight == BooleanConstant.TRUE) {
      return Finally.of(pLeft);
    }

    return new StrongRelease(pLeft, pRight);
  }

  @Override
  public String getSymbol() {
    return "S";
  }

  @Override
  public WeakUntil not() {
    return new WeakUntil(getLeft().not(), getRight().not());
  }

  @Override
  public String accept(LtlFormulaVisitor v) {
    return v.visit(this);
  }
}
