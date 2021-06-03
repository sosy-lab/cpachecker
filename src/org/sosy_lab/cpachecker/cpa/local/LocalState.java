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
import java.util.function.BiFunction;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.ConstantIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;

public class LocalState implements LatticeAbstractState<LocalState> {

  public static enum DataType {
    LOCAL,
    GLOBAL;

    @Override
    public String toString() {
      return name().toLowerCase();
    }

    public static final BiFunction<DataType, DataType, DataType> max =
        (d1, d2) -> {
          if (d1 == GLOBAL || d2 == GLOBAL) {
            return GLOBAL;
          } else if (d1 == null || d2 == null) {
            return null;
          } else {
            return LOCAL;
          }
        };
  }
  // map from variable id to its type
  protected final LocalState previousState;
  protected final Map<AbstractIdentifier, DataType> DataInfo;
  protected final ImmutableSet<String> alwaysLocalData;

  protected LocalState(
      Map<AbstractIdentifier, DataType> oldMap, LocalState state, ImmutableSet<String> localData) {
    DataInfo = new HashMap<>(oldMap);
    // Strange, but 'new TreeMap<>(oldMap)' lost some values: "id -> null" appears
    previousState = state;
    alwaysLocalData = localData;
  }

  public static LocalState createInitialLocalState(ImmutableSet<String> localData) {
    return new LocalState(new HashMap<>(), null, localData);
  }

  public LocalState createInitialLocalState() {
    return createInitialLocalState(this.alwaysLocalData);
  }

  public LocalState createNextLocalState() {
    return copy(new HashMap<>(), this);
  }

  public LocalState getClonedPreviousState() {
    return previousState.copy();
  }

  public LocalState copy() {
    return copy(this.previousState);
  }

  protected LocalState copy(LocalState pPreviousState) {
    return copy(this.DataInfo, pPreviousState);
  }

  protected LocalState copy(Map<AbstractIdentifier, DataType> oldMap, LocalState state) {
    return new LocalState(oldMap, state, this.alwaysLocalData);
  }

  public LocalState expand(LocalState rootState) {
    return copy(rootState.previousState);
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
    if (this.equals(pState2)) {
      return pState2;
    }
    LocalState joinedPreviousState = null;
    if (this.previousState != null && pState2.previousState == null) {
      // One of them was already reduced and one not yet
      return pState2;
    } else if (this.previousState == null && pState2.previousState != null) {
      return this;
    } else if (this.previousState != null
        && pState2.previousState != null
        && !this.previousState.equals(pState2.previousState)) {
      // it can be, when we join states, called from different functions
      joinedPreviousState = this.previousState.join(pState2.previousState);
    } else if (this.previousState != null
        && pState2.previousState != null
        && this.previousState.equals(pState2.previousState)) {
      joinedPreviousState = this.previousState;
    }

    LocalState joinState = this.copy(joinedPreviousState);

    Sets.union(this.DataInfo.keySet(), pState2.DataInfo.keySet())
        .forEach(
            id ->
                joinState.putIntoDataInfo(
                    id, DataType.max.apply(DataInfo.get(id), pState2.DataInfo.get(id))));

    return joinState;
  }

  @Override
  public boolean isLessOrEqual(LocalState pState2) {
    // LOCAL < NULL < GLOBAL
    if (from(this.DataInfo.keySet())
        .filter(Predicates.not(this::isLocal))
        .anyMatch(i -> !pState2.DataInfo.containsKey(i) || pState2.isLocal(i))) {
      return false;
    }

    if (from(pState2.DataInfo.keySet())
        .filter(pState2::isLocal)
        .anyMatch(i -> !this.DataInfo.containsKey(i))) {
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
    if (obj == null || !(obj instanceof LocalState)) {
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

  public static class LocalStateComplete extends LocalState {
    protected LocalStateComplete(
        Map<AbstractIdentifier, DataType> oldMap, LocalState state, ImmutableSet<String> localData) {
      super(oldMap, state, localData);
    }

    public static LocalStateComplete
        createInitialLocalStateComplete(ImmutableSet<String> localData) {
      return new LocalStateComplete(new HashMap<>(), null, localData);
    }

    @Override
    public LocalState createInitialLocalState() {
      return createInitialLocalStateComplete(this.alwaysLocalData);
    }

    @Override
    public LocalState createNextLocalState() {
      // Store DataInfo from previous function to be able to handle linked identifiers, which may
      // refer to the outer functions
      return new LocalStateComplete(this.DataInfo, this, this.alwaysLocalData);
    }

    @Override
    protected LocalStateComplete copy(Map<AbstractIdentifier, DataType> oldMap, LocalState state) {
      return new LocalStateComplete(oldMap, state, this.alwaysLocalData);
    }

  }
}
