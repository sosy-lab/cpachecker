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

import java.util.Collection;

import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;


public class LiveVariablesState implements LatticeAbstractState<LiveVariablesState>, Graphable {

  private final ImmutableSet<String> liveVars;

  public LiveVariablesState() {
    liveVars = ImmutableSet.of();
  }

  public LiveVariablesState(final ImmutableSet<String> pLiveVariables) {
    liveVars = pLiveVariables;
  }

  public LiveVariablesState union(LiveVariablesState pState2) {
    if (isSubsetOf(pState2)) { return pState2; }
    Builder<String> builder = ImmutableSet.builder();
    return new LiveVariablesState(builder.addAll(liveVars).addAll(pState2.liveVars).build());
  }

  public boolean isSubsetOf(LiveVariablesState pState2) {
    return pState2.liveVars.containsAll(liveVars);
  }

  public LiveVariablesState addLiveVariable(String pLiveVariable) {
    if (pLiveVariable == null || liveVars.contains(pLiveVariable)) { return this; }
    return new LiveVariablesState(ImmutableSet.<String> builder().addAll(liveVars).add(pLiveVariable).build());
  }

  public LiveVariablesState addLiveVariables(Collection<? extends String> pLiveVariables) {
    if (pLiveVariables == null || pLiveVariables.size() == 0 || liveVars.containsAll(pLiveVariables)) { return this; }
    return new LiveVariablesState(ImmutableSet.<String> builder().addAll(liveVars).addAll(pLiveVariables).build());
  }

  public LiveVariablesState removeLiveVariable(String pNonLiveVariable) {
    if (pNonLiveVariable == null || !liveVars.contains(pNonLiveVariable)) { return this; }
    Builder<String> builder = ImmutableSet.builder();
    for (String liveVar : liveVars) {
      if (!liveVar.equals(pNonLiveVariable)) {
        builder.add(liveVar);
      }
    }

    return new LiveVariablesState(builder.build());
  }

  public LiveVariablesState removeLiveVariables(Collection<? extends String> pNonLiveVariables) {
    if (pNonLiveVariables == null || pNonLiveVariables.size() == 0) { return this; }
    Builder<String> builder = ImmutableSet.builder();
    for (String liveVar : liveVars) {
      if (!pNonLiveVariables.contains(liveVar)) {
        builder.add(liveVar);
      }
    }

    return new LiveVariablesState(builder.build());
  }

  public LiveVariablesState removeAndAddLiveVariables(String pNonLiveVariable, String pLiveVariable) {
    if (pNonLiveVariable == null || pNonLiveVariable.equals(pLiveVariable)) { return addLiveVariable(pLiveVariable); }
    if (pLiveVariable == null) { return removeLiveVariable(pNonLiveVariable); }

    Builder<String> builder = ImmutableSet.builder();
    for (String liveVar : liveVars) {
      if (!pNonLiveVariable.equals(liveVar) || liveVar.equals(pLiveVariable)) {
        builder.add(liveVar);
      }
    }

    builder.add(pLiveVariable);

    return new LiveVariablesState(builder.build());
  }

  public LiveVariablesState removeAndAddLiveVariables(String pNonLiveVariable,
      Collection<? extends String> pLiveVariables) {
    if (pLiveVariables == null || pLiveVariables.size() == 0) { return removeLiveVariable(pNonLiveVariable); }
    if (pNonLiveVariable == null || pLiveVariables.contains(pNonLiveVariable)) { return addLiveVariables(pLiveVariables); }

    Builder<String> builder = ImmutableSet.builder();
    for (String liveVar : liveVars) {
      if (!pNonLiveVariable.equals(liveVar) || pLiveVariables.contains(liveVar)) {
        builder.add(liveVar);
      }
    }

    builder.addAll(pLiveVariables);

    return new LiveVariablesState(builder.build());
  }

  public LiveVariablesState removeAndAddLiveVariables(Collection<? extends String> pNonLiveVariables,
      String pLiveVariable) {
    if (pLiveVariable == null) { return removeLiveVariables(pNonLiveVariables); }
    if (pNonLiveVariables == null || pNonLiveVariables.size() == 0) { return addLiveVariable(pLiveVariable); }

    Builder<String> builder = ImmutableSet.builder();
    for (String liveVar : liveVars) {
      if (!pNonLiveVariables.contains(liveVar) || pLiveVariable.equals(liveVar)) {
        builder.add(liveVar);
      }
    }

    builder.add(pLiveVariable);

    return new LiveVariablesState(builder.build());
  }

  public LiveVariablesState removeAndAddLiveVariables(Collection<? extends String> pNonLiveVariables,
      Collection<? extends String> pLiveVariables) {
    if (pLiveVariables == null || pLiveVariables.size() == 0) { return removeLiveVariables(pNonLiveVariables); }
    if (pNonLiveVariables == null || pNonLiveVariables.size() == 0 || pLiveVariables.containsAll(pNonLiveVariables)) { return addLiveVariables(pLiveVariables); }

    Builder<String> builder = ImmutableSet.builder();
    for (String liveVar : liveVars) {
      if (!pNonLiveVariables.contains(liveVar) || pLiveVariables.contains(liveVar)) {
        builder.add(liveVar);
      }
    }

    builder.addAll(pLiveVariables);

    return new LiveVariablesState(builder.build());
  }

  @Override
  public String toString() {
    return liveVars == null ? "" : liveVars.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((liveVars == null) ? 0 : liveVars.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    LiveVariablesState other = (LiveVariablesState) obj;
    if (liveVars == null) {
      if (other.liveVars != null) {
        return false;
      }
    } else if (!liveVars.equals(other.liveVars)) {
      return false;
    }
    return true;
  }

  @Override
  public LiveVariablesState join(LiveVariablesState pOther) throws CPAException {
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
    Joiner.on(", ").appendTo(sb, liveVars);
    sb.append("]");

    return sb.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

}
