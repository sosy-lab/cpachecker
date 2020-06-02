// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.overflow2;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

/**
 * Abstract state for tracking overflows.
 */
final class OverflowState2
    implements AbstractStateWithAssumptions,
    Graphable,
    AbstractQueryableState {

  private final ImmutableSet<? extends AExpression> assumptions;
  private final boolean hasOverflow;
  private final boolean nextHasOverflow;
  private static final String PROPERTY_OVERFLOW = "overflow";
  private ImmutableSet<AbstractState> currentStates;
  private boolean alreadyStrengthened;

  public OverflowState2(Set<? extends AExpression> pAssumptions, boolean pHasOverflow, boolean pNextHasOverflow) {
    this(pAssumptions, pHasOverflow, pNextHasOverflow, null);
  }

  public OverflowState2(
      Set<? extends AExpression> pAssumptions, boolean pHasOverflow, boolean pNextHasOverflow, OverflowState2 parent) {
    assumptions = ImmutableSet.copyOf(pAssumptions);
    hasOverflow = pHasOverflow;
    nextHasOverflow = pNextHasOverflow;
    if (parent != null) {
      currentStates = parent.currentStates;
    } else {
      currentStates = ImmutableSet.of();
    }
  }

  public boolean hasOverflow() {
    return hasOverflow;
  }

  public boolean nextHasOverflow() {
    return nextHasOverflow;
  }

  @Override
  public List<? extends AExpression> getAssumptions() {
    return ImmutableList.copyOf(assumptions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(assumptions, hasOverflow);
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }
    OverflowState2
        that = (OverflowState2) pO;
    return nextHasOverflow == that.nextHasOverflow && hasOverflow == that.hasOverflow && Objects.equals(assumptions, that.assumptions);
  }

  @Override
  public String toString() {
    return "OverflowState2{" + ", assumeEdges=" + getReadableAssumptions() + ", hasOverflow="
        + hasOverflow + ", nextHasOverflow=" + nextHasOverflow + '}';
  }

  @Override
  public String toDOTLabel() {
    if (hasOverflow) {
      return Joiner.on('\n').join(getReadableAssumptions());
    }
    return "";
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  private List<String> getReadableAssumptions() {
    return assumptions.stream().map(x -> x.toASTString()).collect(Collectors.toList());
  }

  @Override
  public String getCPAName() {
    return "OverflowCPA2";
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    if (pProperty.equals(PROPERTY_OVERFLOW)) {
      return hasOverflow;
    }
    throw new InvalidQueryException("Query '" + pProperty + "' is invalid.");
  }

  protected void updateStatesForPreconditions(Iterable<AbstractState> pCurrentStates) {
    if (!alreadyStrengthened) {
      // update current states while deliberately removing "this".
      // Other states may get hold of this set via getStatesForPreconditions().
      // We want to prevent infinite recursion and accelerate garbage collection.
      currentStates = FluentIterable.from(pCurrentStates).filter(x -> !Objects.equals(x, this)).toSet();
      alreadyStrengthened = true;
    }
  }
}
