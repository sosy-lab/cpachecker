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
package org.sosy_lab.cpachecker.cpa.callstack;

import java.util.Map.Entry;
import java.util.Objects;

import javax.annotation.Nullable;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;

public class MultiCallstackState implements AbstractState, Partitionable {

  static final String INITIAL_THREAD_NAME = "thread0";

  private final PersistentMap<String, CallstackState> threadContextCallstacks;

  /** It is not granted that thread is inside threadContextCallStack.
   * This attribute is nullable. This might shrink the abstract state. */
  @Nullable private final String thread;

  /**
   * changes context of state
   */
  private MultiCallstackState(String thread, PersistentMap<String, CallstackState> stacks) {
    this.thread = thread;
    this.threadContextCallstacks = stacks;
  }

  /**
   * for stacking
   */
  protected MultiCallstackState(@Nullable MultiCallstackState previousState,
      String thread, String function, CFANode callerNode) {
    assert previousState != null || thread.equals(INITIAL_THREAD_NAME); //TODO use dynamic name
    assert previousState != null || function.equals("main__0");  // creates a new context
    this.thread = thread;

    CallstackState nextState;
    final PersistentMap<String, CallstackState> stackHeads;
    if(previousState == null) {
      stackHeads = PathCopyingPersistentTreeMap.of();
      nextState = new CallstackState(null, function, callerNode);
    } else {
      stackHeads = previousState.threadContextCallstacks;
      nextState = new CallstackState(previousState.getCurrentStack(), function, callerNode);
    }
    this.threadContextCallstacks = stackHeads.putAndCopy(thread, nextState);
  }

  public CallstackState getCurrentStack() {
    return threadContextCallstacks.get(thread);
  }

  /**
   * Creates new context
   */
  public MultiCallstackState setContext(@Nullable String thread) {
    return new MultiCallstackState(thread, threadContextCallstacks);
  }

  public String getThreadName() {
    return thread;
  }

  /** TODO Note! If a new context was created and a function returns after this, then new created context must be known by the state!! */
  public MultiCallstackState getPreviousState() {
    final PersistentMap<String, CallstackState> stackHeads;
    if(getCurrentStack().getPreviousState() == null) {
      stackHeads = threadContextCallstacks.removeAndCopy(thread);
    } else {
      stackHeads = threadContextCallstacks.putAndCopy(thread, getCurrentStack().getPreviousState());
    }
    return new MultiCallstackState(thread, stackHeads);
  }

  @Override
  public Object getPartitionKey() {
    return threadContextCallstacks;
  }

  @Override
  public String toString() {
    StringBuilder rep = new StringBuilder();
    for (Entry<String, CallstackState> entry : threadContextCallstacks.entrySet()) {
      if (entry.getKey().equals(thread)) {
        rep.append("* ");
      }
      rep.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
    }
    return rep.toString();
  }

//TODO Thread shouldn't change the
  // state, because every thread has
  // it's own functions. Note except
  // of pthread_join etc. TODO Maybe
  // it affects the state because
  // function call edges will only
  // which thread will be created
  // with the right thread context

  //!isMapEqual(other.threadContextCallstacks)

  @Override
  public int hashCode() {
    return Objects.hash(thread, threadContextCallstacks);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof MultiCallstackState)) {
      return false;
    }
    MultiCallstackState other = (MultiCallstackState) obj;
    return Objects.equals(thread, other.thread)
        && Objects.equals(threadContextCallstacks, other.threadContextCallstacks);
  }
}
