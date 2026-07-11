// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.mutex;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.mutex.MutexLock.MutexLockType;

/**
 * Tracks the state of mutexes in concurrent programs. Records <em>which thread</em> (by PID) holds
 * each lock. A thread that already holds a lock may re-lock it (no-op), which models
 * recursive/reentrant locking behavior.
 *
 * <p>Supports both POSIX pthread mutexes and C11 threading mutexes.
 */
public class MutexState implements AbstractState {

  public static final MutexState EMPTY =
      new MutexState(ImmutableSet.of(), ImmutableMap.of(), null);

  private final ImmutableSet<String> initializedMutexes;

  /**
   * Maps mutex lock to the PIDs of the threads that currently hold the lock.
   */
  private final ImmutableMap<MutexLock, ImmutableSet<Integer>> lockedMutexes;

  /**
   * The PID of the thread currently inside a {@code __VERIFIER_atomic_begin/end} block, or {@code
   * null} if no thread is executing atomically. While non-null, all other threads are blocked.
   */
  private final @Nullable Integer atomicHolder;

  private @Nullable Map<CFAEdge, Integer> edgePidMap = null;

  MutexState(
      ImmutableSet<String> pInitializedMutexes,
      ImmutableMap<MutexLock, ImmutableSet<Integer>> pLockedMutexes,
      @Nullable Integer pAtomicHolder) {
    initializedMutexes = pInitializedMutexes;
    lockedMutexes = pLockedMutexes;
    atomicHolder = pAtomicHolder;
  }

  public void addEdgePids(Map<CFAEdge, Integer> pEdgePidMap) {
    if (edgePidMap == null) {
      edgePidMap = pEdgePidMap;
    } else {
      edgePidMap.putAll(pEdgePidMap);
    }
  }

  public Integer getEdgePid(CFAEdge edge) {
    return edgePidMap != null ? edgePidMap.get(edge) : null;
  }

  public ImmutableSet<String> getInitializedMutexes() {
    return initializedMutexes;
  }

  public ImmutableMap<MutexLock, ImmutableSet<Integer>> getLockedMutexes() {
    return lockedMutexes;
  }

  /**
   * Returns {@code true} if the given mutex is currently locked (by any thread).
   */
  public boolean isLocked(MutexLock mutex) {
    return lockedMutexes.containsKey(mutex);
  }

  /**
   * Returns the PID of the thread holding the given mutex, or {@code null} if not locked.
   */
  public ImmutableSet<Integer> getHolders(MutexLock mutex) {
    return lockedMutexes.get(mutex);
  }

  /**
   * Returns the PID of the thread currently in an atomic block, or {@code null}.
   */
  public @Nullable Integer getAtomicHolder() {
    return atomicHolder;
  }

  /**
   * Returns {@code true} if there is an active atomic block held by a thread other than the
   * specified one.
   */
  public boolean isAtomicBlockedFor(int pid) {
    return atomicHolder != null && atomicHolder != pid;
  }

  /**
   * Returns {@code true} if the given mutex is currently locked by a thread other than the
   * specified one.
   */
  public boolean isMutexBlockedFor(MutexLock mutex, int pid) {
    for (var blockingMutex : mutex.getBlockingLocks()) {
      ImmutableSet<Integer> holders = lockedMutexes.get(blockingMutex);
      if (holders != null && !holders.contains(pid)) {
        return true;
      }
    }
    return false;
  }

  public boolean isInitialized(String mutex) {
    return initializedMutexes.contains(mutex);
  }

  /**
   * Returns a new state with the given mutex marked as initialized and unlocked.
   */
  public MutexState withInit(String mutex) {
    return new MutexState(
        ImmutableSet.<String>builder().addAll(initializedMutexes).add(mutex).build(),
        lockedMutexes,
        atomicHolder);
  }

  /**
   * Returns a new state with the given mutex locked by the specified thread. If the mutex is
   * already held by the same thread, this is a no-op (reentrant lock).
   *
   * @throws IllegalStateException if the mutex is locked by a different thread
   */
  public MutexState withLock(MutexLock mutex, int holderPid) {
    if (isMutexBlockedFor(mutex, holderPid)) {
      return null;
    }

    ImmutableSet<Integer> currentHolders = lockedMutexes.get(mutex);
    ImmutableSet<Integer> updatedHolders;
    if (currentHolders != null) {
      if (currentHolders.contains(holderPid)) {
        return this; // re-lock by same thread: no-op
      }
      updatedHolders =
          ImmutableSet.<Integer>builder().addAll(currentHolders).add(holderPid).build();
    } else {
      updatedHolders = ImmutableSet.of(holderPid);
    }

    return new MutexState(
        initializedMutexes,
        ImmutableMap.<MutexLock, ImmutableSet<Integer>>builder()
            .putAll(lockedMutexes)
            .put(mutex, updatedHolders)
            .buildKeepingLast(),
        atomicHolder);
  }

