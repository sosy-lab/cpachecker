// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.threading.locks;

import com.google.common.base.Preconditions;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;

public class ConditionVariable {

  private final String name;
  private final PersistentSet<String> waitingThreads;
  private final PersistentSet<String> signalledThreads;
  private final String lockId;

  public ConditionVariable(String pName) {
    this(pName, PersistentSet.of(), PersistentSet.of(), null);
  }

  private ConditionVariable(
      String pName,
      PersistentSet<String> pWaitingThreads,
      PersistentSet<String> pSignalledThreads,
      String pLockId) {
    Preconditions.checkNotNull(pName);
    Preconditions.checkNotNull(pWaitingThreads);
    Preconditions.checkNotNull(pSignalledThreads);

    name = pName;
    waitingThreads = pWaitingThreads;
    signalledThreads = pSignalledThreads;
    lockId = pLockId;
  }

  public String getName() {
    return name;
  }

  public Set<String> getWaitingThreads() {
    return waitingThreads.asSet();
  }

  public Set<String> getSignalledThreads() {
    return signalledThreads.asSet();
  }

  public String getLockId() {
    return lockId;
  }

  public ConditionVariable wait(String pThreadId, String pLockId) {
    Preconditions.checkNotNull(pThreadId);
    Preconditions.checkNotNull(pLockId);
    Preconditions.checkState(lockId == null || lockId.equals(pLockId));

    return new ConditionVariable(
        name,
        waitingThreads.addAndCopy(pThreadId),
        signalledThreads.removeAndCopy(pThreadId),
        lockId == null ? pLockId : lockId);
  }

  public ConditionVariable signal() {
    // It is not necessary to only wake specific threads, because pthread_cond_signal
    // explicitly allows spurious wakeups to happen.
    Set<String> signalled = new HashSet<>(signalledThreads);
    signalled.addAll(waitingThreads);
    return new ConditionVariable(name, PersistentSet.of(), PersistentSet.copyOf(signalled), lockId);
  }

  public ConditionVariable removeThread(String pThreadId) {
    Preconditions.checkNotNull(pThreadId);
    Preconditions.checkState(
        waitingThreads.contains(pThreadId) || signalledThreads.contains(pThreadId));

    if (waitingThreads.contains(pThreadId)) {
      // This is a spurious wakeup (without being signalled).
      boolean resetLock = waitingThreads.size() == 1 && signalledThreads.isEmpty();
      return new ConditionVariable(
          name,
          waitingThreads.removeAndCopy(pThreadId),
          signalledThreads,
          resetLock ? null : lockId);
    }

    boolean resetLock = waitingThreads.isEmpty() && signalledThreads.size() == 1;
    return new ConditionVariable(
        name, waitingThreads, signalledThreads.removeAndCopy(pThreadId), resetLock ? null : lockId);
  }
}
