/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.thread;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleState;

public class ThreadTMState extends ThreadState {

  private final String current;

  public ThreadTMState(
      Map<String, ThreadStatus> Tset,
      ImmutableMap<ThreadLabel, ThreadStatus> Rset,
      List<ThreadLabel> pOrder) {
    super(Tset, Rset, pOrder);
    if (pOrder.size() > 0) {
      String tmp = null;
      for (int i = pOrder.size() - 1; i >= 0; i--) {
        ThreadLabel l = pOrder.get(i);
        if (Tset.get(l.getVarName()) == ThreadStatus.CREATED_THREAD) {
          tmp = l.getVarName();
          break;
        }
      }
      if (tmp != null) {
        current = tmp;
      } else {
        current = "";
      }
    } else {
      current = "";
    }
  }

  @Override
  public boolean isCompatibleWith(CompatibleState state) {
    Preconditions.checkArgument(state instanceof ThreadTMState);
    ThreadTMState other = (ThreadTMState) state;

    if (this.current.equals(other.current)) {
      return threadSet.get(current) == ThreadStatus.SELF_PARALLEL_THREAD;
    }
    // Does not matter which set to iterate, anyway we need an intersection
    if ((this.threadSet.containsKey(other.current) || other.current.equals(""))
        && (other.threadSet.containsKey(this.current) || this.current.equals(""))) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    ThreadTMState other = (ThreadTMState) obj;
    return Objects.equals(current, other.current);
  }

  @Override
  public int compareTo(CompatibleState pOther) {
    int result = super.compareTo(pOther);
    ThreadTMState other = (ThreadTMState) pOther;

    if (result != 0) {
      return result;
    }
    result = this.current.compareTo(other.current);
    return result;
  }

  public ThreadTMStateWithEdge copyWithEdge(ThreadAbstractEdge pEdge) {
    return new ThreadTMStateWithEdge(threadSet, removedSet, getOrder(), pEdge);
  }

  public static ThreadState emptyState() {
    return new ThreadTMState(ImmutableMap.of(), ImmutableMap.of(), ImmutableList.of());
  }

  @Override
  public ThreadState copyWith(Map<String, ThreadStatus> tSet, List<ThreadLabel> pOrder) {
    return new ThreadTMState(tSet, this.removedSet, pOrder);
  }

  @Override
  public ThreadState prepareToStore() {
    return new ThreadTMState(this.threadSet, ImmutableMap.of(), ImmutableList.of());
  }

  @Override
  public boolean isLessOrEqual(ThreadState pOther) {
    assert (pOther instanceof ThreadTMState);
    return Objects.equals(current, ((ThreadTMState) pOther).current)
        && Objects.equals(removedSet, pOther.removedSet)
        && pOther.threadSet.entrySet().containsAll(this.threadSet.entrySet());
  }
}
