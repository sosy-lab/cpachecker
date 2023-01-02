// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ltl.formulas;

import org.sosy_lab.cpachecker.util.ltl.LtlFormulaVisitor;

public final class BooleanConstant implements LtlFormula {

  public static final BooleanConstant FALSE = new BooleanConstant(false);
  public static final BooleanConstant TRUE = new BooleanConstant(true);

  private final boolean value;

  public static BooleanConstant of(boolean pValue) {
    return pValue ? TRUE : FALSE;
  }

  private BooleanConstant(boolean pValue) {
    value = pValue;
  }

  @Override
  public BooleanConstant not() {
    return value ? FALSE : TRUE;
  }

  @Override
  public String accept(LtlFormulaVisitor v) {
    return v.visit(this);
  }

  @Override
  public int hashCode() {
    return Boolean.hashCode(value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }
    BooleanConstant other = (BooleanConstant) obj;
    return value == other.value;
  }

  @Override
  public String toString() {
    return value ? "true" : "false";
  }
}
