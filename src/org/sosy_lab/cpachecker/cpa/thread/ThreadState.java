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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.c.CThreadOperationStatement.CThreadCreateStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CThreadOperationStatement.CThreadJoinStatement;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleNode;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleState;
import org.sosy_lab.cpachecker.exceptions.HandleCodeException;

public class ThreadState implements LatticeAbstractState<ThreadState>, CompatibleNode {

  public enum ThreadStatus {
    PARENT_THREAD,
    CREATED_THREAD,
    SELF_PARALLEL_THREAD;
  }

  public class ThreadStateBuilder {
    private final Map<String, ThreadStatus> tSet;
    private final List<ThreadLabel> bOrder;
    private boolean changed = false;

    private ThreadStateBuilder(ThreadState state) {
      tSet = new HashMap<>(state.threadSet);
      bOrder = new ArrayList<>(state.order);
    }

    public void handleParentThread(CThreadCreateStatement tCall) throws HandleCodeException {
      createThread(tCall, ThreadStatus.PARENT_THREAD);
    }

    public void handleChildThread(CThreadCreateStatement tCall) throws HandleCodeException {
      createThread(
          tCall,
          tCall.isSelfParallel() ? ThreadStatus.SELF_PARALLEL_THREAD : ThreadStatus.CREATED_THREAD);
    }

    private void createThread(CThreadCreateStatement tCall, ThreadStatus pParentThread)
        throws HandleCodeException {
      final String pVarName = tCall.getVariableName();
      //Just to info
      final String pFunctionName = tCall.getFunctionCallExpression().getFunctionNameExpression().toASTString();

      if (tSet.containsKey(pVarName)) {
        throw new HandleCodeException(
            "Can not create thread " + pFunctionName + ", it was already created");
      }

      ThreadStatus status = pParentThread;
      if (!tSet.isEmpty()) {
        ThreadLabel last = bOrder.get(bOrder.size() - 1);
        if (tSet.get(last.getVarName()) == ThreadStatus.SELF_PARALLEL_THREAD) {
          // Can add only the same status
          status = ThreadStatus.SELF_PARALLEL_THREAD;
        }
      }
      ThreadLabel label = new ThreadLabel(pFunctionName, pVarName);
      tSet.put(pVarName, status);
      bOrder.add(label);
      changed = true;
    }

    public ThreadState build() {
      if (changed) {
        return new ThreadState(tSet, removedSet, bOrder);
      } else {
        return ThreadState.this;
      }
    }

    public boolean joinThread(CThreadJoinStatement jCall) {
      // If we found several labels for different functions
      // it means, that there are several thread created for one thread variable.
      // Not a good situation, but it is not forbidden, so join the last assigned thread
      Optional<ThreadLabel> result =
          from(bOrder).filter(l -> l.getVarName().equals(jCall.getVariableName())).last();
      // Do not self-join
      if (result.isPresent()) {
        ThreadLabel toRemove = result.get();
        String var = toRemove.getVarName();
        if (tSet.containsKey(var) && tSet.get(var) != ThreadStatus.CREATED_THREAD) {
          tSet.remove(var);
          bOrder.remove(toRemove);
        }
        changed = true;
      }
      return false;
    }

    public int getThreadSize() {
      //Only for statistics
      return tSet.size();
    }
  }

  private final ImmutableMap<String, ThreadStatus> threadSet;
  // The removedSet is useless now, but it will be used in future in more complicated cases
  // Do not remove it now
  private final ImmutableMap<ThreadLabel, ThreadStatus> removedSet;
  private final List<ThreadLabel> order;

  private ThreadState(
      Map<String, ThreadStatus> Tset,
      ImmutableMap<ThreadLabel, ThreadStatus> Rset,
      List<ThreadLabel> pOrder) {
    threadSet = ImmutableMap.copyOf(Tset);
    removedSet = Rset;
    order = ImmutableList.copyOf(pOrder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(removedSet, threadSet);
  }

  @Override
  public boolean equals(Object obj) {
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
    int result = 0;
    int size = this.order.size();

    result = other.order.size() - size; // decreasing queue

    if (result != 0) {
      return result;
    }

    //Sizes are equal
    for (int i = 0; i < size; i++) {
      result = this.order.get(i).compareTo(other.order.get(i));
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
    return new ThreadState(this.threadSet, ImmutableMap.of(), Collections.emptyList());
  }

  public ThreadStateBuilder getBuilder() {
    return new ThreadStateBuilder(this);
  }

  public static ThreadState emptyState() {
    return new ThreadState(Collections.emptyMap(), ImmutableMap.of(), Collections.emptyList());
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
    return Objects.equals(removedSet, pOther.removedSet)
        && pOther.threadSet.entrySet().containsAll(threadSet.entrySet());
  }
}
