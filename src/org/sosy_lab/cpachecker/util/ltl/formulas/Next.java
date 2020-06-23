// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ltl.formulas;

import org.sosy_lab.cpachecker.util.ltl.LtlFormulaVisitor;

/** Next. */
public final class Next extends UnaryFormula {

  public Next(LtlFormula pFormula) {
    super(pFormula);
  }

  public static LtlFormula of(LtlFormula pOperand) {
    return of(pOperand, 1);
  }

  public static LtlFormula of(LtlFormula pOperand, int pCount) {
    if (pOperand instanceof BooleanConstant) {
      return pOperand;
    }

    LtlFormula ltlFormula = pOperand;

    for (int i = 0; i < pCount; i++) {
      ltlFormula = new Next(ltlFormula);
    }

    return ltlFormula;
  }

  @Override
  public String getSymbol() {
    return "X";
  }

  @Override
  public LtlFormula not() {
    return new Next(getOperand().not());
  }

  @Override
  public String accept(LtlFormulaVisitor v) {
    return v.visit(this);
  }
}
