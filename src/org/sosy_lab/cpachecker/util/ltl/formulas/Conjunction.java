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
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.cpachecker.util.ltl.LtlFormulaVisitor;

public final class Conjunction extends PropositionalFormula {

  public Conjunction(Iterable<? extends LtlFormula> pConjuncts) {
    super(pConjuncts);
  }

  public Conjunction(LtlFormula... pConjuncts) {
    super(pConjuncts);
  }

  public static LtlFormula of(LtlFormula pLeft, LtlFormula pRight) {
    return of(Arrays.asList(pLeft, pRight));
  }

  public static LtlFormula of(LtlFormula... pFormulas) {
    return of(Arrays.asList(pFormulas));
  }

  public static LtlFormula of(Iterable<? extends LtlFormula> pIterable) {
    ImmutableSet.Builder<LtlFormula> builder = ImmutableSet.builder();

    for (LtlFormula child : pIterable) {

      if (child == BooleanConstant.FALSE) {
        return BooleanConstant.FALSE;
      }

      if (child == BooleanConstant.TRUE) {
        continue;
      }

      if (child instanceof Conjunction) {
        builder.addAll(((Conjunction) child).getChildren());
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
    return new Disjunction(
        Collections3.transformedImmutableSetCopy(getChildren(), LtlFormula::not));
  }

  @Override
  public String accept(LtlFormulaVisitor v) {
    return v.visit(this);
  }

  @Override
  public String getSymbol() {
    return "&&";
  }
}
