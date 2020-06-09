// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.overflow;

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
final class OverflowState
    implements AbstractStateWithAssumptions,
    Graphable,
    AbstractQueryableState {

  private final ImmutableSet<? extends AExpression> assumptions;
  private final boolean hasOverflow;
  private static final String PROPERTY_OVERFLOW = "overflow";
  private ImmutableSet<AbstractState> previousStates;
  private ImmutableSet<AbstractState> currentStates;
  private boolean alreadyStrengthened;

  public OverflowState(Set<? extends AExpression> pAssumptions, boolean pHasOverflow) {
    this(pAssumptions, pHasOverflow, null);
  }

  public OverflowState(
      Set<? extends AExpression> pAssumptions, boolean pHasOverflow, OverflowState parent) {
    assumptions = ImmutableSet.copyOf(pAssumptions);
    hasOverflow = pHasOverflow;
    previousStates = null;
    if (parent != null) {
      currentStates = parent.currentStates;
    } else {
      currentStates = ImmutableSet.of();
    }
  }

  public boolean hasOverflow() {
    return hasOverflow;
  }

  @Override
  public List<? extends AExpression> getAssumptions() {
    return ImmutableList.of();
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
    OverflowState that = (OverflowState) pO;
    return hasOverflow == that.hasOverflow && Objects.equals(assumptions, that.assumptions);
  }

  @Override
  public String toString() {
    return "OverflowState{" + ", assumeEdges=" + getReadableAssumptions() + ", hasOverflow="
        + hasOverflow + '}';
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
    return "OverflowCPA";
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    if (pProperty.equals(PROPERTY_OVERFLOW)) {
      return hasOverflow;
    }
    throw new InvalidQueryException("Query '" + pProperty + "' is invalid.");
  }

  @Override
  public Set<AbstractState> getStatesForPreconditions() {
    if (alreadyStrengthened) {
      assert (previousStates != null)
          : "Expected state information to be not null after strengthening!";
      return previousStates;
    } else {
      return currentStates;
    }
  }

  @Override
  public Set<? extends AExpression> getPreconditionAssumptions() {
    return assumptions;
  }

  void updateStatesForPreconditions(Iterable<AbstractState> pCurrentStates) {
    if (!alreadyStrengthened) {
      previousStates = currentStates;
      // update current states while deliberately removing "this".
      // Other states may get hold of this set via getStatesForPreconditions().
      // We want to prevent infinite recursion and accelerate garbage collection.
      currentStates = FluentIterable.from(pCurrentStates).filter(x -> !x.equals(this)).toSet();
      alreadyStrengthened = true;
    }
  }
}
