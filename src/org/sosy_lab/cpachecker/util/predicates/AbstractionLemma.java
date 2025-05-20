// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates;

import com.google.common.collect.ImmutableSet;
import org.checkerframework.checker.units.qual.A;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class AbstractionLemma {
  private final String identifier;
  private final ImmutableSet<BooleanFormula> formulas;

  public AbstractionLemma(String pIdentifier, Iterable<BooleanFormula> pFormulas) {
    identifier = pIdentifier;
    formulas = ImmutableSet.copyOf(pFormulas);
  }

  public AbstractionLemma addFormulas(Iterable<BooleanFormula> pFormulas) {
    ImmutableSet.Builder<BooleanFormula> newFormulas = ImmutableSet.builder();
    newFormulas.addAll(this.formulas).addAll(pFormulas);
    return new AbstractionLemma(this.identifier, newFormulas.build());
  }

  public String getIdentifier() {
    return identifier;
  }

  public ImmutableSet<BooleanFormula> getFormulas() {
    return formulas;
  }
}
