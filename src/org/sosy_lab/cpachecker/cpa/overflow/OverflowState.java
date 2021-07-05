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

/**
 * Abstract state for tracking overflows.
 */
public final class OverflowState
    implements AbstractStateWithAssumptions,
    Graphable,
    AbstractQueryableState {

  private final ImmutableSet<? extends AExpression> assumptions;
  private final OverflowState parent;
  private final boolean nextHasOverflow;
  private final boolean nextHasUnderflow;
  private static final String PROPERTY_OVERFLOW = "overflow";
  private static final String PROPERTY_UNDERFLOW = "underflow";

  public OverflowState(
      Set<? extends AExpression> pAssumptions,
      boolean pNextHasOverflow,
      boolean pNextHasUnderflow) {
    this(pAssumptions, pNextHasOverflow, pNextHasUnderflow, null);
  }

  public OverflowState(
      Set<? extends AExpression> pAssumptions,
      boolean pNextHasOverflow,
      boolean pNextHasUnderflow,
      OverflowState pParent) {
    assumptions = ImmutableSet.copyOf(pAssumptions);
    parent = pParent;
    nextHasOverflow = pNextHasOverflow;
    nextHasUnderflow = pNextHasUnderflow;
  }

  public boolean hasOverflow() {
    return parent != null && parent.nextHasOverflow;
  }

  public boolean nextHasOverflow() {
    return nextHasOverflow;
  }

  public boolean hasUnderflow() {
    return parent != null && parent.nextHasUnderflow;
  }

  public boolean nextHasUnderflow() {
    return nextHasUnderflow;
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
    return Objects.hash(assumptions, nextHasOverflow, nextHasUnderflow);
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
    return nextHasOverflow == that.nextHasOverflow
        && nextHasUnderflow == that.nextHasUnderflow
        && assumptions.equals(that.assumptions);
  }

  @Override
  public String toString() {
    return "OverflowState{assumeEdges=["
        + getReadableAssumptions()
        + "], nextHasOverflow="
        + nextHasOverflow
        + ", nextHasUnderflow="
        + nextHasUnderflow
        + '}';
  }

  @Override
  public String toDOTLabel() {
    if (hasOverflow() || hasUnderflow()) {
      return "Assumptions:\n" + getReadableAssumptions(this).replaceAll(", ", "\n");
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

    switch (pProperty) {
      case PROPERTY_OVERFLOW:
        return hasOverflow();

      case PROPERTY_UNDERFLOW:
        return hasUnderflow();

      default:
        throw new InvalidQueryException("Query '" + pProperty + "' is invalid.");
    }
  }
}
