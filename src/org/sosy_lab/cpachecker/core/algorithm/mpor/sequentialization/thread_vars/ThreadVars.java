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
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqNameBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqInitializers.SeqInitializer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class ThreadVars {

  public final Optional<AtomicInUse> atomicInUse;

  /** The map of threads and their {@code {thread}_ACTIVE} variables. */
  public final ImmutableMap<MPORThread, ThreadActive> active;

  /** The map of {@code pthread_mutex_t} objects to their {@code {mutex}_LOCKED} variables. */
  public final ImmutableMap<CIdExpression, MutexLocked> locked;

  /**
   * Each thread and {@code pthread_mutex_t} object are mapped to their {@code
   * {thread}_LOCKS_{mutex}} variable.
   */
  public final ImmutableMap<MPORThread, ImmutableMap<CIdExpression, ThreadLocksMutex>> locks;

  /** Each thread joining a thread is mapped to a {@code {thread}_JOINS_{threads}} variable. */
  public final ImmutableMap<MPORThread, ImmutableMap<MPORThread, ThreadJoinsThread>> joins;

  /**
   * Each thread beginning an atomic section is mapped to a {@code {thread}_BEGINS_ATOMIC} variable.
   */
  public final ImmutableMap<MPORThread, ThreadBeginsAtomic> begins;

  public ThreadVars(
      ImmutableMap<MPORThread, ThreadActive> pActive,
      ImmutableMap<CIdExpression, MutexLocked> pLocked,
      ImmutableMap<MPORThread, ImmutableMap<CIdExpression, ThreadLocksMutex>> pLocks,
      ImmutableMap<MPORThread, ImmutableMap<MPORThread, ThreadJoinsThread>> pJoins,
      ImmutableMap<MPORThread, ThreadBeginsAtomic> pBegins) {

    active = pActive;
    locked = pLocked;
    locks = pLocks;
    joins = pJoins;
    begins = pBegins;
    if (begins.isEmpty()) {
      atomicInUse = Optional.empty();
    } else {
      CIdExpression var =
          SeqIdExpression.buildIntIdExpr(
              SeqNameBuilder.buildAtomicInUseName(), SeqInitializer.INT_0);
      atomicInUse = Optional.of(new AtomicInUse(var));
    }
  }

  /** Returns all CIdExpressions of the vars in the order active - locked - locks - joins. */
  public ImmutableList<CIdExpression> getIdExpressions() {
    ImmutableList.Builder<CIdExpression> rIdExpressions = ImmutableList.builder();
    for (ThreadActive var : active.values()) {
      rIdExpressions.add(var.idExpression);
    }
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
    for (ThreadBeginsAtomic var : begins.values()) {
      rIdExpressions.add(var.idExpression);
    }
    return rIdExpressions.build();
  }
}
