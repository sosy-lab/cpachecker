// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ltl.formulas;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.core.specification.Property;

public final class LabelledFormula implements Property {

  public static LabelledFormula of(LtlFormula pFormula, List<Literal> pList) {
    return new LabelledFormula(pFormula, pList);
  }

  private final LtlFormula formula;
  private final ImmutableList<Literal> atomicPropositions;

  private LabelledFormula(LtlFormula pFormula, List<Literal> pList) {
    formula = checkNotNull(pFormula);
    atomicPropositions = ImmutableList.copyOf(checkNotNull(pList));
  }

  public LabelledFormula not() {
    return of(formula.not(), atomicPropositions);
  }

  public LtlFormula getFormula() {
    return formula;
  }

  public ImmutableList<Literal> getAPs() {
    return atomicPropositions;
  }

  @Override
  public int hashCode() {
    return Objects.hash(atomicPropositions, formula);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof LabelledFormula)) {
      return false;
    }
    LabelledFormula other = (LabelledFormula) obj;
    return formula.equals(other.formula) && atomicPropositions.equals(other.atomicPropositions);
  }

  @Override
  public String toString() {
    return getFormula().toString();
  }
}
