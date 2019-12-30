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
import static org.sosy_lab.cpachecker.cpa.thread.ThreadTransferRelation.isThreadCreateFunction;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CThreadOperationStatement.CThreadCreateStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ThreadIdProvider;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleNode;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleState;
import org.sosy_lab.cpachecker.util.Pair;

public class ThreadState
    implements LatticeAbstractState<ThreadState>, CompatibleNode, ThreadIdProvider {
  public enum ThreadStatus {
    PARENT_THREAD,
    CREATED_THREAD,
    SELF_PARALLEL_THREAD;
  }

  protected final Map<String, ThreadStatus> threadSet;
  // The removedSet is useless now, but it will be used in future in more complicated cases
  // Do not remove it now
  protected final ImmutableMap<ThreadLabel, ThreadStatus> removedSet;
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

        /*
         * In case of self-parallel we need to consider it to be parallel with any other to support
         * such strange cases: pthread_create(&t, func1); pthread_create(&t, func2);
         */

        if (s == ThreadStatus.SELF_PARALLEL_THREAD
            || otherL == ThreadStatus.SELF_PARALLEL_THREAD
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
    Optional<ThreadLabel> createdThread = getMainThread();


    if (createdThread.isPresent()) {
      return createdThread.get().getName();
    } else {
      return "main";
    }
  }

  protected Optional<ThreadLabel> getMainThread() {
    return from(order)
        .filter(l -> threadSet.getOrDefault(l.getVarName(), null) == ThreadStatus.CREATED_THREAD)
        .last();
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
    return pOther.threadSet.entrySet().containsAll(threadSet.entrySet());
  }

  public boolean hasEmptyEffect() {
    return true;
  }

  public Map<String, ThreadStatus> getThreadSet() {
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

  @Override
  public String getThreadIdForEdge(CFAEdge pEdge) {
    return this.toString();
  }

  public ThreadState copyWith(Map<String, ThreadStatus> tSet, List<ThreadLabel> pOrder) {
    return new ThreadState(tSet, this.removedSet, pOrder);
  }

  @Override
  public java.util.Optional<Pair<String, String>>
      getSpawnedThreadIdByEdge(CFAEdge pEdge, ThreadIdProvider pSuccessor) {

    if (pEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
      CFunctionCall fCall = ((CFunctionCallEdge) pEdge).getSummaryEdge().getExpression();
      if (isThreadCreateFunction(fCall)) {
        CThreadCreateStatement tCall = (CThreadCreateStatement) fCall;
        return java.util.Optional
            .of(Pair.of(tCall.getVariableName(), pEdge.getSuccessor().getFunctionName()));
      }
    }
    if (pEdge instanceof CFunctionSummaryStatementEdge) {
      CFunctionCall functionCall = ((CFunctionSummaryStatementEdge) pEdge).getFunctionCall();
      if (isThreadCreateFunction(functionCall)) {
        CThreadCreateStatement tCall = (CThreadCreateStatement) functionCall;
        return java.util.Optional.of(
            Pair.of(
                tCall.getVariableName(),
                ((CFunctionSummaryStatementEdge) pEdge).getFunctionName()));
      }
    }
    return java.util.Optional.empty();
  }
}
