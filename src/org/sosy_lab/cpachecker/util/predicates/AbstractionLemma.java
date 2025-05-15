// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class AbstractionLemma {
  private final ImmutableSet<BooleanFormula> formulas;

  public AbstractionLemma(Iterable<BooleanFormula> pFormulas) {
    formulas = ImmutableSet.copyOf(pFormulas);
  }

  public ImmutableSet<BooleanFormula> getFormulas() {
    return formulas;
  }
}
