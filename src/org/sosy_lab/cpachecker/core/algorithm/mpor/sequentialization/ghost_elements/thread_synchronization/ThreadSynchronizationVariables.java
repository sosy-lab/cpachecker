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

public class ThreadSynchronizationVariables {

  /** The map of {@code pthread_mutex_t} objects to their {@code {mutex}_LOCKED} variables. */
  public final ImmutableMap<CIdExpression, MutexLocked> locked;

  public ThreadSynchronizationVariables(ImmutableMap<CIdExpression, MutexLocked> pLocked) {
    locked = pLocked;
  }

  /** Returns all CIdExpressions of the vars in the order locked - locks - joins. */
  public ImmutableList<CSimpleDeclaration> getDeclarations() {
    ImmutableList.Builder<CSimpleDeclaration> rDeclarations = ImmutableList.builder();
    for (MutexLocked mutexLockedVariable : locked.values()) {
      rDeclarations.add(mutexLockedVariable.idExpression.getDeclaration());
    }
    return rDeclarations.build();
  }
}
