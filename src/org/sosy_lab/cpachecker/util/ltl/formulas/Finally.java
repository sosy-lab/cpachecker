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

/** Finally. */
public final class Finally extends UnaryFormula {

  public Finally(LtlFormula f) {
    super(f);
  }

  public static LtlFormula of(LtlFormula pOperand) {
    if (pOperand instanceof BooleanConstant) {
      return pOperand;
    }

    if (pOperand instanceof Finally) {
      return pOperand;
    }

    if (pOperand instanceof Globally && ((Globally) pOperand).getOperand() instanceof Finally) {
      return pOperand;
    }

    if (pOperand instanceof Until) {
      return of(((Until) pOperand).getRight());
    }

    if (pOperand instanceof Disjunction) {
      return new Disjunction(
          Collections3.transformedImmutableListCopy(
              ((Disjunction) pOperand).getChildren(), Finally::of));
    }

    return new Finally(pOperand);
  }

  @Override
  public String getSymbol() {
    return "F";
  }

  @Override
  public Globally not() {
    return new Globally(getOperand().not());
  }

  @Override
  public String accept(LtlFormulaVisitor v) {
    return v.visit(this);
  }
}
