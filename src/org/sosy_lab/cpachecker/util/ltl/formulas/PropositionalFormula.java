// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ltl.formulas;

import com.google.common.collect.ImmutableList;
import java.util.stream.Collectors;

public abstract class PropositionalFormula implements LtlFormula {

  private final ImmutableList<? extends LtlFormula> children;

  PropositionalFormula(Iterable<? extends LtlFormula> pChildren) {
    children = ImmutableList.copyOf(pChildren);
  }

  PropositionalFormula(LtlFormula... pChildren) {
    children = ImmutableList.copyOf(pChildren);
  }

  public ImmutableList<? extends LtlFormula> getChildren() {
    return children;
  }

  @Override
  public final int hashCode() {
    final int prime = 31;
    return prime + children.hashCode();
  }

  @Override
  public final boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    return obj instanceof PropositionalFormula other
        && getSymbol().equals(other.getSymbol())
        && children.equals(other.children);
  }

  public abstract String getSymbol();

  @Override
  public String toString() {
    return children.stream()
        .map(Object::toString)
        .collect(Collectors.joining(" " + getSymbol() + " ", "(", ")"));
  }
}
