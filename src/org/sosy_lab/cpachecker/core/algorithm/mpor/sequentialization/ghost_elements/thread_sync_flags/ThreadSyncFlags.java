// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class ThreadSyncFlags {

  private final ImmutableMap<CIdExpression, CondSignaledFlag> condSignaledFlags;

  /** The map of {@code pthread_mutex_t} objects to their {@code {mutex}_LOCKED} variables. */
  private final ImmutableMap<CIdExpression, MutexLockedFlag> mutexLockedFlags;

  private final ImmutableMap<CIdExpression, RwLockNumReadersWritersFlag> rwLockFlags;

  /**
   * The map of {@link MPORThread}s to their {@code sync} flag that indicates whether a thread is at
   * a location that synchronizes threads, e.g. {@code pthread_join}.
   */
  private final ImmutableMap<MPORThread, CIdExpression> syncFlags;

  ThreadSyncFlags(
      ImmutableMap<CIdExpression, CondSignaledFlag> pCondSignaledFlags,
      ImmutableMap<CIdExpression, MutexLockedFlag> pMutexLockedFlags,
      ImmutableMap<CIdExpression, RwLockNumReadersWritersFlag> pRwLockFlags,
      ImmutableMap<MPORThread, CIdExpression> pSyncFlags) {

    mutexLockedFlags = pMutexLockedFlags;
    condSignaledFlags = pCondSignaledFlags;
    rwLockFlags = pRwLockFlags;
    syncFlags = pSyncFlags;
  }

  /** Returns all declarations of the thread synchronization variables. */
  public ImmutableList<CSimpleDeclaration> getDeclarations(MPOROptions pOptions) {
    ImmutableList.Builder<CSimpleDeclaration> rDeclarations = ImmutableList.builder();
    for (CondSignaledFlag condSignaledFlag : condSignaledFlags.values()) {
      rDeclarations.add(condSignaledFlag.idExpression.getDeclaration());
    }
    for (MutexLockedFlag mutexLockedFlag : mutexLockedFlags.values()) {
      rDeclarations.add(mutexLockedFlag.idExpression.getDeclaration());
    }
    for (RwLockNumReadersWritersFlag rwLockFlag : rwLockFlags.values()) {
      rDeclarations.add(rwLockFlag.readersIdExpression.getDeclaration());
      rDeclarations.add(rwLockFlag.writersIdExpression.getDeclaration());
    }
    if (pOptions.kIgnoreZeroReduction) {
      for (CIdExpression syncFlag : syncFlags.values()) {
        rDeclarations.add(syncFlag.getDeclaration());
      }
    }
    return rDeclarations.build();
  }

  // Getters =======================================================================================

  public CondSignaledFlag getCondSignaledFlag(CIdExpression pIdExpression) {
    checkArgument(
        condSignaledFlags.containsKey(pIdExpression),
        "pIdExpression %s was not found in condSignaled map",
        pIdExpression);
    return condSignaledFlags.get(pIdExpression);
  }

  public MutexLockedFlag getMutexLockedFlag(CIdExpression pIdExpression) {
    checkArgument(
        mutexLockedFlags.containsKey(pIdExpression),
        "pIdExpression %s was not found in mutexLocked map",
        pIdExpression);
    return mutexLockedFlags.get(pIdExpression);
  }

  public RwLockNumReadersWritersFlag getRwLockFlag(CIdExpression pIdExpression) {
    checkArgument(
        rwLockFlags.containsKey(pIdExpression),
        "pIdExpression %s was not found in rwLockFlags map",
        pIdExpression);
    return Objects.requireNonNull(rwLockFlags.get(pIdExpression));
  }

  public CIdExpression getSyncFlag(MPORThread pThread) {
    checkArgument(syncFlags.containsKey(pThread), "pThread was not found in syncFlags map");
    return syncFlags.get(pThread);
  }
}
