/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.local;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;


public class LocalState implements LatticeAbstractState<LocalState> {

  public static enum DataType {
    LOCAL,
    GLOBAL;

    @Override
    public String toString() {
      return name().toLowerCase();
    }

    public static DataType max(DataType op1, DataType op2) {
      if (op1 == GLOBAL || op2 == GLOBAL) {
        return GLOBAL;
      } else if (op1 == null || op2 == null) {
        return null;
      } else {
        return LOCAL;
      }
    }
  }
  //map from variable id to its type
  private final LocalState previousState;
  private final Map<AbstractIdentifier, DataType> DataInfo;
  private final ImmutableSet<String> alwaysLocalData;

  private LocalState(Map<AbstractIdentifier, DataType> oldMap, LocalState state, ImmutableSet<String> localData) {
    DataInfo = new HashMap<>(oldMap);
    //Strange, but 'new TreeMap<>(oldMap)' lost some values: "id -> null" appears
    previousState = state;
    alwaysLocalData = localData;
  }

  public static LocalState createInitialLocalState(Set<String> localData) {
    return new LocalState(new HashMap<>(), null, ImmutableSet.copyOf(localData));
  }

  public static LocalState createInitialLocalState(LocalState state) {
    return new LocalState(new HashMap<>(), null, state.alwaysLocalData);
  }

  public static LocalState createNextLocalState(LocalState state) {
    return new LocalState(new HashMap<>(), state, state.alwaysLocalData);
  }

  public LocalState getClonedPreviousState() {
    return previousState.clone();
  }

  @Override
  public LocalState clone() {
    return new LocalState(this.DataInfo, this.previousState, this.alwaysLocalData);
  }

  private LocalState clone(LocalState pPreviousState) {
    return new LocalState(this.DataInfo, pPreviousState, this.alwaysLocalData);
  }

  public LocalState expand(LocalState rootState) {
    return this.clone(rootState.previousState);
  }

  public LocalState reduce() {
    return this.clone(null);
  }

  private boolean checkIsAlwaysLocal(AbstractIdentifier name) {
    if (name instanceof SingleIdentifier) {
      if (alwaysLocalData.contains(((SingleIdentifier)name).getName())) {
        return true;
      }
    }
    return false;
  }

  private void putIntoDataInfo(AbstractIdentifier name, DataType type) {
    if (checkIsAlwaysLocal(name)) {
      //We put it down to be able to dump it into log
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

  private boolean checkSharednessOfComposedIds(AbstractIdentifier name) {
    Collection<AbstractIdentifier> innerIds = name.getComposedIdentifiers();
    for (AbstractIdentifier id : innerIds) {
      if (getDataInfo(id) == DataType.GLOBAL) {
        return true;
      }
    }
    return false;
  }

  public void set(AbstractIdentifier name, DataType type) {
    if (name.isGlobal()) {
      //Don't save obvious information
      return;
    }

    //Do not care about old value of the target id
    if (checkSharednessOfComposedIds(name)) {
      putIntoDataInfo(name, DataType.GLOBAL);
      return;
    }
    putIntoDataInfo(name, type);
  }

  public DataType getType(AbstractIdentifier pName) {
    DataType directResult = getDataInfo(pName);
    if (checkSharednessOfComposedIds(pName)) {
      putIntoDataInfo(pName, DataType.GLOBAL);
      return DataType.GLOBAL;
    }
    return directResult;
  }

  @Override
  public LocalState join(LocalState pState2) {
    //by definition of Merge operator we should return state2, not this!
    if (this.equals(pState2)) {
      return pState2;
    }
    LocalState joinedPreviousState = null;
    if ((this.previousState != null && pState2.previousState == null)
        || (this.previousState == null && pState2.previousState != null) ) {
      //One of them was already reduced and one not yet
      if (this.previousState == null) {
        return this;
      } else {
        return pState2;
      }
    } else if (this.previousState != null && pState2.previousState != null
        && !this.previousState.equals(pState2.previousState)) {
      //it can be, when we join states, called from different functions
      joinedPreviousState = this.previousState.join(pState2.previousState);
    } else if (this.previousState != null && pState2.previousState != null
        && this.previousState.equals(pState2.previousState)) {
      joinedPreviousState = this.previousState;
    }

    LocalState joinState = this.clone(joinedPreviousState);
    Set<AbstractIdentifier> toDelete = new HashSet<>();

    for (AbstractIdentifier name : joinState.DataInfo.keySet()) {
      if (!pState2.DataInfo.containsKey(name) && joinState.DataInfo.get(name) != DataType.GLOBAL) {
        toDelete.add(name);
      }
    }

    for (AbstractIdentifier del : toDelete) {
      joinState.DataInfo.remove(del);
    }

    for (AbstractIdentifier name : pState2.DataInfo.keySet()) {
      if (!joinState.DataInfo.containsKey(name) && pState2.DataInfo.get(name) == DataType.GLOBAL) {
        joinState.DataInfo.put(name, DataType.GLOBAL);
      } else if (joinState.DataInfo.containsKey(name)) {
        joinState.DataInfo.put(name, DataType.max(this.DataInfo.get(name), pState2.DataInfo.get(name)));
      }
    }

    return joinState;
  }

  @Override
  public boolean isLessOrEqual(LocalState pState2) {
    //LOCAL < NULL < GLOBAL
    for (AbstractIdentifier name : this.DataInfo.keySet()) {
      if (this.getDataInfo(name) == DataType.LOCAL) {
        continue;
      }
      //Here thisType can be only Global, so pState2 also should contains Global
      if (!pState2.DataInfo.containsKey(name) || pState2.getDataInfo(name) == DataType.LOCAL) {
        return false;
      }
    }
    for (AbstractIdentifier name : pState2.DataInfo.keySet()) {
      if (!this.DataInfo.containsKey(name) && pState2.getDataInfo(name) == DataType.LOCAL) {
        return false;
      }
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
    final int prime = 31;
    int result = 1;
    result = prime * result + ((DataInfo == null) ? 0 : DataInfo.hashCode());
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
    LocalState other = (LocalState) obj;
    if (DataInfo == null) {
      if (other.DataInfo != null) {
        return false;
      }
    } else if (!DataInfo.equals(other.DataInfo)) {
      return false;
    }
    return true;
  }

  public String toLog() {
    StringBuilder sb = new StringBuilder();
    for (AbstractIdentifier id : DataInfo.keySet()) {
      if (id instanceof SingleIdentifier) {
        sb.append(((SingleIdentifier)id).toLog() + ";" + getDataInfo(id) + "\n");
      }
    }

    if (sb.length() > 2) {
      sb.delete(sb.length() - 1, sb.length());
    }
    return sb.toString();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    for (AbstractIdentifier id : DataInfo.keySet()) {
      sb.append(id.toString() + " - " + getDataInfo(id) + "\n");
    }

    if (sb.length() > 2) {
      sb.delete(sb.length() - 1, sb.length());
    }
    return sb.toString();
  }
}
