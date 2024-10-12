// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.thread;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.total_strict_order.MPORCreate;
import org.sosy_lab.cpachecker.core.algorithm.mpor.total_strict_order.MPORJoin;
import org.sosy_lab.cpachecker.core.algorithm.mpor.total_strict_order.MPORMutex;

/**
 * An object for a thread containing an identifier (threadObject) and entry / exit Nodes of the
 * threads to identify which parts of a CFA are executed by the thread.
 */
public class MPORThread {

  public final int id;

  /** The pthread_t object. Set to empty for the main thread. */
  public final Optional<CIdExpression> threadObject;

  /** The {@link CFunctionType} of the startRoutine (pthreads) or main function (main thread). */
  public final CFunctionType startRoutine;

  /** The set of local variable declarations of this thread, used to identify variables. */
  public final ImmutableSet<CVariableDeclaration> localVars;

  public final ImmutableSet<MPORCreate> creates;

  public final ImmutableSet<MPORMutex> mutexes;

  public final ImmutableSet<MPORJoin> joins;

  /** The subset of the original CFA executed by the thread. */
  public final ThreadCFA cfa;

  protected MPORThread(
      int pId,
      CFunctionType pStartRoutine,
      Optional<CIdExpression> pThreadObject,
      ImmutableSet<CVariableDeclaration> pLocalVars,
      ImmutableSet<MPORCreate> pCreates,
      ImmutableSet<MPORMutex> pMutexes,
      ImmutableSet<MPORJoin> pJoins,
      ThreadCFA pCfa) {
    id = pId;
    startRoutine = pStartRoutine;
    threadObject = pThreadObject;
    localVars = pLocalVars;
    creates = pCreates;
    mutexes = pMutexes;
    joins = pJoins;
    cfa = pCfa;
  }

  public boolean isMain() {
    return threadObject.isEmpty();
  }
}
