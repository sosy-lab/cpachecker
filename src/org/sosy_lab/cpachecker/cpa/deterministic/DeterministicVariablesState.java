// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.deterministic;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Equivalence.Wrapper;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.LiveVariables;

public class DeterministicVariablesState
    implements LatticeAbstractState<DeterministicVariablesState>, Graphable {

  private final Set<Wrapper<ASimpleDeclaration>> deterministicVariables;

  DeterministicVariablesState() {
    deterministicVariables = new HashSet<>();
  }

  DeterministicVariablesState(final Set<Wrapper<ASimpleDeclaration>> pDeterministicVariables) {
    checkNotNull(pDeterministicVariables);
    deterministicVariables = pDeterministicVariables;
  }

  boolean isDeterministic(Wrapper<ASimpleDeclaration> variableName) {
    return deterministicVariables.contains(variableName);
  }

  DeterministicVariablesState addDeterministicVariable(
      Wrapper<ASimpleDeclaration> pDeterministicVariable) {
    checkNotNull(pDeterministicVariable);

    if (deterministicVariables.contains(pDeterministicVariable)) {
      return this;
    }

    Set<Wrapper<ASimpleDeclaration>> newState = new HashSet<>(deterministicVariables);
    newState.add(pDeterministicVariable);

    return new DeterministicVariablesState(newState);
  }

  DeterministicVariablesState addDeterministicVariables(
      Collection<Wrapper<ASimpleDeclaration>> pDeterministicVariables) {
    checkNotNull(pDeterministicVariables);

    if (pDeterministicVariables.isEmpty()
        || deterministicVariables.containsAll(pDeterministicVariables)) {
      return this;
    }

    Set<Wrapper<ASimpleDeclaration>> newState = new HashSet<>(deterministicVariables);
    newState.addAll(pDeterministicVariables);

    return new DeterministicVariablesState(newState);
  }

  DeterministicVariablesState removeDeterministicVariable(
      Wrapper<ASimpleDeclaration> pNonDeterministicVariable) {
    checkNotNull(pNonDeterministicVariable);

    if (!deterministicVariables.contains(pNonDeterministicVariable)) {
      return this;
    }

    Set<Wrapper<ASimpleDeclaration>> newState = new HashSet<>(deterministicVariables);
    newState.remove(pNonDeterministicVariable);

    return new DeterministicVariablesState(newState);
  }

  DeterministicVariablesState removeDeterministicVariables(
      Collection<Wrapper<ASimpleDeclaration>> pNonDeterministicVariables) {
    checkNotNull(pNonDeterministicVariables);

    Set<Wrapper<ASimpleDeclaration>> newState = new HashSet<>(deterministicVariables);
    newState.removeAll(pNonDeterministicVariables);

    return new DeterministicVariablesState(newState);
  }

  @Override
  public String toString() {
    return deterministicVariables.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(deterministicVariables);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof DeterministicVariablesState)) {
      return false;
    }

    DeterministicVariablesState other = (DeterministicVariablesState) obj;

    return Objects.equals(deterministicVariables, other.deterministicVariables);
  }

  @Override
  public DeterministicVariablesState join(DeterministicVariablesState pOther) {
    ImmutableSet<Wrapper<ASimpleDeclaration>> deterministicVariablesInboth =
        Sets.intersection(deterministicVariables, pOther.deterministicVariables).immutableCopy();

    if (deterministicVariablesInboth.equals(pOther.deterministicVariables)) {
      return pOther;
    }

    return new DeterministicVariablesState(deterministicVariablesInboth);
  }

  @Override
  public boolean isLessOrEqual(DeterministicVariablesState pOther)
      throws CPAException, InterruptedException {

    if (deterministicVariables.size() < pOther.deterministicVariables.size()) {
      return false;
    }

    return deterministicVariables.containsAll(pOther.deterministicVariables);
  }

  @Override
  public String toDOTLabel() {
    StringBuilder sb = new StringBuilder();

    sb.append("[");
    Joiner.on(", ")
        .appendTo(
            sb, from(deterministicVariables).transform(LiveVariables.FROM_EQUIV_WRAPPER_TO_STRING));
    sb.append("]");

    return sb.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }
}
