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
import java.util.Objects;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

/**
 * Tracks the state of mutexes in concurrent programs. Records <em>which thread</em> (by PID) holds
 * each lock. A thread that already holds a lock may re-lock it (no-op), which models
 * recursive/reentrant locking behavior.
 *
 * <p>Supports both POSIX pthread mutexes and C11 threading mutexes.
 */
public class MutexState implements AbstractState {

  public static final MutexState EMPTY = new MutexState(ImmutableSet.of(), ImmutableMap.of());

  private final ImmutableSet<String> initializedMutexes;

  /** Maps mutex name to the PID of the thread that currently holds the lock. */
  private final ImmutableMap<String, Integer> lockedMutexes;

  MutexState(
      ImmutableSet<String> pInitializedMutexes, ImmutableMap<String, Integer> pLockedMutexes) {
    initializedMutexes = pInitializedMutexes;
    lockedMutexes = pLockedMutexes;
  }

  public ImmutableSet<String> getInitializedMutexes() {
    return initializedMutexes;
  }

  public ImmutableMap<String, Integer> getLockedMutexes() {
    return lockedMutexes;
  }

  /** Returns {@code true} if the given mutex is currently locked (by any thread). */
  public boolean isLocked(String mutex) {
    return lockedMutexes.containsKey(mutex);
  }

  /** Returns the PID of the thread holding the given mutex, or {@code null} if not locked. */
  public Integer getHolder(String mutex) {
    return lockedMutexes.get(mutex);
  }

  /**
   * Returns {@code true} if the given mutex is currently locked by a thread other than the
   * specified one.
   */
  public boolean isLockedByOther(String mutex, int pid) {
    Integer holder = lockedMutexes.get(mutex);
    return holder != null && holder != pid;
  }

  public boolean isInitialized(String mutex) {
    return initializedMutexes.contains(mutex);
  }

  /** Returns a new state with the given mutex marked as initialized and unlocked. */
  public MutexState withInit(String mutex) {
    return new MutexState(
        ImmutableSet.<String>builder().addAll(initializedMutexes).add(mutex).build(),
        lockedMutexes);
  }

  /**
   * Returns a new state with the given mutex locked by the specified thread. If the mutex is
   * already held by the same thread, this is a no-op (reentrant lock).
   *
   * @throws IllegalStateException if the mutex is locked by a different thread
   */
  public MutexState withLock(String mutex, int holderPid) {
    Integer currentHolder = lockedMutexes.get(mutex);
    if (currentHolder != null) {
      if (currentHolder == holderPid) {
        return this; // re-lock by same thread: no-op
      }
      throw new IllegalStateException(
          "Mutex '%s' is already held by thread %d, cannot be locked by thread %d"
              .formatted(mutex, currentHolder, holderPid));
    }
    return new MutexState(
        initializedMutexes,
        ImmutableMap.<String, Integer>builder()
            .putAll(lockedMutexes)
            .put(mutex, holderPid)
            .build());
  }

  /** Returns a new state with the given mutex marked as unlocked. */
  public MutexState withUnlock(String mutex) {
    if (!lockedMutexes.containsKey(mutex)) {
      return this;
    }
    ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
    for (var entry : lockedMutexes.entrySet()) {
      if (!entry.getKey().equals(mutex)) {
        builder.put(entry);
      }
    }
    return new MutexState(initializedMutexes, builder.build());
  }

  /** Returns a new state with the given mutex removed (destroyed). */
  public MutexState withDestroy(String mutex) {
    ImmutableSet.Builder<String> initBuilder = ImmutableSet.builder();
    for (String m : initializedMutexes) {
      if (!m.equals(mutex)) {
        initBuilder.add(m);
      }
    }
    ImmutableMap.Builder<String, Integer> lockBuilder = ImmutableMap.builder();
    for (var entry : lockedMutexes.entrySet()) {
      if (!entry.getKey().equals(mutex)) {
        lockBuilder.put(entry);
      }
    }
    return new MutexState(initBuilder.build(), lockBuilder.build());
  }

  @Override
  public String toString() {
    return "mutexes: init=%s, locked=%s".formatted(initializedMutexes, lockedMutexes);
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
        && Objects.equals(lockedMutexes, other.lockedMutexes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(initializedMutexes, lockedMutexes);
  }
}
