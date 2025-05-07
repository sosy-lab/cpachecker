// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread_simulation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class ThreadSimulationVariables {

  /** The map of {@code pthread_mutex_t} objects to their {@code {mutex}_LOCKED} variables. */
  public final ImmutableMap<CIdExpression, MutexLocked> locked;

  // TODO rename to requests, otherwise the naming scheme is confusing
  /**
   * Each thread and {@code pthread_mutex_t} object are mapped to their {@code
   * {thread}_LOCKS_{mutex}} variable.
   */
  public final ImmutableMap<MPORThread, ImmutableMap<CIdExpression, ThreadLocksMutex>> locks;

  /** Each thread joining a thread is mapped to a {@code {thread}_JOINS_{threads}} variable. */
  public final ImmutableMap<MPORThread, ImmutableMap<MPORThread, ThreadJoinsThread>> joins;

  public ThreadSimulationVariables(
      ImmutableMap<CIdExpression, MutexLocked> pLocked,
      ImmutableMap<MPORThread, ImmutableMap<CIdExpression, ThreadLocksMutex>> pLocks,
      ImmutableMap<MPORThread, ImmutableMap<MPORThread, ThreadJoinsThread>> pJoins) {

    locked = pLocked;
    locks = pLocks;
    joins = pJoins;
  }

  /** Returns all CIdExpressions of the vars in the order locked - locks - joins. */
  public ImmutableList<CIdExpression> getIdExpressions() {
    ImmutableList.Builder<CIdExpression> rIdExpressions = ImmutableList.builder();
    for (MutexLocked var : locked.values()) {
      rIdExpressions.add(var.idExpression);
    }
    for (ImmutableMap<CIdExpression, ThreadLocksMutex> map : locks.values()) {
      for (ThreadLocksMutex var : map.values()) {
        rIdExpressions.add(var.idExpression);
      }
    }
    for (ImmutableMap<MPORThread, ThreadJoinsThread> map : joins.values()) {
      for (ThreadJoinsThread var : map.values()) {
        rIdExpressions.add(var.idExpression);
      }
    }
    return rIdExpressions.build();
  }
}
