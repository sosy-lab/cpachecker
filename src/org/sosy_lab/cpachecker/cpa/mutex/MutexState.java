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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.ThreadId;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.mutex.MutexLock.MutexLockType;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

/**
 * Tracks the state of mutexes in concurrent programs. Records <em>which thread</em> (by PID) holds
 * each lock. A thread that already holds a lock may re-lock it (no-op), which models
 * recursive/reentrant locking behavior.
 *
 * <p>Supports both POSIX pthread mutexes and C11 threading mutexes.
 */
public class MutexState implements AbstractState {

  public static final MutexState EMPTY = new MutexState(ImmutableSet.of(), ImmutableMap.of(), null);

  private final ImmutableSet<MutexHandle> initializedMutexes;

  /** Maps mutex lock to the PIDs of the threads that currently hold the lock. */
  private final ImmutableMap<MutexLock, ImmutableSet<ThreadId>> lockedMutexes;

  /**
   * The PID of the thread currently inside a {@code __VERIFIER_atomic_begin/end} block, or {@code
   * null} if no thread is executing atomically. While non-null, all other threads are blocked.
   */
  private final @Nullable ThreadId atomicHolder;

  /** Maps CFA edges to the PIDs of the threads that execute them. */
  private final Map<CFAEdge, ThreadId> edgePidMap = new HashMap<>();

  MutexState(
      ImmutableSet<MutexHandle> pInitializedMutexes,
      ImmutableMap<MutexLock, ImmutableSet<ThreadId>> pLockedMutexes,
      @Nullable ThreadId pAtomicHolder) {
    initializedMutexes = pInitializedMutexes;
    lockedMutexes = pLockedMutexes;
    atomicHolder = pAtomicHolder;
  }

  public void addEdgePids(Map<CFAEdge, ThreadId> pEdgePidMap) {
    edgePidMap.putAll(pEdgePidMap);
  }

  public ThreadId getEdgePid(CFAEdge edge) {
    return edgePidMap.get(edge);
  }

  public ImmutableSet<MutexHandle> getInitializedMutexes() {
    return initializedMutexes;
  }

  public ImmutableMap<MutexLock, ImmutableSet<ThreadId>> getLockedMutexes() {
    return lockedMutexes;
  }

  /** Returns {@code true} if the given mutex is currently locked (by any thread). */
  public boolean isLocked(MutexLock mutex) {
    return lockedMutexes.containsKey(mutex);
  }

  /** Returns the PID of the thread holding the given mutex, or {@code null} if not locked. */
  public ImmutableSet<ThreadId> getHolders(MutexLock mutex) {
    return lockedMutexes.get(mutex);
  }

  /** Returns the PID of the thread currently in an atomic block, if there is one. */
  public Optional<ThreadId> getAtomicHolder() {
    return Optional.ofNullable(atomicHolder);
  }

  /**
   * Returns {@code true} if there is an active atomic block held by a thread other than the
   * specified one.
   */
  public boolean isAtomicBlockedFor(ThreadId pid) {
    return atomicHolder != null && !atomicHolder.equals(pid);
  }

  /**
   * Returns {@code true} if the given mutex is currently locked by a thread other than the
   * specified one.
   */
  public boolean isMutexBlockedFor(MutexLock mutex, ThreadId pid) {
    for (var blockingMutex : mutex.getBlockingLocks()) {
      ImmutableSet<ThreadId> holders = lockedMutexes.get(blockingMutex);
      if (holders != null && !holders.contains(pid)) {
        return true;
      }
    }
    return false;
  }

  /** Returns a new state with the given mutex marked as initialized and unlocked. */
  public MutexState withInit(MutexHandle mutex) {
    return new MutexState(
        ImmutableSet.<MutexHandle>builder().addAll(initializedMutexes).add(mutex).build(),
        lockedMutexes,
        atomicHolder);
  }

  /**
   * Returns a new state with the given mutex locked by the specified thread. If the mutex is
   * already held by the same thread, this is a no-op (reentrant lock).
   *
   * @throws IllegalStateException if the mutex is locked by a different thread
   */
  public Optional<MutexState> withLock(MutexLock mutex, ThreadId holderPid) {
    if (isMutexBlockedFor(mutex, holderPid)) {
      return Optional.empty();
    }

    ImmutableSet<ThreadId> currentHolders = lockedMutexes.get(mutex);
    ImmutableSet<ThreadId> updatedHolders;
    if (currentHolders != null) {
      if (currentHolders.contains(holderPid)) {
        return Optional.of(this); // re-lock by same thread: no-op
      }
      updatedHolders =
          ImmutableSet.<ThreadId>builder().addAll(currentHolders).add(holderPid).build();
    } else {
      updatedHolders = ImmutableSet.of(holderPid);
    }

    return Optional.of(
        new MutexState(
            initializedMutexes,
            ImmutableMap.<MutexLock, ImmutableSet<ThreadId>>builder()
                .putAll(lockedMutexes)
                .put(mutex, updatedHolders)
                .buildKeepingLast(),
            atomicHolder));
  }

