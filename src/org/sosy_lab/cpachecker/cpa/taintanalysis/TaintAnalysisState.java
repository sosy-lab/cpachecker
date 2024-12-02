// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.taintanalysis;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.defaults.SimpleTargetInformation;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class TaintAnalysisState
    implements LatticeAbstractState<TaintAnalysisState>, Targetable, Serializable, Graphable {

  @Serial private static final long serialVersionUID = -7715698130795640052L;

  private boolean violatesProperty = false;
  private Set<CIdExpression> taintedVariables;

  public TaintAnalysisState(Set<CIdExpression> pElements) {
    this.taintedVariables = pElements;
  }

  public Set<CIdExpression> getTaintedVariables() {
    return taintedVariables;
  }

  @Override
  public boolean isLessOrEqual(TaintAnalysisState other) {
    return other.getTaintedVariables().containsAll(this.taintedVariables);
  }

  @Override
  public int hashCode() {
    return Objects.hash(taintedVariables);
  }

  /**
   * Compares the specified object with this TaintAnalysisState for equality. Returns {@code true}
   * if the specified object is also a TaintAnalysisState, and both have equivalent sets of tainted
   * variables.
   *
   * @param obj the object to be compared for equality with this state
   * @return {@code true} if the specified object is equal to this state; {@code false} otherwise
   */
  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    return obj instanceof TaintAnalysisState other
        && Objects.equals(taintedVariables, other.taintedVariables);
  }

  @Override
  public String toString() {
    return toDOTLabel();
  }

  @Override
  public String toDOTLabel() {
    return "{" + this.taintedVariables + "}";
  }

  @Override
  public TaintAnalysisState join(TaintAnalysisState pOther)
      throws CPAException, InterruptedException {
    if (this.isLessOrEqual(pOther)) {
      return pOther;
    } else if (pOther.isLessOrEqual(this)) {
      return this;
    }
    Set<CIdExpression> resSet = new HashSet<>(this.taintedVariables);
    resSet.addAll(this.taintedVariables);
    resSet.addAll(pOther.getTaintedVariables());
    return new TaintAnalysisState(resSet);
  }

  @Override
  public boolean shouldBeHighlighted() {
    return this.isTarget();
  }

  @Override
  public boolean isTarget() {
    return this.violatesProperty;
  }

  @Override
  public Set<TargetInformation> getTargetInformation() throws IllegalStateException {
    if (isTarget()) {
      Set<TargetInformation> resSet = new HashSet<>();
      resSet.add(SimpleTargetInformation.create("Leaking inforation"));
      return resSet;
    } else {
      return new HashSet<>();
    }
  }

  @SuppressWarnings("unused")
  public void setViolatesProperty() {
    violatesProperty = true;
  }
}
