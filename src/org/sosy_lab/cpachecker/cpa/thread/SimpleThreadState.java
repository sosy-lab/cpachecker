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

public class SimpleThreadState extends ThreadState {

  public SimpleThreadState(
      @SuppressWarnings("unused") Map<String, ThreadStatus> Tset,
      @SuppressWarnings("unused") ImmutableMap<ThreadLabel, ThreadStatus> Rset,
      List<ThreadLabel> pOrder) {
    super(
        ImmutableMap.of(),
        ImmutableMap.of(),
        pOrder);
  }

  @Override
  public boolean isCompatibleWith(CompatibleState state) {
    // Consider compatibility as if we knows only the last state
    Preconditions.checkArgument(state instanceof ThreadTMState);
    ThreadTMState other = (ThreadTMState) state;

    ThreadLabel currentLabel = this.getOrder().get(0);
    ThreadLabel otherLabel = other.getOrder().get(0);

    return !currentLabel.getVarName().equals(otherLabel.getVarName());
  }

  public static ThreadState emptyState() {
    return new SimpleThreadState(ImmutableMap.of(), ImmutableMap.of(), ImmutableList.of());
  }

  @Override
  public ThreadState copyWith(Map<String, ThreadStatus> tSet, List<ThreadLabel> pOrder) {
    return new SimpleThreadState(tSet, this.removedSet, pOrder);
  }

  @Override
  public ThreadState prepareToStore() {
    return new SimpleThreadState(this.threadSet, ImmutableMap.of(), ImmutableList.of());
  }

  @Override
  public boolean isLessOrEqual(ThreadState pOther) {
    assert (pOther instanceof SimpleThreadState);

    List<ThreadLabel> currentOrder = this.getOrder();
    List<ThreadLabel> otherOrder = pOther.getOrder();

    if (currentOrder.isEmpty() && otherOrder.isEmpty()) {
      return true;
    } else if (currentOrder.isEmpty() || otherOrder.isEmpty()) {
      return false;
    } else {
      ThreadLabel currentLabel = currentOrder.get(0);
      ThreadLabel otherLabel = otherOrder.get(0);

      return Objects.equals(currentLabel.getVarName(), otherLabel.getVarName());
    }
  }
}
