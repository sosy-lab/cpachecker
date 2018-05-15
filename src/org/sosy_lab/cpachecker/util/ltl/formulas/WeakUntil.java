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

/** Weak Until. */
public final class WeakUntil extends BinaryFormula {

  public WeakUntil(Formula left, Formula right) {
    super(left, right);
  }

  public static Formula of(Formula left, Formula right) {
    if (left == BooleanConstant.TRUE || right == BooleanConstant.TRUE) {
      return BooleanConstant.TRUE;
    }

    if (left == BooleanConstant.FALSE) {
      return right;
    }

    if (left.equals(right)) {
      return left;
    }

    if (right == BooleanConstant.FALSE) {
      return Globally.of(left);
    }

    if (left instanceof Globally) {
      return Disjunction.of(left, right);
    }

    return new WeakUntil(left, right);
  }

  @Override
  public char getSymbol() {
    return 'W';
  }

  @Override
  public StrongRelease not() {
    return new StrongRelease(left.not(), right.not());
  }
}
