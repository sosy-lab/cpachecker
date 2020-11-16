// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.thread;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.sosy_lab.cpachecker.cpa.thread.ThreadState.ThreadStatus;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleState;
import org.sosy_lab.cpachecker.cpa.usage.storage.Delta;

public class ThreadDelta implements Delta<CompatibleState> {

  protected final Map<String, ThreadStatus> threadSet;
  // Do not support rSet now

  ThreadDelta(Map<String, ThreadStatus> tSet) {
    threadSet = tSet;
  }

  @Override
  public CompatibleState apply(CompatibleState pState) {
    if (threadSet.isEmpty()) {
      return pState;
    }
    ThreadState pOther = (ThreadState) pState;
    Map<String, ThreadStatus> newSet = new TreeMap<>(threadSet);
    Map<String, ThreadStatus> reduced = pOther.getThreadSet();
    for (Entry<String, ThreadStatus> entry : reduced.entrySet()) {
      if (newSet.containsKey(entry.getKey())) {
        throw new UnsupportedOperationException(
            "Cannot expand state with thread " + entry.getKey() + ", it is already present");
      }
      newSet.put(entry.getKey(), entry.getValue());
    }
    return pOther.copyWith(newSet);
  }

  @Override
  public boolean covers(Delta<CompatibleState> pDelta) {
    ThreadDelta pOther = (ThreadDelta) pDelta;
    // TODO contains?
    return threadSet.equals(pOther.threadSet);
  }

  @Override
  public Delta<CompatibleState> add(Delta<CompatibleState> pDelta) {
    ThreadDelta pOther = (ThreadDelta) pDelta;
    if (pOther.threadSet.isEmpty()) {
      return this;
    }
    if (threadSet.isEmpty()) {
      return pDelta;
    }
    Map<String, ThreadStatus> newSet = new TreeMap<>(threadSet);
    for (Entry<String, ThreadStatus> entry : pOther.threadSet.entrySet()) {
      if (newSet.containsKey(entry.getKey())) {
        throw new UnsupportedOperationException(
            "Cannot add a thread " + entry.getKey() + ", it is already present");
      }
      newSet.put(entry.getKey(), entry.getValue());
    }
    return new ThreadDelta(newSet);
  }

}
