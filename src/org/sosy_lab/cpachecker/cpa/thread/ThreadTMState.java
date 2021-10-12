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
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Objects;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleState;

public class ThreadTMState extends ThreadState {

  protected ThreadTMState(
      String pCurrent,
      Map<String, ThreadStatus> Tset,
      ImmutableMap<ThreadLabel, ThreadStatus> Rset) {

    super(pCurrent, Tset, Rset);
  }

  @Override
  public boolean isCompatibleWith(CompatibleState state) {
    Preconditions.checkArgument(state instanceof ThreadTMState);
    ThreadTMState other = (ThreadTMState) state;

    if (this.currentThread.equals(other.currentThread)) {
      return threadSet.get(currentThread) == ThreadStatus.SELF_PARALLEL_THREAD;
    }
    // Does not matter which set to iterate, anyway we need an intersection
    if ((this.threadSet.containsKey(other.currentThread) || other.currentThread.equals(mainThread))
        && (other.threadSet.containsKey(this.currentThread)
            || this.currentThread.equals(mainThread))) {
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
    return Objects.equals(currentThread, other.currentThread);
  }

  @Override
  public int hashCode() {
    return Objects.hash(removedSet, threadSet, currentThread);
  }

  @Override
  public int compareTo(CompatibleState pOther) {
    int result = super.compareTo(pOther);
    ThreadTMState other = (ThreadTMState) pOther;

    if (result != 0) {
      return result;
    }
    result = this.currentThread.compareTo(other.currentThread);
    return result;
  }

  public ThreadTMStateWithEdge copyWithEdge(ThreadAbstractEdge pEdge) {
    return new ThreadTMStateWithEdge(currentThread, threadSet, removedSet, pEdge);
  }

  public static ThreadState emptyState() {
    return new ThreadTMState(mainThread, ImmutableMap.of(), ImmutableMap.of());
  }

  @Override
  public ThreadState copyWith(String pCurrent, Map<String, ThreadStatus> tSet) {
    return new ThreadTMState(pCurrent, tSet, this.removedSet);
  }

  @Override
  public boolean isLessOrEqual(ThreadState pOther) {
    return Objects.equals(currentThread, ((ThreadTMState) pOther).currentThread)
        && Objects.equals(removedSet, pOther.removedSet)
        && Objects.equals(threadSet, pOther.threadSet);
  }
}
