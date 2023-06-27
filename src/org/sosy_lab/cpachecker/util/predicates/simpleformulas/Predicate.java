// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.simpleformulas;

import com.google.common.base.Preconditions;

public final class Predicate {

  public enum Comparison {
    GREATER_OR_EQUAL(">="),
    GREATER(">"),
    EQUAL("=="),
    LESS_OR_EQUAL("<="),
    LESS("<"),
    NOT_EQUAL("!="),
    ;

    private final String symbol;

    Comparison(String pSymbol) {
      symbol = pSymbol;
    }

    public String operatorSymbol() {
      return symbol;
    }
  }

  private final Comparison mComparison;
  private final Term mLeftTerm;
  private final Term mRightTerm;
  private final String mString;

  public Predicate(Term pLeftTerm, Comparison pComparison, Term pRightTerm) {
    mLeftTerm = Preconditions.checkNotNull(pLeftTerm);
    mComparison = Preconditions.checkNotNull(pComparison);
    mRightTerm = Preconditions.checkNotNull(pRightTerm);

    mString = mLeftTerm + " " + mComparison.operatorSymbol() + " " + mRightTerm;
  }

  public Predicate negate() {
    Comparison lComparison =
        switch (mComparison) {
          case GREATER_OR_EQUAL -> Comparison.LESS;
          case GREATER -> Comparison.LESS_OR_EQUAL;
          case EQUAL -> Comparison.NOT_EQUAL;
          case LESS_OR_EQUAL -> Comparison.GREATER;
          case LESS -> Comparison.GREATER_OR_EQUAL;
          case NOT_EQUAL -> Comparison.EQUAL;
        };

    return new Predicate(mLeftTerm, lComparison, mRightTerm);
  }

  public Comparison getComparison() {
    return mComparison;
  }

  public Term getLeftTerm() {
    return mLeftTerm;
  }

  public Term getRightTerm() {
    return mRightTerm;
  }

  @Override
  public String toString() {
    return mString;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther == null || pOther.getClass() != getClass()) {
      return false;
    }
    Predicate lOther = (Predicate) pOther;

    return mLeftTerm.equals(lOther.mLeftTerm)
        && mRightTerm.equals(lOther.mRightTerm)
        && mComparison.equals(lOther.mComparison);
  }

  @Override
  public int hashCode() {
    return 3045820 + mLeftTerm.hashCode() + mComparison.hashCode() + mRightTerm.hashCode();
  }
}
