/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.ltl.formulas;

import static java.util.Objects.requireNonNull;

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
    this.atom = pOther.atom;
    this.negated = !pOther.negated;
    this.negation = pOther;
  }

  public Literal(String pName) {
    this(pName, false);
  }

  public Literal(String pName, boolean pNegated) {
    requireNonNull(pName);

    this.atom = pName;
    this.negated = pNegated;
    this.negation = new Literal(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((atom == null) ? 0 : atom.hashCode());
    result = prime * result + (negated ? 1231 : 1237);
    return result;
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
    Literal other = (Literal) obj;
    if (atom == null) {
      if (other.atom != null) {
        return false;
      }
    } else if (!atom.equals(other.atom)) {
      return false;
    }
    if (negated != other.negated) {
      return false;
    }
    return true;
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
