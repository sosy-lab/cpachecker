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
package org.sosy_lab.cpachecker.cpa.livevar;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;

import java.util.Collection;
import java.util.Objects;

import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.LiveVariables;

import com.google.common.base.Equivalence.Wrapper;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;


public class LiveVariablesState implements LatticeAbstractState<LiveVariablesState>, Graphable {

  private final ImmutableSet<Wrapper<ASimpleDeclaration>> liveVars;

  public LiveVariablesState() {
    liveVars = ImmutableSet.of();
  }

  public LiveVariablesState(final ImmutableSet<Wrapper<ASimpleDeclaration>> pLiveVariables) {
    checkNotNull(pLiveVariables);
    liveVars = pLiveVariables;
  }

  public LiveVariablesState union(LiveVariablesState pState2) {
    if (isSubsetOf(pState2)) {
      return pState2;
    } else if (pState2.isSubsetOf(this)) {
      return this;
    }

    Builder<Wrapper<ASimpleDeclaration>> builder = ImmutableSet.builder();
    builder.addAll(liveVars);
    builder.addAll(pState2.liveVars);

    return new LiveVariablesState(builder.build());
  }

  public boolean isSubsetOf(LiveVariablesState pState2) {
    return pState2.liveVars.containsAll(liveVars);
  }

  public boolean contains(Wrapper<ASimpleDeclaration> variableName) {
    return liveVars.contains(variableName);
  }

  public LiveVariablesState addLiveVariables(Collection<Wrapper<ASimpleDeclaration>> pLiveVariables) {
    checkNotNull(pLiveVariables);

    if (pLiveVariables.isEmpty()
        || liveVars.containsAll(pLiveVariables)) {
      return this;
    }

    Builder<Wrapper<ASimpleDeclaration>> builder = ImmutableSet.builder();
    builder.addAll(liveVars);
    builder.addAll(pLiveVariables);

    return new LiveVariablesState(builder.build());
  }

  public LiveVariablesState removeLiveVariables(Collection<Wrapper<ASimpleDeclaration>> pNonLiveVariables) {
    checkNotNull(pNonLiveVariables);

    if (pNonLiveVariables.isEmpty()) {
      return this;
    }

    Builder<Wrapper<ASimpleDeclaration>> builder = ImmutableSet.builder();
    for (Wrapper<ASimpleDeclaration> liveVar : liveVars) {
      if (!pNonLiveVariables.contains(liveVar)) {
        builder.add(liveVar);
      }
    }

    return new LiveVariablesState(builder.build());
  }

  public LiveVariablesState removeAndAddLiveVariables(Collection<Wrapper<ASimpleDeclaration>> pNonLiveVariables,
                                                      Collection<Wrapper<ASimpleDeclaration>> pLiveVariables) {
    checkNotNull(pLiveVariables);
    checkNotNull(pNonLiveVariables);

    if (pLiveVariables.isEmpty()) {
      return removeLiveVariables(pNonLiveVariables);
    }

    if (pNonLiveVariables.isEmpty()
        || pLiveVariables.containsAll(pNonLiveVariables)) {
      return addLiveVariables(pLiveVariables);
    }

    Builder<Wrapper<ASimpleDeclaration>> builder = ImmutableSet.builder();
    for (Wrapper<ASimpleDeclaration> liveVar : liveVars) {
      if (!pNonLiveVariables.contains(liveVar) || pLiveVariables.contains(liveVar)) {
        builder.add(liveVar);
      }
    }

    builder.addAll(pLiveVariables);

    return new LiveVariablesState(builder.build());
  }

  @Override
  public String toString() {
    return liveVars.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(liveVars);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof LiveVariablesState)) {
      return false;
    }

    LiveVariablesState other = (LiveVariablesState) obj;

    return Objects.equals(liveVars, other.liveVars);
  }

  @Override
  public LiveVariablesState join(LiveVariablesState pOther) {
    return union(pOther);
  }

  @Override
  public boolean isLessOrEqual(LiveVariablesState pOther) throws CPAException, InterruptedException {
    return isSubsetOf(pOther);
  }

  @Override
  public String toDOTLabel() {
    StringBuilder sb = new StringBuilder();

    sb.append("[");
    Joiner.on(", ").appendTo(sb, from(liveVars).transform(LiveVariables.FROM_EQUIV_WRAPPER_TO_STRING));
    sb.append("]");

    return sb.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  public Iterable<Wrapper<ASimpleDeclaration>> getLiveVariables() {
    return liveVars;
  }
}
