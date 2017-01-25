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

  public abstract Set<String> getNondetVariables();

  public abstract Set<String> getBlockNondetVariables();

  public static class NondeterminismNonAbstractionState extends NondeterminismState {

    private final PersistentSortedMap<String, Object> nondetVariables;

    NondeterminismNonAbstractionState() {
      nondetVariables = PathCopyingPersistentTreeMap.of();
    }

    private NondeterminismNonAbstractionState(
        PersistentSortedMap<String, Object> pNondetVariables) {
      nondetVariables = Objects.requireNonNull(pNondetVariables);
    }

    private NondeterminismNonAbstractionState(Map<String, Object> pNondetVariables) {
      if (pNondetVariables instanceof PersistentSortedMap) {
        nondetVariables = (PersistentSortedMap<String, Object>) pNondetVariables;
      } else {
        nondetVariables = PathCopyingPersistentTreeMap.copyOf(pNondetVariables);
      }
    }

    public NondeterminismNonAbstractionState addNondetVariable(String pVariable) {
      Objects.requireNonNull(pVariable);
      return addNondetVariables(Collections.singleton(pVariable));
    }

    public NondeterminismNonAbstractionState addNondetVariables(Set<String> pVariables) {
      PersistentSortedMap<String, Object> extended = nondetVariables;
      for (String variable : pVariables) {
        extended = extended.putAndCopy(variable, NondeterminismState.class);
      }
      if (extended == nondetVariables) {
        return this;
      }
      return new NondeterminismNonAbstractionState(extended);
    }

    public NondeterminismNonAbstractionState removeNondetVariable(String pVariable) {
      return removeNondetVariables(Collections.singleton(pVariable));
    }

    public NondeterminismNonAbstractionState removeNondetVariables(Set<String> pVariables) {
      PersistentSortedMap<String, Object> remaining = nondetVariables;
      for (String variable : pVariables) {
        remaining = remaining.removeAndCopy(variable);
      }
      if (remaining == nondetVariables) {
        return this;
      }
      return new NondeterminismNonAbstractionState(remaining);
    }

    @Override
    public NondeterminismState join(NondeterminismState pOther) {
      if (pOther instanceof NondeterminismNonAbstractionState) {
        NondeterminismNonAbstractionState other = (NondeterminismNonAbstractionState) pOther;
        SortedMapDifference<String, Object> diff =
            Maps.difference(nondetVariables, other.nondetVariables);
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
      return getNondetVariables().containsAll(pOther.getNondetVariables());
    }

    @Override
    public Set<String> getNondetVariables() {
      return nondetVariables.keySet();
    }

    @Override
    public int hashCode() {
      return nondetVariables.hashCode();
    }

    @Override
    public boolean equals(Object pObj) {
      if (pObj == this) {
        return true;
      }
      if (pObj instanceof NondeterminismNonAbstractionState) {
        NondeterminismNonAbstractionState other = (NondeterminismNonAbstractionState) pObj;
        return nondetVariables.equals(other.nondetVariables);
      }
      return super.equals(pObj);
    }

    @Override
    public String toString() {
      return "Nondeterministic variables " + getNondetVariables();
    }

    @Override
    public Set<String> getBlockNondetVariables() {
      return getNondetVariables();
    }
  }

  public static class NondeterminismAbstractionState extends NondeterminismState {

    private final Set<String> nondetVariablesPreAbstraction;

    NondeterminismAbstractionState(Set<String> pNondetVariablesPreAbstraction) {
      nondetVariablesPreAbstraction = ImmutableSet.copyOf(pNondetVariablesPreAbstraction);
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
    public Set<String> getNondetVariables() {
      return Collections.emptySet();
    }

    public Set<String> getNondetVariablesPreAbstraction() {
      return nondetVariablesPreAbstraction;
    }

    @Override
    public int hashCode() {
      return nondetVariablesPreAbstraction.hashCode();
    }

    @Override
    public boolean equals(Object pObj) {
      if (pObj == this) {
        return true;
      }
      if (pObj instanceof NondeterminismAbstractionState) {
        NondeterminismAbstractionState other = (NondeterminismAbstractionState) pObj;
        return nondetVariablesPreAbstraction.equals(other.nondetVariablesPreAbstraction);
      }
      return super.equals(pObj);
    }

    @Override
    public String toString() {
      return "Abstraction of nondeterministic variables " + nondetVariablesPreAbstraction;
    }

    @Override
    public Set<String> getBlockNondetVariables() {
      return nondetVariablesPreAbstraction;
    }
  }
}
