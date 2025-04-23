// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.thread;

import com.google.common.collect.ImmutableMultimap;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;

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

  public final Optional<ThreadEdge> startRoutineCall;

  /** The set of context-sensitive local variable declarations of this thread. */
  public final ImmutableMultimap<CVariableDeclaration, Optional<ThreadEdge>> localVariables;

  /** The subset of the original CFA executed by the thread. */
  public final ThreadCFA cfa;

  protected MPORThread(
      int pId,
      Optional<CIdExpression> pThreadObject,
      CFunctionType pStartRoutine,
      Optional<ThreadEdge> pStartRoutineCall,
      ImmutableMultimap<CVariableDeclaration, Optional<ThreadEdge>> pLocalVariables,
      ThreadCFA pCfa) {

    id = pId;
    threadObject = pThreadObject;
    startRoutine = pStartRoutine;
    startRoutineCall = pStartRoutineCall;
    localVariables = pLocalVariables;
    cfa = pCfa;
  }

  public boolean isMain() {
    return threadObject.isEmpty();
  }
}
