// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements;

import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public enum SeqThreadStatementType {
  ASSUME(true, false, false),
  // ATOMIC_BEGIN does not synchronize threads because executing it is not conditional
  ATOMIC_BEGIN(true, false, false),
  ATOMIC_END(true, false, false),
  COND_SIGNAL(true, false, false),
  COND_WAIT(true, true, false),
  /**
   * Represents a special CPAchecker case where a {@code const CPAchecker_TMP} variable is declared
   * and assigned inside a case clause.
   *
   * <p>A {@code const CPAchecker_TMP} is e.g. used for field references:
   *
   * <p>{@code const int __CPAchecker_TMP = q->head; q->head = (q->head) + 1; CPAchecker_TMP;}
   *
   * <p>The original code contained only one statement, but CPAchecker may transform it into 2 or 3,
   * which are treated as one atomic section in the sequentialization, i.e., inside a single {@link
   * SeqThreadStatement}.
   *
   * <p>Reasoning: given that we declare all variables outside the main function in the
   * sequentialization, a const declaration will be assigned an undeclared value e.g. {@code
   * q->head}.
   */
  CONST_CPACHECKER_TMP(true, false, true),
  CPACHECKER_TMP_WITHOUT_INITIALIZER(true, false, false),
  /** A default statement requires no specific handling of the underlying {@link CFAEdge}. */
  DEFAULT(true, false, false),
  /** A statement that contains only ghost code without any statement from the input program. */
  GHOST_ONLY(true, false, false),
  /**
   * A local variable that is declared inside a function ({@code int l = 9;}) is declared as a
   * global variable outside the {@code main()} function in the sequentialization ({@code int l;}).
   * The thread simulation inside the respective function then only initializes the local variable
   * ({@code l = 9;}).
   */
  LOCAL_VARIABLE_INITIALIZATION(true, false, false),
  MUTEX_LOCK(true, true, false),
  MUTEX_UNLOCK(true, false, false),
  PARAMETER_ASSIGNMENT(true, false, false),
  RETURN_VALUE_ASSIGNMENT(true, false, false),
  RW_LOCK_RD_LOCK(true, true, true),
  RW_LOCK_UNLOCK(true, false, true),
  RW_LOCK_WR_LOCK(true, true, false),
  THREAD_CREATION(true, false, false),
  THREAD_EXIT(false, false, false),
  THREAD_JOIN(true, true, false),
  /**
   * A {@code while (1)} loop head is transformed into {@code CFANode -> BlankEdge -> CFANode'}
   * which would be pruned from the output program. This statement type preserves the {@code
   * BlankEdge} so that the information on the loop head is not pruned.
   */
  WHILE_TRUE_LOOP_HEAD(true, false, false);

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

  /**
   * Whether this statement type contains {@link CVariableDeclaration}. The declarations are
   * generally put outside the {@code main()} function, but in some cases intermediate variables are
   * declared, e.g. for increments or decrements.
   */
  public final boolean containsVariableDeclarations;

  SeqThreadStatementType(
      boolean pIsLinkable, boolean pSynchronizesThreads, boolean pContainsVariableDeclarations) {

    isLinkable = pIsLinkable;
    synchronizesThreads = pSynchronizesThreads;
    containsVariableDeclarations = pContainsVariableDeclarations;
  }

  public boolean in(SeqThreadStatementType... pSeqThreadStatementTypes) {
    for (SeqThreadStatementType statementType : pSeqThreadStatementTypes) {
      if (statementType.equals(this)) {
        return true;
      }
    }
    return false;
  }
}
