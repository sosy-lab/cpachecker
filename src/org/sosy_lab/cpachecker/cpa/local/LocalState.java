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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.GlobalVariableIdentifier;
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

  public LocalState(LocalState state) {
    DataInfo = new HashMap<>();
    previousState = state;
  }

  private LocalState(Map<AbstractIdentifier, DataType> oldMap, LocalState state) {
    DataInfo = new HashMap<>(oldMap);
    //Strange, but 'new TreeMap<>(oldMap)' lost some values: "id -> null" appears
    previousState = state;
  }

  public LocalState getPreviousState() {
    return previousState;
  }

  public void forceSetLocal(AbstractIdentifier name) {
    DataInfo.put(name, DataType.LOCAL);
  }

  public void set(AbstractIdentifier name, DataType type) {
    if (name instanceof GlobalVariableIdentifier) {
      //Don't save obvious information
      return;
    }
    //Check information we've already have;
    AbstractIdentifier infoId = name.containsIn(DataInfo.keySet());
    if (infoId == null) {
      //We have no information
      if (type != null) {
        DataInfo.put(name, type);
      }
      return;
    }
    DataType result;
    if (name.equals(infoId)) {
      result = type;
    } else {
      DataType lastType = DataInfo.get(infoId);
      result = DataType.max(type, lastType);
    }
    if (result == null) {
      DataInfo.remove(name);
    } else {
      DataInfo.put(name, result);
    }
  }

  public DataType getType(AbstractIdentifier pName) {
    return pName.getType(DataInfo);
  }

  @Override
  public LocalState clone() {
    return new LocalState(this.DataInfo, this.previousState);
  }

  private LocalState clone(LocalState pPreviousState) {
    return new LocalState(this.DataInfo, pPreviousState);
  }

  public LocalState expand(LocalState rootState) {
    return this.clone(rootState.previousState);
  }

  public LocalState reduce() {
    return this.clone(null);
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
      if (this.DataInfo.get(name) == DataType.LOCAL) {
        continue;
      }
      //Here thisType can be only Global, so pState2 also should contains Global
      if (!pState2.DataInfo.containsKey(name) || pState2.DataInfo.get(name) == DataType.LOCAL) {
        return false;
      }
    }
    for (AbstractIdentifier name : pState2.DataInfo.keySet()) {
      if (!this.DataInfo.containsKey(name) && pState2.DataInfo.get(name) == DataType.LOCAL) {
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
        sb.append(((SingleIdentifier)id).toLog() + ";" + DataInfo.get(id) + "\n");
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
      sb.append(id.toString() + " - " + DataInfo.get(id) + "\n");
    }

    if (sb.length() > 2) {
      sb.delete(sb.length() - 1, sb.length());
    }
    return sb.toString();
  }
}
