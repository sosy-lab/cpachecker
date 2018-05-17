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

/** Weak Release. */
public final class Release extends BinaryFormula {

  public Release(LtlFormula left, LtlFormula right) {
    super(left, right);
  }

  public static LtlFormula of(LtlFormula left, LtlFormula right) {
    if (left == BooleanConstant.TRUE || right instanceof BooleanConstant) {
      return right;
    }

    if (left.equals(right)) {
      return left;
    }

    if (left == BooleanConstant.FALSE) {
      return Globally.of(right);
    }

    if (right instanceof Globally) {
      return right;
    }

    return new Release(left, right);
  }

  @Override
  public char getSymbol() {
    return 'R';
  }

  @Override
  public Until not() {
    return new Until(left.not(), right.not());
  }
}
