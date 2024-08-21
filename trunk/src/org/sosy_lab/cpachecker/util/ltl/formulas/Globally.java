// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ltl.formulas;

import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.cpachecker.util.ltl.LtlFormulaVisitor;

/** Globally. */
public final class Globally extends UnaryFormula {

  public Globally(LtlFormula f) {
    super(f);
  }

  public static LtlFormula of(LtlFormula pOperand) {
    if (pOperand instanceof BooleanConstant) {
      return pOperand;
    }

    if (pOperand instanceof Globally) {
      return pOperand;
    }

    if (pOperand instanceof Finally && ((Finally) pOperand).getOperand() instanceof Globally) {
      return pOperand;
    }

    if (pOperand instanceof Release) {
      return of(((Release) pOperand).getRight());
    }

    if (pOperand instanceof Conjunction) {
      return new Conjunction(
          Collections3.transformedImmutableListCopy(
              ((Conjunction) pOperand).getChildren(), Globally::of));
    }

    return new Globally(pOperand);
  }

  @Override
  public String getSymbol() {
    return "G";
  }

  @Override
  public UnaryFormula not() {
    return new Finally(getOperand().not());
  }

  @Override
  public String accept(LtlFormulaVisitor v) {
    return v.visit(this);
  }
}
