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

    if (pOperand instanceof Finally f && f.getOperand() instanceof Globally) {
      return pOperand;
    }

    if (pOperand instanceof Release release) {
      return of(release.getRight());
    }

    if (pOperand instanceof Conjunction conjunction) {
      return new Conjunction(
          Collections3.transformedImmutableListCopy(conjunction.getChildren(), Globally::of));
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
