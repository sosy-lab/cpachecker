// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.mutex;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.mutex.MutexLock.MutexLockType;

public class MutexStateTest {

  @Test
  public void twoReadersOnSameRwlockDoNotCrash() {
    MutexLock read = new MutexLock("rwlock", MutexLockType.READ);

    MutexState state = MutexState.EMPTY.withInit("rwlock");
    state = state.withLock(read, 1);
    // A 2nd concurrent reader used to throw IllegalArgumentException("Multiple entries with
    // same key") because withLock's builder put()'d the already-putAll()'d key again.
    state = state.withLock(read, 2);

    assertThat(state.getHolders(read)).containsExactly(1, 2);
  }

  @Test
  public void unlockingOneReaderKeepsTheOtherLocked() {
    MutexLock read = new MutexLock("rwlock", MutexLockType.READ);

    MutexState state = MutexState.EMPTY.withInit("rwlock");
    state = state.withLock(read, 1);
    state = state.withLock(read, 2);
    state = state.withUnlock(read, 1);

    assertThat(state.getHolders(read)).containsExactly(2);
    assertThat(state.isMutexBlockedFor(read, 2)).isFalse();
    assertThat(state.isMutexBlockedFor(read, 3)).isFalse();
    assertThat(state.isMutexBlockedFor(new MutexLock("rwlock", MutexLockType.WRITE), 3)).isTrue();
  }

  @Test
  public void unlockingLastReaderRemovesTheEntry() {
    MutexLock read = new MutexLock("rwlock", MutexLockType.READ);

    MutexState state = MutexState.EMPTY.withInit("rwlock");
    state = state.withLock(read, 1);
    state = state.withUnlock(read, 1);

    assertThat(state.isLocked(read)).isFalse();
  }
}
