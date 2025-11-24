// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

/**
 * All ghost variable flags in the sequentialization used to synchronize threads (= simulate pthread
 * methods).
 *
 * @param condSignaledFlags maps {@code pthread_cond_t} objects to their {@code {cond}_SIGNALED}
 *     flag.
 * @param mutexLockedFlags maps {@code pthread_mutex_t} objects to their {@code {mutex}_LOCKED}
 *     flag.
 * @param rwLockFlags maps {@code pthread_rwlock_t} objects to their {@code * {rwlock}_NUM_READERS}
 *     and {@code {rwlock}_NUM_WRITERS} flags.
 * @param syncFlags maps {@link MPORThread}s to their {@code sync} flag that indicates whether a
 *     thread is at a location that synchronizes threads, e.g. {@code pthread_join}.
 */
public record ThreadSyncFlags(
    ImmutableMap<CIdExpression, CondSignaledFlag> condSignaledFlags,
    ImmutableMap<CIdExpression, MutexLockedFlag> mutexLockedFlags,
    ImmutableMap<CIdExpression, RwLockNumReadersWritersFlag> rwLockFlags,
    ImmutableMap<MPORThread, CIdExpression> syncFlags) {

  /** Returns all declarations of the thread synchronization variables. */
  public ImmutableList<CSimpleDeclaration> getDeclarations(MPOROptions pOptions) {
    ImmutableList.Builder<CSimpleDeclaration> rDeclarations = ImmutableList.builder();
    for (CondSignaledFlag condSignaledFlag : condSignaledFlags.values()) {
      rDeclarations.add(condSignaledFlag.idExpression().getDeclaration());
    }
    for (MutexLockedFlag mutexLockedFlag : mutexLockedFlags.values()) {
      rDeclarations.add(mutexLockedFlag.idExpression().getDeclaration());
    }
    for (RwLockNumReadersWritersFlag rwLockFlag : rwLockFlags.values()) {
      rDeclarations.add(rwLockFlag.readersIdExpression().getDeclaration());
      rDeclarations.add(rwLockFlag.writersIdExpression().getDeclaration());
    }
    if (pOptions.reduceIgnoreSleep()) {
      for (CIdExpression syncFlag : syncFlags.values()) {
        rDeclarations.add(syncFlag.getDeclaration());
      }
    }
    return rDeclarations.build();
  }

  // Getters =======================================================================================

  public CondSignaledFlag getCondSignaledFlag(CIdExpression pIdExpression) {
    return Objects.requireNonNull(condSignaledFlags.get(pIdExpression));
  }

  public MutexLockedFlag getMutexLockedFlag(CIdExpression pIdExpression) {
    return Objects.requireNonNull(mutexLockedFlags.get(pIdExpression));
  }

  public RwLockNumReadersWritersFlag getRwLockFlag(CIdExpression pIdExpression) {
    return Objects.requireNonNull(rwLockFlags.get(pIdExpression));
  }

  public CIdExpression getSyncFlag(MPORThread pThread) {
    return Objects.requireNonNull(syncFlags.get(pThread));
  }
}
