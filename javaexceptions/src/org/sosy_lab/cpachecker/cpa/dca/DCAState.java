// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.dca;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

public class DCAState
    implements AbstractQueryableState,
        Targetable,
        Graphable,
        Serializable,
        AbstractStateWithAssumptions {

  private static final long serialVersionUID = -3454798281550882095L;

  private final AutomatonState buechiState;
  private final ImmutableList<AutomatonState> compositeStates;

  private final ImmutableList<AutomatonState> productStates;

  public DCAState(AutomatonState pBuechiState, List<AutomatonState> pCompositeStates) {
    buechiState = checkNotNull(pBuechiState);
    compositeStates = ImmutableList.copyOf(pCompositeStates);
    productStates =
        new ImmutableList.Builder<AutomatonState>()
            .add(buechiState)
            .addAll(compositeStates)
            .build();
  }

  @Override
  public boolean isTarget() {
    return !productStates.isEmpty() && productStates.stream().allMatch(AutomatonState::isTarget);
  }

  @Override
  public @NonNull Set<TargetInformation> getTargetInformation() throws IllegalStateException {
    checkArgument(isTarget());
    return productStates.stream()
        .flatMap(x -> x.getTargetInformation().stream())
        .collect(ImmutableSet.toImmutableSet());
  }

  @Override
  public String getCPAName() {
    return "DCAState";
  }

  AutomatonState getBuechiState() {
    return buechiState;
  }

  List<AutomatonState> getCompositeStates() {
    return compositeStates;
  }

  @Override
  public ImmutableList<AExpression> getAssumptions() {
    return productStates.stream()
        .flatMap(x -> x.getAssumptions().stream())
        .distinct()
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public int hashCode() {
    return Objects.hash(buechiState, compositeStates);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DCAState)) {
      return false;
    }
    DCAState other = (DCAState) obj;
    return Objects.equals(buechiState, other.buechiState)
        && Objects.equals(compositeStates, other.compositeStates);
  }

  @Override
  public String toString() {
    if (productStates.isEmpty()) {
      return "_empty_state_";
    }

    return FluentIterable.from(productStates)
        .transform(AutomatonState::toString)
        .filter(x -> !x.contains("init_state"))
        .join(Joiner.on("; "));
  }

  @Override
  public String toDOTLabel() {
    if (productStates.isEmpty()) {
      return "_empty_state_";
    }

    return FluentIterable.from(productStates)
        .transform(AutomatonState::toDOTLabel)
        .filter(x -> !x.contains("init_state"))
        .join(Joiner.on("\n"));
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    if (buechiState.getAssumptions().isEmpty()) {
      return true;
    }

    String predecessorStateBuechiExpressions =
        Joiner.on("; ")
            .join(Collections2.transform(buechiState.getAssumptions(), AExpression::toASTString));

    return predecessorStateBuechiExpressions.equals(pProperty);
  }
}
