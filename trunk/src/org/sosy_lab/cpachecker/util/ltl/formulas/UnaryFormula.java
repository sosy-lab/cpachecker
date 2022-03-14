// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ltl.formulas;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

public abstract class UnaryFormula implements LtlFormula {

  private final LtlFormula operand;

  UnaryFormula(LtlFormula pOperand) {
    operand = requireNonNull(pOperand);
  }

  public LtlFormula getOperand() {
    return operand;
  }

  @Override
  public final int hashCode() {
    return Objects.hash(operand, getSymbol());
  }

  @Override
  public final boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof UnaryFormula)) {
      return false;
    }
    UnaryFormula other = (UnaryFormula) obj;
    return getSymbol().equals(other.getSymbol()) && operand.equals(other.operand);
  }

  public abstract String getSymbol();

  @Override
  public String toString() {
    return String.format("%s %s", getSymbol(), operand);
  }
}
