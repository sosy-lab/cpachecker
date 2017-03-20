/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.usage;

import com.google.common.base.Preconditions;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.lock.LockState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;


public class UsageInfo implements Comparable<UsageInfo> {

  public static enum Access {
    WRITE,
    READ;
  }

  private final static UsageInfo UNSUPPORTED_USAGE = new UsageInfo();

  private final LineInfo line;
  private final Access accessType;
  private AbstractState keyState;
  private List<CFAEdge> path;
  private final SingleIdentifier id;
  //Can not be immutable due to reduce/expand - lock states are modified (may be smth else)
  private final Map<Class<? extends CompatibleState>, CompatibleState> compatibleStates = new LinkedHashMap<>();
  private boolean isLooped;
  private boolean isReachable;

  private UsageInfo() {
    //Only for unsupported usage
    line = null;
    accessType = Access.WRITE;
    keyState = null;
    isLooped = false;
    isReachable = false;
    id = null;
  }

  private UsageInfo(@Nonnull Access atype, @Nonnull LineInfo l, SingleIdentifier ident) {
    line = l;
    accessType = atype;
    keyState = null;
    isLooped = false;
    isReachable = true;
    id = ident;
  }

  public static UsageInfo createUsageInfo(@Nonnull Access atype,  int l,
      @Nonnull UsageState state, AbstractIdentifier ident) {
    if (ident instanceof SingleIdentifier) {
      UsageInfo result = new UsageInfo(atype, new LineInfo(l, AbstractStates.extractLocation(state)), (SingleIdentifier)ident);
      AbstractStates.asIterable(state)
        .filter(CompatibleState.class)
        .forEach(s -> result.compatibleStates.put(s.getClass(), s.prepareToStore()));
      return result;
    } else {
      return UNSUPPORTED_USAGE;
    }
  }

  public CompatibleState getState(Class<? extends CompatibleState> pClass) {
    return compatibleStates.get(pClass);
  }

  public List<CompatibleState> getAllCompatibleStates() {
    return new LinkedList<>(compatibleStates.values());
  }

  public @Nonnull Access getAccess() {
    return accessType;
  }

  public @Nonnull LineInfo getLine() {
    return line;
  }

  public @Nonnull SingleIdentifier getId() {
    assert(id != null);
    return id;
  }

  public void setAsLooped() {
    isLooped = true;
  }

  public boolean isLooped() {
    return isLooped;
  }

  public boolean isSupported() {
    return this != UNSUPPORTED_USAGE;
  }

  public @Nonnull void setId(SingleIdentifier pId) {
    //Now it is set while creation
    assert id == null || id.getName().equals(pId.getName()) : "Old id " + id + ", new one - " + pId;
    //id = pId;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((accessType == null) ? 0 : accessType.hashCode());
    result = prime * result + ((line == null) ? 0 : line.hashCode());
    result = prime * result + ((compatibleStates == null) ? 0 : compatibleStates.hashCode());
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
    UsageInfo other = (UsageInfo) obj;
    if (accessType != other.accessType) {
      return false;
    }
    if (line == null) {
      if (other.line != null) {
        return false;
      }
    } else if (!line.equals(other.line)) {
      return false;
    }
    if (compatibleStates == null) {
      if (other.compatibleStates != null) {
        return false;
      }
    } else if (!compatibleStates.equals(other.compatibleStates)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString(){
    StringBuilder sb = new StringBuilder();

    if (id != null) {
      sb.append("Id ");
      sb.append(id.toString());
      sb.append(", ");
    }
    sb.append("line ");
    sb.append(line.toString());
    sb.append(" (" + accessType + ")");
    sb.append(", " + compatibleStates.get(LockState.class));

    return sb.toString();
  }

  public String getWarningMessage() {
    StringBuilder sb = new StringBuilder();

    sb.append(accessType);
    sb.append(" access to ");
    sb.append(id);
    LockState locks = (LockState) compatibleStates.get(LockState.class);
    if (locks.getSize() == 0) {
      sb.append(" without locks");
    } else {
      sb.append(" with ");
      sb.append(locks);
    }

    return sb.toString();
  }

  public void setKeyState(AbstractState state) {
    keyState = state;
  }

  public void setRefinedPath(List<CFAEdge> p) {
    keyState = null;
    path = p;
  }

  public AbstractState getKeyState() {
    return keyState;
  }

  public List<CFAEdge> getPath() {
    //assert path != null;
    return path;
  }

  @Override
  public int compareTo(UsageInfo pO) {
    int result;

    if (this == pO) {
      return 0;
    }
    Set<Class<? extends CompatibleState>> currentStateTypes = compatibleStates.keySet();
    Set<Class<? extends CompatibleState>> otherStateTypes = pO.compatibleStates.keySet();
    Preconditions.checkArgument(currentStateTypes.equals(otherStateTypes),
        "Different compatible states in usages are not supported");
    for (Class<? extends CompatibleState> pClass : currentStateTypes) {
      //May be sorted not in the convenient order: Locks last
      CompatibleState currentState = this.getState(pClass);
      if (currentState != null) {
        result = currentState.compareTo(pO.getState(pClass));
        if (result != 0) {
          //Usages without locks are more convenient to analyze
          return -result;
        }
      }
    }

    result = this.line.compareTo(pO.line);
    if (result != 0) {
      return result;
    }
    result = this.accessType.compareTo(pO.accessType);
    if (result != 0) {
      return result;
    }
    /* We can't use key states for ordering, because the treeSets can't understand,
     * that old refined usage with zero key state is the same as new one
     */
    if (this.id != null && pO.id != null) {
      //Identifiers may not be equal here:
      // if (a.b > c.b)
      // FieldIdentifiers are the same (when we add to container),
      // but full identifiers (here) are not equal
      // TODO should we distinguish them?

    }
    return 0;
  }

  public boolean isReachable() {
    return isReachable;
  }

  public void setAsUnreachable() {
    isReachable = false;
  }

  @Override
  public UsageInfo clone() {
    UsageInfo result = new UsageInfo(accessType, line, id);
    result.keyState = this.keyState;
    result.path = this.path;
    result.isLooped = this.isLooped;
    result.isReachable = this.isReachable;
    result.compatibleStates.putAll(this.compatibleStates);
    return result;
  }

  public UsageInfo expand(LockState expandedState) {
    UsageInfo result = clone();

    result.compatibleStates.put(LockState.class, expandedState);
    return result;
  }
}
