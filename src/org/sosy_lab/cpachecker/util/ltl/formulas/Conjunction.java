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

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;

public final class Conjunction extends PropositionalFormula {

  public Conjunction(Iterable<? extends LtlFormula> conjuncts) {
    super(conjuncts);
  }

  public Conjunction(LtlFormula... conjuncts) {
    super(conjuncts);
  }

  public static LtlFormula of(LtlFormula left, LtlFormula right) {
    return of(Arrays.asList(left, right));
  }

  public static LtlFormula of(LtlFormula... formulas) {
    return of(Arrays.asList(formulas));
  }

  public static LtlFormula of(Iterable<? extends LtlFormula> iterable) {
    ImmutableSet.Builder<LtlFormula> builder = ImmutableSet.builder();

    for (LtlFormula child : iterable) {

      if (child == BooleanConstant.FALSE) {
        return BooleanConstant.FALSE;
      }

      if (child == BooleanConstant.TRUE) {
        continue;
      }

      if (child instanceof Conjunction) {
        builder.addAll(((Conjunction) child).children);
      } else {
        builder.add(child);
      }
    }

    ImmutableSet<LtlFormula> set = builder.build();

    if (set.isEmpty()) {
      return BooleanConstant.TRUE;
    }

    if (set.size() == 1) {
      return set.iterator().next();
    }

    if (set.stream().anyMatch(x -> set.contains(x.not()))) {
      return BooleanConstant.FALSE;
    }

    return new Conjunction(set);
  }

  @Override
  public LtlFormula not() {
    ImmutableSet.Builder<LtlFormula> builder = ImmutableSet.builder();

    for (LtlFormula child : children) {
      builder.add(child.not());
    }

    ImmutableSet<LtlFormula> set = builder.build();
    return new Disjunction(set);
  }

  @Override
  protected String getSymbol() {
    return "&&";
  }
}
