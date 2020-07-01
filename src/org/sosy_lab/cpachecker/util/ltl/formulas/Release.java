// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ltl.formulas;

import org.sosy_lab.cpachecker.util.ltl.LtlFormulaVisitor;

/** Weak Release. */
public final class Release extends BinaryFormula {

  public Release(LtlFormula pLeft, LtlFormula pRight) {
    super(pLeft, pRight);
  }

  public static LtlFormula of(LtlFormula pLeft, LtlFormula pRight) {
    if (pLeft == BooleanConstant.TRUE || pRight instanceof BooleanConstant) {
      return pRight;
    }

    if (pLeft.equals(pRight)) {
      return pLeft;
    }

    if (pLeft == BooleanConstant.FALSE) {
      return Globally.of(pRight);
    }

    if (pRight instanceof Globally) {
      return pRight;
    }

    return new Release(pLeft, pRight);
  }

  @Override
  public String getSymbol() {
    return "R";
  }

  @Override
  public Until not() {
    return new Until(getLeft().not(), getRight().not());
  }

  @Override
  public String accept(LtlFormulaVisitor v) {
    return v.visit(this);
  }
}
