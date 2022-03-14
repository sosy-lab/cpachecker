// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ltl.formulas;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.cpachecker.util.ltl.LtlFormulaVisitor;

public final class Disjunction extends PropositionalFormula {

  public Disjunction(Iterable<? extends LtlFormula> pDisjuncts) {
    super(pDisjuncts);
  }

  public Disjunction(LtlFormula... pDisjuncts) {
    super(pDisjuncts);
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

      if (child == BooleanConstant.TRUE) {
        return BooleanConstant.TRUE;
      }

      if (child == BooleanConstant.FALSE) {
        continue;
      }

      if (child instanceof Disjunction) {
        builder.addAll(((Disjunction) child).getChildren());
      } else {
        builder.add(child);
      }
    }

    ImmutableSet<LtlFormula> set = builder.build();

    if (set.isEmpty()) {
      return BooleanConstant.FALSE;
    }

    if (set.size() == 1) {
      return set.iterator().next();
    }

    if (set.stream().anyMatch(x -> set.contains(x.not()))) {
      return BooleanConstant.TRUE;
    }

    return new Disjunction(set);
  }

  @Override
  public LtlFormula not() {
    return new Conjunction(
        Collections3.transformedImmutableSetCopy(getChildren(), LtlFormula::not));
  }

  @Override
  public String accept(LtlFormulaVisitor v) {
    return v.visit(this);
  }

  @Override
  public String getSymbol() {
    return "||";
  }
}
