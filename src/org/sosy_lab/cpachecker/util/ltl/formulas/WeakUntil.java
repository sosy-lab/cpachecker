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

import org.sosy_lab.cpachecker.util.ltl.LtlFormulaVisitor;

/** Weak Until. */
public final class WeakUntil extends BinaryFormula {

  public WeakUntil(LtlFormula pLeft, LtlFormula pRight) {
    super(pLeft, pRight);
  }

  public static LtlFormula of(LtlFormula pLeft, LtlFormula pRight) {
    if (pLeft == BooleanConstant.TRUE || pRight == BooleanConstant.TRUE) {
      return BooleanConstant.TRUE;
    }

    if (pLeft == BooleanConstant.FALSE) {
      return pRight;
    }

    if (pLeft.equals(pRight)) {
      return pLeft;
    }

    if (pRight == BooleanConstant.FALSE) {
      return Globally.of(pLeft);
    }

    if (pLeft instanceof Globally) {
      return Disjunction.of(pLeft, pRight);
    }

    return new WeakUntil(pLeft, pRight);
  }

  @Override
  public String getSymbol() {
    return "W";
  }

  @Override
  public StrongRelease not() {
    return new StrongRelease(getLeft().not(), getRight().not());
  }

  @Override
  public String accept(LtlFormulaVisitor v) {
    return v.visit(this);
  }
}
