/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.nondeterminism;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.SortedMapDifference;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public abstract class NondeterminismState implements LatticeAbstractState<NondeterminismState> {

  private NondeterminismState() {}

  public abstract Set<String> getUnconstrainedNondetVariables();

  public abstract Set<String> getBlockUnconstrainedNondetVariables();

  public static class NondeterminismNonAbstractionState extends NondeterminismState {

    private final PersistentSortedMap<String, Object> unconstrainedNondetVariables;

    NondeterminismNonAbstractionState() {
      unconstrainedNondetVariables = PathCopyingPersistentTreeMap.of();
    }

    private NondeterminismNonAbstractionState(
        PersistentSortedMap<String, Object> pUnconstrainedNondetVariables) {
      unconstrainedNondetVariables = Objects.requireNonNull(pUnconstrainedNondetVariables);
    }

    private NondeterminismNonAbstractionState(Map<String, Object> pUnconstrainedNondetVariables) {
      if (pUnconstrainedNondetVariables instanceof PersistentSortedMap) {
        unconstrainedNondetVariables =
            (PersistentSortedMap<String, Object>) pUnconstrainedNondetVariables;
      } else {
        unconstrainedNondetVariables =
            PathCopyingPersistentTreeMap.copyOf(pUnconstrainedNondetVariables);
      }
    }

    public NondeterminismNonAbstractionState addUnconstrainedNondetVariable(String pVariable) {
      Objects.requireNonNull(pVariable);
      return addUnconstrainedNondetVariables(Collections.singleton(pVariable));
    }

    public NondeterminismNonAbstractionState addUnconstrainedNondetVariables(
        Set<String> pVariables) {
      PersistentSortedMap<String, Object> extended = unconstrainedNondetVariables;
      for (String variable : pVariables) {
        extended = extended.putAndCopy(variable, NondeterminismState.class);
      }
      if (extended == unconstrainedNondetVariables) {
        return this;
      }
      return new NondeterminismNonAbstractionState(extended);
    }

    public NondeterminismNonAbstractionState removeUnconstrainedNondetVariable(String pVariable) {
      return removeUnconstrainedNondetVariables(Collections.singleton(pVariable));
    }

    public NondeterminismNonAbstractionState removeUnconstrainedNondetVariables(
        Set<String> pVariables) {
      PersistentSortedMap<String, Object> remaining = unconstrainedNondetVariables;
      for (String variable : pVariables) {
        remaining = remaining.removeAndCopy(variable);
      }
      if (remaining == unconstrainedNondetVariables) {
        return this;
      }
      return new NondeterminismNonAbstractionState(remaining);
    }

    @Override
    public NondeterminismState join(NondeterminismState pOther) {
      if (pOther instanceof NondeterminismNonAbstractionState) {
        NondeterminismNonAbstractionState other = (NondeterminismNonAbstractionState) pOther;
        SortedMapDifference<String, Object> diff =
            Maps.difference(unconstrainedNondetVariables, other.unconstrainedNondetVariables);
        if (diff.entriesOnlyOnLeft().isEmpty()) {
          return this;
        }
        return new NondeterminismNonAbstractionState(diff.entriesInCommon());
      }
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isLessOrEqual(NondeterminismState pOther)
        throws CPAException, InterruptedException {
      return getUnconstrainedNondetVariables()
          .containsAll(pOther.getUnconstrainedNondetVariables());
    }

    @Override
    public Set<String> getUnconstrainedNondetVariables() {
      return unconstrainedNondetVariables.keySet();
    }

    @Override
    public int hashCode() {
      return unconstrainedNondetVariables.hashCode();
    }

    @Override
    public boolean equals(Object pObj) {
      if (pObj == this) {
        return true;
      }
      if (pObj instanceof NondeterminismNonAbstractionState) {
        NondeterminismNonAbstractionState other = (NondeterminismNonAbstractionState) pObj;
        return unconstrainedNondetVariables.equals(other.unconstrainedNondetVariables);
      }
      return super.equals(pObj);
    }

    @Override
    public String toString() {
      return "Unconstrained nondeterministic variables " + getUnconstrainedNondetVariables();
    }

    @Override
    public Set<String> getBlockUnconstrainedNondetVariables() {
      return getUnconstrainedNondetVariables();
    }
  }

  public static class NondeterminismAbstractionState extends NondeterminismState {

    private final Set<String> unconstrainedNondetVariablesPreAbstraction;

    NondeterminismAbstractionState(Set<String> pUnconstrainedNondetVariablesPreAbstraction) {
      unconstrainedNondetVariablesPreAbstraction =
          ImmutableSet.copyOf(pUnconstrainedNondetVariablesPreAbstraction);
    }

    @Override
    public NondeterminismState join(NondeterminismState pOther) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isLessOrEqual(NondeterminismState pOther)
        throws CPAException, InterruptedException {
      return pOther instanceof NondeterminismAbstractionState;
    }

    @Override
    public Set<String> getUnconstrainedNondetVariables() {
      return Collections.emptySet();
    }

    public Set<String> getUnconstrainedNondetVariablesPreAbstraction() {
      return unconstrainedNondetVariablesPreAbstraction;
    }

    @Override
    public int hashCode() {
      return unconstrainedNondetVariablesPreAbstraction.hashCode();
    }

    @Override
    public boolean equals(Object pObj) {
      if (pObj == this) {
        return true;
      }
      if (pObj instanceof NondeterminismAbstractionState) {
        NondeterminismAbstractionState other = (NondeterminismAbstractionState) pObj;
        return unconstrainedNondetVariablesPreAbstraction.equals(
            other.unconstrainedNondetVariablesPreAbstraction);
      }
      return super.equals(pObj);
    }

    @Override
    public String toString() {
      return "Abstraction of unconstrained nondeterministic variables "
          + unconstrainedNondetVariablesPreAbstraction;
    }

    @Override
    public Set<String> getBlockUnconstrainedNondetVariables() {
      return unconstrainedNondetVariablesPreAbstraction;
    }
  }
}