  /**
   * Returns a new state with the given mutex marked as unlocked.
   */
  public MutexState withUnlock(MutexLock mutex, int holderPid) {
    if (!lockedMutexes.containsKey(mutex)) {
      return this;
    }
    ImmutableMap.Builder<MutexLock, ImmutableSet<Integer>> builder = ImmutableMap.builder();
    for (var entry : lockedMutexes.entrySet()) {
      if (entry.getKey().equals(mutex)
          || (entry.getKey().handle().equals(mutex.handle()) && mutex.type() == MutexLockType.BOTH)) {
        ImmutableSet<Integer> holders = entry.getValue();
        if (!holders.contains(holderPid)) {
          return this;
        }
        if (holders.size() == 1) {
          continue; // no more holders, remove entry
        }
        ImmutableSet.Builder<Integer> updatedHoldersBuilder = ImmutableSet.builder();
        for (int pid : holders) {
          if (pid != holderPid) {
            updatedHoldersBuilder.add(pid);
          }
        }
        ImmutableSet<Integer> updatedHolders = updatedHoldersBuilder.build();
        builder.put(entry.getKey(), updatedHolders);
      } else {
        builder.put(entry);
      }
    }
    return new MutexState(initializedMutexes, builder.buildKeepingLast(), atomicHolder);
  }

  /**
   * Returns a new state with the given mutex removed (destroyed).
   */
  public MutexState withDestroy(String mutex) {
    ImmutableSet.Builder<String> initBuilder = ImmutableSet.builder();
    for (String m : initializedMutexes) {
      if (!m.equals(mutex)) {
        initBuilder.add(m);
      }
    }
    ImmutableMap.Builder<MutexLock, ImmutableSet<Integer>> lockBuilder = ImmutableMap.builder();
    for (var entry : lockedMutexes.entrySet()) {
      if (!entry.getKey().handle().equals(mutex)) {
        lockBuilder.put(entry);
      }
    }
    return new MutexState(initBuilder.build(), lockBuilder.build(), atomicHolder);
  }

  /**
   * Returns a new state with the specified thread entering an atomic block.
   */
  public MutexState withAtomicBegin(int pid) {
    if (atomicHolder != null && atomicHolder != pid) {
      throw new IllegalStateException(
          "Thread %d cannot enter atomic block: thread %d already holds it"
              .formatted(pid, atomicHolder));
    }
    return new MutexState(initializedMutexes, lockedMutexes, pid);
  }

  /**
   * Returns a new state with the atomic block released.
   */
  public MutexState withAtomicEnd() {
    return new MutexState(initializedMutexes, lockedMutexes, null);
  }

  public MutexState update(CFAEdge edge, int pid) {
    // Handle __VERIFIER_atomic_begin / __VERIFIER_atomic_end (no parameters needed)
    if (MutexFunctions.isAtomicBeginCall(edge)) {
      if (isAtomicBlockedFor(pid)) {
        // Another thread already holds the atomic block — should not happen (POR filters it)
        return null;
      }
      return withAtomicBegin(pid);
    }

    if (MutexFunctions.isAtomicEndCall(edge)) {
      return withAtomicEnd();
    }

    MutexLock mutexToLock = MutexFunctions.getLockMutex(edge);
    if (mutexToLock != null) {
      MutexState updatedState = withLock(mutexToLock, pid);
      if (updatedState == null) {
        // Blocked: another thread holds the mutex — bottom (no successor)
        return null;
      }
      return updatedState;
    }

    MutexLock mutexToUnlock = MutexFunctions.getUnlockMutex(edge);
    if (mutexToUnlock != null) {
      return withUnlock(mutexToUnlock, pid);
    }

    if (edge instanceof AStatementEdge sEdge
        && sEdge.getStatement() instanceof AFunctionCall funcCall) {
      AExpression funcNameExpr =
          funcCall.getFunctionCallExpression().getFunctionNameExpression();
      if (funcNameExpr instanceof AIdExpression funcName) {
        String functionName = funcName.getName();

        var params = funcCall.getFunctionCallExpression().getParameterExpressions();
        if (!params.isEmpty()) {
          String mutexName = MutexFunctions.extractMutexName(params.getFirst());
          if (mutexName != null) {
            if (MutexFunctions.isInitFunction(functionName)) {
              return withInit(mutexName);
            }

            if (MutexFunctions.isDestroyFunction(functionName)) {
              return withDestroy(mutexName);
            }
          }
        }
      }
    }

    return this;
  }

  @Override
  public String toString() {
    String atomicStr = atomicHolder != null ? ", atomic=T" + atomicHolder : "";
    return "mutexes: init=%s, locked=%s%s".formatted(initializedMutexes, lockedMutexes, atomicStr);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MutexState other)) {
      return false;
    }
    return Objects.equals(initializedMutexes, other.initializedMutexes)
        && Objects.equals(lockedMutexes, other.lockedMutexes)
        && Objects.equals(atomicHolder, other.atomicHolder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(initializedMutexes, lockedMutexes, atomicHolder);
  }
}
