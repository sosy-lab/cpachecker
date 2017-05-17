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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.c.CThreadOperationStatement.CThreadCreateStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CThreadOperationStatement.CThreadJoinStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.thread.ThreadLabel.LabelStatus;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleState;
import org.sosy_lab.cpachecker.cpa.usage.UsageTreeNode;
import org.sosy_lab.cpachecker.exceptions.HandleCodeException;


public class ThreadState implements AbstractState, AbstractStateWithLocation, Partitionable,
    AbstractWrapperState, UsageTreeNode {

  public static class ThreadStateBuilder {
    private List<ThreadLabel> tSet;
    private List<ThreadLabel> rSet;

    private ThreadStateBuilder(ThreadState state) {
      tSet = new LinkedList<>(state.threadSet);
      rSet = new LinkedList<>(state.removedSet);
    }

    public void handleParentThread(CThreadCreateStatement tCall) throws HandleCodeException {
      createThread(tCall, LabelStatus.PARENT_THREAD);
    }

    public void handleChildThread(CThreadCreateStatement tCall) throws HandleCodeException {
      createThread(tCall, tCall.isSelfParallel() ? LabelStatus.SELF_PARALLEL_THREAD : LabelStatus.CREATED_THREAD);
    }

    private void createThread(CThreadCreateStatement tCall, LabelStatus pParentThread) throws HandleCodeException {
      String pVarName = tCall.getVariableName();
      //Just to info
      String pFunctionName = tCall.getFunctionCallExpression().getFunctionNameExpression().toASTString();

      ThreadLabel label = new ThreadLabel(pFunctionName, pVarName, pParentThread);
      for (ThreadLabel created : tSet) {
        if (created.equals(label)) {
          throw new HandleCodeException("Can not create thread " + label.getName() + ", it was already created");
        }
      }
      if (!tSet.isEmpty() && tSet.get(tSet.size() - 1).isSelfParallel()) {
        //Can add only the same status
        label = label.toSelfParallelLabel();
      }
      tSet.add(label);
    }

    public ThreadState build(LocationState l, CallstackState c) {
      //May be called several times per one builder
      return new ThreadState(l, c, tSet, rSet);
    }

    public boolean joinThread(CThreadJoinStatement jCall) {
      String pVarName = jCall.getVariableName();
      ThreadLabel result = null;
      for (ThreadLabel tmpLabel : tSet) {
        if (tmpLabel.getVarName().equals(pVarName)) {
          assert result == null : "Found several threads with the same variable";
          assert tmpLabel.isParentThread() : "Try to self-join";
          result = tmpLabel;
        }
      }
      if (result == null) {
        return false;
      } else {
        return tSet.remove(result);
      }
    }

    public int getThreadSize() {
      //Only for statistics
      return tSet.size();
    }
  }

  private final LocationState location;
  private final CallstackState callstack;
  private final ImmutableList<ThreadLabel> threadSet;
  private final ImmutableList<ThreadLabel> removedSet;

  private ThreadState(LocationState l, CallstackState c, List<ThreadLabel> Tset, List<ThreadLabel> Rset) {
    location = l;
    callstack = c;
    threadSet = ImmutableList.copyOf(Tset);
    removedSet = Rset == null ? null : ImmutableList.copyOf(Rset);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((callstack == null) ? 0 : callstack.hashCode());
    result = prime * result + ((location == null) ? 0 : location.hashCode());
    result = prime * result + ((removedSet == null) ? 0 : removedSet.hashCode());
    result = prime * result + ((threadSet == null) ? 0 : threadSet.hashCode());
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
    ThreadState other = (ThreadState) obj;
    if (callstack == null) {
      if (other.callstack != null) {
        return false;
      }
    } else if (!callstack.equals(other.callstack)) {
      return false;
    }
    if (location == null) {
      if (other.location != null) {
        return false;
      }
    } else if (!location.equals(other.location)) {
      return false;
    }
    if (removedSet == null) {
      if (other.removedSet != null) {
        return false;
      }
    } else if (!removedSet.equals(other.removedSet)) {
      return false;
    }
    if (threadSet == null) {
      if (other.threadSet != null) {
        return false;
      }
    } else if (!threadSet.equals(other.threadSet)) {
      return false;
    }
    return true;
  }

  @Override
  public Object getPartitionKey() {
    List<Object> keys = new ArrayList<>(2);
    keys.add(location.getPartitionKey());
    keys.add(callstack.getPartitionKey());
    return keys;
  }

  @Override
  public CFANode getLocationNode() {
    return location.getLocationNode();
  }

  @Override
  public Iterable<CFANode> getLocationNodes() {
    return location.getLocationNodes();
  }

  @Override
  public Iterable<CFAEdge> getOutgoingEdges() {
    return location.getOutgoingEdges();
  }

  @Override
  public Iterable<CFAEdge> getIngoingEdges() {
    return location.getIngoingEdges();
  }

  @Override
  public Iterable<AbstractState> getWrappedStates() {
    List<AbstractState> states = new ArrayList<>(2);
    states.add(location);
    states.add(callstack);
    return states;
  }

  public List<ThreadLabel> getThreadSet() {
    return threadSet;
  }

  public List<ThreadLabel> getRemovedSet() {
    return removedSet;
  }

  public LocationState getLocationState() {
    return location;
  }

  public CallstackState getCallstackState() {
    return callstack;
  }

  @Override
  public int compareTo(CompatibleState pOther) {
    ThreadState other = (ThreadState) pOther;
    int result = 0;

    result = other.threadSet.size() - this.threadSet.size(); //decreasing queue

    if (result != 0) {
      return result;
    }

    Iterator<ThreadLabel> iterator1 = threadSet.iterator();
    Iterator<ThreadLabel> iterator2 = other.threadSet.iterator();
    //Sizes are equal
    while (iterator1.hasNext()) {
      ThreadLabel label1 = iterator1.next();
      ThreadLabel label2 = iterator2.next();
      result = label1.compareTo(label2);
      if (result != 0) {
        return result;
      }
    }
    //Use compare only for StoredThreadState
    Preconditions.checkArgument(location == null && callstack == null);
    return 0;
  }

  @Override
  public boolean isCompatibleWith(CompatibleState state) {
    Preconditions.checkArgument(state instanceof ThreadState);
    ThreadState other = (ThreadState) state;
    for (ThreadLabel label : threadSet) {
      for (ThreadLabel oLabel : other.threadSet) {
        if (label.isCompatibleWith(oLabel)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public ThreadState prepareToStore() {
    return new StoredThreadState(this);
  }

  public ThreadStateBuilder getBuilder() {
    return new ThreadStateBuilder(this);
  }

  public static ThreadState emptyState(LocationState l, CallstackState c) {
    List<ThreadLabel> emptySet = new LinkedList<>();
    return new ThreadState(l, c, emptySet, emptySet);
  }

  @Override
  public String toString() {
    return threadSet.toString();
  }

  public static class StoredThreadState extends ThreadState {
    StoredThreadState(ThreadState origin) {
      super(null, null, origin.threadSet, null);
    }
  }

  @Override
  public UsageTreeNode getTreeNode() {
    return this;
  }

  @Override
  public boolean cover(UsageTreeNode pNode) {
    return this.threadSet.containsAll(((ThreadState)pNode).threadSet);
  }

  @Override
  public boolean hasEmptyLockSet() {
    return true;
  }
}
