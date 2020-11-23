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

public class SimpleThreadState extends ThreadState {

  protected SimpleThreadState(
      String pCurrent,
      @SuppressWarnings("unused") Map<String, ThreadStatus> Tset,
      @SuppressWarnings("unused") ImmutableMap<ThreadLabel, ThreadStatus> Rset) {
    super(pCurrent, Tset, Rset);
  }

  @Override
  public boolean isCompatibleWith(CompatibleState state) {
    // Consider compatibility as if we knows only the last state
    Preconditions.checkArgument(state instanceof SimpleThreadState);
    SimpleThreadState other = (SimpleThreadState) state;

    String otherThread = other.getCurrentThread();

    boolean res = Objects.equals(currentThread, otherThread);

    if (!res) {
      return true;
    }

    ThreadStatus currentStatus = threadSet.get(currentThread);
    ThreadStatus status = other.threadSet.get(otherThread);

    return currentStatus == ThreadStatus.SELF_PARALLEL_THREAD
        || status == ThreadStatus.SELF_PARALLEL_THREAD;
  }

  public static ThreadState emptyState() {
    return new SimpleThreadState(mainThread, ImmutableMap.of(), ImmutableMap.of());
  }

  @Override
  public ThreadState copyWith(String pCurrent, Map<String, ThreadStatus> tSet) {
    return new SimpleThreadState(pCurrent, tSet, this.removedSet);
  }
}
