/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.thread;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleNode;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleState;

public class ThreadState implements LatticeAbstractState<ThreadState>, CompatibleNode {

  public enum ThreadStatus {
    PARENT_THREAD,
    CREATED_THREAD,
    SELF_PARALLEL_THREAD;
  }

  public static class SimpleThreadState extends ThreadState {

    public SimpleThreadState(
        Map<String, ThreadStatus> Tset,
        ImmutableMap<ThreadLabel, ThreadStatus> Rset,
        List<ThreadLabel> pOrder) {
      super(Tset, Rset, pOrder);
    }

    @Override
    public boolean isCompatibleWith(CompatibleState state) {
      return !Objects.equals(this.getThreadSet(), ((ThreadState) state).getThreadSet());
    }

    @Override
    public ThreadState prepareToStore() {
      return new SimpleThreadState(this.getThreadSet(), ImmutableMap.of(), ImmutableList.of());
    }

    public static ThreadState emptyState() {
      return new SimpleThreadState(ImmutableMap.of(), ImmutableMap.of(), ImmutableList.of());
    }
  }

  private final Map<String, ThreadStatus> threadSet;
  // The removedSet is useless now, but it will be used in future in more complicated cases
  // Do not remove it now
  private final ImmutableMap<ThreadLabel, ThreadStatus> removedSet;
  private final List<ThreadLabel> order;

  public ThreadState(
      Map<String, ThreadStatus> Tset,
      ImmutableMap<ThreadLabel, ThreadStatus> Rset,
      List<ThreadLabel> pOrder) {
    threadSet = Tset;
    removedSet = Rset;
    order = ImmutableList.copyOf(pOrder);
  }

  @Override
  public final int hashCode() {
    return Objects.hash(removedSet, threadSet);
  }

  @Override
  // refactoring would be better, but currently safe for the existing subclass
  @SuppressWarnings("EqualsGetClass")
  public final boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null ||
        getClass() != obj.getClass()) {
      return false;
    }
    ThreadState other = (ThreadState) obj;
    return Objects.equals(removedSet, other.removedSet)
        && Objects.equals(threadSet, other.threadSet);
  }

  @Override
  public int compareTo(CompatibleState pOther) {
    ThreadState other = (ThreadState) pOther;
    int result = other.threadSet.size() - this.threadSet.size(); // decreasing queue

    if (result != 0) {
      return result;
    }

    Iterator<Entry<String, ThreadStatus>> thisIterator = this.threadSet.entrySet().iterator();
    Iterator<Entry<String, ThreadStatus>> otherIterator = other.threadSet.entrySet().iterator();

    while (thisIterator.hasNext() && otherIterator.hasNext()) {
      Entry<String, ThreadStatus> thisEntry = thisIterator.next();
      Entry<String, ThreadStatus> otherEntry = otherIterator.next();
      String thisLabel = thisEntry.getKey();
      String otherLabel = otherEntry.getKey();
      result = thisLabel.compareTo(otherLabel);
      if (result != 0) {
        return result;
      }
      ThreadStatus thisStatus = this.threadSet.get(thisLabel);
      ThreadStatus otherStatus = other.threadSet.get(otherLabel);
      result = thisStatus.compareTo(otherStatus);
      if (result != 0) {
        return result;
      }
    }
    return 0;
  }

  @Override
  public boolean isCompatibleWith(CompatibleState state) {
    Preconditions.checkArgument(state instanceof ThreadState);
    ThreadState other = (ThreadState) state;

    // Does not matter which set to iterate, anyway we need an intersection
    for (Entry<String, ThreadStatus> entry : threadSet.entrySet()) {
      String l = entry.getKey();
      ThreadStatus s = entry.getValue();

      if (other.threadSet.containsKey(l)) {
        ThreadStatus otherL = other.threadSet.get(l);
        if ((s == ThreadStatus.SELF_PARALLEL_THREAD && otherL != ThreadStatus.CREATED_THREAD)
            || (s == ThreadStatus.PARENT_THREAD && otherL != ThreadStatus.PARENT_THREAD)
            || (s == ThreadStatus.CREATED_THREAD && otherL == ThreadStatus.PARENT_THREAD)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public ThreadState prepareToStore() {
    return new ThreadState(this.threadSet, ImmutableMap.of(), ImmutableList.of());
  }

  public static ThreadState emptyState() {
    return new ThreadState(ImmutableMap.of(), ImmutableMap.of(), ImmutableList.of());
  }

  @Override
  public String toString() {
    // Info method, in difficult cases may be wrong
    Optional<ThreadLabel> createdThread =
        from(order)
            .filter(
                l -> threadSet.getOrDefault(l.getVarName(), null) == ThreadStatus.CREATED_THREAD)
            .last();

    if (createdThread.isPresent()) {
      return createdThread.get().getName();
    } else {
      return "";
    }
  }

  @Override
  public boolean cover(CompatibleNode pNode) {
    return ((ThreadState)pNode).isLessOrEqual(this);
  }

  @Override
  public ThreadState join(ThreadState pOther) {
    throw new UnsupportedOperationException("Join is not implemented for ThreadCPA");
  }

  @Override
  public boolean isLessOrEqual(ThreadState pOther) {
    boolean b =
        Objects.equals(removedSet, pOther.removedSet);
    if (b && pOther.threadSet == threadSet) {
      return true;
    }
    b &= pOther.threadSet.entrySet().containsAll(threadSet.entrySet());
    return b;
  }

  Map<String, ThreadStatus> getThreadSet() {
    return threadSet;
  }

  ImmutableMap<ThreadLabel, ThreadStatus> getRemovedSet() {
    return removedSet;
  }

  List<ThreadLabel> getOrder() {
    return order;
  }

  int getThreadSize() {
    // Only for statistics
    return threadSet.size();
  }
}
