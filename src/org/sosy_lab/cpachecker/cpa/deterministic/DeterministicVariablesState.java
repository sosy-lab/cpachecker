/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.deterministic;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.LiveVariables;

import com.google.common.base.Equivalence.Wrapper;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public class DeterministicVariablesState implements LatticeAbstractState<DeterministicVariablesState>, Graphable {

  private final Set<Wrapper<ASimpleDeclaration>> deterministicVariables;

  DeterministicVariablesState() {
    deterministicVariables = Sets.newHashSet();
  }

  DeterministicVariablesState(final Set<Wrapper<ASimpleDeclaration>> pDeterministicVariables) {
    checkNotNull(pDeterministicVariables);
    deterministicVariables = pDeterministicVariables;
  }

  boolean isDeterministic(Wrapper<ASimpleDeclaration> variableName) {
    return deterministicVariables.contains(variableName);
  }

  DeterministicVariablesState addDeterministicVariable(Wrapper<ASimpleDeclaration> pDeterministicVariable) {
    checkNotNull(pDeterministicVariable);

    if (deterministicVariables.contains(pDeterministicVariable)) {
      return this;
    }

    Set<Wrapper<ASimpleDeclaration>> newState = Sets.newHashSet(deterministicVariables);
    newState.add(pDeterministicVariable);

    return new DeterministicVariablesState(newState);
  }

  DeterministicVariablesState addDeterministicVariables(Collection<Wrapper<ASimpleDeclaration>> pDeterministicVariables) {
    checkNotNull(pDeterministicVariables);

    if (pDeterministicVariables.isEmpty()
        || deterministicVariables.containsAll(pDeterministicVariables)) {
      return this;
    }

    Set<Wrapper<ASimpleDeclaration>> newState = Sets.newHashSet(deterministicVariables);
    newState.addAll(pDeterministicVariables);

    return new DeterministicVariablesState(newState);
  }

  DeterministicVariablesState removeDeterministicVariable(Wrapper<ASimpleDeclaration> pNonDeterministicVariable) {
    checkNotNull(pNonDeterministicVariable);

    if (!deterministicVariables.contains(pNonDeterministicVariable)) {
      return this;
    }

    Set<Wrapper<ASimpleDeclaration>> newState = Sets.newHashSet(deterministicVariables);
    newState.remove(pNonDeterministicVariable);

    return new DeterministicVariablesState(newState);
  }

  DeterministicVariablesState removeDeterministicVariables(Collection<Wrapper<ASimpleDeclaration>> pNonDeterministicVariables) {
    checkNotNull(pNonDeterministicVariables);

    Set<Wrapper<ASimpleDeclaration>> newState = Sets.newHashSet(deterministicVariables);
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
    ImmutableSet<Wrapper<ASimpleDeclaration>> deterministicVariablesInboth = Sets.intersection(deterministicVariables, pOther.deterministicVariables).immutableCopy();

    if (deterministicVariablesInboth.equals(pOther.deterministicVariables)) {
      return pOther;
    }

    return new DeterministicVariablesState(deterministicVariablesInboth);
  }

  @Override
  public boolean isLessOrEqual(DeterministicVariablesState pOther) throws CPAException, InterruptedException {

    if(deterministicVariables.size() < pOther.deterministicVariables.size()) {
      return false;
    }

    return deterministicVariables.containsAll(pOther.deterministicVariables);
  }

  @Override
  public String toDOTLabel() {
    StringBuilder sb = new StringBuilder();

    sb.append("[");
    Joiner.on(", ").appendTo(sb, from(deterministicVariables).transform(LiveVariables.FROM_EQUIV_WRAPPER_TO_STRING));
    sb.append("]");

    return sb.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }
}
