/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.simpleformulas;

import com.google.common.base.Preconditions;

public class Predicate {

  public static enum Comparison {
    GREATER_OR_EQUAL(">="),
    GREATER         (">"),
    EQUAL           ("=="),
    LESS_OR_EQUAL   ("<="),
    LESS            ("<"),
    NOT_EQUAL       ("!="),
    ;

    private final String symbol;

    private Comparison(String pSymbol) {
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

    mString = mLeftTerm.toString() + " " + mComparison.operatorSymbol() + " " + mRightTerm.toString();
  }

  public Predicate negate() {
    Comparison lComparison = null;

    switch (mComparison) {
    case GREATER_OR_EQUAL:
      lComparison = Comparison.LESS;
      break;
    case GREATER:
      lComparison = Comparison.LESS_OR_EQUAL;
      break;
    case EQUAL:
      lComparison = Comparison.NOT_EQUAL;
      break;
    case LESS_OR_EQUAL:
      lComparison = Comparison.GREATER;
      break;
    case LESS:
      lComparison = Comparison.GREATER_OR_EQUAL;
      break;
    case NOT_EQUAL:
      lComparison = Comparison.EQUAL;
      break;
    default:
      throw new AssertionError();
    }

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

    if (pOther == null) {
      return false;
    }

    if (pOther.getClass() == getClass()) {
      Predicate lOther = (Predicate)pOther;

      return (mLeftTerm.equals(lOther.mLeftTerm) && mRightTerm.equals(lOther.mRightTerm) && mComparison.equals(lOther.mComparison));
    }

    return false;
  }

  @Override
  public int hashCode() {
    return 3045820 + mLeftTerm.hashCode() + mComparison.hashCode() + mRightTerm.hashCode();
  }

}