  /** Returns a new state with the given mutex marked as unlocked. */
  public MutexState withUnlock(MutexLock mutex, ThreadId holderPid) {
    if (!lockedMutexes.containsKey(mutex)) {
      return this;
    }
    ImmutableMap.Builder<MutexLock, ImmutableSet<ThreadId>> builder = ImmutableMap.builder();
    for (var entry : lockedMutexes.entrySet()) {
      // handle() is guaranteed non-null by MutexLock's compact constructor, so both handles
      // being compared here are always non-null: MutexFunctions never constructs a MutexLock for
      // a mutex expression it could not resolve to a canonical key, it just doesn't model that
      // edge as a mutex operation (see MutexFunctions#getMutexLockForFunctionSet).
      if (entry.getKey().equals(mutex)
          || (entry.getKey().handle().equals(mutex.handle())
              && mutex.type() == MutexLockType.BOTH)) {
        ImmutableSet<ThreadId> holders = entry.getValue();
        if (!holders.contains(holderPid)) {
          return this;
        }
        if (holders.size() == 1) {
          continue; // no more holders, remove entry
        }
        ImmutableSet.Builder<ThreadId> updatedHoldersBuilder = ImmutableSet.builder();
        for (ThreadId pid : holders) {
          if (!pid.equals(holderPid)) {
            updatedHoldersBuilder.add(pid);
          }
        }
        ImmutableSet<ThreadId> updatedHolders = updatedHoldersBuilder.build();
        builder.put(entry.getKey(), updatedHolders);
      } else {
        builder.put(entry);
      }
    }
    return new MutexState(initializedMutexes, builder.buildKeepingLast(), atomicHolder);
  }

  /** Returns a new state with the given mutex removed (destroyed). */
  public MutexState withDestroy(MutexHandle mutex) {
    ImmutableSet.Builder<MutexHandle> initBuilder = ImmutableSet.builder();
    for (MutexHandle m : initializedMutexes) {
      if (!m.equals(mutex)) {
        initBuilder.add(m);
      }
    }
    ImmutableMap.Builder<MutexLock, ImmutableSet<ThreadId>> lockBuilder = ImmutableMap.builder();
    for (var entry : lockedMutexes.entrySet()) {
      if (!entry.getKey().handle().equals(mutex)) {
        lockBuilder.put(entry);
      }
    }
    return new MutexState(initBuilder.build(), lockBuilder.buildOrThrow(), atomicHolder);
  }

  /** Returns a new state with the specified thread entering an atomic block. */
  public MutexState withAtomicBegin(ThreadId pid) {
    if (atomicHolder != null && !atomicHolder.equals(pid)) {
      throw new IllegalStateException(
          "Thread %s cannot enter atomic block: thread %s already holds it"
              .formatted(pid, atomicHolder));
    }
    return new MutexState(initializedMutexes, lockedMutexes, pid);
  }

  /** Returns a new state with the atomic block released. */
  public MutexState withAtomicEnd() {
    return new MutexState(initializedMutexes, lockedMutexes, null);
  }

  /**
   * Returns the updated mutex state or empty if the operation is blocked by a mutex.
   *
   * @throws UnsupportedCodeException if the edge locks or unlocks a mutex whose handle cannot be
   *     determined statically or was reassigned
   */
  public Optional<MutexState> update(
      CFAEdge edge, ThreadId pid, ImmutableMap<MutexHandle, MutexHandle> mutexCandidates)
      throws UnsupportedCodeException {
    // Handle __VERIFIER_atomic_begin / __VERIFIER_atomic_end (no parameters needed)
    if (MutexFunctions.isAtomicBegin(edge)) {
      if (isAtomicBlockedFor(pid)) {
        // Another thread already holds the atomic block — should not happen (POR filters it)
        return Optional.empty();
      }
      return Optional.of(withAtomicBegin(pid));
    }

    if (MutexFunctions.isAtomicEnd(edge)) {
      return Optional.of(withAtomicEnd());
    }

    Optional<MutexLock> mutexToLock = MutexFunctions.getLockMutex(edge);
    if (mutexToLock.isPresent()) {
      MutexLock aliasUpdated = updateAlias(mutexToLock.orElseThrow(), mutexCandidates, edge);
      return withLock(aliasUpdated, pid);
    }

    Optional<MutexLock> mutexToUnlock = MutexFunctions.getUnlockMutex(edge);
    if (mutexToUnlock.isPresent()) {
      MutexLock aliasUpdated = updateAlias(mutexToUnlock.orElseThrow(), mutexCandidates, edge);
      return Optional.of(withUnlock(aliasUpdated, pid));
    }

    if (edge instanceof AStatementEdge sEdge
        && sEdge.getStatement() instanceof AFunctionCall funcCall) {
      AExpression funcNameExpr = funcCall.getFunctionCallExpression().getFunctionNameExpression();
      if (funcNameExpr instanceof AIdExpression funcName) {
        String functionName = funcName.getName();

        var params = funcCall.getFunctionCallExpression().getParameterExpressions();
        if (!params.isEmpty()) {
          Optional<MutexHandle> mutexName = MutexFunctions.extractMutexName(params.getFirst());
          if (mutexName.isPresent()) {
            if (MutexFunctions.isInitFunction(functionName)) {
              return Optional.of(withInit(mutexName.orElseThrow()));
            }

            if (MutexFunctions.isDestroyFunction(functionName)) {
              return Optional.of(withDestroy(mutexName.orElseThrow()));
            }
          }
        }
      }
    }

    return Optional.of(this);
  }

  private MutexLock updateAlias(
      MutexLock lock, ImmutableMap<MutexHandle, MutexHandle> mutexCandidates, CFAEdge edge)
      throws UnsupportedCodeException {
    if (mutexCandidates == null) {
      return lock;
    }
    if (!mutexCandidates.containsKey(lock.handle())) {
      throw new UnsupportedCodeException("reassigned mutex handle " + lock.handle(), edge);
    }
    MutexHandle aliased = mutexCandidates.get(lock.handle());
    if (lock.handle().equals(aliased)) {
      return lock;
    }
    return new MutexLock(aliased, lock.type());
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
    return o instanceof MutexState other
        && Objects.equals(initializedMutexes, other.initializedMutexes)
        && Objects.equals(lockedMutexes, other.lockedMutexes)
        && Objects.equals(atomicHolder, other.atomicHolder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(initializedMutexes, lockedMutexes, atomicHolder);
  }
}
