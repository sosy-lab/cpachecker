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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

/** Abstract state for tracking overflows. */
public final class OverflowState
    implements AbstractStateWithAssumptions, Graphable, AbstractQueryableState {

  private final ImmutableSet<? extends AExpression> assumptions;
  private final OverflowState parent;
  private final @Nullable CFAEdge overflowingNextEdge; // overflows if the assumptions hold
  private final boolean hasOverflow; // whether the edge that led to this state overflows
  private static final String PROPERTY_OVERFLOW = "overflow";

  public OverflowState(
      Set<? extends AExpression> pAssumptions,
      @Nullable CFAEdge pOverflowingNextEdge,
      OverflowState pParent,
      boolean pHasOverflow) {
    assumptions = ImmutableSet.copyOf(pAssumptions);
    overflowingNextEdge = pOverflowingNextEdge;
    parent = pParent;
    hasOverflow = pHasOverflow;
  }

  /** Whether the edge that led to this state overflows, i.e., whether this is a target state. */
  public boolean hasOverflow() {
    return hasOverflow;
  }

  /** Whether the assumptions say that the edge {@link #getOverflowingNextEdge()} overflows. */
  public boolean nextHasOverflow() {
    return overflowingNextEdge != null;
  }

  /**
   * The edge that overflows if the assumptions of this state hold. Returns null if the assumptions
   * do not say that an edge overflows, e.g., for the no-overflow successors of the lookahead, for
   * states after terminal edges, for target states, and for the initial state.
   */
  public @Nullable CFAEdge getOverflowingNextEdge() {
    return overflowingNextEdge;
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
    return Objects.hash(assumptions, overflowingNextEdge, hasOverflow);
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
    return hasOverflow == that.hasOverflow
        && Objects.equals(overflowingNextEdge, that.overflowingNextEdge)
        && assumptions.equals(that.assumptions);
  }

  @Override
  public String toString() {
    return "OverflowState{assumeEdges=["
        + getReadableAssumptions()
        + "], overflowingNextEdge="
        + overflowingNextEdge
        + ", hasOverflow="
        + hasOverflow
        + '}';
  }

  @Override
  public String toDOTLabel() {
    if (hasOverflow() && parent != null) {
      return "Assumptions:\n" + getReadableAssumptions(parent).replace(", ", "\n");
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
    Joiner.on(", ").appendTo(sb, s.assumptions.stream().map(AExpression::toASTString).iterator());
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
