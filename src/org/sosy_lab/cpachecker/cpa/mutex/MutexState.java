// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.mutex;

import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;

/**
 * Abstract state for the {@link MutexCPA} that tracks the set of initialized and currently locked
 * mutexes.
 */
public class MutexState implements AbstractState, Graphable {

  public static final MutexState EMPTY = new MutexState(ImmutableSet.of(), ImmutableSet.of());

  private final ImmutableSet<String> initializedMutexes;
  private final ImmutableSet<String> lockedMutexes;

  MutexState(ImmutableSet<String> pInitializedMutexes, ImmutableSet<String> pLockedMutexes) {
    initializedMutexes = pInitializedMutexes;
    lockedMutexes = pLockedMutexes;
  }

  public ImmutableSet<String> getInitializedMutexes() {
    return initializedMutexes;
  }

  public ImmutableSet<String> getLockedMutexes() {
    return lockedMutexes;
  }

  public boolean isLocked(String mutex) {
    return lockedMutexes.contains(mutex);
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

  /** Returns a new state with the given mutex marked as locked. */
  public MutexState withLock(String mutex) {
    return new MutexState(
        initializedMutexes,
        ImmutableSet.<String>builder().addAll(lockedMutexes).add(mutex).build());
  }

  /** Returns a new state with the given mutex marked as unlocked. */
  public MutexState withUnlock(String mutex) {
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    for (String m : lockedMutexes) {
      if (!m.equals(mutex)) {
        builder.add(m);
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
    ImmutableSet.Builder<String> lockBuilder = ImmutableSet.builder();
    for (String m : lockedMutexes) {
      if (!m.equals(mutex)) {
        lockBuilder.add(m);
      }
    }
    return new MutexState(initBuilder.build(), lockBuilder.build());
  }

  @Override
  public String toDOTLabel() {
    return "mutexes: init=%s, locked=%s".formatted(initializedMutexes, lockedMutexes);
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
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
