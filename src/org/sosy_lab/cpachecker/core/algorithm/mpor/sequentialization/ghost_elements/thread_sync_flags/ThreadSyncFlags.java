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
 * All ghost variable flags in the sequentialization used to synchronize threads.
 *
 * @param syncFlags maps {@link MPORThread}s to their {@code sync} flag that indicates whether a
 *     thread is at a location that synchronizes threads, e.g. {@code pthread_join}.
 */
public record ThreadSyncFlags(ImmutableMap<MPORThread, CIdExpression> syncFlags) {

  /** Returns all declarations of the thread synchronization variables. */
  public ImmutableList<CSimpleDeclaration> getDeclarations(MPOROptions pOptions) {
    ImmutableList.Builder<CSimpleDeclaration> rDeclarations = ImmutableList.builder();
    if (pOptions.executeCommutingThreadsFirst()) {
      for (CIdExpression syncFlag : syncFlags.values()) {
        rDeclarations.add(syncFlag.getDeclaration());
      }
    }
    return rDeclarations.build();
  }

  // Getters =======================================================================================

  public CIdExpression getSyncFlag(MPORThread pThread) {
    return Objects.requireNonNull(syncFlags.get(pThread));
  }
}
