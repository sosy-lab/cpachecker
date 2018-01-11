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
package org.sosy_lab.cpachecker.cpa.simplethread;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.c.CThreadOperationStatement.CThreadCreateStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CThreadOperationStatement.CThreadJoinStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;


public class SimpleThreadState implements LatticeAbstractState<SimpleThreadState>, AbstractStateWithLocation, Partitionable,
    AbstractWrapperState, Comparable<SimpleThreadState> {

  public static class SimpleThreadStateBuilder {

    private SimpleThreadLabel pThread;

    private SimpleThreadStateBuilder(SimpleThreadState state) {
      pThread = state.thread;
    }

    public void createThread(CThreadCreateStatement tCall) throws CPATransferException {
      final String pVarName = tCall.getVariableName();
      //Just to info
      final String pFunctionName = tCall.getFunctionCallExpression().getFunctionNameExpression().toASTString();

      if (pThread != null && pThread.getName().equals(pFunctionName) && pThread.getVarName().equals(pVarName)) {
        throw new CPATransferException("Can not create thread " + pFunctionName + ", it was already created");
      }

      pThread = new SimpleThreadLabel(pFunctionName, pVarName);
    }

    public SimpleThreadState build(LocationState l, CallstackState c) {
      //May be called several times per one builder
      return new SimpleThreadState(l, c, pThread);
    }

    public boolean joinThread(CThreadJoinStatement jCall) {
      /*String pVarName = jCall.getVariableName();
      SimpleThreadLabel result = null;
      for (SimpleThreadLabel tmpLabel : tSet) {
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
      }*/
      return true;
    }

    public int getThreadSize() {
      //Only for statistics
      return 1;
    }
  }

  private final LocationState location;
  private final CallstackState callstack;
  private final SimpleThreadLabel thread;

  private SimpleThreadState(LocationState l, CallstackState c, SimpleThreadLabel pThread) {
    location = l;
    callstack = c;
    thread = pThread;
  }

  public String getCurrentThreadName() {
    //Info method, in difficult cases may be wrong
    return thread.getName();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Objects.hashCode(callstack);
    result = prime * result + Objects.hashCode(location);
    result = prime * result + Objects.hashCode(thread);
    return result;
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
    SimpleThreadState other = (SimpleThreadState) obj;
    return Objects.equals(callstack, other.callstack)
        && Objects.equals(location, other.location)
        && Objects.equals(thread, other.thread);
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

  public LocationState getLocationState() {
    return location;
  }

  public CallstackState getCallstackState() {
    return callstack;
  }

  @Override
  public int compareTo(SimpleThreadState other) {
    //Use compare only for StoredThreadState
    Preconditions.checkArgument(location == null && callstack == null);

    return this.thread.compareTo(other.thread);
  }

  public boolean isCompatibleWith(SimpleThreadState other) {
    return !Objects.equals(this.thread, other.thread);
  }

  public SimpleThreadState prepareToStore() {
    return new StoredThreadState(this);
  }

  public SimpleThreadStateBuilder getBuilder() {
    return new SimpleThreadStateBuilder(this);
  }

  public static SimpleThreadState emptyState(LocationState l, CallstackState c) {
    return new SimpleThreadState(l, c, null);
  }

  @Override
  public String toString() {
    return thread.toString();
  }

  public static class StoredThreadState extends SimpleThreadState {
    StoredThreadState(SimpleThreadState origin) {
      super(null, null, origin.thread);
    }
  }

  @Override
  public SimpleThreadState join(SimpleThreadState pOther) {
    Preconditions.checkArgument(false, "Join of Thread states is not supported");
    return null;
  }

  @Override
  public boolean isLessOrEqual(SimpleThreadState pOther) throws CPAException, InterruptedException {
    return Objects.equals(thread, pOther.thread)
        && location.equals(pOther.location)
        && callstack.equals(pOther.callstack);
  }
}
