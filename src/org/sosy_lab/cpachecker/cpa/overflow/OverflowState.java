// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.overflow;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

/** Abstract state for tracking overflows. */
public final class OverflowState
    implements AbstractStateWithAssumptions, Graphable, AbstractQueryableState {

  private final ImmutableSet<? extends AExpression> assumptions;
  private final OverflowState parent;
  private final boolean nextHasOverflow;
  private static final String PROPERTY_OVERFLOW = "overflow";

  public OverflowState(Set<? extends AExpression> pAssumptions, boolean pNextHasOverflow) {
    this(pAssumptions, pNextHasOverflow, null);
  }

  public OverflowState(
      Set<? extends AExpression> pAssumptions, boolean pNextHasOverflow, OverflowState pParent) {
    assumptions = ImmutableSet.copyOf(pAssumptions);
    parent = pParent;
    nextHasOverflow = pNextHasOverflow;
  }

  public boolean hasOverflow() {
    return parent != null && parent.nextHasOverflow;
  }

  public boolean nextHasOverflow() {
    return nextHasOverflow;
  }

  public AbstractStateWithAssumptions getParent() {
    return parent;
  }

  @Override
  public List<? extends AExpression> getAssumptions() {
    return assumptions.asList();
  }

  @Override
  public int hashCode() {
    return Objects.hash(assumptions, nextHasOverflow);
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }
    OverflowState that = (OverflowState) pO;
    return nextHasOverflow == that.nextHasOverflow && assumptions.equals(that.assumptions);
  }

  @Override
  public String toString() {
    return "OverflowState{assumeEdges=["
        + getReadableAssumptions()
        + "], nextHasOverflow="
        + nextHasOverflow
        + '}';
  }

  @Override
  public String toDOTLabel() {
    if (hasOverflow()) {
      return "Assumptions:\n" + getReadableAssumptions(this).replace(", ", "\n");
    }
    return "";
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  private String getReadableAssumptions() {
    return getReadableAssumptions(this);
  }

  private static String getReadableAssumptions(OverflowState s) {
    StringBuilder sb = new StringBuilder();
    Joiner.on(", ").appendTo(sb, s.assumptions.stream().map(x -> x.toASTString()).iterator());
    return sb.toString();
  }

  @Override
  public String getCPAName() {
    return "OverflowCPA";
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    if (pProperty.equals(PROPERTY_OVERFLOW)) {
      return hasOverflow();
    }
    throw new InvalidQueryException("Query '" + pProperty + "' is invalid.");
  }
}
