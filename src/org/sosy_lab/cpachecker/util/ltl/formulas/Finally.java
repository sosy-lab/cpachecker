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

/** Finally. */
public final class Finally extends UnaryFormula {

  public Finally(Formula f) {
    super(f);
  }

  public static Formula of(Formula operand) {
    if (operand instanceof BooleanConstant) {
      return operand;
    }

    if (operand instanceof Finally) {
      return operand;
    }

    if (operand instanceof Globally && ((Globally) operand).operand instanceof Finally) {
      return operand;
    }

    if (operand instanceof Until) {
      return of(((Until) operand).right);
    }

    if (operand instanceof Disjunction) {
      ImmutableList.Builder<Formula> builder = ImmutableList.builder();

      for (Formula child : ((Disjunction) operand).children) {
        builder.add(Finally.of(child));
      }

      ImmutableList<Formula> list = builder.build();

      return new Disjunction(list);
    }

    return new Finally(operand);
  }

  @Override
  public char getSymbol() {
    return 'F';
  }

  @Override
  public Globally not() {
    return new Globally(operand.not());
  }
}
