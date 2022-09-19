// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.thread;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
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
      return !Objects.equals(getThreadSet(), ((ThreadState) state).getThreadSet());
    }

    @Override
    public ThreadState prepareToStore() {
      return new SimpleThreadState(getThreadSet(), ImmutableMap.of(), ImmutableList.of());
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
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    ThreadState other = (ThreadState) obj;
    return Objects.equals(removedSet, other.removedSet)
        && Objects.equals(threadSet, other.threadSet);
  }

  @Override
  public int compareTo(CompatibleState pOther) {
    ThreadState other = (ThreadState) pOther;
    int result = other.threadSet.size() - threadSet.size(); // decreasing queue

    if (result != 0) {
      return result;
    }

    Iterator<Entry<String, ThreadStatus>> thisIterator = threadSet.entrySet().iterator();
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
      ThreadStatus thisStatus = threadSet.get(thisLabel);
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
    return new ThreadState(threadSet, ImmutableMap.of(), ImmutableList.of());
  }

  public static ThreadState emptyState() {
    return new ThreadState(ImmutableMap.of(), ImmutableMap.of(), ImmutableList.of());
  }

  @Override
  public String toString() {
    // Info method, in difficult cases may be wrong
    return Lists.reverse(order).stream()
        .filter(l -> threadSet.getOrDefault(l.getVarName(), null) == ThreadStatus.CREATED_THREAD)
        .findFirst()
        .map(ThreadLabel::getName)
        .orElse("");
  }

  @Override
  public boolean cover(CompatibleNode pNode) {
    return ((ThreadState) pNode).isLessOrEqual(this);
  }

  @Override
  public ThreadState join(ThreadState pOther) {
    throw new UnsupportedOperationException("Join is not implemented for ThreadCPA");
  }

  @Override
  public boolean isLessOrEqual(ThreadState pOther) {
    boolean b = Objects.equals(removedSet, pOther.removedSet);
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
