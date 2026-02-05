// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public enum SeqThreadStatementType {
  ASSUME(true, false),
  // ATOMIC_BEGIN does not synchronize threads because executing it is not conditional
  ATOMIC_BEGIN(true, false),
  ATOMIC_END(true, false),
  COND_SIGNAL(true, false),
  COND_WAIT(true, true),
  CONST_CPACHECKER_TMP(true, false),
  /** A default statement requires no specific handling of the underlying {@link CFAEdge}. */
  DEFAULT(true, false),
  /** A statement that contains only ghost code without any statement from the input program. */
  GHOST_ONLY(true, false),
  LOCAL_VARIABLE_DECLARATION_WITH_INITIALIZER(true, false),
  MUTEX_LOCK(true, true),
  MUTEX_UNLOCK(true, false),
  PARAMETER_ASSIGNMENT(true, false),
  RETURN_VALUE_ASSIGNMENT(true, false),
  RW_LOCK_RD_LOCK(true, true),
  RW_LOCK_UNLOCK(true, false),
  RW_LOCK_WR_LOCK(true, true),
  THREAD_CREATION(true, false),
  THREAD_EXIT(false, false),
  THREAD_JOIN(true, true);

  /**
   * Whether this statement type can be linked to its target statement. This is false e.g. for
   * statements that terminate a thread.
   */
  public final boolean isLinkable;

  /**
   * Whether the continuation of the thread executing this statement depends on a {@code
   * assume(...);} statement, e.g., to check whether a {@code pthread_mutex_lock} is currently
   * unlocked.
   */
  public final boolean synchronizesThreads;

  SeqThreadStatementType(boolean pIsLinkable, boolean pSynchronizesThreads) {
    isLinkable = pIsLinkable;
    synchronizesThreads = pSynchronizesThreads;
  }
}
