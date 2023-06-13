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
import org.sosy_lab.cpachecker.util.ltl.LtlFormulaVisitor;

public final class Literal implements LtlFormula {

  private final String atom;
  private final boolean negated;
  private final Literal negation;

  public static Literal of(String pName, boolean pNegated) {
    return new Literal(pName, pNegated);
  }

  private Literal(Literal pOther) {
    requireNonNull(pOther);
    atom = pOther.atom;
    negated = !pOther.negated;
    negation = pOther;
  }

  public Literal(String pName) {
    this(pName, false);
  }

  public Literal(String pName, boolean pNegated) {
    requireNonNull(pName);

    atom = pName;
    negated = pNegated;
    negation = new Literal(this);
  }

  @Override
  public int hashCode() {
    return Objects.hash(atom, negated);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof Literal)) {
      return false;
    }
    Literal other = (Literal) obj;
    return atom.equals(other.atom) && negated == other.negated;
  }

  public String getAtom() {
    return atom;
  }

  public boolean isNegated() {
    return negated;
  }

  @Override
  public Literal not() {
    return negation;
  }

  @Override
  public String accept(LtlFormulaVisitor v) {
    return v.visit(this);
  }

  @Override
  public String toString() {
    return isNegated() ? "! " + atom : atom;
  }
}
