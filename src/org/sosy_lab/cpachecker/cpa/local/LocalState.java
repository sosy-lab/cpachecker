// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.local;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.ConstantIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;

public final class LocalState implements LatticeAbstractState<LocalState> {

  public enum DataType {
    LOCAL,
    GLOBAL;

    @Override
    public String toString() {
      return name().toLowerCase();
    }

    public static final DataType max(DataType d1, DataType d2) {
      if (d1 == GLOBAL || d2 == GLOBAL) {
        return GLOBAL;
      } else if (d1 == null || d2 == null) {
        return null;
      } else {
        return LOCAL;
      }
    }
  }
  // map from variable id to its type
  private final LocalState previousState;
  private final Map<AbstractIdentifier, DataType> DataInfo;
  private final ImmutableSet<String> alwaysLocalData;

  private LocalState(
      Map<AbstractIdentifier, DataType> oldMap, LocalState state, ImmutableSet<String> localData) {
    DataInfo = new HashMap<>(oldMap);
    // Strange, but 'new TreeMap<>(oldMap)' lost some values: "id -> null" appears
    previousState = state;
    alwaysLocalData = localData;
  }

  private LocalState(LocalState state, ImmutableSet<String> localData) {
    this(new HashMap<>(), state, localData);
  }

  public static LocalState createInitialLocalState(Set<String> localData) {
    return new LocalState(null, ImmutableSet.copyOf(localData));
  }

  public static LocalState createInitialLocalState(LocalState state) {
    return new LocalState(null, state.alwaysLocalData);
  }

  public static LocalState createNextLocalState(LocalState state) {
    return new LocalState(state, state.alwaysLocalData);
  }

  public LocalState getClonedPreviousState() {
    return previousState.copy();
  }

  public LocalState copy() {
    return new LocalState(DataInfo, previousState, alwaysLocalData);
  }

  private LocalState clone(LocalState pPreviousState) {
    return new LocalState(DataInfo, pPreviousState, alwaysLocalData);
  }

  public LocalState expand(LocalState rootState) {
    return this.clone(rootState.previousState);
  }

  public boolean checkIsAlwaysLocal(AbstractIdentifier name) {
    if (name instanceof SingleIdentifier) {
      return alwaysLocalData.contains(((SingleIdentifier) name).getName());
    }
    return false;
  }

  private void putIntoDataInfo(AbstractIdentifier name, DataType type) {
    if (checkIsAlwaysLocal(name)) {
      // We put it down to be able to dump it into log
      DataInfo.put(name, DataType.LOCAL);
      return;
    }
    if (type == null) {
      DataInfo.remove(name);
    } else {
      DataInfo.put(name, type);
    }
  }

  private DataType getDataInfo(AbstractIdentifier name) {
    if (checkIsAlwaysLocal(name)) {
      return DataType.LOCAL;
    }
    return DataInfo.get(name);
  }

  private boolean isLocal(AbstractIdentifier name) {
    return getDataInfo(name) == DataType.LOCAL;
  }

  private boolean isGlobal(AbstractIdentifier name) {
    return getDataInfo(name) == DataType.GLOBAL;
  }

  private boolean checkSharednessOfComposedIds(AbstractIdentifier name) {
    return checkStatusOfComposedIds(name, this::isGlobal);
  }

  private boolean checkLocalityOfComposedIds(AbstractIdentifier name) {
    return checkStatusOfComposedIds(name, this::isLocal);
  }

  private boolean checkStatusOfComposedIds(
      AbstractIdentifier name, Predicate<? super AbstractIdentifier> pred) {
    return from(name.getComposedIdentifiers()).anyMatch(pred);
  }

  public void set(AbstractIdentifier name, DataType type) {
    if (name.isGlobal() || !name.isDereferenced() || name instanceof ConstantIdentifier) {
      // Don't save obvious information
      return;
    }

    // Do not care about old value of the target id
    if (checkSharednessOfComposedIds(name)) {
      putIntoDataInfo(name, DataType.GLOBAL);
      return;
    }
    putIntoDataInfo(name, type);
  }

  public DataType getType(AbstractIdentifier pName) {
    DataType directResult = getDataInfo(pName);
    if (checkSharednessOfComposedIds(pName)) {
      // putIntoDataInfo(pName, DataType.GLOBAL);
      return DataType.GLOBAL;
    }
    if (directResult == null && checkLocalityOfComposedIds(pName)) {
      return DataType.LOCAL;
    }
    return directResult;
  }

  @Override
  public LocalState join(LocalState pState2) {
    // by definition of Merge operator we should return state2, not this!
    if (equals(pState2)) {
      return pState2;
    }
    LocalState joinedPreviousState = null;
    if (previousState != null && pState2.previousState == null) {
      // One of them was already reduced and one not yet
      return pState2;
    } else if (previousState == null && pState2.previousState != null) {
      return this;
    } else if (previousState != null
        && pState2.previousState != null
        && !previousState.equals(pState2.previousState)) {
      // it can be, when we join states, called from different functions
      joinedPreviousState = previousState.join(pState2.previousState);
    } else if (previousState != null
        && pState2.previousState != null
        && previousState.equals(pState2.previousState)) {
      joinedPreviousState = previousState;
    }

    LocalState joinState = this.clone(joinedPreviousState);

    Sets.union(DataInfo.keySet(), pState2.DataInfo.keySet())
        .forEach(
            id ->
                joinState.putIntoDataInfo(
                    id, DataType.max(DataInfo.get(id), pState2.DataInfo.get(id))));

    return joinState;
  }

  @Override
  public boolean isLessOrEqual(LocalState pState2) {
    // LOCAL < NULL < GLOBAL
    if (from(DataInfo.keySet())
        .filter(Predicates.not(this::isLocal))
        .anyMatch(i -> !pState2.DataInfo.containsKey(i) || pState2.isLocal(i))) {
      return false;
    }

    if (from(pState2.DataInfo.keySet())
        .filter(pState2::isLocal)
        .anyMatch(i -> !DataInfo.containsKey(i))) {
      return false;
    }
    /*for (AbstractIdentifier name : this.DataInfo.keySet()) {
      if (this.getType(name) != pState2.getType(name)) {
        return false;
      }
    }*/
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(DataInfo);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    LocalState other = (LocalState) obj;
    return Objects.equals(DataInfo, other.DataInfo);
  }

  public String toLog() {
    return from(DataInfo.keySet())
        .filter(SingleIdentifier.class)
        .transform(id -> id.toLog() + ";" + getDataInfo(id))
        .join(Joiner.on("\n"));
  }

  @Override
  public String toString() {
    return from(DataInfo.keySet())
        .transform(id -> id + " - " + getDataInfo(id))
        .join(Joiner.on("\n"));
  }
}
