// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_synchronization;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class ThreadSynchronizationVariables {

  public final ImmutableMap<CIdExpression, CondSignaled> condSignaled;

  /** The map of {@code pthread_mutex_t} objects to their {@code {mutex}_LOCKED} variables. */
  public final ImmutableMap<CIdExpression, MutexLocked> locked;

  /**
   * The map of {@link MPORThread}s to their {@code sync} flag that indicates whether a thread is at
   * a location that synchronizes threads, e.g. {@code pthread_join}.
   */
  public final ImmutableMap<MPORThread, CIdExpression> sync;

  ThreadSynchronizationVariables(
      ImmutableMap<CIdExpression, CondSignaled> pCondSignaled,
      ImmutableMap<CIdExpression, MutexLocked> pLocked,
      ImmutableMap<MPORThread, CIdExpression> pSync) {

    locked = pLocked;
    condSignaled = pCondSignaled;
    sync = pSync;
  }

  /** Returns all declarations of the thread synchronization variables. */
  public ImmutableList<CSimpleDeclaration> getDeclarations(MPOROptions pOptions) {
    ImmutableList.Builder<CSimpleDeclaration> rDeclarations = ImmutableList.builder();
    for (CondSignaled condSignaledVariable : condSignaled.values()) {
      rDeclarations.add(condSignaledVariable.idExpression.getDeclaration());
    }
    for (MutexLocked mutexLockedVariable : locked.values()) {
      rDeclarations.add(mutexLockedVariable.idExpression.getDeclaration());
    }
    if (pOptions.kIgnoreZeroReduction) {
      for (CIdExpression syncVariable : sync.values()) {
        rDeclarations.add(syncVariable.getDeclaration());
      }
    }
    return rDeclarations.build();
  }
}
