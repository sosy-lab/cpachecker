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

public abstract class BinaryFormula implements LtlFormula {

  private final LtlFormula left;
  private final LtlFormula right;

  BinaryFormula(LtlFormula pLeft, LtlFormula pRight) {
    left = requireNonNull(pLeft);
    right = requireNonNull(pRight);
  }

  public LtlFormula getLeft() {
    return left;
  }

  public LtlFormula getRight() {
    return right;
  }

  @Override
  public final int hashCode() {
    return Objects.hash(left, right, getSymbol());
  }

  @Override
  public final boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof BinaryFormula)) {
      return false;
    }
    BinaryFormula other = (BinaryFormula) obj;
    return getSymbol().equals(other.getSymbol())
        && left.equals(other.left)
        && right.equals(other.right);
  }

  public abstract String getSymbol();

  @Override
  public String toString() {
    return String.format("(%s %s %s)", left, getSymbol(), right);
  }
}
