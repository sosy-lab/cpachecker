// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.thread_vars;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class ThreadVars {

  /** The list of {@code {thread}_active} variables, indexed by {@link MPORThread#id}. */
  public final ImmutableList<ThreadActive> active;

  /** The map of {@code pthread_mutex_t} objects to their {@code {mutex}_locked} variables. */
  public final ImmutableMap<CIdExpression, MutexLocked> locked;

  /** Each thread and thread object are mapped to their {@code {thread}_awaits_{mutex}} variable */
  public final ImmutableMap<MPORThread, ImmutableMap<CIdExpression, ThreadAwaitsMutex>> awaits;

  /** Each thread joining a thread is mapped to a {@code {thread}_joins_{threads}} variable. */
  public final ImmutableMap<MPORThread, ImmutableMap<MPORThread, ThreadJoinsThread>> joins;

  public ThreadVars(
      ImmutableList<ThreadActive> pActive,
      ImmutableMap<CIdExpression, MutexLocked> pLocked,
      ImmutableMap<MPORThread, ImmutableMap<CIdExpression, ThreadAwaitsMutex>> pAwaits,
      ImmutableMap<MPORThread, ImmutableMap<MPORThread, ThreadJoinsThread>> pJoins) {

    active = pActive;
    locked = pLocked;
    awaits = pAwaits;
    joins = pJoins;
  }

  /** Returns all CIdExpressions of the vars in the order active - locked - awaits - joins. */
  public ImmutableList<CIdExpression> getIdExpressions() {
    ImmutableList.Builder<CIdExpression> rIdExpressions = ImmutableList.builder();
    for (ThreadActive var : active) {
      rIdExpressions.add(var.idExpression);
    }
    for (MutexLocked var : locked.values()) {
      rIdExpressions.add(var.idExpression);
    }
    for (ImmutableMap<CIdExpression, ThreadAwaitsMutex> map : awaits.values()) {
      for (ThreadAwaitsMutex var : map.values()) {
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
