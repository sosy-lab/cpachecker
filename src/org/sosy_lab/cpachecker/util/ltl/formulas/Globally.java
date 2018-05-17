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

import com.google.common.collect.ImmutableList;

/** Globally. */
public final class Globally extends UnaryFormula {

  public Globally(LtlFormula f) {
    super(f);
  }

  public static LtlFormula of(LtlFormula operand) {
    if (operand instanceof BooleanConstant) {
      return operand;
    }

    if (operand instanceof Globally) {
      return operand;
    }

    if (operand instanceof Finally && ((Finally) operand).operand instanceof Globally) {
      return operand;
    }

    if (operand instanceof Release) {
      return of(((Release) operand).right);
    }

    if (operand instanceof Conjunction) {
      ImmutableList.Builder<LtlFormula> builder = ImmutableList.builder();

      for (LtlFormula child : ((Conjunction) operand).children) {
        builder.add(Globally.of(child));
      }

      ImmutableList<LtlFormula> list = builder.build();

      return new Conjunction(list);
    }

    return new Globally(operand);
  }

  @Override
  public char getSymbol() {
    return 'G';
  }

  @Override
  public UnaryFormula not() {
    return new Finally(operand.not());
  }
}
