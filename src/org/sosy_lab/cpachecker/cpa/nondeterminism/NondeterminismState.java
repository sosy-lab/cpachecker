// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
    @SuppressWarnings("JdkObsolete")
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
      return ImmutableSet.of();
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
