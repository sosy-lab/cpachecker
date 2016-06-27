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
package org.sosy_lab.cpachecker.cpa.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context.AThread;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context.AThreadContainer;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;


public class ThreadState implements AbstractState {

  private final ImmutableMap<String, Thread> threads;
  private final Thread currentThread;

  public ThreadState(Collection<Thread> threads, Thread currentThread) {
    assert threads.contains(currentThread);
    ImmutableMap.Builder<String, Thread> builder = ImmutableMap.builder();
    for(Thread thread : threads) {
      builder.put(thread.getThreadName(), thread);
    }
    this.threads = builder.build();
    this.currentThread = currentThread;
  }

  public ImmutableCollection<Thread> getThreads() {
    return threads.values();
  }

  public Thread getThread(String threadName) {
    return threads.get(threadName);
  }

  public Thread getCurrentThread() {
    return currentThread;
  }

  public static AbstractState getInitialState(AThreadContainer initialThreads) {
    ImmutableSet.Builder<Thread> builder = ImmutableSet.<Thread>builder();
    Thread mainThread = null;
    for(AThread thread : initialThreads.getAllThreads()) {
      int maxProgramCounter = thread.getContextSwitchPoints().size();
      if(initialThreads.getMainThread().equals(thread)) {
        assert mainThread == null; // there can only be one main thread
        mainThread = new Thread(thread.getThreadName(), true, false, 0, maxProgramCounter);
        builder.add(mainThread);
      } else {
        builder.add(new Thread(thread.getThreadName(), false, false, 0, maxProgramCounter));
      }
    }

    return new ThreadState(builder.build(), mainThread);
  }

  @Override
  public int hashCode() {
    return Objects.hash(currentThread, threads);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ThreadState)) {
      return false;
    }
    ThreadState other = (ThreadState) obj;
    return Objects.equals(currentThread, other.currentThread)
        && Objects.equals(threads, other.threads);
  }

  @Override
  public String toString() {
    List<String> threadNames = new ArrayList<>();
    for(Thread thread : threads.values()) {
      if(currentThread.equals(thread)) {
        threadNames.add("*" + thread.toString());
      } else {
        threadNames.add(thread.toString());
      }
    }
    return "ThreadState=" + threadNames;
  }
}
