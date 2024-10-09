// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.helper_vars;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class PthreadVars {

  // TODO keeping track of which CIdExpression represents which is difficult, its best to create
  //  PthreadT, PthreadMutexT, ThreadActive, MutexLocked, ThreadAwaitsMutex, ThreadJoiningThread
  //  classes for better overview?

  /** The list of {@code {thread}_active} variables, indexed by {@link MPORThread#id}. */
  public final ImmutableList<CIdExpression> threadActive;

  /** The map of {@code pthread_mutex_t} objects to their {@code {mutex}_locked} variables. */
  public final ImmutableMap<CIdExpression, CIdExpression> mutexLocked;

  /**
   * The map of threads to {@code pthread_mutex_t} objects they (un)lock. Each thread and object are
   * mapped to their {@code {thread}_awaits_{mutex}} variable
   */
  public final ImmutableMap<MPORThread, ImmutableMap<CIdExpression, CIdExpression>> mutexAwaits;

  /**
   * The map threads to threads they join. Each thread joining a thread is mapped to a {@code
   * {thread}_joins_{threads}} variable.
   */
  public final ImmutableMap<MPORThread, ImmutableMap<MPORThread, CIdExpression>> threadJoins;

  public PthreadVars(
      ImmutableList<CIdExpression> pThreadActiveVars,
      ImmutableMap<CIdExpression, CIdExpression> pMutexLockedVars,
      ImmutableMap<MPORThread, ImmutableMap<CIdExpression, CIdExpression>> pMutexAwaits,
      ImmutableMap<MPORThread, ImmutableMap<MPORThread, CIdExpression>> pThreadJoinsVars) {

    threadActive = pThreadActiveVars;
    mutexLocked = pMutexLockedVars;
    mutexAwaits = pMutexAwaits;
    threadJoins = pThreadJoinsVars;
  }

  /** Returns all CIdExpressions of the vars in the order active - locked - awaits - joins. */
  public ImmutableList<CIdExpression> getIdExpressions() {
    ImmutableList.Builder<CIdExpression> rIdExpressions = ImmutableList.builder();
    for (CIdExpression active : threadActive) {
      rIdExpressions.add(active);
    }
    for (CIdExpression locked : mutexLocked.values()) {
      rIdExpressions.add(locked);
    }
    for (ImmutableMap<CIdExpression, CIdExpression> map : mutexAwaits.values()) {
      for (CIdExpression awaits : map.values()) {
        rIdExpressions.add(awaits);
      }
    }
    for (ImmutableMap<MPORThread, CIdExpression> map : threadJoins.values()) {
      for (CIdExpression joins : map.values()) {
        rIdExpressions.add(joins);
      }
    }
    return rIdExpressions.build();
  }
}
