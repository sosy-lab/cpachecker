// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.simpleformulas;

public final class Constant implements Term {

  private int mValue;

  public Constant(int pValue) {
    mValue = pValue;
  }

  public int getValue() {
    return mValue;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }

    if (pOther == null) {
      return false;
    }

    if (!pOther.getClass().equals(getClass())) {
      return false;
    }

    Constant lConstant = (Constant) pOther;

    return mValue == lConstant.mValue;
  }

  @Override
  public int hashCode() {
    return mValue;
  }

  @Override
  public String toString() {
    return "" + mValue;
  }

  @Override
  public <T> T accept(TermVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }
}
